package io.oneapi.admin.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for rate limiting and throttling API requests.
 */
@Service
public class ThrottleService {

    private static final Logger log = LoggerFactory.getLogger(ThrottleService.class);

    // Request counters per user (sliding window)
    private final Cache<String, AtomicInteger> requestCounts;

    // Default rate limits
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 60;
    private static final int DEFAULT_REQUESTS_PER_HOUR = 1000;

    // Custom rate limits per user
    private final ConcurrentHashMap<String, Integer> customRateLimits = new ConcurrentHashMap<>();

    public ThrottleService() {
        this.requestCounts = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .build();
    }

    /**
     * Check if a request should be allowed for a user.
     */
    public boolean allowRequest(String userId) {
        AtomicInteger count = requestCounts.get(userId, k -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();

        int rateLimit = customRateLimits.getOrDefault(userId, DEFAULT_REQUESTS_PER_MINUTE);

        if (currentCount > rateLimit) {
            log.warn("Rate limit exceeded for user: {} ({}/{})", userId, currentCount, rateLimit);
            return false;
        }

        return true;
    }

    /**
     * Get current request count for a user.
     */
    public int getRequestCount(String userId) {
        AtomicInteger count = requestCounts.getIfPresent(userId);
        return count != null ? count.get() : 0;
    }

    /**
     * Get remaining requests for a user.
     */
    public int getRemainingRequests(String userId) {
        int limit = customRateLimits.getOrDefault(userId, DEFAULT_REQUESTS_PER_MINUTE);
        int current = getRequestCount(userId);
        return Math.max(0, limit - current);
    }

    /**
     * Set custom rate limit for a user.
     */
    public void setRateLimit(String userId, int requestsPerMinute) {
        customRateLimits.put(userId, requestsPerMinute);
        log.info("Set rate limit for user {}: {} requests/min", userId, requestsPerMinute);
    }

    /**
     * Reset rate limit for a user.
     */
    public void resetRateLimit(String userId) {
        requestCounts.invalidate(userId);
        log.info("Reset rate limit for user: {}", userId);
    }
}
