package io.oneapi.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for query session management.
 */
@Configuration
@ConfigurationProperties(prefix = "oneapi.query.session")
public class QuerySessionProperties {

    /**
     * Default page size if user hasn't configured one.
     */
    private int defaultPageSize = 100;

    /**
     * Maximum page size allowed (upper limit).
     */
    private int maxPageSize = 1000;

    /**
     * Minimum page size allowed (lower limit).
     */
    private int minPageSize = 10;

    /**
     * Default TTL in minutes if user hasn't configured one.
     */
    private int defaultTtlMinutes = 15;

    /**
     * Maximum TTL in minutes allowed.
     */
    private int maxTtlMinutes = 120;

    /**
     * Minimum TTL in minutes allowed.
     */
    private int minTtlMinutes = 5;

    /**
     * Maximum concurrent sessions per user.
     */
    private int maxSessionsPerUser = 10;

    /**
     * Cleanup scheduler interval in minutes.
     */
    private int cleanupIntervalMinutes = 5;

    /**
     * Cache type: CAFFEINE or REDIS.
     */
    private CacheType cacheType = CacheType.CAFFEINE;

    /**
     * Maximum number of sessions to cache.
     */
    private long maxCacheSize = 1000;

    /**
     * Enable Redis for distributed caching.
     */
    private boolean redisEnabled = false;

    /**
     * Redis key prefix for query sessions.
     */
    private String redisKeyPrefix = "query:session:";

    public enum CacheType {
        CAFFEINE,   // In-memory, iterator-based (fast)
        REDIS,      // Distributed, offset-based (slower but scalable)
        BOTH        // Use Caffeine as L1, Redis as L2
    }

    // Getters and Setters

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public void setDefaultPageSize(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public int getMinPageSize() {
        return minPageSize;
    }

    public void setMinPageSize(int minPageSize) {
        this.minPageSize = minPageSize;
    }

    public int getDefaultTtlMinutes() {
        return defaultTtlMinutes;
    }

    public void setDefaultTtlMinutes(int defaultTtlMinutes) {
        this.defaultTtlMinutes = defaultTtlMinutes;
    }

    public int getMaxTtlMinutes() {
        return maxTtlMinutes;
    }

    public void setMaxTtlMinutes(int maxTtlMinutes) {
        this.maxTtlMinutes = maxTtlMinutes;
    }

    public int getMinTtlMinutes() {
        return minTtlMinutes;
    }

    public void setMinTtlMinutes(int minTtlMinutes) {
        this.minTtlMinutes = minTtlMinutes;
    }

    public int getMaxSessionsPerUser() {
        return maxSessionsPerUser;
    }

    public void setMaxSessionsPerUser(int maxSessionsPerUser) {
        this.maxSessionsPerUser = maxSessionsPerUser;
    }

    public int getCleanupIntervalMinutes() {
        return cleanupIntervalMinutes;
    }

    public void setCleanupIntervalMinutes(int cleanupIntervalMinutes) {
        this.cleanupIntervalMinutes = cleanupIntervalMinutes;
    }

    public CacheType getCacheType() {
        return cacheType;
    }

    public void setCacheType(CacheType cacheType) {
        this.cacheType = cacheType;
    }

    public long getMaxCacheSize() {
        return maxCacheSize;
    }

    public void setMaxCacheSize(long maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }

    public boolean isRedisEnabled() {
        return redisEnabled;
    }

    public void setRedisEnabled(boolean redisEnabled) {
        this.redisEnabled = redisEnabled;
    }

    public String getRedisKeyPrefix() {
        return redisKeyPrefix;
    }

    public void setRedisKeyPrefix(String redisKeyPrefix) {
        this.redisKeyPrefix = redisKeyPrefix;
    }

    /**
     * Validate and constrain page size.
     */
    public int validatePageSize(Integer userPageSize) {
        if (userPageSize == null) {
            return defaultPageSize;
        }
        return Math.max(minPageSize, Math.min(userPageSize, maxPageSize));
    }

    /**
     * Validate and constrain TTL.
     */
    public int validateTtl(Integer userTtlMinutes) {
        if (userTtlMinutes == null) {
            return defaultTtlMinutes;
        }
        return Math.max(minTtlMinutes, Math.min(userTtlMinutes, maxTtlMinutes));
    }
}
