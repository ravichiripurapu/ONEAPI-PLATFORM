package io.oneapi.admin.service.reporting;

import io.oneapi.admin.dto.reporting.SavedQueryDTO;
import io.oneapi.admin.entity.Catalog;
import io.oneapi.admin.entity.SourceInfo;
import io.oneapi.admin.entity.SavedQuery;
import io.oneapi.admin.mapper.ReportingMapper;
import io.oneapi.admin.repository.CatalogRepository;
import io.oneapi.admin.repository.SourceInfoRepository;
import io.oneapi.admin.repository.SavedQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing saved queries.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SavedQueryService {

    private final SavedQueryRepository savedQueryRepository;
    private final SourceInfoRepository connectionRepository;
    private final CatalogRepository catalogRepository;
    private final ReportingMapper mapper;
    private final io.oneapi.admin.service.DatabaseQueryService databaseQueryService;

    /**
     * Create a new saved query.
     */
    public SavedQueryDTO create(SavedQueryDTO dto) {
        log.debug("Creating new saved query: {}", dto.getName());

        SourceInfo connection = connectionRepository.findById(dto.getDatasourceId())
            .orElseThrow(() -> new IllegalArgumentException("Connection not found with ID: " + dto.getDatasourceId()));

        Catalog catalog = null;
        if (dto.getCatalogId() != null) {
            catalog = catalogRepository.findById(dto.getCatalogId())
                .orElseThrow(() -> new IllegalArgumentException("Catalog not found with ID: " + dto.getCatalogId()));
        }

        SavedQuery entity = mapper.toEntity(dto, connection, catalog);
        SavedQuery saved = savedQueryRepository.save(entity);
        log.info("Created saved query with ID: {}", saved.getId());

        return mapper.toDTO(saved);
    }

    /**
     * Update an existing saved query.
     */
    public SavedQueryDTO update(Long id, SavedQueryDTO dto) {
        log.debug("Updating saved query ID: {}", id);

        SavedQuery entity = savedQueryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("SavedQuery not found with ID: " + id));

        SourceInfo connection = connectionRepository.findById(dto.getDatasourceId())
            .orElseThrow(() -> new IllegalArgumentException("Connection not found with ID: " + dto.getDatasourceId()));

        Catalog catalog = null;
        if (dto.getCatalogId() != null) {
            catalog = catalogRepository.findById(dto.getCatalogId())
                .orElseThrow(() -> new IllegalArgumentException("Catalog not found with ID: " + dto.getCatalogId()));
        }

        mapper.updateEntityFromDTO(dto, entity, connection, catalog);
        SavedQuery updated = savedQueryRepository.save(entity);
        log.info("Updated saved query ID: {}", id);

        return mapper.toDTO(updated);
    }

    /**
     * Get saved query by ID.
     */
    @Transactional(readOnly = true)
    public Optional<SavedQueryDTO> findById(Long id) {
        log.debug("Finding saved query by ID: {}", id);
        return savedQueryRepository.findById(id).map(mapper::toDTO);
    }

    /**
     * Get all saved queries.
     */
    @Transactional(readOnly = true)
    public List<SavedQueryDTO> findAll() {
        log.debug("Finding all saved queries");
        return mapper.queriesToDTOs(savedQueryRepository.findAll());
    }

    /**
     * Get queries created by specific user.
     */
    @Transactional(readOnly = true)
    public List<SavedQueryDTO> findByCreatedBy(String username) {
        log.debug("Finding saved queries created by: {}", username);
        return mapper.queriesToDTOs(savedQueryRepository.findByCreatedBy(username));
    }

    /**
     * Get queries by connection.
     */
    @Transactional(readOnly = true)
    public List<SavedQueryDTO> findByConnection(Long datasourceId) {
        log.debug("Finding saved queries for connection: {}", datasourceId);
        return mapper.queriesToDTOs(savedQueryRepository.findBySourceId(datasourceId));
    }

    /**
     * Get queries by catalog.
     */
    @Transactional(readOnly = true)
    public List<SavedQueryDTO> findByCatalog(Long catalogId) {
        log.debug("Finding saved queries for catalog: {}", catalogId);
        return mapper.queriesToDTOs(savedQueryRepository.findByCatalogId(catalogId));
    }

    /**
     * Get public queries.
     */
    @Transactional(readOnly = true)
    public List<SavedQueryDTO> findPublicQueries() {
        log.debug("Finding public saved queries");
        return mapper.queriesToDTOs(savedQueryRepository.findByIsPublicTrue());
    }

    /**
     * Get favorite queries for user.
     */
    @Transactional(readOnly = true)
    public List<SavedQueryDTO> findFavorites(String username) {
        log.debug("Finding favorite queries for user: {}", username);
        return mapper.queriesToDTOs(savedQueryRepository.findByIsFavoriteTrueAndCreatedBy(username));
    }

    /**
     * Get accessible queries for user (public + owned).
     */
    @Transactional(readOnly = true)
    public List<SavedQueryDTO> findAccessibleQueries(String username) {
        log.debug("Finding accessible queries for user: {}", username);
        return mapper.queriesToDTOs(savedQueryRepository.findAccessibleQueries(username));
    }

    /**
     * Search queries.
     */
    @Transactional(readOnly = true)
    public List<SavedQueryDTO> search(String username, String searchTerm) {
        log.debug("Searching queries for user {} with term: {}", username, searchTerm);
        return mapper.queriesToDTOs(savedQueryRepository.searchQueries(username, searchTerm));
    }

    /**
     * Toggle favorite status.
     */
    public SavedQueryDTO toggleFavorite(Long id, String username) {
        log.debug("Toggling favorite status for query ID: {}", id);

        SavedQuery entity = savedQueryRepository.findByIdAndCreatedBy(id, username)
            .orElseThrow(() -> new IllegalArgumentException("SavedQuery not found or not owned by user: " + id));

        entity.setIsFavorite(!entity.getIsFavorite());
        SavedQuery updated = savedQueryRepository.save(entity);
        log.info("Toggled favorite for query ID {}: {}", id, updated.getIsFavorite());

        return mapper.toDTO(updated);
    }

    /**
     * Record query execution.
     */
    public void recordExecution(Long id, long executionTimeMs) {
        log.debug("Recording execution for query ID: {}", id);

        SavedQuery entity = savedQueryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("SavedQuery not found with ID: " + id));

        Long currentCount = entity.getExecutionCount();
        Long currentAvg = entity.getAvgExecutionTimeMs();

        // Calculate new average
        long newAvg = currentAvg == null ? executionTimeMs :
            ((currentAvg * currentCount) + executionTimeMs) / (currentCount + 1);

        entity.setExecutionCount(currentCount + 1);
        entity.setAvgExecutionTimeMs(newAvg);
        entity.setLastExecutedAt(LocalDateTime.now());

        savedQueryRepository.save(entity);
        log.info("Recorded execution for query ID {}: count={}, avg={}ms", id, currentCount + 1, newAvg);
    }

    /**
     * Delete a saved query.
     */
    public void delete(Long id) {
        log.debug("Deleting saved query ID: {}", id);

        if (!savedQueryRepository.existsById(id)) {
            throw new IllegalArgumentException("SavedQuery not found with ID: " + id);
        }

        savedQueryRepository.deleteById(id);
        log.info("Deleted saved query ID: {}", id);
    }

    /**
     * Check if query exists.
     */
    @Transactional(readOnly = true)
    public boolean exists(Long id) {
        return savedQueryRepository.existsById(id);
    }

    // ========== NEW: Query Execution Methods ==========

    /**
     * Execute a saved query with session-based pagination.
     *
     * @param savedQueryId The ID of the saved query
     * @param parameters Runtime parameters for substitution (optional)
     * @param sessionKey Session key for pagination continuation (optional)
     * @param userId Current user ID
     * @return QueryResponse with sessionKey for pagination
     */
    public io.oneapi.admin.model.QueryResponse executeSavedQuery(
            Long savedQueryId,
            java.util.Map<String, Object> parameters,
            String sessionKey,
            String userId) {

        // Load saved query
        SavedQuery savedQuery = savedQueryRepository.findById(savedQueryId)
            .orElseThrow(() -> new IllegalArgumentException("SavedQuery not found with ID: " + savedQueryId));

        // Check permissions
        if (!savedQuery.getIsPublic() && !savedQuery.getCreatedBy().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                "Not authorized to execute this query"
            );
        }

        // If continuing session, just pass sessionKey
        if (sessionKey != null && !sessionKey.isEmpty()) {
            io.oneapi.admin.model.QueryRequest request = new io.oneapi.admin.model.QueryRequest();
            request.setSessionKey(sessionKey);
            return databaseQueryService.queryData(request, userId);
        }

        // New execution: substitute parameters in SQL
        String sql = substituteParameters(savedQuery.getQueryText(), parameters);

        // Build request
        io.oneapi.admin.model.QueryRequest request = new io.oneapi.admin.model.QueryRequest();
        request.setDatasourceId(savedQuery.getSource().getId());
        request.setSqlQuery(sql);

        // Execute via DatabaseQueryService (returns first page + sessionKey)
        io.oneapi.admin.model.QueryResponse response = databaseQueryService.queryData(request, userId);

        // Update execution metrics
        savedQuery.setExecutionCount(savedQuery.getExecutionCount() + 1);
        savedQuery.setLastExecutedAt(LocalDateTime.now());
        savedQueryRepository.save(savedQuery);

        return response;
    }

    /**
     * Preview a query (LIMIT 10, no pagination, no session).
     *
     * @param datasourceId Database connection ID
     * @param sql SQL query to preview
     * @param userId Current user ID
     * @return List of records (max 10)
     */
    public java.util.List<java.util.Map<String, Object>> previewQuery(
            Long datasourceId,
            String sql,
            String userId) {

        // Wrap SQL with LIMIT
        String previewSql = wrapWithLimit(sql, 10);

        // Build request
        io.oneapi.admin.model.QueryRequest request = new io.oneapi.admin.model.QueryRequest();
        request.setDatasourceId(datasourceId);
        request.setSqlQuery(previewSql);

        // Execute (will return first page only)
        io.oneapi.admin.model.QueryResponse response = databaseQueryService.queryData(request, userId);

        return response.getRecords();
    }

    // ========== Helper Methods ==========

    /**
     * Substitute parameters in SQL query.
     * Replaces {{paramName}} with actual values.
     */
    private String substituteParameters(String sql, java.util.Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return sql;
        }

        String result = sql;
        for (java.util.Map.Entry<String, Object> entry : params.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = formatSqlValue(entry.getValue());
            result = result.replace(placeholder, value);
        }
        return result;
    }

    /**
     * Format a value for SQL substitution.
     */
    private String formatSqlValue(Object value) {
        if (value == null) {
            return "NULL";
        } else if (value instanceof String) {
            // Escape single quotes
            return "'" + ((String) value).replace("'", "''") + "'";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else {
            // For other types, convert to string and quote
            return "'" + value.toString().replace("'", "''") + "'";
        }
    }

    /**
     * Wrap SQL with LIMIT clause if not already present.
     */
    private String wrapWithLimit(String sql, int limit) {
        if (sql.toUpperCase().contains("LIMIT")) {
            return sql; // Already has LIMIT
        }
        return sql.trim() + " LIMIT " + limit;
    }
}
