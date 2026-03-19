package io.oneapi.admin.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.oneapi.admin.model.QuerySession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for query session caching.
 * Supports both Caffeine (in-memory) and Redis (distributed).
 */
@Configuration
public class QuerySessionCacheConfig {

    private static final Logger log = LoggerFactory.getLogger(QuerySessionCacheConfig.class);

    private final QuerySessionProperties properties;

    public QuerySessionCacheConfig(QuerySessionProperties properties) {
        this.properties = properties;
    }

    /**
     * Caffeine cache for fast in-memory caching with iterator support.
     * Used for single-instance deployments or as L1 cache.
     */
    @Bean
    public Cache<String, QuerySession> querySessionCaffeineCache() {
        log.info("Configuring Caffeine cache for query sessions: maxSize={}, ttl={} minutes",
                properties.getMaxCacheSize(), properties.getDefaultTtlMinutes());

        return Caffeine.newBuilder()
                .maximumSize(properties.getMaxCacheSize())
                .expireAfterAccess(properties.getDefaultTtlMinutes(), TimeUnit.MINUTES)
                .removalListener((String key, QuerySession session, RemovalCause cause) -> {
                    if (session != null) {
                        log.debug("Evicting query session: key={}, cause={}, totalFetched={}",
                                key, cause, session.getTotalFetched());
                        // Cleanup iterator resources
                        session.closeIterator();
                    }
                })
                .recordStats()
                .build();
    }

    /**
     * Redis template for distributed caching.
     * Used for multi-instance deployments or as L2 cache.
     */
    @Bean
    @ConditionalOnProperty(name = "oneapi.query.session.redis-enabled", havingValue = "true")
    public RedisTemplate<String, QuerySession> querySessionRedisTemplate(
            RedisConnectionFactory connectionFactory) {

        log.info("Configuring Redis template for query sessions: keyPrefix={}",
                properties.getRedisKeyPrefix());

        RedisTemplate<String, QuerySession> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serializer for values
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
