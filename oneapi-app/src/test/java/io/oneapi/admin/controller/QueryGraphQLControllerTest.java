package io.oneapi.admin.controller;

import io.oneapi.admin.entity.UserPreferences;
import io.oneapi.admin.model.QueryRequest;
import io.oneapi.admin.model.QueryResponse;
import io.oneapi.admin.service.DatabaseQueryService;
import io.oneapi.admin.service.QuerySessionManager;
import io.oneapi.admin.service.UserPreferencesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@GraphQlTest(QueryGraphQLController.class)
class QueryGraphQLControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockBean
    private DatabaseQueryService queryService;

    @MockBean
    private QuerySessionManager sessionManager;

    @MockBean
    private UserPreferencesService preferencesService;

    private QueryResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockResponse = new QueryResponse();
        mockResponse.setSessionKey("session-key-123");
        mockResponse.setRecords(createMockRecords(3));

        QueryResponse.QueryMetadata metadata = new QueryResponse.QueryMetadata();
        metadata.setRecordCount(3);
        metadata.setTotalFetched(3);
        metadata.setHasMore(true);
        metadata.setPageSize(100);
        mockResponse.setMetadata(metadata);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testQueryData_NewQuery() {
        // Given
        when(queryService.queryData(any(QueryRequest.class), eq("testuser")))
            .thenReturn(mockResponse);

        // When/Then
        graphQlTester.document("""
            query {
                queryData(connectionId: 1, tableName: "users") {
                    sessionKey
                    records
                    metadata {
                        recordCount
                        totalFetched
                        hasMore
                        pageSize
                    }
                }
            }
            """)
            .execute()
            .path("queryData.sessionKey").entity(String.class).isEqualTo("session-key-123")
            .path("queryData.records").entityList(Object.class).hasSize(3)
            .path("queryData.metadata.recordCount").entity(Integer.class).isEqualTo(3)
            .path("queryData.metadata.hasMore").entity(Boolean.class).isEqualTo(true);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testQueryData_WithSchema() {
        // Given
        when(queryService.queryData(any(QueryRequest.class), eq("testuser")))
            .thenReturn(mockResponse);

        // When/Then
        graphQlTester.document("""
            query {
                queryData(connectionId: 1, tableName: "users", schema: "public") {
                    sessionKey
                    records
                    metadata {
                        recordCount
                    }
                }
            }
            """)
            .execute()
            .path("queryData.sessionKey").entity(String.class).isEqualTo("session-key-123")
            .path("queryData.records").entityList(Object.class).hasSize(3);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testQueryData_Continuation() {
        // Given
        when(queryService.queryData(any(QueryRequest.class), eq("testuser")))
            .thenReturn(mockResponse);

        // When/Then
        graphQlTester.document("""
            query {
                queryData(sessionKey: "session-key-123") {
                    sessionKey
                    records
                    metadata {
                        recordCount
                        totalFetched
                        hasMore
                    }
                }
            }
            """)
            .execute()
            .path("queryData.sessionKey").entity(String.class).isEqualTo("session-key-123")
            .path("queryData.records").entityList(Object.class).hasSize(3)
            .path("queryData.metadata.hasMore").entity(Boolean.class).isEqualTo(true);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testQueryData_LastPage() {
        // Given
        QueryResponse lastPageResponse = new QueryResponse();
        lastPageResponse.setSessionKey(null); // No more pages
        lastPageResponse.setRecords(createMockRecords(2));

        QueryResponse.QueryMetadata metadata = new QueryResponse.QueryMetadata();
        metadata.setRecordCount(2);
        metadata.setTotalFetched(102);
        metadata.setHasMore(false);
        metadata.setPageSize(100);
        lastPageResponse.setMetadata(metadata);

        when(queryService.queryData(any(QueryRequest.class), eq("testuser")))
            .thenReturn(lastPageResponse);

        // When/Then
        graphQlTester.document("""
            query {
                queryData(sessionKey: "session-key-123") {
                    sessionKey
                    records
                    metadata {
                        hasMore
                        totalFetched
                    }
                }
            }
            """)
            .execute()
            .path("queryData.sessionKey").valueIsNull()
            .path("queryData.metadata.hasMore").entity(Boolean.class).isEqualTo(false)
            .path("queryData.metadata.totalFetched").entity(Long.class).isEqualTo(102L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCloseSession() {
        // When/Then
        graphQlTester.document("""
            mutation {
                closeSession(sessionKey: "session-key-123")
            }
            """)
            .execute()
            .path("closeSession").entity(Boolean.class).isEqualTo(true);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCloseAllSessions() {
        // When/Then
        graphQlTester.document("""
            mutation {
                closeAllSessions
            }
            """)
            .execute()
            .path("closeAllSessions").entity(Boolean.class).isEqualTo(true);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetActiveSessions() {
        // Given
        when(sessionManager.countUserSessions("testuser")).thenReturn(3L);

        // When/Then
        graphQlTester.document("""
            query {
                activeSessions
            }
            """)
            .execute()
            .path("activeSessions").entity(Integer.class).isEqualTo(3);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUpdateUserPreferences() {
        // Given
        UserPreferences preferences = new UserPreferences();
        preferences.setUserId("testuser");
        preferences.setPageSize(200);
        preferences.setTtlMinutes(30);
        preferences.setMaxConcurrentSessions(5);

        when(preferencesService.updatePreferences(eq("testuser"), eq(200), eq(30), eq(5)))
            .thenReturn(preferences);

        // When/Then
        graphQlTester.document("""
            mutation {
                updateUserPreferences(input: {
                    pageSize: 200
                    ttlMinutes: 30
                    maxConcurrentSessions: 5
                }) {
                    userId
                    pageSize
                    ttlMinutes
                    maxConcurrentSessions
                }
            }
            """)
            .execute()
            .path("updateUserPreferences.userId").entity(String.class).isEqualTo("testuser")
            .path("updateUserPreferences.pageSize").entity(Integer.class).isEqualTo(200)
            .path("updateUserPreferences.ttlMinutes").entity(Integer.class).isEqualTo(30)
            .path("updateUserPreferences.maxConcurrentSessions").entity(Integer.class).isEqualTo(5);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetUserPreferences() {
        // Given
        UserPreferences preferences = new UserPreferences();
        preferences.setUserId("testuser");
        preferences.setPageSize(150);
        preferences.setTtlMinutes(20);
        preferences.setMaxConcurrentSessions(8);

        when(preferencesService.getPreferences("testuser")).thenReturn(preferences);

        // When/Then
        graphQlTester.document("""
            query {
                userPreferences {
                    userId
                    pageSize
                    ttlMinutes
                    maxConcurrentSessions
                }
            }
            """)
            .execute()
            .path("userPreferences.userId").entity(String.class).isEqualTo("testuser")
            .path("userPreferences.pageSize").entity(Integer.class).isEqualTo(150)
            .path("userPreferences.ttlMinutes").entity(Integer.class).isEqualTo(20)
            .path("userPreferences.maxConcurrentSessions").entity(Integer.class).isEqualTo(8);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testQueryData_ComplexRecords() {
        // Given
        QueryResponse complexResponse = new QueryResponse();
        complexResponse.setSessionKey("session-key-456");

        List<Map<String, Object>> complexRecords = new ArrayList<>();
        Map<String, Object> record = new HashMap<>();
        record.put("id", 1);
        record.put("name", "John Doe");
        record.put("age", 30);
        record.put("address", Map.of(
            "street", "123 Main St",
            "city", "Springfield",
            "zip", "12345"
        ));
        record.put("tags", List.of("vip", "premium", "active"));
        complexRecords.add(record);

        complexResponse.setRecords(complexRecords);

        QueryResponse.QueryMetadata metadata = new QueryResponse.QueryMetadata();
        metadata.setRecordCount(1);
        metadata.setTotalFetched(1);
        metadata.setHasMore(false);
        metadata.setPageSize(100);
        complexResponse.setMetadata(metadata);

        when(queryService.queryData(any(QueryRequest.class), eq("testuser")))
            .thenReturn(complexResponse);

        // When/Then
        graphQlTester.document("""
            query {
                queryData(connectionId: 1, tableName: "customers") {
                    sessionKey
                    records
                    metadata {
                        recordCount
                    }
                }
            }
            """)
            .execute()
            .path("queryData.sessionKey").entity(String.class).isEqualTo("session-key-456")
            .path("queryData.records").entityList(Object.class).hasSize(1);
    }

    // Helper methods

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
}
