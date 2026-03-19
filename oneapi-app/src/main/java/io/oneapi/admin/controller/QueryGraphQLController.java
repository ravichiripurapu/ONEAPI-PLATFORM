package io.oneapi.admin.controller;

import io.oneapi.admin.entity.UserPreferences;
import io.oneapi.admin.model.QueryRequest;
import io.oneapi.admin.model.QueryResponse;
import io.oneapi.admin.service.DatabaseQueryService;
import io.oneapi.admin.service.QuerySessionManager;
import io.oneapi.admin.service.UserPreferencesService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * GraphQL controller for query operations with session-based pagination.
 */
@Controller
public class QueryGraphQLController {

    private final DatabaseQueryService queryService;
    private final QuerySessionManager sessionManager;
    private final UserPreferencesService preferencesService;

    public QueryGraphQLController(DatabaseQueryService queryService,
                                 QuerySessionManager sessionManager,
                                 UserPreferencesService preferencesService) {
        this.queryService = queryService;
        this.sessionManager = sessionManager;
        this.preferencesService = preferencesService;
    }

    /**
     * Query data with session-based pagination.
     *
     * For new query:
     * query {
     *   queryData(datasourceId: "1", tableName: "users") {
     *     sessionKey
     *     records
     *     metadata { hasMore, totalFetched }
     *   }
     * }
     *
     * For next page:
     * query {
     *   queryData(sessionKey: "abc-123") {
     *     sessionKey
     *     records
     *     metadata { hasMore }
     *   }
     * }
     */
    @QueryMapping
    public QueryResponse queryData(@Argument Long datasourceId,
                                   @Argument String tableName,
                                   @Argument String schema,
                                   @Argument String sessionKey) {
        String userId = getCurrentUserId();

        QueryRequest request = new QueryRequest();

        if (sessionKey != null) {
            // Continuation
            request.setSessionKey(sessionKey);
        } else {
            // New query
            request.setDatasourceId(datasourceId);
            request.setTableName(tableName);
            request.setSchema(schema);
        }

        return queryService.queryData(request, userId);
    }

    /**
     * Get user's query preferences.
     */
    @QueryMapping
    public UserPreferences getUserPreferences() {
        String userId = getCurrentUserId();
        return preferencesService.getPreferences(userId);
    }

    /**
     * Close a query session.
     *
     * mutation {
     *   closeSession(sessionKey: "abc-123")
     * }
     */
    @MutationMapping
    public Boolean closeSession(@Argument String sessionKey) {
        sessionManager.closeSession(sessionKey);
        return true;
    }

    /**
     * Update user query preferences.
     *
     * mutation {
     *   updateUserPreferences(input: {
     *     pageSize: 200
     *     ttlMinutes: 30
     *   }) {
     *     pageSize
     *     ttlMinutes
     *   }
     * }
     */
    @MutationMapping
    public UserPreferences updateUserPreferences(@Argument Map<String, Object> input) {
        String userId = getCurrentUserId();

        Integer pageSize = input.containsKey("pageSize") ?
                ((Number) input.get("pageSize")).intValue() : null;
        Integer ttlMinutes = input.containsKey("ttlMinutes") ?
                ((Number) input.get("ttlMinutes")).intValue() : null;
        Integer maxSessions = input.containsKey("maxConcurrentSessions") ?
                ((Number) input.get("maxConcurrentSessions")).intValue() : null;

        return preferencesService.updatePreferences(userId, pageSize, ttlMinutes, maxSessions);
    }

    /**
     * Get current user ID from security context.
     */
    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }
}
