package io.oneapi.admin.service;

import com.github.benmanes.caffeine.cache.Cache;
import io.oneapi.admin.config.QuerySessionProperties;
import io.oneapi.admin.exception.SessionExpiredException;
import io.oneapi.admin.exception.SessionNotFoundException;
import io.oneapi.admin.exception.TooManySessionsException;
import io.oneapi.admin.model.QuerySession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Manages query sessions with dual caching strategy.
 * - Caffeine: Fast in-memory caching with iterator support (Option 1)
 * - Redis: Distributed caching with offset-based pagination (Option 2)
 */
@Service
public class QuerySessionManager {

    private static final Logger log = LoggerFactory.getLogger(QuerySessionManager.class);

    private final QuerySessionProperties properties;
    private final Cache<String, QuerySession> caffeineCache;
    private final RedisTemplate<String, QuerySession> redisTemplate;

    public QuerySessionManager(QuerySessionProperties properties,
                               Cache<String, QuerySession> caffeineCache,
                               @Autowired(required = false) RedisTemplate<String, QuerySession> redisTemplate) {
        this.properties = properties;
        this.caffeineCache = caffeineCache;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Create a new query session.
     */
    public QuerySession createSession(String userId, Long datasourceId, String tableName,
                                      int pageSize, int ttlMinutes) {
        // Validate user session limit
        validateSessionLimit(userId);

        // Generate unique session key
        String sessionKey = UUID.randomUUID().toString();

        // Create session
        QuerySession session = new QuerySession(sessionKey, userId, datasourceId, tableName);
        session.setPageSize(pageSize);
        session.touch(ttlMinutes);

        // Use OFFSET strategy for all cache types (production-style, no iterator dependency)
        session.setUseIteratorStrategy(false);

        // Store in appropriate cache
        storeSession(session, ttlMinutes);

        log.info("Created query session: key={}, user={}, table={}, strategy=OFFSET, ttl={} min",
                sessionKey, userId, tableName, ttlMinutes);

        return session;
    }

    /**
     * Get existing session by key.
     */
    public QuerySession getSession(String sessionKey) {
        QuerySession session = retrieveSession(sessionKey);

        if (session == null) {
            throw new SessionNotFoundException("Session not found: " + sessionKey);
        }

        if (session.isExpired()) {
            removeSession(sessionKey);
            throw new SessionExpiredException("Session expired: " + sessionKey);
        }

        if (session.isClosed()) {
            removeSession(sessionKey);
            throw new SessionExpiredException("Session already closed: " + sessionKey);
        }

        return session;
    }

    /**
     * Update session after fetching data.
     */
    public void updateSession(QuerySession session, int ttlMinutes) {
        session.touch(ttlMinutes);
        storeSession(session, ttlMinutes);

        log.debug("Updated query session: key={}, offset={}, totalFetched={}, hasMore={}",
                session.getSessionKey(), session.getOffset(),
                session.getTotalFetched(), session.isHasMore());
    }

    /**
     * Remove session from cache.
     */
    public void removeSession(String sessionKey) {
        QuerySession session = retrieveSession(sessionKey);
        if (session != null) {
            session.close(); // Cleanup resources
        }

        // Remove from both caches
        caffeineCache.invalidate(sessionKey);

        if (properties.isRedisEnabled() && redisTemplate != null) {
            String redisKey = properties.getRedisKeyPrefix() + sessionKey;
            redisTemplate.delete(redisKey);
        }

        log.info("Removed query session: key={}", sessionKey);
    }

    /**
     * Close session and cleanup resources.
     */
    public void closeSession(String sessionKey) {
        QuerySession session = retrieveSession(sessionKey);
        if (session != null) {
            session.close();
            updateSession(session, 1); // Keep for 1 minute for final cleanup
        }
        removeSession(sessionKey);
    }

    /**
     * Count active sessions for a user.
     */
    public long countUserSessions(String userId) {
        // For Caffeine
        long caffeineCount = caffeineCache.asMap().values().stream()
                .filter(s -> s.getUserId().equals(userId))
                .filter(s -> !s.isExpired() && !s.isClosed())
                .count();

        // For Redis (if enabled)
        long redisCount = 0;
        if (properties.isRedisEnabled() && redisTemplate != null) {
            // This would require scanning Redis keys (expensive)
            // Better to maintain a user -> session set in Redis
            // For now, just use Caffeine count
        }

        return caffeineCount + redisCount;
    }

    /**
     * Validate user hasn't exceeded session limit.
     */
    private void validateSessionLimit(String userId) {
        long userSessions = countUserSessions(userId);

        if (userSessions >= properties.getMaxSessionsPerUser()) {
            throw new TooManySessionsException(
                    "Maximum " + properties.getMaxSessionsPerUser() +
                    " concurrent sessions allowed. Close existing sessions first.");
        }
    }

    /**
     * Store session in cache based on strategy.
     */
    private void storeSession(QuerySession session, int ttlMinutes) {
        String sessionKey = session.getSessionKey();

        QuerySessionProperties.CacheType cacheType = properties.getCacheType();

        switch (cacheType) {
            case CAFFEINE:
                // Store in Caffeine only (with iterator)
                caffeineCache.put(sessionKey, session);
                break;

            case REDIS:
                // Store in Redis only (without iterator - not serializable)
                if (redisTemplate != null) {
                    session.setIterator(null); // Remove iterator before serialization
                    String redisKey = properties.getRedisKeyPrefix() + sessionKey;
                    redisTemplate.opsForValue().set(redisKey, session,
                            Duration.ofMinutes(ttlMinutes));
                }
                break;

            case BOTH:
                // L1: Caffeine (with iterator)
                caffeineCache.put(sessionKey, session);

                // L2: Redis (without iterator)
                if (redisTemplate != null) {
                    QuerySession redisSession = cloneSessionForRedis(session);
                    String redisKey = properties.getRedisKeyPrefix() + sessionKey;
                    redisTemplate.opsForValue().set(redisKey, redisSession,
                            Duration.ofMinutes(ttlMinutes));
                }
                break;
        }
    }

    /**
     * Retrieve session from cache based on strategy.
     */
    private QuerySession retrieveSession(String sessionKey) {
        QuerySessionProperties.CacheType cacheType = properties.getCacheType();

        switch (cacheType) {
            case CAFFEINE:
                return caffeineCache.getIfPresent(sessionKey);

            case REDIS:
                if (redisTemplate != null) {
                    String redisKey = properties.getRedisKeyPrefix() + sessionKey;
                    return redisTemplate.opsForValue().get(redisKey);
                }
                return null;

            case BOTH:
                // Try L1 first (Caffeine)
                QuerySession session = caffeineCache.getIfPresent(sessionKey);
                if (session != null) {
                    return session;
                }

                // Try L2 (Redis)
                if (redisTemplate != null) {
                    String redisKey = properties.getRedisKeyPrefix() + sessionKey;
                    session = redisTemplate.opsForValue().get(redisKey);
                    if (session != null) {
                        // Promote to L1
                        caffeineCache.put(sessionKey, session);
                    }
                    return session;
                }
                return null;

            default:
                return null;
        }
    }

    /**
     * Clone session for Redis (without iterator).
     */
    private QuerySession cloneSessionForRedis(QuerySession original) {
        QuerySession clone = new QuerySession();
        clone.setSessionKey(original.getSessionKey());
        clone.setUserId(original.getUserId());
        clone.setDatasourceId(original.getDatasourceId());
        clone.setTableName(original.getTableName());
        clone.setSchema(original.getSchema());
        clone.setFilters(original.getFilters());
        clone.setOffset(original.getOffset());
        clone.setPageSize(original.getPageSize());
        clone.setCreatedAt(original.getCreatedAt());
        clone.setLastAccessedAt(original.getLastAccessedAt());
        clone.setExpiresAt(original.getExpiresAt());
        clone.setHasMore(original.isHasMore());
        clone.setClosed(original.isClosed());
        clone.setTotalFetched(original.getTotalFetched());
        clone.setRequestCount(original.getRequestCount());
        clone.setUseIteratorStrategy(false); // Redis uses offset strategy
        // Note: iterator is NOT copied (not serializable)
        return clone;
    }

    /**
     * Scheduled cleanup of expired sessions.
     */
    @Scheduled(fixedDelayString = "${oneapi.query.session.cleanup-interval-minutes:5}", timeUnit = TimeUnit.MINUTES)
    public void cleanupExpiredSessions() {
        log.debug("Running scheduled cleanup of expired query sessions");

        // Cleanup Caffeine (auto-evicts, but we can force cleanup)
        caffeineCache.cleanUp();

        long size = caffeineCache.estimatedSize();
        log.debug("Caffeine cache size after cleanup: {}", size);

        // Redis auto-expires based on TTL, no manual cleanup needed
    }

    /**
     * Get cache statistics.
     */
    public String getCacheStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("Query Session Cache Statistics:\n");
        stats.append("Caffeine size: ").append(caffeineCache.estimatedSize()).append("\n");
        stats.append("Caffeine stats: ").append(caffeineCache.stats()).append("\n");

        if (properties.isRedisEnabled() && redisTemplate != null) {
            // Redis stats would require scanning keys (expensive)
            stats.append("Redis: enabled\n");
        }

        return stats.toString();
    }
}
