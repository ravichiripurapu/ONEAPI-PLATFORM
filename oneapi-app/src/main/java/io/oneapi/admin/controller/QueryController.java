package io.oneapi.admin.controller;

import io.oneapi.admin.model.QueryRequest;
import io.oneapi.admin.model.QueryResponse;
import io.oneapi.admin.service.DatabaseQueryService;
import io.oneapi.admin.service.QuerySessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * REST controller for query operations with session-based pagination.
 */
@RestController
@RequestMapping("/api/query")
public class QueryController {

    private static final Logger log = LoggerFactory.getLogger(QueryController.class);

    private final DatabaseQueryService queryService;
    private final QuerySessionManager sessionManager;

    public QueryController(DatabaseQueryService queryService,
                          QuerySessionManager sessionManager) {
        this.queryService = queryService;
        this.sessionManager = sessionManager;
    }

    /**
     * Unified query endpoint.
     * Handles both new queries and pagination (next page) requests.
     *
     * For new query:
     * POST /api/query
     * { "datasourceId": 1, "tableName": "users" }
     *
     * For next page:
     * POST /api/query
     * { "sessionKey": "550e8400-e29b-41d4-a716-446655440000" }
     */
    @PostMapping
    public ResponseEntity<QueryResponse> queryData(@RequestBody QueryRequest request,
                                                   Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";

        log.info("Query request: userId={}, continuation={}, datasourceId={}, table={}",
                userId, request.isContinuation(), request.getDatasourceId(), request.getTableName());

        QueryResponse response = queryService.queryData(request, userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Simplified GET endpoint for fetching next page.
     * GET /api/query?key={sessionKey}
     */
    @GetMapping
    public ResponseEntity<QueryResponse> getNextPage(@RequestParam("key") String sessionKey,
                                                     Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";

        log.debug("Get next page: userId={}, sessionKey={}", userId, sessionKey);

        QueryRequest request = new QueryRequest(sessionKey);
        QueryResponse response = queryService.queryData(request, userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Close an active session.
     * DELETE /api/query/{sessionKey}
     */
    @DeleteMapping("/{sessionKey}")
    public ResponseEntity<Void> closeSession(@PathVariable String sessionKey,
                                            Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";

        log.info("Close session: userId={}, sessionKey={}", userId, sessionKey);

        sessionManager.closeSession(sessionKey);

        return ResponseEntity.noContent().build();
    }

    /**
     * Get cache statistics (admin only).
     * GET /api/query/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<String> getCacheStats() {
        log.debug("Getting cache stats");

        String stats = sessionManager.getCacheStats();

        return ResponseEntity.ok(stats);
    }
}
