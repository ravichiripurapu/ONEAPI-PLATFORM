package io.oneapi.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.oneapi.admin.connector.ConnectorFactory;
import io.oneapi.admin.entity.SourceInfo;
import io.oneapi.admin.model.QueryRequest;
import io.oneapi.admin.model.QueryResponse;
import io.oneapi.admin.model.QuerySession;
import io.oneapi.admin.repository.SourceInfoRepository;
import io.oneapi.sdk.base.Source;
import io.oneapi.sdk.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for executing queries against databases using connectors.
 */
@Service
@Transactional
public class DatabaseQueryService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseQueryService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ConnectorFactory connectorFactory;
    private final SourceInfoRepository connectionRepository;
    private final QuerySessionManager sessionManager;
    private final UserPreferencesService userPreferencesService;

    public DatabaseQueryService(ConnectorFactory connectorFactory,
                                SourceInfoRepository connectionRepository,
                                QuerySessionManager sessionManager,
                                UserPreferencesService userPreferencesService) {
        this.connectorFactory = connectorFactory;
        this.connectionRepository = connectionRepository;
        this.sessionManager = sessionManager;
        this.userPreferencesService = userPreferencesService;
    }

    /**
     * Test connection to a database.
     */
    public ConnectionStatus testConnection(Long datasourceId) {
        SourceInfo connection = connectionRepository.findById(datasourceId)
                .orElseThrow(() -> new RuntimeException("Connection not found: " + datasourceId));

        return testConnection(connection);
    }

    /**
     * Test connection using connection details.
     */
    public ConnectionStatus testConnection(SourceInfo connection) {
        Source source = null;
        try {
            source = connectorFactory.createSource(connection);
            JsonNode config = buildConfig(connection);
            return source.check(config);
        } catch (Exception e) {
            log.error("Error testing connection", e);
            return new ConnectionStatus(ConnectionStatus.Status.FAILED, e.getMessage());
        } finally {
            if (source != null && source instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) source).close();
                } catch (Exception e) {
                    log.warn("Error closing source", e);
                }
            }
        }
    }

    /**
     * Discover catalog (schema) for a database.
     */
    public Domain discoverCatalog(Long datasourceId) {
        SourceInfo connection = connectionRepository.findById(datasourceId)
                .orElseThrow(() -> new RuntimeException("Connection not found: " + datasourceId));

        Source source = null;
        try {
            source = connectorFactory.createSource(connection);
            JsonNode config = buildConfig(connection);
            return source.discover(config);
        } catch (Exception e) {
            log.error("Error discovering catalog", e);
            throw new RuntimeException("Failed to discover catalog: " + e.getMessage(), e);
        } finally {
            if (source != null && source instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) source).close();
                } catch (Exception e) {
                    log.warn("Error closing source", e);
                }
            }
        }
    }

    /**
     * Read data from a specific table.
     */
    public List<Map<String, Object>> readTable(Long datasourceId, String tableName, Integer limit) {
        SourceInfo connection = connectionRepository.findById(datasourceId)
                .orElseThrow(() -> new RuntimeException("Connection not found: " + datasourceId));

        Source source = null;
        try {
            source = connectorFactory.createSource(connection);
            JsonNode config = buildConfig(connection);

            // Discover domain to find the table
            Domain domain = source.discover(config);

            // Filter to only the requested table
            List<DataEntity> filteredEntities = domain.getEntities().stream()
                    .filter(e -> e.getName().equalsIgnoreCase(tableName))
                    .toList();

            if (filteredEntities.isEmpty()) {
                throw new RuntimeException("Table not found: " + tableName);
            }

            Domain filteredDomain = new Domain(filteredEntities);
            State state = new State();

            List<Map<String, Object>> results = new ArrayList<>();
            try (EntityRecordIterator<EntityRecord> iterator = source.read(config, filteredDomain, state)) {
                int count = 0;
                while (iterator.hasNext() && (limit == null || count < limit)) {
                    EntityRecord record = iterator.next();
                    // getData() now returns Map<String, Object> directly
                    results.add(record.getData());
                    count++;
                }
            }

            return results;
        } catch (Exception e) {
            log.error("Error reading table data", e);
            throw new RuntimeException("Failed to read table: " + e.getMessage(), e);
        } finally {
            if (source != null && source instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) source).close();
                } catch (Exception e) {
                    log.warn("Error closing source", e);
                }
            }
        }
    }

    /**
     * Execute a custom query (if supported by connector).
     */
    public List<Map<String, Object>> executeQuery(Long datasourceId, String query) {
        // For now, throw unsupported - can be implemented later
        throw new UnsupportedOperationException("Custom SQL queries not yet supported");
    }

    /**
     * UNIFIED QUERY ENDPOINT - ENHANCED
     * Handles both new queries and continuation (next page) requests.
     * Now supports:
     * 1. Table reading (existing)
     * 2. Custom SQL execution (NEW)
     * 3. Session continuation for both
     *
     * If sessionKey is provided: Fetch next page from existing session
     * If datasourceId + tableName provided: Start new table query session
     * If datasourceId + sqlQuery provided: Start new SQL query session
     */
    /**
     * Execute a query (either new SQL query or continuation with sessionKey).
     * Only supports SQL queries - table queries have been removed.
     */
    public QueryResponse queryData(QueryRequest request, String userId) {
        if (request.isContinuation()) {
            // Continue existing SQL query session
            return fetchNextPage(request.getSessionKey(), userId);
        } else if (request.isSqlQuery()) {
            // Start new SQL query
            return startNewSqlQuery(request, userId);
        } else {
            throw new IllegalArgumentException(
                "Request must contain either sessionKey or (datasourceId + sqlQuery). " +
                "Table queries are not supported - use saved SQL queries instead."
            );
        }
    }

    /**
     * Fetch next page from existing SQL query session.
     */
    private QueryResponse fetchNextPage(String sessionKey, String userId) {
        log.info("Fetching next page: user={}, sessionKey={}", userId, sessionKey);

        // Get session
        QuerySession session = sessionManager.getSession(sessionKey);

        // Validate ownership
        if (!session.getUserId().equals(userId)) {
            throw new SecurityException("Session does not belong to user: " + userId);
        }

        // All sessions are SQL query sessions now
        return fetchSqlPage(session, userId);
    }

    /**
     * Start a new SQL query session.
     */
    private QueryResponse startNewSqlQuery(QueryRequest request, String userId) {
        log.info("Starting new SQL query: user={}, connection={}, sql={}",
                userId, request.getDatasourceId(),
                request.getSqlQuery().substring(0, Math.min(100, request.getSqlQuery().length())));

        // Get user preferences
        int pageSize = userPreferencesService.getPageSize(userId);
        int ttlMinutes = userPreferencesService.getTtlMinutes(userId);

        // Create session (pass null for tableName since this is SQL query)
        QuerySession session = sessionManager.createSession(
                userId,
                request.getDatasourceId(),
                null,
                pageSize,
                ttlMinutes
        );

        // Set SQL query fields
        session.setSqlQuery(request.getSqlQuery());
        session.setIsSqlQuery(true);

        // Fetch first page
        return fetchSqlPage(session, userId);
    }

    /**
     * Fetch a page of SQL query results using OFFSET-based pagination.
     * This approach is production-ready and works consistently across all cache types.
     */
    private QueryResponse fetchSqlPage(QuerySession session, String userId) {
        log.info("fetchSqlPage (OFFSET strategy): sessionKey={}, requestCount={}, offset={}, totalFetched={}",
                session.getSessionKey(), session.getRequestCount(), session.getOffset(), session.getTotalFetched());

        SourceInfo connection = connectionRepository.findById(session.getDatasourceId())
                .orElseThrow(() -> new RuntimeException("Connection not found: " + session.getDatasourceId()));

        List<Map<String, Object>> records = new ArrayList<>();
        boolean hasMore = false;

        Source source = null;
        try {
            // Create connector and database
            source = connectorFactory.createSource(connection);
            io.oneapi.sdk.database.JdbcDatabase database = getJdbcDatabase(source, connection);

            // Build paginated SQL query with LIMIT and OFFSET
            String originalSql = session.getSqlQuery().trim();
            // Remove existing LIMIT/OFFSET if present (to avoid double pagination)
            String baseSql = removeLimitOffset(originalSql);

            // Add LIMIT pageSize+1 to check if more records exist
            // Add OFFSET to skip already fetched records
            String paginatedSql = String.format("%s LIMIT %d OFFSET %d",
                    baseSql, session.getPageSize() + 1, session.getOffset());

            log.info("Executing paginated SQL: {} | LIMIT={}, OFFSET={}",
                    paginatedSql, session.getPageSize() + 1, session.getOffset());

            // Execute query
            try (java.sql.Connection conn = database.getConnection();
                 java.sql.Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery(paginatedSql)) {

                // Fetch records - fetch up to pageSize+1 to check if more exist
                int count = 0;
                while (rs.next()) {
                    if (count < session.getPageSize()) {
                        // Add to results (only first pageSize records)
                        Map<String, Object> record = new HashMap<>();
                        int columnCount = rs.getMetaData().getColumnCount();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = rs.getMetaData().getColumnName(i);
                            record.put(columnName, rs.getObject(i));
                        }
                        records.add(record);
                    } else {
                        // We got the (pageSize+1)th record, so there are more pages
                        hasMore = true;
                        break;
                    }
                    count++;
                }

                log.info("Fetched {} records from DB, returned {} to user, hasMore={}",
                        count, records.size(), hasMore);
            }

        } catch (Exception e) {
            log.error("Error executing SQL query with offset", e);
            throw new RuntimeException("Failed to execute SQL query: " + e.getMessage(), e);
        } finally {
            if (source != null && source instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) source).close();
                } catch (Exception e) {
                    log.warn("Error closing source", e);
                }
            }
        }

        // Update session offset for next request
        session.setOffset(session.getOffset() + records.size());
        session.incrementRequestCount();
        session.addFetchedRecords(records.size());
        session.setHasMore(hasMore);

        int ttlMinutes = userPreferencesService.getTtlMinutes(userId);

        if (!hasMore) {
            // Last page - close session
            sessionManager.closeSession(session.getSessionKey());
        } else {
            // Update session in cache with new offset
            sessionManager.updateSession(session, ttlMinutes);
        }

        // Build response
        QueryResponse.QueryMetadata metadata = new QueryResponse.QueryMetadata(
                records.size(),
                session.getTotalFetched(),
                hasMore,
                hasMore ? session.getExpiresAt() : null,
                session.getPageSize(),
                session.getRequestCount()
        );

        String returnedSessionKey = hasMore ? session.getSessionKey() : null;
        log.info("fetchSqlPage completed: offset={}, recordCount={}, totalFetched={}, hasMore={}, returnedSessionKey={}",
                session.getOffset(), records.size(), session.getTotalFetched(), hasMore,
                returnedSessionKey != null ? returnedSessionKey.substring(0, Math.min(8, returnedSessionKey.length())) + "..." : "null");

        return new QueryResponse(
                returnedSessionKey,
                records,
                metadata
        );
    }

    /**
     * Remove LIMIT and OFFSET clauses from SQL query.
     */
    private String removeLimitOffset(String sql) {
        // Simple regex to remove LIMIT and OFFSET (case insensitive)
        // This is a basic implementation - production code might need more sophisticated parsing
        String result = sql.replaceAll("(?i)\\s+LIMIT\\s+\\d+", "");
        result = result.replaceAll("(?i)\\s+OFFSET\\s+\\d+", "");
        return result.trim();
    }

    /**
     * NEW: Helper to extract JdbcDatabase from Source connector.
     */
    private io.oneapi.sdk.database.JdbcDatabase getJdbcDatabase(Source source, SourceInfo connection) throws Exception {
        JsonNode config = buildConfig(connection);

        // All database sources are AbstractDatabaseSource
        if (source instanceof io.oneapi.sdk.base.AbstractDatabaseSource) {
            // Use reflection to access createDatabase method
            java.lang.reflect.Method method = source.getClass()
                .getSuperclass()
                .getDeclaredMethod("createDatabase", JsonNode.class);
            method.setAccessible(true);
            return (io.oneapi.sdk.database.JdbcDatabase) method.invoke(source, config);
        }

        throw new UnsupportedOperationException("Source does not support SQL query execution");
    }

    /**
     * Build JSON configuration from SourceInfo entity.
     */
    private JsonNode buildConfig(SourceInfo connection) {
        Map<String, Object> configMap = new HashMap<>();

        configMap.put("host", connection.getHost());
        configMap.put("port", connection.getPort());
        configMap.put("database", connection.getDatabase());
        configMap.put("username", connection.getUsername());
        configMap.put("password", connection.getPassword());

        if (connection.getAdditionalParams() != null && !connection.getAdditionalParams().isEmpty()) {
            // Parse additional params if needed
            configMap.put("additionalParams", connection.getAdditionalParams());
        }

        return MAPPER.valueToTree(configMap);
    }
}
