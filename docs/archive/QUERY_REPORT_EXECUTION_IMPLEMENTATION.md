# Query & Report Execution Implementation Overview
**Using OneAPI SDK and Connectors**

---

## Architecture Overview

### Current SDK Capabilities

The OneAPI SDK provides:
- **`Source` interface**: High-level abstraction for database connectors
- **`JdbcDatabase` class**: Low-level JDBC operations with query execution
- **Connector implementations**: PostgreSQL, H2, SQL Server with database creation

### Key SDK Components for Query Execution

```java
// 1. Source Interface (High-level)
public interface Source {
    ConnectionStatus check(JsonNode config);
    Catalog discover(JsonNode config);
    EntityRecordIterator<EntityRecord> read(JsonNode config, Catalog catalog, State state);
}

// 2. JdbcDatabase (Low-level query execution)
public class JdbcDatabase {
    public <T> List<T> query(String sql, ResultSetHandler<T> handler);
    public <T> List<T> query(String sql, List<Object> params, ResultSetHandler<T> handler);
    public int execute(String sql);
    public Connection getConnection();
}

// 3. AbstractDatabaseSource (Base class for connectors)
protected abstract Database createDatabase(JsonNode config);
```

---

## Implementation Strategy

### Approach: Use SDK's JdbcDatabase for Query Execution

We will:
1. **Reuse ConnectorFactory** to get appropriate Source connector
2. **Access JdbcDatabase** from Source connector for low-level query execution
3. **Execute custom SQL queries** using JdbcDatabase.query()
4. **Convert ResultSets** to JSON/CSV/Excel formats

---

## Component Design

### 1. QueryExecutionService

**Purpose**: Execute ad-hoc and saved queries using SDK connectors

```java
@Service
@Slf4j
@Transactional
public class QueryExecutionService {

    private final ConnectorFactory connectorFactory;
    private final DatabaseConnectionRepository connectionRepository;
    private final AccessControlService accessControlService;

    /**
     * Execute a custom SQL query.
     *
     * @param connectionId Database connection ID
     * @param sql SQL query to execute
     * @param parameters Query parameters (for PreparedStatement)
     * @param currentUser Current authenticated user
     * @return QueryResult with rows and metadata
     */
    public QueryResult executeQuery(
            Long connectionId,
            String sql,
            Map<String, Object> parameters,
            User currentUser) throws Exception {

        // 1. Load connection
        DatabaseConnection connection = connectionRepository.findById(connectionId)
            .orElseThrow(() -> new EntityNotFoundException("Connection not found"));

        // 2. Check permissions
        accessControlService.checkDatabaseAccess(currentUser, connection);

        // 3. Get connector and create database instance
        Source source = connectorFactory.getSource(connection);
        JsonNode config = connectorFactory.toConnectorConfig(connection);
        JdbcDatabase database = createJdbcDatabase(source, config);

        // 4. Parse and validate SQL (extract tables)
        List<String> tables = extractTablesFromSql(sql);
        for (String table : tables) {
            accessControlService.checkTableAccess(currentUser, connection, table);
        }

        // 5. Substitute parameters in SQL
        String finalSql = substituteParameters(sql, parameters);

        // 6. Execute query using JdbcDatabase
        List<Map<String, Object>> rows = database.query(finalSql, rs -> {
            List<Map<String, Object>> results = new ArrayList<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }
            return results;
        });

        // 7. Extract metadata
        QueryMetadata metadata = extractMetadata(database, finalSql);

        // 8. Return result
        return new QueryResult(rows, metadata);
    }

    /**
     * Execute a saved query by ID.
     */
    public QueryResult executeSavedQuery(
            Long savedQueryId,
            Map<String, Object> parameters,
            User currentUser) throws Exception {

        SavedQuery savedQuery = savedQueryRepository.findById(savedQueryId)
            .orElseThrow(() -> new EntityNotFoundException("Saved query not found"));

        // Check ownership or sharing
        if (!savedQuery.getOwner().equals(currentUser) && !savedQuery.getIsShared()) {
            throw new AccessDeniedException("Not authorized to execute this query");
        }

        return executeQuery(
            savedQuery.getConnection().getId(),
            savedQuery.getSqlQuery(),
            parameters,
            currentUser
        );
    }

    /**
     * Preview query (limit 10 rows, validate syntax).
     */
    public QueryResult previewQuery(
            Long connectionId,
            String sql,
            User currentUser) throws Exception {

        // Wrap SQL with LIMIT
        String previewSql = wrapWithLimit(sql, 10);

        return executeQuery(connectionId, previewSql, Map.of(), currentUser);
    }

    // Helper methods

    private JdbcDatabase createJdbcDatabase(Source source, JsonNode config) throws Exception {
        // Sources are AbstractDatabaseSource instances
        // Call their createDatabase method (may need to add to Source interface)
        // For now, use reflection or create adapter

        if (source instanceof AbstractDatabaseSource) {
            AbstractDatabaseSource dbSource = (AbstractDatabaseSource) source;
            return (JdbcDatabase) dbSource.createDatabase(config);
        }

        throw new UnsupportedOperationException("Source does not support query execution");
    }

    private String substituteParameters(String sql, Map<String, Object> params) {
        // Replace {{paramName}} with actual values
        String result = sql;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = formatValue(entry.getValue());
            result = result.replace(placeholder, value);
        }
        return result;
    }

    private List<String> extractTablesFromSql(String sql) {
        // Simple regex-based SQL parser
        // Extract table names from FROM and JOIN clauses
        // Use JSqlParser library for robust parsing
        List<String> tables = new ArrayList<>();

        Pattern fromPattern = Pattern.compile("FROM\\s+([\\w.]+)", Pattern.CASE_INSENSITIVE);
        Pattern joinPattern = Pattern.compile("JOIN\\s+([\\w.]+)", Pattern.CASE_INSENSITIVE);

        Matcher fromMatcher = fromPattern.matcher(sql);
        while (fromMatcher.find()) {
            tables.add(fromMatcher.group(1));
        }

        Matcher joinMatcher = joinPattern.matcher(sql);
        while (joinMatcher.find()) {
            tables.add(joinMatcher.group(1));
        }

        return tables;
    }
}
```

---

### 2. ReportExecutionService

**Purpose**: Execute reports with parameter substitution and format conversion

```java
@Service
@Slf4j
@Transactional
public class ReportExecutionService {

    private final QueryExecutionService queryExecutionService;
    private final ReportRepository reportRepository;
    private final ReportFormatService formatService;

    /**
     * Execute a report and return results in specified format.
     */
    public ReportOutput executeReport(
            Long reportId,
            Map<String, Object> parameters,
            OutputFormat format,
            User currentUser) throws Exception {

        // 1. Load report
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new EntityNotFoundException("Report not found"));

        // 2. Check permissions
        if (!report.getOwner().equals(currentUser) && !report.getIsShared()) {
            throw new AccessDeniedException("Not authorized to run this report");
        }

        // 3. Merge report parameters with runtime parameters
        Map<String, Object> allParams = mergeParameters(
            report.getParameters(),
            parameters
        );

        // 4. Execute underlying query
        QueryResult result = queryExecutionService.executeSavedQuery(
            report.getSavedQuery().getId(),
            allParams,
            currentUser
        );

        // 5. Convert to requested format
        byte[] output;
        String mimeType;

        switch (format) {
            case JSON:
                output = formatService.toJson(result);
                mimeType = "application/json";
                break;
            case CSV:
                output = formatService.toCsv(result);
                mimeType = "text/csv";
                break;
            case EXCEL:
                output = formatService.toExcel(result);
                mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                break;
            default:
                output = formatService.toJson(result);
                mimeType = "application/json";
        }

        // 6. Log execution
        logReportExecution(report, currentUser, result.getRowCount());

        return new ReportOutput(
            report.getName(),
            format,
            mimeType,
            output,
            result.getRowCount()
        );
    }
}
```

---

### 3. ReportFormatService

**Purpose**: Convert query results to various output formats

```java
@Service
public class ReportFormatService {

    private final ObjectMapper objectMapper;

    /**
     * Convert QueryResult to JSON.
     */
    public byte[] toJson(QueryResult result) throws Exception {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("metadata", result.getMetadata());
        output.put("rows", result.getRows());
        output.put("rowCount", result.getRowCount());

        return objectMapper.writeValueAsBytes(output);
    }

    /**
     * Convert QueryResult to CSV using Apache Commons CSV.
     */
    public byte[] toCsv(QueryResult result) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);

        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            // Write header
            if (!result.getRows().isEmpty()) {
                Map<String, Object> firstRow = result.getRows().get(0);
                printer.printRecord(firstRow.keySet());

                // Write data rows
                for (Map<String, Object> row : result.getRows()) {
                    printer.printRecord(row.values());
                }
            }
        }

        return out.toByteArray();
    }

    /**
     * Convert QueryResult to Excel using Apache POI.
     */
    public byte[] toExcel(QueryResult result) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Report");

            // Header row
            if (!result.getRows().isEmpty()) {
                Map<String, Object> firstRow = result.getRows().get(0);
                Row headerRow = sheet.createRow(0);
                int colIndex = 0;
                for (String columnName : firstRow.keySet()) {
                    Cell cell = headerRow.createCell(colIndex++);
                    cell.setCellValue(columnName);
                }

                // Data rows
                int rowIndex = 1;
                for (Map<String, Object> dataRow : result.getRows()) {
                    Row excelRow = sheet.createRow(rowIndex++);
                    colIndex = 0;
                    for (Object value : dataRow.values()) {
                        Cell cell = excelRow.createCell(colIndex++);
                        setCellValue(cell, value);
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
```

---

## SDK Enhancement Required

### Option 1: Add Query Execution to Source Interface

```java
public interface Source {
    // Existing methods
    ConnectionStatus check(JsonNode config);
    Catalog discover(JsonNode config);
    EntityRecordIterator<EntityRecord> read(JsonNode config, Catalog catalog, State state);

    // NEW: Add query execution method
    QueryResult executeQuery(JsonNode config, String sql, List<Object> parameters) throws Exception;
}
```

### Option 2: Expose Database from AbstractDatabaseSource

```java
public abstract class AbstractDatabaseSource {
    // NEW: Public method to get JdbcDatabase
    public JdbcDatabase getDatabase(JsonNode config) throws Exception {
        return (JdbcDatabase) createDatabase(config);
    }
}
```

**Recommendation**: **Option 2** is simpler and doesn't break the Source interface contract.

---

## REST API Endpoints

### SavedQueryController

```java
@RestController
@RequestMapping("/api/v1/queries")
public class SavedQueryController {

    @PostMapping("/{id}/execute")
    public ResponseEntity<QueryResult> executeQuery(
            @PathVariable Long id,
            @RequestBody Map<String, Object> parameters,
            Principal principal) {

        User user = userService.getUserByLogin(principal.getName());
        QueryResult result = queryExecutionService.executeSavedQuery(id, parameters, user);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/preview")
    public ResponseEntity<QueryResult> previewQuery(
            @RequestParam Long connectionId,
            @RequestBody String sql,
            Principal principal) {

        User user = userService.getUserByLogin(principal.getName());
        QueryResult result = queryExecutionService.previewQuery(connectionId, sql, user);
        return ResponseEntity.ok(result);
    }
}
```

### ReportController

```java
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    @PostMapping("/{id}/run")
    public ResponseEntity<byte[]> runReport(
            @PathVariable Long id,
            @RequestParam(defaultValue = "JSON") OutputFormat format,
            @RequestBody(required = false) Map<String, Object> parameters,
            Principal principal) {

        User user = userService.getUserByLogin(principal.getName());
        ReportOutput output = reportExecutionService.executeReport(
            id,
            parameters != null ? parameters : Map.of(),
            format,
            user
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(output.getMimeType()));
        headers.setContentDisposition(
            ContentDisposition.attachment()
                .filename(output.getFileName())
                .build()
        );

        return ResponseEntity.ok()
            .headers(headers)
            .body(output.getData());
    }
}
```

---

## Dependencies Required

### Add to pom.xml

```xml
<!-- Apache Commons CSV for CSV export -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-csv</artifactId>
    <version>1.10.0</version>
</dependency>

<!-- Apache POI for Excel export (optional) -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>

<!-- JSqlParser for SQL parsing (optional, for table extraction) -->
<dependency>
    <groupId>com.github.jsqlparser</groupId>
    <artifactId>jsqlparser</artifactId>
    <version>4.7</version>
</dependency>
```

---

## Implementation Timeline

### Phase 1: Query Execution (1 hour)
1. Enhance AbstractDatabaseSource to expose getDatabase()
2. Implement QueryExecutionService
3. Add execute and preview endpoints to SavedQueryController
4. Test with PostgreSQL and H2

### Phase 2: Report Execution (1 hour)
1. Implement ReportExecutionService
2. Implement ReportFormatService (JSON, CSV)
3. Add /run endpoint to ReportController
4. Test report execution with parameters

### Phase 3: Excel Export (30 minutes)
1. Add Apache POI dependency
2. Implement Excel conversion in ReportFormatService
3. Test Excel download

### Phase 4: Integration Testing (30 minutes)
1. Test all formats (JSON, CSV, Excel)
2. Test parameter substitution
3. Test permission enforcement
4. Test with multiple databases

---

## Security Considerations

1. **SQL Injection Prevention**:
   - Use PreparedStatement for user-provided values
   - Validate and sanitize SQL before execution
   - Use JSqlParser for safe SQL parsing

2. **Permission Enforcement**:
   - Check database-level permissions
   - Check table-level permissions (extracted from SQL)
   - Verify query ownership for saved queries

3. **Resource Limits**:
   - Set query timeout (30 seconds default)
   - Limit result set size (10,000 rows max)
   - Implement pagination for large results

4. **Audit Logging**:
   - Log all query executions
   - Track who executed what query
   - Record execution time and row count

---

## Summary

This implementation:
- ✅ Uses OneAPI SDK and connectors
- ✅ Leverages JdbcDatabase for query execution
- ✅ Supports multiple output formats (JSON, CSV, Excel)
- ✅ Enforces permissions at database and table level
- ✅ Provides query preview functionality
- ✅ Supports parameterized queries and reports
- ✅ Minimal SDK changes required (expose getDatabase method)

**Total Implementation Time**: ~3 hours
