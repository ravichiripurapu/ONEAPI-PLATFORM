# Query/Report Execution Implementation Status

## ✅ Completed (Phase 1)

### 1. SQLResultIterator - DONE
**File**: `oneapi-app/src/main/java/io/oneapi/admin/model/SQLResultIterator.java`
- Custom iterator for SQL query ResultSet
- Supports pagination through large result sets
- Auto-closeable with proper resource cleanup
- Caches column names for efficiency
- Set fetch size (100) for memory efficiency
- Set query timeout (30 seconds)

### 2. QueryRequest Enhanced - DONE
**File**: `oneapi-app/src/main/java/io/oneapi/admin/model/QueryRequest.java`
- Added `sqlQuery` field for custom SQL
- Added `parameters` field for SQL parameter substitution
- Added `isSqlQuery()` method to distinguish SQL vs table queries
- Added `isTableQuery()` method
- Maintains backward compatibility with existing table queries

### 3. QuerySession Enhanced - DONE
**File**: `oneapi-app/src/main/java/io/oneapi/admin/model/QuerySession.java`
- Added `sqlQuery` field
- Added `isSqlQuery` flag
- Added `SQLResultIterator sqlIterator` (transient)
- Added `boolean sqlIteratorActive` (transient)
- Added `closeSqlIterator()` method
- Updated `close()` to cleanup SQL iterator
- All getters/setters added

---

## ⏳ In Progress (Phase 2)

### 4. DatabaseQueryService Enhancement - IN PROGRESS
**File**: `oneapi-app/src/main/java/io/oneapi/admin/service/DatabaseQueryService.java`

**What needs to be added:**

```java
/**
 * ENHANCED queryData method to support both table and SQL queries
 */
public QueryResponse queryData(QueryRequest request, String userId) {
    if (request.isContinuation()) {
        return fetchNextPage(request.getSessionKey(), userId);
    } else if (request.isTableQuery()) {
        return startNewQuery(request, userId); // EXISTING
    } else if (request.isSqlQuery()) {
        return startNewSqlQuery(request, userId); // NEW
    } else {
        throw new IllegalArgumentException(...);
    }
}

/**
 * NEW: Start a new SQL query session
 */
private QueryResponse startNewSqlQuery(QueryRequest request, String userId) {
    // Get user preferences
    int pageSize = userPreferencesService.getPageSize(userId);
    int ttlMinutes = userPreferencesService.getTtlMinutes(userId);

    // Create session
    QuerySession session = sessionManager.createSession(...);
    session.setSqlQuery(request.getSqlQuery());
    session.setIsSqlQuery(true);

    // Fetch first page
    return fetchSqlPage(session, userId);
}

/**
 * NEW: Fetch a page of SQL query results
 */
private QueryResponse fetchSqlPage(QuerySession session, String userId) {
    DatabaseConnection connection = ...;
    List<Map<String, Object>> records = new ArrayList<>();
    boolean hasMore = false;

    try {
        // Get or create SQL iterator
        if (session.getSqlIterator() == null) {
            Source source = connectorFactory.createSource(connection);
            JdbcDatabase database = getJdbcDatabase(source, connection);
            SQLResultIterator iterator = new SQLResultIterator(database, session.getSqlQuery());
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
            iterator.close();
            session.setSqlIterator(null);
        }
    } catch (SQLException e) {
        throw new RuntimeException("Failed to execute SQL: " + e.getMessage(), e);
    }

    // Update session and return response (same as table queries)
    ...
}

/**
 * NEW: Helper to extract JdbcDatabase from Source
 */
private JdbcDatabase getJdbcDatabase(Source source, DatabaseConnection connection) throws Exception {
    JsonNode config = buildConfig(connection);

    if (source instanceof io.oneapi.sdk.base.AbstractDatabaseSource) {
        // Use reflection to access createDatabase method
        Method method = source.getClass()
            .getSuperclass()
            .getDeclaredMethod("createDatabase", JsonNode.class);
        method.setAccessible(true);
        return (JdbcDatabase) method.invoke(source, config);
    }

    throw new UnsupportedOperationException("Source does not support SQL queries");
}
```

---

## 📝 Pending (Phase 3)

### 5. SavedQueryService Implementation
**File**: `oneapi-app/src/main/java/io/oneapi/admin/service/reporting/SavedQueryService.java`

Currently has basic CRUD. Needs to add:

```java
/**
 * Execute a saved query with parameters
 */
public QueryResponse executeSavedQuery(
        Long savedQueryId,
        Map<String, Object> parameters,
        String sessionKey,
        Principal principal) {

    SavedQuery query = findById(savedQueryId);

    // Check permissions
    if (!canExecute(query, principal)) {
        throw new AccessDeniedException(...);
    }

    // Substitute parameters
    String sql = substituteParameters(query.getQueryText(), parameters);

    // Build request
    QueryRequest request = new QueryRequest();
    if (sessionKey != null) {
        request.setSessionKey(sessionKey);
    } else {
        request.setConnectionId(query.getConnection().getId());
        request.setSqlQuery(sql);
    }

    // Execute via DatabaseQueryService
    QueryResponse response = databaseQueryService.queryData(request, principal.getName());

    // Update execution metrics
    query.setExecutionCount(query.getExecutionCount() + 1);
    query.setLastExecutedAt(LocalDateTime.now());
    update(query);

    return response;
}

/**
 * Preview query (LIMIT 10)
 */
public List<Map<String, Object>> previewQuery(Long connectionId, String sql, Principal principal) {
    String previewSql = wrapWithLimit(sql, 10);

    QueryRequest request = new QueryRequest();
    request.setConnectionId(connectionId);
    request.setSqlQuery(previewSql);

    QueryResponse response = databaseQueryService.queryData(request, principal.getName());
    return response.getRecords();
}

private String substituteParameters(String sql, Map<String, Object> params) {
    String result = sql;
    for (Map.Entry<String, Object> entry : params.entrySet()) {
        String placeholder = "{{" + entry.getKey() + "}}";
        String value = formatSqlValue(entry.getValue());
        result = result.replace(placeholder, value);
    }
    return result;
}

private String formatSqlValue(Object value) {
    if (value == null) return "NULL";
    if (value instanceof String) return "'" + ((String) value).replace("'", "''") + "'";
    if (value instanceof Number || value instanceof Boolean) return value.toString();
    return "'" + value.toString().replace("'", "''") + "'";
}

private String wrapWithLimit(String sql, int limit) {
    if (sql.toUpperCase().contains("LIMIT")) return sql;
    return sql + " LIMIT " + limit;
}
```

### 6. SavedQueryController Updates
**File**: `oneapi-app/src/main/java/io/oneapi/admin/controller/reporting/SavedQueryController.java`

Add endpoints:

```java
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
        principal
    );

    return ResponseEntity.ok(response);
}

@PostMapping("/preview")
public ResponseEntity<List<Map<String, Object>>> previewQuery(
        @RequestParam Long connectionId,
        @RequestBody String sql,
        Principal principal) {

    List<Map<String, Object>> results = savedQueryService.previewQuery(
        connectionId,
        sql,
        principal
    );

    return ResponseEntity.ok(results);
}
```

---

## 🔮 Future Phases

### Phase 4: Report Format Conversion
- ReportFormatService (JSON, CSV, Excel)
- ReportExecutionService
- Add dependencies: Apache Commons CSV, Apache POI

### Phase 5: Testing
- Test SQL query execution with H2
- Test session-based pagination
- Test parameter substitution
- Integration testing

---

## Current Status Summary

**Completed**: 60%
- ✅ SQLResultIterator
- ✅ QueryRequest enhanced
- ✅ QuerySession enhanced
- ⏳ DatabaseQueryService (50% - needs SQL execution methods)

**Next Steps**:
1. Complete DatabaseQueryService SQL execution methods
2. Enhance SavedQueryService with execute/preview
3. Update SavedQueryController endpoints
4. Add dependencies to pom.xml
5. Compile and test

**Estimated Time Remaining**: 2-3 hours
