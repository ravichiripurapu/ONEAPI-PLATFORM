package io.oneapi.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.oneapi.admin.config.QuerySessionProperties;
import io.oneapi.admin.exception.SessionNotFoundException;
import io.oneapi.admin.model.QueryRequest;
import io.oneapi.admin.model.QueryResponse;
import io.oneapi.admin.service.DatabaseQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QueryController.class)
class QueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DatabaseQueryService queryService;

    @MockBean
    private QuerySessionProperties properties;

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
    void testQueryData_NewQuery_Success() throws Exception {
        // Given
        QueryRequest request = new QueryRequest();
        request.setDatasourceId(1L);
        request.setTableName("users");

        when(queryService.queryData(any(QueryRequest.class), eq("testuser")))
            .thenReturn(mockResponse);

        // When/Then
        mockMvc.perform(post("/api/query")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionKey").value("session-key-123"))
            .andExpect(jsonPath("$.records").isArray())
            .andExpect(jsonPath("$.records.length()").value(3))
            .andExpect(jsonPath("$.metadata.recordCount").value(3))
            .andExpect(jsonPath("$.metadata.hasMore").value(true));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testQueryData_Continuation_Success() throws Exception {
        // Given
        QueryRequest request = new QueryRequest();
        request.setSessionKey("session-key-123");

        when(queryService.queryData(any(QueryRequest.class), eq("testuser")))
            .thenReturn(mockResponse);

        // When/Then
        mockMvc.perform(post("/api/query")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionKey").value("session-key-123"))
            .andExpect(jsonPath("$.records.length()").value(3));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetNextPage_Success() throws Exception {
        // Given
        String sessionKey = "session-key-123";

        when(queryService.queryData(any(QueryRequest.class), eq("testuser")))
            .thenReturn(mockResponse);

        // When/Then
        mockMvc.perform(get("/api/query")
                .param("key", sessionKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionKey").value(sessionKey))
            .andExpect(jsonPath("$.records.length()").value(3));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testQueryData_LastPage() throws Exception {
        // Given
        QueryRequest request = new QueryRequest();
        request.setSessionKey("session-key-123");

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
        mockMvc.perform(post("/api/query")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionKey").doesNotExist())
            .andExpect(jsonPath("$.metadata.hasMore").value(false))
            .andExpect(jsonPath("$.metadata.totalFetched").value(102));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testQueryData_SessionNotFound() throws Exception {
        // Given
        QueryRequest request = new QueryRequest();
        request.setSessionKey("invalid-key");

        when(queryService.queryData(any(QueryRequest.class), eq("testuser")))
            .thenThrow(new SessionNotFoundException("Session not found"));

        // When/Then
        mockMvc.perform(post("/api/query")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testQueryData_WithSchema() throws Exception {
        // Given
        QueryRequest request = new QueryRequest();
        request.setDatasourceId(1L);
        request.setTableName("users");
        request.setSchema("public");

        when(queryService.queryData(any(QueryRequest.class), eq("testuser")))
            .thenReturn(mockResponse);

        // When/Then
        mockMvc.perform(post("/api/query")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionKey").exists());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testQueryData_InvalidRequest_MissingConnectionId() throws Exception {
        // Given - Neither sessionKey nor connectionId provided
        QueryRequest request = new QueryRequest();
        request.setTableName("users");

        // When/Then
        mockMvc.perform(post("/api/query")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testQueryData_Unauthorized() throws Exception {
        // Given
        QueryRequest request = new QueryRequest();
        request.setDatasourceId(1L);
        request.setTableName("users");

        // When/Then - No authentication
        mockMvc.perform(post("/api/query")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCloseSession_Success() throws Exception {
        // Given
        String sessionKey = "session-key-123";

        // When/Then
        mockMvc.perform(post("/api/query/close")
                .with(csrf())
                .param("key", sessionKey))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCloseAllSessions_Success() throws Exception {
        // When/Then
        mockMvc.perform(post("/api/query/close-all")
                .with(csrf()))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetActiveSessions_Success() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/query/active"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
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
