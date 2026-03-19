package io.oneapi.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.oneapi.admin.connector.ConnectorFactory;
import io.oneapi.admin.entity.SourceInfo;
import io.oneapi.admin.exception.SessionNotFoundException;
import io.oneapi.admin.model.QueryRequest;
import io.oneapi.admin.model.QueryResponse;
import io.oneapi.admin.model.QuerySession;
import io.oneapi.admin.repository.SourceInfoRepository;
import io.oneapi.sdk.base.Source;
import io.oneapi.sdk.model.Domain;
import io.oneapi.sdk.model.EntityRecord;
import io.oneapi.sdk.model.EntityRecordIterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseQueryServiceTest {

    @Mock
    private SourceInfoRepository connectionRepository;

    @Mock
    private QuerySessionManager sessionManager;

    @Mock
    private ConnectorFactory connectorFactory;

    @Mock
    private UserPreferencesService userPreferencesService;

    @Mock
    private Source mockSource;

    @Mock
    private EntityRecordIterator<EntityRecord> mockIterator;

    private DatabaseQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new DatabaseQueryService(
            connectorFactory,
            connectionRepository,
            sessionManager,
            userPreferencesService
        );
    }

    @Test
    void testQueryData_NewQuery_Success() throws Exception {
        // Given
        String userId = "user123";
        Long connectionId = 1L;
        String tableName = "users";

        SourceInfo connection = createMockConnection();
        QuerySession session = createMockSession(userId, connectionId, tableName);
        List<Map<String, Object>> mockRecords = createMockRecords(3);

        when(connectionRepository.findById(connectionId)).thenReturn(Optional.of(connection));
        when(sessionManager.createSession(eq(userId), eq(connectionId), eq(tableName), anyInt(), anyInt()))
            .thenReturn(session);
        when(connectorFactory.createSource(any(SourceInfo.class))).thenReturn(mockSource);
        when(mockSource.discover(any(JsonNode.class))).thenReturn(createMockCatalog());

        // Setup iterator to return records
        setupMockIterator(mockRecords);
        when(mockSource.read(any(JsonNode.class), any(Domain.class), any()))
            .thenReturn(mockIterator);

        QueryRequest request = new QueryRequest();
        request.setDatasourceId(connectionId);
        request.setTableName(tableName);

        // When
        QueryResponse response = queryService.queryData(request, userId);

        // Then
        assertNotNull(response);
        assertNotNull(response.getSessionKey());
        assertEquals(3, response.getRecords().size());
        assertEquals(3, response.getMetadata().getRecordCount());
        assertEquals(3, response.getMetadata().getTotalFetched());
        assertTrue(response.getMetadata().isHasMore());

        verify(sessionManager).createSession(eq(userId), eq(connectionId), eq(tableName), anyInt(), anyInt());
        verify(sessionManager).updateSession(any(QuerySession.class), anyInt());
    }

    @Test
    void testQueryData_Continuation_Success() throws Exception {
        // Given
        String userId = "user123";
        String sessionKey = "session-key-123";
        Long connectionId = 1L;

        QuerySession session = createMockSession(userId, connectionId, "users");
        session.setSessionKey(sessionKey);
        session.setTotalFetched(100);
        session.setOffset(100);

        SourceInfo connection = createMockConnection();
        List<Map<String, Object>> mockRecords = createMockRecords(2);

        when(sessionManager.getSession(sessionKey)).thenReturn(session);
        when(connectionRepository.findById(connectionId)).thenReturn(Optional.of(connection));
        when(connectorFactory.createSource(any(SourceInfo.class))).thenReturn(mockSource);
        when(mockSource.discover(any(JsonNode.class))).thenReturn(createMockCatalog());

        setupMockIterator(mockRecords);
        when(mockSource.read(any(JsonNode.class), any(Domain.class), any()))
            .thenReturn(mockIterator);

        QueryRequest request = new QueryRequest();
        request.setSessionKey(sessionKey);

        // When
        QueryResponse response = queryService.queryData(request, userId);

        // Then
        assertNotNull(response);
        assertEquals(sessionKey, response.getSessionKey());
        assertEquals(2, response.getRecords().size());
        assertEquals(2, response.getMetadata().getRecordCount());
        assertEquals(102, response.getMetadata().getTotalFetched()); // Previous 100 + new 2

        verify(sessionManager).getSession(sessionKey);
        verify(sessionManager).updateSession(any(QuerySession.class), anyInt());
    }

    @Test
    void testQueryData_LastPage_SessionClosed() throws Exception {
        // Given
        String userId = "user123";
        String sessionKey = "session-key-123";

        QuerySession session = createMockSession(userId, 1L, "users");
        session.setSessionKey(sessionKey);
        session.setPageSize(100);

        SourceInfo connection = createMockConnection();
        List<Map<String, Object>> mockRecords = createMockRecords(50); // Less than page size

        when(sessionManager.getSession(sessionKey)).thenReturn(session);
        when(connectionRepository.findById(anyLong())).thenReturn(Optional.of(connection));
        when(connectorFactory.createSource(any(SourceInfo.class))).thenReturn(mockSource);
        when(mockSource.discover(any(JsonNode.class))).thenReturn(createMockCatalog());

        // Iterator returns false for hasNext after fetching records
        setupMockIteratorLastPage(mockRecords);
        when(mockSource.read(any(JsonNode.class), any(Domain.class), any()))
            .thenReturn(mockIterator);

        QueryRequest request = new QueryRequest();
        request.setSessionKey(sessionKey);

        // When
        QueryResponse response = queryService.queryData(request, userId);

        // Then
        assertNotNull(response);
        assertNull(response.getSessionKey()); // No session key = no more data
        assertEquals(50, response.getRecords().size());
        assertFalse(response.getMetadata().isHasMore());

        verify(sessionManager).closeSession(sessionKey);
    }

    @Test
    void testQueryData_InvalidSessionKey() {
        // Given
        String userId = "user123";
        String invalidSessionKey = "invalid-key";

        when(sessionManager.getSession(invalidSessionKey))
            .thenThrow(new SessionNotFoundException("Session not found"));

        QueryRequest request = new QueryRequest();
        request.setSessionKey(invalidSessionKey);

        // When/Then
        assertThrows(SessionNotFoundException.class, () -> {
            queryService.queryData(request, userId);
        });
    }

    @Test
    void testQueryData_ConnectionNotFound() {
        // Given
        String userId = "user123";
        Long connectionId = 999L;

        when(connectionRepository.findById(connectionId)).thenReturn(Optional.empty());

        QueryRequest request = new QueryRequest();
        request.setDatasourceId(connectionId);
        request.setTableName("users");

        // When/Then
        assertThrows(RuntimeException.class, () -> {
            queryService.queryData(request, userId);
        });
    }

    @Test
    void testQueryData_WithSchema() throws Exception {
        // Given
        String userId = "user123";
        Long connectionId = 1L;
        String tableName = "users";
        String schema = "public";

        SourceInfo connection = createMockConnection();
        QuerySession session = createMockSession(userId, connectionId, tableName);
        session.setSchema(schema);

        when(connectionRepository.findById(connectionId)).thenReturn(Optional.of(connection));
        when(sessionManager.createSession(eq(userId), eq(connectionId), eq(tableName), anyInt(), anyInt()))
            .thenReturn(session);
        when(connectorFactory.createSource(any(SourceInfo.class))).thenReturn(mockSource);
        when(mockSource.discover(any(JsonNode.class))).thenReturn(createMockCatalog());

        setupMockIterator(createMockRecords(1));
        when(mockSource.read(any(JsonNode.class), any(Domain.class), any()))
            .thenReturn(mockIterator);

        QueryRequest request = new QueryRequest();
        request.setDatasourceId(connectionId);
        request.setTableName(tableName);
        request.setSchema(schema);

        // When
        QueryResponse response = queryService.queryData(request, userId);

        // Then
        assertNotNull(response);
        verify(sessionManager).createSession(eq(userId), eq(connectionId), eq(tableName), anyInt(), anyInt());
    }

    @Test
    void testQueryData_IteratorStrategy() throws Exception {
        // Given
        String userId = "user123";
        String sessionKey = "session-key-123";

        QuerySession session = createMockSession(userId, 1L, "users");
        session.setSessionKey(sessionKey);
        session.setUseIteratorStrategy(true);
        session.setIterator(mockIterator); // Cached iterator

        SourceInfo connection = createMockConnection();
        List<Map<String, Object>> mockRecords = createMockRecords(3);

        when(sessionManager.getSession(sessionKey)).thenReturn(session);
        when(connectionRepository.findById(anyLong())).thenReturn(Optional.of(connection));
        when(connectorFactory.createSource(any(SourceInfo.class))).thenReturn(mockSource);
        when(mockSource.discover(any(JsonNode.class))).thenReturn(createMockCatalog());

        setupMockIterator(mockRecords);

        QueryRequest request = new QueryRequest();
        request.setSessionKey(sessionKey);

        // When
        QueryResponse response = queryService.queryData(request, userId);

        // Then
        assertNotNull(response);
        assertEquals(3, response.getRecords().size());

        // Should use cached iterator, not create new one
        verify(mockSource, never()).read(any(), any(), any());
    }

    // Helper methods

    private SourceInfo createMockConnection() {
        SourceInfo connection = new SourceInfo();
        connection.setId(1L);
        connection.setName("Test Connection");
        connection.setType(SourceInfo.DatabaseType.POSTGRESQL);
        connection.setHost("localhost");
        connection.setPort(5432);
        connection.setDatabase("testdb");
        connection.setUsername("testuser");
        connection.setPassword("testpass");
        return connection;
    }

    private QuerySession createMockSession(String userId, Long connectionId, String tableName) {
        QuerySession session = new QuerySession();
        session.setSessionKey(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setDatasourceId(connectionId);
        session.setTableName(tableName);
        session.setPageSize(100);
        session.setOffset(0);
        session.setHasMore(true);
        session.setTotalFetched(0);
        session.setRequestCount(0);
        session.setUseIteratorStrategy(false);
        return session;
    }

    private List<Map<String, Object>> createMockRecords(int count) {
        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", i + 1);
            record.put("name", "User " + (i + 1));
            record.put("email", "user" + (i + 1) + "@example.com");
            records.add(record);
        }
        return records;
    }

    private void setupMockIterator(List<Map<String, Object>> records) {
        Iterator<Map<String, Object>> recordIterator = records.iterator();

        when(mockIterator.hasNext()).thenAnswer(inv -> recordIterator.hasNext());
        when(mockIterator.next()).thenAnswer(inv -> {
            Map<String, Object> data = recordIterator.next();
            EntityRecord record = new EntityRecord();
            record.setData(data);
            return record;
        });
    }

    private void setupMockIteratorLastPage(List<Map<String, Object>> records) {
        Iterator<Map<String, Object>> recordIterator = records.iterator();
        int[] count = {0};

        when(mockIterator.hasNext()).thenAnswer(inv -> {
            if (count[0] < records.size()) {
                return true;
            }
            return false; // No more records after this page
        });

        when(mockIterator.next()).thenAnswer(inv -> {
            Map<String, Object> data = recordIterator.next();
            count[0]++;
            EntityRecord record = new EntityRecord();
            record.setData(data);
            return record;
        });
    }

    private Domain createMockCatalog() {
        Domain catalog = new Domain();
        // Add mock entities/tables as needed
        return catalog;
    }
}
