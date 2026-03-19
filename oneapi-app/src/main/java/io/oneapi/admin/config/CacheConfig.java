package io.oneapi.admin.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for security permission caching
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                // RBAC security caches
                "sourceAccess",
                "domainAccess",
                "entityAccess",
                "fieldAccess",
                "accessibleSourceIds",
                "accessibleDomainIds",
                "accessibleEntityIds",
                "accessibleFieldIds",
                "isSuperAdmin",
                "userRoleIds",
                // User authentication caches
                "usersByLogin",
                "users"
        );
    }
}
