# Query & Report Execution - REVISED Implementation
**Leveraging Existing DatabaseQueryService with Session-Based Pagination**

---

## Current Implementation Analysis

### ✅ **What You Already Have**

#### 1. DatabaseQueryService (Sophisticated Query Execution)
- **Session-based pagination** with `QuerySessionManager`
- **Dual caching strategy**:
  - **Caffeine**: In-memory caching with iterator support (fast)
  - **Redis**: Distributed caching with offset-based pagination (scalable)
- **User preferences** for page size and TTL
- **Table reading** using SDK's `Source.read()` and `EntityRecordIterator`
- **Catalog discovery** using `Source.discover()`
- **Connection testing** using `Source.check()`

#### 2. QuerySessionManager (Advanced Session Management)
- Session creation with unique keys
- Expiration handling (TTL-based)
- Session limits per user
- Strategy selection (iterator vs offset)
- Caffeine + Redis dual caching

#### 3. Query Models
- `QueryRequest`: Contains connectionId, tableName, schema, filters, sessionKey
- `QueryResponse`: Contains sessionKey, records, metadata
- `QuerySession`: Session state with iterator caching
- `QueryMetadata`: Row count, total fetched, hasMore, expiresAt, etc.

### ❌ **What's Missing (Line 163)**

```java
public List<Map<String, Object>> executeQuery(Long connectionId, String query) {
    // For now, throw unsupported - can be implemented later
    throw new UnsupportedOperationException("Custom SQL queries not yet supported");
}
```

**This is the gap!** The service can read tables but cannot execute **custom SQL queries**.

---

## Implementation Strategy - REVISED

### Goal: Add Custom SQL Query Support to Existing Architecture

We need to:
1. ✅ **Reuse session-based pagination** (QuerySession + QuerySessionManager)
2. ✅ **Reuse dual caching strategy** (Caffeine/Redis)
3. ✅ **Add SQL query execution** using SDK's JdbcDatabase
4. ✅ **Integrate with SavedQuery and Report** entities
5. ✅ **Add format conversion** (JSON, CSV, Excel)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    SavedQueryService                             │
│  - Execute saved queries with parameters                         │
│  - Preview queries (LIMIT 10)                                    │
│  - Manage favorites, sharing                                     │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ↓
┌─────────────────────────────────────────────────────────────────┐
│               DatabaseQueryService (ENHANCED)                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ EXISTING: queryData(QueryRequest) - Table Reading        │   │
│  │  - Session-based pagination with sessionKey              │   │
│  │  - Iterator caching (Caffeine) or Offset (Redis)         │   │
│  │  - Uses Source.read() → EntityRecordIterator             │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ NEW: executeCustomQuery(QueryRequest) - SQL Execution    │   │
│  │  - Session-based pagination with sessionKey              │   │
│  │  - ResultSet caching using custom SQLResultIterator      │   │
│  │  - Uses JdbcDatabase.query() → ResultSet                 │   │
│  └──────────────────────────────────────────────────────────┘   │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ↓
┌─────────────────────────────────────────────────────────────────┐
│                  QuerySessionManager                             │
│  - Create/get/update/close sessions                             │
│  - Store in Caffeine or Redis                                    │
│  - Session expiration and limits                                │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ↓
┌─────────────────────────────────────────────────────────────────┐
│                    ReportExecutionService                        │
│  - Execute reports (delegates to SavedQueryService)             │
│  - Parameter substitution                                        │
│  - Format conversion (JSON/CSV/Excel)                           │
└──────────────────────────────────────────────────────────────────┘
```

---

## Implementation Details

### 1. Enhance QueryRequest Model

**Add SQL query support**:

```java
public class QueryRequest {
    // Existing fields for table reading
    private Long connectionId;
    private String tableName;
    private String schema;
    private Map<String, Object> filters;

    // NEW: For session continuation
    private String sessionKey;

    // NEW: For custom SQL execution
    private String sqlQuery;              // Custom SQL to execute
    private Map<String, Object> parameters; // SQL parameters for substitution

    public boolean isContinuation() {
        return sessionKey != null;
    }

    public boolean isTableQuery() {
        return tableName != null && sqlQuery == null;
    }

    public boolean isSqlQuery() {
        return sqlQuery != null && tableName == null;
    }
}
```

### 2. Enhance QuerySession Model

**Add SQL query state**:

```java
public class QuerySession {
    // Existing fields
    private String sessionKey;
    private String userId;
    private Long connectionId;
    private String tableName;
    private int pageSize;
    private long offset;
    private boolean useIteratorStrategy;
    private EntityRecordIterator<EntityRecord> iterator; // For table reading

    // NEW: For SQL query execution
    private String sqlQuery;                  // Original SQL
    private SQLResultIterator sqlIterator;    // Custom iterator for ResultSet
    private boolean isSqlQuery;               // Flag to distinguish from table query
}
```

### 3. Create SQLResultIterator

**Custom iterator for SQL query results with pagination**:

```java
package io.oneapi.admin.model;

import io.oneapi.sdk.database.JdbcDatabase;
import java.sql.*;
import java.util.*;

/**
 * Iterator for SQL query results that supports pagination.
 * Similar to EntityRecordIterator but for custom SQL queries.
 */
public class SQLResultIterator implements AutoCloseable {

    private final JdbcDatabase database;
    private final String sql;
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;
    private ResultSetMetaData metadata;
    private boolean hasNext;

    public SQLResultIterator(JdbcDatabase database, String sql) throws SQLException {
        this.database = database;
        this.sql = sql;
        initialize();
    }

    private void initialize() throws SQLException {
        this.connection = database.getConnection();
        this.statement = connection.createStatement(
            ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_READ_ONLY
        );

        // Set fetch size for memory efficiency
        statement.setFetchSize(100);

        this.resultSet = statement.executeQuery(sql);
        this.metadata = resultSet.getMetaData();
        this.hasNext = resultSet.next();
    }

    public boolean hasNext() {
        return hasNext;
    }

    public Map<String, Object> next() throws SQLException {
        if (!hasNext) {
            throw new NoSuchElementException("No more results");
        }

        Map<String, Object> record = new LinkedHashMap<>();
        int columnCount = metadata.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = metadata.getColumnName(i);
            Object value = resultSet.getObject(i);
            record.put(columnName, value);
        }

        // Advance to next row
        hasNext = resultSet.next();

        return record;
    }

    @Override
    public void close() throws SQLException {
        if (resultSet != null) resultSet.close();
        if (statement != null) statement.close();
        if (connection != null) connection.close();
    }
}
```

### 4. Enhance DatabaseQueryService

**Add custom SQL execution method**:

```java
@Service
@Transactional
public class DatabaseQueryService {

    // Existing fields...

    /**
     * UNIFIED QUERY ENDPOINT - ENHANCED
     * Now supports:
     * 1. Table reading (existing)
     * 2. Custom SQL execution (NEW)
     * 3. Session continuation for both
     */
    public QueryResponse queryData(QueryRequest request, String userId) {
        if (request.isContinuation()) {
            // Continue existing session (works for both table and SQL queries)
            return fetchNextPage(request.getSessionKey(), userId);
        } else if (request.isTableQuery()) {
            // Existing: Table reading with Source.read()
            return startNewTableQuery(request, userId);
        } else if (request.isSqlQuery()) {
            // NEW: Custom SQL execution
            return startNewSqlQuery(request, userId);
        } else {
            throw new IllegalArgumentException(
                "Request must contain either sessionKey, tableName, or sqlQuery"
            );
        }
    }

    /**
     * NEW: Start a new SQL query session.
     */
    private QueryResponse startNewSqlQuery(QueryRequest request, String userId) {
        log.info("Starting new SQL query: user={}, connection={}, sql={}",
                userId, request.getConnectionId(),
                request.getSqlQuery().substring(0, Math.min(50, request.getSqlQuery().length())));

        // Get user preferences
        int pageSize = userPreferencesService.getPageSize(userId);
        int ttlMinutes = userPreferencesService.getTtlMinutes(userId);

        // Create session
        QuerySession session = sessionManager.createSession(
                userId,
                request.getConnectionId(),
                null, // No table name for SQL queries
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
     * NEW: Fetch a page of data from SQL query.
     */
    private QueryResponse fetchSqlPage(QuerySession session, String userId) {
        DatabaseConnection connection = connectionRepository.findById(session.getConnectionId())
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        List<Map<String, Object>> records = new ArrayList<>();
        boolean hasMore = false;

        try {
            // Get or create SQL iterator
            if (session.getSqlIterator() == null) {
                // Create JdbcDatabase from connector
                Source source = connectorFactory.createSource(connection);
                JdbcDatabase database = getJdbcDatabase(source, connection);

                // Create iterator
                SQLResultIterator iterator = new SQLResultIterator(
                    database,
                    session.getSqlQuery()
                );
                session.setSqlIterator(iterator);
            }

            // Fetch page
            SQLResultIterator iterator = session.getSqlIterator();
            int count = 0;
            while (iterator.hasNext() && count < session.getPageSize()) {
                records.add(iterator.next());
                count++;
            }

            hasMore = iterator.hasNext();

            if (!hasMore) {
                // Close iterator
                iterator.close();
                session.setSqlIterator(null);
            }

        } catch (SQLException e) {
            log.error("Error executing SQL query", e);
            throw new RuntimeException("Failed to execute SQL query: " + e.getMessage(), e);
        }

        // Update session
        session.incrementRequestCount();
        session.addFetchedRecords(records.size());
        session.setHasMore(hasMore);

        int ttlMinutes = userPreferencesService.getTtlMinutes(userId);

        if (!hasMore) {
            sessionManager.closeSession(session.getSessionKey());
        } else {
            sessionManager.updateSession(session, ttlMinutes);
        }

        // Build response (same format as table queries)
        QueryResponse.QueryMetadata metadata = new QueryResponse.QueryMetadata(
                records.size(),
                session.getTotalFetched(),
                hasMore,
                hasMore ? session.getExpiresAt() : null,
                session.getPageSize(),
                session.getRequestCount()
        );

        return new QueryResponse(
                hasMore ? session.getSessionKey() : null,
                records,
                metadata
        );
    }

    /**
     * Helper: Extract JdbcDatabase from Source connector.
     */
    private JdbcDatabase getJdbcDatabase(Source source, DatabaseConnection connection)
            throws Exception {

        JsonNode config = connectorFactory.toConnectorConfig(connection);

        // All database sources are AbstractDatabaseSource
        if (source instanceof io.oneapi.sdk.base.AbstractDatabaseSource) {
            // Access the createDatabase method via reflection or add public accessor
            // Option 1: Add getDatabase() method to AbstractDatabaseSource
            // Option 2: Use reflection (temporary)

            Method method = source.getClass()
                .getSuperclass()
                .getDeclaredMethod("createDatabase", JsonNode.class);
            method.setAccessible(true);
            return (JdbcDatabase) method.invoke(source, config);
        }

        throw new UnsupportedOperationException(
            "Source does not support SQL query execution"
        );
    }

    /**
     * Existing method - rename for clarity
     */
    private QueryResponse startNewTableQuery(QueryRequest request, String userId) {
        // This is the existing startNewQuery() method
        // Just renamed for clarity
        // ... existing implementation ...
    }
}
```

### 5. Enhance SavedQueryService

**Add query execution methods**:

```java
@Service
@Transactional
public class SavedQueryService {

    private final SavedQueryRepository repository;
    private final DatabaseQueryService queryService;
    private final UserService userService;

    /**
     * Execute a saved query with pagination.
     */
    public QueryResponse executeSavedQuery(
            Long savedQueryId,
            Map<String, Object> parameters,
            String sessionKey,
            String userId) {

        // Load saved query
        SavedQuery savedQuery = repository.findById(savedQueryId)
            .orElseThrow(() -> new EntityNotFoundException("Saved query not found"));

        // Check permissions
        if (!savedQuery.getOwner().getLogin().equals(userId) && !savedQuery.getIsShared()) {
            throw new AccessDeniedException("Not authorized to execute this query");
        }

        // Substitute parameters in SQL
        String sql = substituteParameters(savedQuery.getSqlQuery(), parameters);

        // Build request
        QueryRequest request = new QueryRequest();

        if (sessionKey != null) {
            // Continue existing session
            request.setSessionKey(sessionKey);
        } else {
            // Start new session
            request.setConnectionId(savedQuery.getConnection().getId());
            request.setSqlQuery(sql);
        }

        // Execute via DatabaseQueryService
        return queryService.queryData(request, userId);
    }

    /**
     * Preview query (LIMIT 10, no session).
     */
    public List<Map<String, Object>> previewQuery(
            Long connectionId,
            String sql,
            String userId) {

        // Wrap SQL with LIMIT
        String previewSql = wrapWithLimit(sql, 10);

        // Build request
        QueryRequest request = new QueryRequest();
        request.setConnectionId(connectionId);
        request.setSqlQuery(previewSql);

        // Execute (will return first page only)
        QueryResponse response = queryService.queryData(request, userId);

        return response.getRecords();
    }

    private String substituteParameters(String sql, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return sql;
        }

        String result = sql;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = formatSqlValue(entry.getValue());
            result = result.replace(placeholder, value);
        }
        return result;
    }

    private String formatSqlValue(Object value) {
        if (value == null) {
            return "NULL";
        } else if (value instanceof String) {
            return "'" + ((String) value).replace("'", "''") + "'";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else {
            return "'" + value.toString().replace("'", "''") + "'";
        }
    }

    private String wrapWithLimit(String sql, int limit) {
        // Simple approach: append LIMIT
        // More robust: Use JSqlParser to parse and add LIMIT
        if (sql.toUpperCase().contains("LIMIT")) {
            return sql; // Already has LIMIT
        }
        return sql + " LIMIT " + limit;
    }
}
```

### 6. ReportExecutionService (Same as before)

Uses SavedQueryService to execute queries, then converts format.

---

## REST API Endpoints

### SavedQueryController - ENHANCED

```java
@RestController
@RequestMapping("/api/v1/queries")
public class SavedQueryController {

    /**
     * Execute saved query with session-based pagination.
     * First call: POST /api/v1/queries/123/execute (returns sessionKey)
     * Next pages: POST /api/v1/queries/123/execute?sessionKey=xxx
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<QueryResponse> executeQuery(
            @PathVariable Long id,
            @RequestParam(required = false) String sessionKey,
            @RequestBody(required = false) Map<String, Object> parameters,
            Principal principal) {

        QueryResponse response = savedQueryService.executeSavedQuery(
            id,
            parameters != null ? parameters : Map.of(),
            sessionKey,
            principal.getName()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Preview query (no pagination, LIMIT 10).
     */
    @PostMapping("/preview")
    public ResponseEntity<List<Map<String, Object>>> previewQuery(
            @RequestParam Long connectionId,
            @RequestBody String sql,
            Principal principal) {

        List<Map<String, Object>> results = savedQueryService.previewQuery(
            connectionId,
            sql,
            principal.getName()
        );

        return ResponseEntity.ok(results);
    }
}
```

---

## Benefits of This Approach

✅ **Reuses existing session infrastructure** - No duplication
✅ **Consistent pagination** - Same sessionKey mechanism for tables and SQL
✅ **Dual caching support** - Caffeine and Redis work for both
✅ **User preferences** - Page size and TTL respected
✅ **Session limits** - Existing user session limits apply
✅ **Minimal changes** - Only adds SQL execution path to existing service
✅ **Backward compatible** - Table reading still works the same way

---

## SDK Enhancement Required

**Option 1**: Add public method to `AbstractDatabaseSource`:

```java
public JdbcDatabase getDatabase(JsonNode config) throws Exception {
    return (JdbcDatabase) createDatabase(config);
}
```

**Option 2**: Use reflection (temporary workaround in code above)

---

## Implementation Timeline

### Phase 1: Core SQL Execution (1.5 hours)
1. Create SQLResultIterator
2. Enhance QueryRequest and QuerySession models
3. Add startNewSqlQuery() to DatabaseQueryService
4. Add fetchSqlPage() to DatabaseQueryService
5. Add getJdbcDatabase() helper
6. Test with PostgreSQL and H2

### Phase 2: SavedQuery Integration (1 hour)
1. Add executeSavedQuery() to SavedQueryService
2. Add previewQuery() to SavedQueryService
3. Add parameter substitution
4. Update SavedQueryController endpoints
5. Test saved query execution

### Phase 3: Report Execution (1 hour)
1. Implement ReportExecutionService
2. Implement ReportFormatService (JSON, CSV)
3. Add /run endpoint to ReportController
4. Test report execution

### Phase 4: Excel Export & Testing (30 minutes)
1. Add Apache POI dependency
2. Implement Excel conversion
3. Integration testing

**Total: 4 hours** (vs 3 hours in original plan, but more robust)

---

## Summary

This revised approach:
- ✅ **Leverages your existing session-based pagination infrastructure**
- ✅ **Reuses QuerySessionManager with dual caching (Caffeine/Redis)**
- ✅ **Adds SQL execution alongside table reading**
- ✅ **Maintains consistent pagination API (sessionKey-based)**
- ✅ **Minimal code changes to DatabaseQueryService**
- ✅ **Fully compatible with user preferences and session limits**

The key insight: Your `DatabaseQueryService` already has world-class pagination infrastructure. We just need to add a SQL execution path that leverages the same session management!
