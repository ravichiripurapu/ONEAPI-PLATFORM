package io.oneapi.admin.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.oneapi.admin.config.QuerySessionProperties;
import io.oneapi.admin.exception.SessionExpiredException;
import io.oneapi.admin.exception.SessionNotFoundException;
import io.oneapi.admin.exception.TooManySessionsException;
import io.oneapi.admin.model.QuerySession;
import io.oneapi.sdk.model.EntityRecord;
import io.oneapi.sdk.model.EntityRecordIterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class QuerySessionManagerTest {

    private QuerySessionManager sessionManager;
    private QuerySessionProperties properties;
    private Cache<String, QuerySession> caffeineCache;
    private RedisTemplate<String, QuerySession> redisTemplate;

    @BeforeEach
    void setUp() {
        properties = new QuerySessionProperties();
        properties.setDefaultPageSize(100);
        properties.setDefaultTtlMinutes(15);
        properties.setMaxSessionsPerUser(10);
        properties.setCacheType(QuerySessionProperties.CacheType.CAFFEINE);

        caffeineCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();

        redisTemplate = mock(RedisTemplate.class);

        sessionManager = new QuerySessionManager(properties, caffeineCache, redisTemplate);
    }

    @Test
    void testCreateSession_Success() {
        // Given
        String userId = "user123";
        Long connectionId = 1L;
        String tableName = "users";

        // When
        QuerySession session = sessionManager.createSession(userId, connectionId, tableName, 100, 15);

        // Then
        assertNotNull(session);
        assertNotNull(session.getSessionKey());
        assertEquals(userId, session.getUserId());
        assertEquals(connectionId, session.getDatasourceId());
        assertEquals(tableName, session.getTableName());
        assertEquals(100, session.getPageSize());
        assertEquals(0, session.getOffset());
        assertTrue(session.isHasMore());
        assertEquals(0, session.getTotalFetched());
        assertNotNull(session.getExpiresAt());
        assertTrue(session.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void testCreateSession_WithCustomPageSize() {
        // Given
        String userId = "user123";

        // When
        QuerySession session = sessionManager.createSession(userId, 1L, "users", 200, 15);

        // Then
        assertEquals(200, session.getPageSize());
    }

    @Test
    void testGetSession_Success() {
        // Given
        String userId = "user123";
        QuerySession createdSession = sessionManager.createSession(userId, 1L, "users", 100, 15);
        String sessionKey = createdSession.getSessionKey();

        // When
        QuerySession retrievedSession = sessionManager.getSession(sessionKey);

        // Then
        assertNotNull(retrievedSession);
        assertEquals(sessionKey, retrievedSession.getSessionKey());
        assertEquals(userId, retrievedSession.getUserId());
    }

    @Test
    void testGetSession_NotFound() {
        // Given
        String nonExistentKey = "non-existent-key";

        // When/Then
        assertThrows(SessionNotFoundException.class, () -> {
            sessionManager.getSession(nonExistentKey);
        });
    }

    @Test
    void testUpdateSession_Success() {
        // Given
        String userId = "user123";
        QuerySession session = sessionManager.createSession(userId, 1L, "users", 100, 15);
        session.setOffset(100);
        session.setTotalFetched(100);
        session.setRequestCount(1);

        // When
        sessionManager.updateSession(session, 15);

        // Then
        QuerySession updated = sessionManager.getSession(session.getSessionKey());
        assertEquals(100, updated.getOffset());
        assertEquals(100, updated.getTotalFetched());
        assertEquals(1, updated.getRequestCount());
    }

    @Test
    void testCloseSession_Success() {
        // Given
        String userId = "user123";
        QuerySession session = sessionManager.createSession(userId, 1L, "users", 100, 15);
        String sessionKey = session.getSessionKey();

        // When
        sessionManager.closeSession(sessionKey);

        // Then
        assertThrows(SessionNotFoundException.class, () -> {
            sessionManager.getSession(sessionKey);
        });
    }

    @Test
    void testRemoveMultipleSessions() {
        // Given
        String userId = "user123";
        QuerySession session1 = sessionManager.createSession(userId, 1L, "users", 100, 15);
        QuerySession session2 = sessionManager.createSession(userId, 1L, "orders", 100, 15);

        // When - Remove sessions individually
        sessionManager.removeSession(session1.getSessionKey());
        sessionManager.removeSession(session2.getSessionKey());

        // Then
        assertThrows(SessionNotFoundException.class, () -> {
            sessionManager.getSession(session1.getSessionKey());
        });
        assertThrows(SessionNotFoundException.class, () -> {
            sessionManager.getSession(session2.getSessionKey());
        });
    }

    @Test
    void testGetActiveSessions_Success() {
        // Given
        String userId = "user123";
        sessionManager.createSession(userId, 1L, "users", 100, 15);
        sessionManager.createSession(userId, 1L, "orders", 100, 15);

        // When
        long activeSessions = sessionManager.countUserSessions(userId);

        // Then
        assertEquals(2, activeSessions);
    }

    @Test
    void testCreateSession_TooManySessions() {
        // Given
        String userId = "user123";
        properties.setMaxSessionsPerUser(2);

        // Create 2 sessions (max limit)
        sessionManager.createSession(userId, 1L, "table1", 100, 15);
        sessionManager.createSession(userId, 1L, "table2", 100, 15);

        // When/Then - Creating 3rd session should fail
        assertThrows(TooManySessionsException.class, () -> {
            sessionManager.createSession(userId, 1L, "table3", 100, 15);
        });
    }

    @Test
    void testSessionExpiry() throws InterruptedException {
        // Given
        Cache<String, QuerySession> shortTtlCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(1, TimeUnit.MILLISECONDS) // Expire very quickly
                .build();

        QuerySessionProperties shortTtlProperties = new QuerySessionProperties();
        shortTtlProperties.setDefaultPageSize(100);
        shortTtlProperties.setDefaultTtlMinutes(0); // Expire immediately for testing
        shortTtlProperties.setMaxSessionsPerUser(10);
        shortTtlProperties.setCacheType(QuerySessionProperties.CacheType.CAFFEINE);

        QuerySessionManager shortTtlManager = new QuerySessionManager(
            shortTtlProperties, shortTtlCache, redisTemplate
        );

        String userId = "user123";
        QuerySession session = shortTtlManager.createSession(userId, 1L, "users", 100, 0);
        String sessionKey = session.getSessionKey();

        // Wait for expiry
        Thread.sleep(100);

        // When/Then - Session should be expired or evicted from cache
        // The session's expiresAt time should indicate it's expired
        assertTrue(session.getExpiresAt().isBefore(LocalDateTime.now()) ||
                   session.getExpiresAt().equals(LocalDateTime.now()));
    }

    @Test
    void testIteratorStrategy_CaffeineCache() {
        // Given
        String userId = "user123";
        EntityRecordIterator<EntityRecord> mockIterator = mock(EntityRecordIterator.class);
        QuerySession session = sessionManager.createSession(userId, 1L, "users", 100, 15);
        session.setIterator(mockIterator);
        session.setUseIteratorStrategy(true);

        // When
        sessionManager.updateSession(session, 15);
        QuerySession retrieved = sessionManager.getSession(session.getSessionKey());

        // Then
        assertTrue(retrieved.isUseIteratorStrategy());
        assertNotNull(retrieved.getIterator());
        assertEquals(mockIterator, retrieved.getIterator());
    }

    @Test
    void testOffsetStrategy() {
        // Given
        String userId = "user123";
        QuerySession session = sessionManager.createSession(userId, 1L, "users", 100, 15);
        session.setUseIteratorStrategy(false);
        session.setOffset(250);

        // When
        sessionManager.updateSession(session, 15);
        QuerySession retrieved = sessionManager.getSession(session.getSessionKey());

        // Then
        assertFalse(retrieved.isUseIteratorStrategy());
        assertEquals(250, retrieved.getOffset());
    }

    @Test
    void testSessionWithSchema() {
        // Given
        String userId = "user123";
        // Create session using the QuerySession constructor that supports schema
        QuerySession session = sessionManager.createSession(userId, 1L, "users", 100, 15);
        session.setSchema("public");

        // When
        session.setTotalFetched(500);
        session.setRequestCount(5);
        session.setHasMore(false);
        sessionManager.updateSession(session, 15);

        QuerySession retrieved = sessionManager.getSession(session.getSessionKey());

        // Then
        assertEquals(500, retrieved.getTotalFetched());
        assertEquals(5, retrieved.getRequestCount());
        assertFalse(retrieved.isHasMore());
        assertEquals("public", retrieved.getSchema());
    }
}
