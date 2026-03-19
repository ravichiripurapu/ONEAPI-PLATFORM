package io.oneapi.admin.controller.reporting;

import io.oneapi.admin.dto.reporting.SavedQueryDTO;
import io.oneapi.admin.service.reporting.SavedQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing saved queries.
 */
@RestController
@RequestMapping("/api/v1/queries")
@Tag(name = "Saved Queries", description = "Manage saved SQL queries")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class SavedQueryController {

    private final SavedQueryService queryService;

    @PostMapping
    @Operation(summary = "Create a new saved query", description = "Creates a new saved SQL query")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SavedQueryDTO> create(@Valid @RequestBody SavedQueryDTO dto) {
        SavedQueryDTO created = queryService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a saved query", description = "Updates an existing saved query")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SavedQueryDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody SavedQueryDTO dto) {
        SavedQueryDTO updated = queryService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get query by ID", description = "Retrieves a saved query by its ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SavedQueryDTO> getById(@PathVariable Long id) {
        return queryService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "List all queries", description = "Retrieves all saved queries")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SavedQueryDTO>> getAll() {
        List<SavedQueryDTO> queries = queryService.findAll();
        return ResponseEntity.ok(queries);
    }

    @GetMapping("/my")
    @Operation(summary = "Get my queries", description = "Retrieves queries created by the current user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SavedQueryDTO>> getMyQueries() {
        // TODO: Get current username from SecurityContext
        String currentUsername = "admin"; // Placeholder
        List<SavedQueryDTO> queries = queryService.findByCreatedBy(currentUsername);
        return ResponseEntity.ok(queries);
    }

    @GetMapping("/connection/{datasourceId}")
    @Operation(summary = "Get queries by connection", description = "Retrieves queries for a specific database connection")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SavedQueryDTO>> getByConnection(@PathVariable Long datasourceId) {
        List<SavedQueryDTO> queries = queryService.findByConnection(datasourceId);
        return ResponseEntity.ok(queries);
    }

    @GetMapping("/source/{sourceId}")
    @Operation(summary = "Get queries by source", description = "Retrieves queries for a specific data source")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SavedQueryDTO>> getBySource(@PathVariable Long sourceId) {
        List<SavedQueryDTO> queries = queryService.findBySource(sourceId);
        return ResponseEntity.ok(queries);
    }

    @GetMapping("/public")
    @Operation(summary = "Get public queries", description = "Retrieves all public saved queries")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SavedQueryDTO>> getPublicQueries() {
        List<SavedQueryDTO> queries = queryService.findPublicQueries();
        return ResponseEntity.ok(queries);
    }

    @GetMapping("/favorites")
    @Operation(summary = "Get favorite queries", description = "Retrieves favorite queries for the current user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SavedQueryDTO>> getFavorites() {
        // TODO: Get current username from SecurityContext
        String currentUsername = "admin"; // Placeholder
        List<SavedQueryDTO> queries = queryService.findFavorites(currentUsername);
        return ResponseEntity.ok(queries);
    }

    @GetMapping("/accessible")
    @Operation(summary = "Get accessible queries", description = "Retrieves queries accessible to the current user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SavedQueryDTO>> getAccessibleQueries() {
        // TODO: Get current username from SecurityContext
        String currentUsername = "admin"; // Placeholder
        List<SavedQueryDTO> queries = queryService.findAccessibleQueries(currentUsername);
        return ResponseEntity.ok(queries);
    }

    @GetMapping("/search")
    @Operation(summary = "Search queries", description = "Searches queries by name or content")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SavedQueryDTO>> search(@RequestParam String q) {
        // TODO: Get current username from SecurityContext
        String currentUsername = "admin"; // Placeholder
        List<SavedQueryDTO> queries = queryService.search(currentUsername, q);
        return ResponseEntity.ok(queries);
    }

    @PostMapping("/{id}/favorite")
    @Operation(summary = "Toggle favorite", description = "Toggles the favorite status of a query")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SavedQueryDTO> toggleFavorite(@PathVariable Long id) {
        // TODO: Get current username from SecurityContext
        String currentUsername = "admin"; // Placeholder
        SavedQueryDTO updated = queryService.toggleFavorite(id, currentUsername);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/record-execution")
    @Operation(summary = "Record execution", description = "Records execution statistics for a query")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> recordExecution(
            @PathVariable Long id,
            @RequestParam long executionTimeMs) {
        queryService.recordExecution(id, executionTimeMs);
        return ResponseEntity.ok().build();
    }

    // ========== NEW: Query Execution Endpoints ==========

    @PostMapping("/{id}/execute")
    @Operation(summary = "Execute saved query",
               description = "Executes a saved query with session-based pagination. " +
                            "First call returns sessionKey for subsequent pages. " +
                            "Use sessionKey parameter to fetch next pages.")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<io.oneapi.admin.model.QueryResponse> executeQuery(
            @PathVariable Long id,
            @RequestParam(required = false) String sessionKey,
            @RequestBody(required = false) java.util.Map<String, Object> parameters,
            java.security.Principal principal) {

        io.oneapi.admin.model.QueryResponse response = queryService.executeSavedQuery(
            id,
            parameters != null ? parameters : java.util.Map.of(),
            sessionKey,
            principal.getName()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/preview")
    @Operation(summary = "Preview SQL query",
               description = "Previews a SQL query (LIMIT 10) without saving. " +
                            "Useful for testing queries before saving them.")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> previewQuery(
            @RequestParam Long datasourceId,
            @RequestBody String sql,
            java.security.Principal principal) {

        java.util.List<java.util.Map<String, Object>> results = queryService.previewQuery(
            datasourceId,
            sql,
            principal.getName()
        );

        return ResponseEntity.ok(results);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a query", description = "Deletes a saved query")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        queryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
