# Metadata Discovery Automation - Implementation Strategy

## Overview
This document outlines the implementation strategy for automatic metadata discovery and cataloging of database schemas.

## Phase 3 Implementation Plan

### 1. Data Model Extensions

#### New Entities to Create:

```java
@Entity
@Table(name = "schema_metadata")
public class SchemaMetadata {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "connection_id")
    private DatabaseConnection connection;

    private String catalogName;
    private String schemaName;

    private Integer tableCount;
    private Integer viewCount;
    private Integer functionCount;

    private LocalDateTime discoveredAt;
    private LocalDateTime lastSyncedAt;

    @OneToMany(mappedBy = "schema", cascade = CascadeType.ALL)
    private Set<TableMetadata> tables;
}

@Entity
@Table(name = "table_metadata")
public class TableMetadata {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "schema_id")
    private SchemaMetadata schema;

    private String tableName;

    @Enumerated(EnumType.STRING)
    private TableType tableType; // TABLE, VIEW, MATERIALIZED_VIEW

    private Long estimatedRowCount;
    private Long sizeInBytes;
    private String tableComment;

    private LocalDateTime discoveredAt;
    private LocalDateTime lastSyncedAt;

    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL)
    private Set<ColumnMetadata> columns;

    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL)
    private Set<IndexMetadata> indexes;

    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL)
    private Set<ForeignKeyMetadata> foreignKeys;
}

@Entity
@Table(name = "column_metadata")
public class ColumnMetadata {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "table_id")
    private TableMetadata table;

    private String columnName;
    private String dataType;
    private String jdbcType;
    private Integer columnSize;
    private Integer decimalDigits;
    private Boolean nullable;
    private String defaultValue;
    private String columnComment;

    private Boolean isPrimaryKey;
    private Boolean isForeignKey;
    private Boolean isUnique;
    private Boolean isIndexed;
    private Boolean isAutoIncrement;

    private Integer ordinalPosition;

    private LocalDateTime discoveredAt;
}

@Entity
@Table(name = "index_metadata")
public class IndexMetadata {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "table_id")
    private TableMetadata table;

    private String indexName;

    @Enumerated(EnumType.STRING)
    private IndexType indexType; // BTREE, HASH, UNIQUE, PRIMARY, FOREIGN

    private Boolean isUnique;
    private Boolean isPrimary;

    @Column(columnDefinition = "TEXT")
    private String columns; // JSON array of column names

    private String filterCondition;

    private LocalDateTime discoveredAt;
}

@Entity
@Table(name = "foreign_key_metadata")
public class ForeignKeyMetadata {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "table_id")
    private TableMetadata table;

    private String constraintName;
    private String referencedSchema;
    private String referencedTable;

    @Column(columnDefinition = "TEXT")
    private String columnMappings; // JSON: [{"from": "user_id", "to": "id"}]

    @Enumerated(EnumType.STRING)
    private ForeignKeyAction onDelete; // CASCADE, SET_NULL, RESTRICT, NO_ACTION

    @Enumerated(EnumType.STRING)
    private ForeignKeyAction onUpdate;

    private LocalDateTime discoveredAt;
}

@Entity
@Table(name = "metadata_version")
public class MetadataVersion {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "connection_id")
    private DatabaseConnection connection;

    private Integer version;
    private LocalDateTime discoveredAt;

    private Boolean changesDetected;

    @Column(columnDefinition = "TEXT")
    private String changesSummary; // JSON summary of changes

    @Column(columnDefinition = "TEXT")
    private String changeDetails; // Full JSON diff
}
```

---

### 2. Discovery Service Implementation

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class MetadataDiscoveryService {

    private final DatabaseConnectionRepository connectionRepository;
    private final SchemaMetadataRepository schemaRepository;
    private final TableMetadataRepository tableRepository;
    private final ColumnMetadataRepository columnRepository;
    private final MetadataVersionRepository versionRepository;

    /**
     * Discover all metadata for a database connection
     */
    @Transactional
    public DiscoveryResult discoverMetadata(Long connectionId) {
        log.info("Starting metadata discovery for connection: {}", connectionId);

        DatabaseConnection connection = connectionRepository.findById(connectionId)
            .orElseThrow(() -> new IllegalArgumentException("Connection not found"));

        try (Connection jdbcConnection = createConnection(connection)) {
            DatabaseMetaData metaData = jdbcConnection.getMetaData();

            // 1. Discover schemas
            List<SchemaMetadata> schemas = discoverSchemas(connection, metaData);

            // 2. Discover tables for each schema
            for (SchemaMetadata schema : schemas) {
                List<TableMetadata> tables = discoverTables(schema, metaData);

                // 3. Discover columns for each table
                for (TableMetadata table : tables) {
                    discoverColumns(table, metaData);
                    discoverIndexes(table, metaData);
                    discoverForeignKeys(table, metaData);
                }
            }

            // 4. Create version snapshot
            MetadataVersion version = createVersionSnapshot(connection);

            log.info("Metadata discovery completed for connection: {}", connectionId);
            return new DiscoveryResult(schemas, version);

        } catch (SQLException e) {
            log.error("Error discovering metadata", e);
            throw new RuntimeException("Metadata discovery failed", e);
        }
    }

    /**
     * Discover schemas/catalogs
     */
    private List<SchemaMetadata> discoverSchemas(
            DatabaseConnection connection,
            DatabaseMetaData metaData) throws SQLException {

        List<SchemaMetadata> schemas = new ArrayList<>();

        ResultSet rs = metaData.getSchemas();
        while (rs.next()) {
            String schemaName = rs.getString("TABLE_SCHEM");
            String catalogName = rs.getString("TABLE_CATALOG");

            SchemaMetadata schema = new SchemaMetadata();
            schema.setConnection(connection);
            schema.setSchemaName(schemaName);
            schema.setCatalogName(catalogName);
            schema.setDiscoveredAt(LocalDateTime.now());

            schemas.add(schemaRepository.save(schema));
        }

        return schemas;
    }

    /**
     * Discover tables and views
     */
    private List<TableMetadata> discoverTables(
            SchemaMetadata schema,
            DatabaseMetaData metaData) throws SQLException {

        List<TableMetadata> tables = new ArrayList<>();

        ResultSet rs = metaData.getTables(
            schema.getCatalogName(),
            schema.getSchemaName(),
            "%",
            new String[]{"TABLE", "VIEW"}
        );

        while (rs.next()) {
            TableMetadata table = new TableMetadata();
            table.setSchema(schema);
            table.setTableName(rs.getString("TABLE_NAME"));
            table.setTableType(TableType.valueOf(rs.getString("TABLE_TYPE")));
            table.setTableComment(rs.getString("REMARKS"));
            table.setDiscoveredAt(LocalDateTime.now());

            tables.add(tableRepository.save(table));
        }

        return tables;
    }

    /**
     * Discover columns with full metadata
     */
    private void discoverColumns(
            TableMetadata table,
            DatabaseMetaData metaData) throws SQLException {

        ResultSet rs = metaData.getColumns(
            table.getSchema().getCatalogName(),
            table.getSchema().getSchemaName(),
            table.getTableName(),
            "%"
        );

        // Get primary keys for this table
        Set<String> primaryKeys = getPrimaryKeys(table, metaData);

        while (rs.next()) {
            ColumnMetadata column = new ColumnMetadata();
            column.setTable(table);
            column.setColumnName(rs.getString("COLUMN_NAME"));
            column.setDataType(rs.getString("TYPE_NAME"));
            column.setJdbcType(String.valueOf(rs.getInt("DATA_TYPE")));
            column.setColumnSize(rs.getInt("COLUMN_SIZE"));
            column.setDecimalDigits(rs.getInt("DECIMAL_DIGITS"));
            column.setNullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
            column.setDefaultValue(rs.getString("COLUMN_DEF"));
            column.setColumnComment(rs.getString("REMARKS"));
            column.setOrdinalPosition(rs.getInt("ORDINAL_POSITION"));
            column.setIsAutoIncrement(rs.getString("IS_AUTOINCREMENT").equals("YES"));

            // Check if primary key
            column.setIsPrimaryKey(primaryKeys.contains(column.getColumnName()));

            column.setDiscoveredAt(LocalDateTime.now());

            columnRepository.save(column);
        }
    }

    /**
     * Discover indexes
     */
    private void discoverIndexes(
            TableMetadata table,
            DatabaseMetaData metaData) throws SQLException {

        ResultSet rs = metaData.getIndexInfo(
            table.getSchema().getCatalogName(),
            table.getSchema().getSchemaName(),
            table.getTableName(),
            false, // unique only = false
            true   // approximate = true
        );

        Map<String, List<String>> indexColumns = new HashMap<>();
        Map<String, Boolean> indexUnique = new HashMap<>();

        while (rs.next()) {
            String indexName = rs.getString("INDEX_NAME");
            if (indexName == null) continue;

            String columnName = rs.getString("COLUMN_NAME");
            boolean nonUnique = rs.getBoolean("NON_UNIQUE");

            indexColumns.computeIfAbsent(indexName, k -> new ArrayList<>()).add(columnName);
            indexUnique.putIfAbsent(indexName, !nonUnique);
        }

        // Create IndexMetadata entries
        for (Map.Entry<String, List<String>> entry : indexColumns.entrySet()) {
            IndexMetadata index = new IndexMetadata();
            index.setTable(table);
            index.setIndexName(entry.getKey());
            index.setIsUnique(indexUnique.get(entry.getKey()));
            index.setColumns(objectMapper.writeValueAsString(entry.getValue()));
            index.setDiscoveredAt(LocalDateTime.now());

            // Index type detection would be database-specific
            // For now, default to BTREE
            index.setIndexType(IndexType.BTREE);
        }
    }

    /**
     * Discover foreign keys
     */
    private void discoverForeignKeys(
            TableMetadata table,
            DatabaseMetaData metaData) throws SQLException {

        ResultSet rs = metaData.getImportedKeys(
            table.getSchema().getCatalogName(),
            table.getSchema().getSchemaName(),
            table.getTableName()
        );

        Map<String, ForeignKeyMetadata> foreignKeys = new HashMap<>();

        while (rs.next()) {
            String fkName = rs.getString("FK_NAME");

            ForeignKeyMetadata fk = foreignKeys.computeIfAbsent(fkName, k -> {
                ForeignKeyMetadata newFk = new ForeignKeyMetadata();
                newFk.setTable(table);
                newFk.setConstraintName(fkName);
                newFk.setReferencedSchema(rs.getString("PKTABLE_SCHEM"));
                newFk.setReferencedTable(rs.getString("PKTABLE_NAME"));
                newFk.setDiscoveredAt(LocalDateTime.now());
                return newFk;
            });

            // Build column mappings
            String fromColumn = rs.getString("FKCOLUMN_NAME");
            String toColumn = rs.getString("PKCOLUMN_NAME");
            // Add to mappings JSON
        }
    }

    /**
     * Get primary key columns for a table
     */
    private Set<String> getPrimaryKeys(
            TableMetadata table,
            DatabaseMetaData metaData) throws SQLException {

        Set<String> primaryKeys = new HashSet<>();

        ResultSet rs = metaData.getPrimaryKeys(
            table.getSchema().getCatalogName(),
            table.getSchema().getSchemaName(),
            table.getTableName()
        );

        while (rs.next()) {
            primaryKeys.add(rs.getString("COLUMN_NAME"));
        }

        return primaryKeys;
    }
}
```

---

### 3. Database-Specific Optimizations

```java
@Service
@Slf4j
public class DatabaseSpecificMetadataService {

    /**
     * Get row count using database-specific optimized queries
     */
    public Long getTableRowCount(DatabaseConnection connection, TableMetadata table) {
        String query = switch (connection.getType()) {
            case POSTGRESQL ->
                "SELECT reltuples::BIGINT FROM pg_class WHERE relname = ?";
            case MYSQL ->
                "SELECT table_rows FROM information_schema.tables " +
                "WHERE table_schema = ? AND table_name = ?";
            case ORACLE ->
                "SELECT num_rows FROM all_tables WHERE table_name = ?";
            case SQL_SERVER ->
                "SELECT SUM(p.rows) FROM sys.partitions p " +
                "JOIN sys.tables t ON p.object_id = t.object_id " +
                "WHERE t.name = ?";
            default ->
                "SELECT COUNT(*) FROM " + table.getTableName();
        };

        // Execute and return count
    }

    /**
     * Get table size using database-specific queries
     */
    public Long getTableSize(DatabaseConnection connection, TableMetadata table) {
        String query = switch (connection.getType()) {
            case POSTGRESQL ->
                "SELECT pg_total_relation_size(?)";
            case MYSQL ->
                "SELECT (data_length + index_length) " +
                "FROM information_schema.tables " +
                "WHERE table_schema = ? AND table_name = ?";
            case ORACLE ->
                "SELECT bytes FROM user_segments WHERE segment_name = ?";
            case SQL_SERVER ->
                "SELECT SUM(a.total_pages) * 8 * 1024 " +
                "FROM sys.tables t " +
                "JOIN sys.partitions p ON t.object_id = p.object_id " +
                "JOIN sys.allocation_units a ON p.partition_id = a.container_id " +
                "WHERE t.name = ?";
            default -> null;
        };

        // Execute and return size
    }
}
```

---

### 4. Change Detection Service

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class SchemaChangeDetectionService {

    private final MetadataVersionRepository versionRepository;
    private final TableMetadataRepository tableRepository;

    /**
     * Detect changes between current and previous metadata version
     */
    public ChangeDetectionResult detectChanges(Long connectionId) {
        // Get previous version
        MetadataVersion previousVersion = versionRepository
            .findLatestByConnection(connectionId)
            .orElse(null);

        if (previousVersion == null) {
            return ChangeDetectionResult.noChanges("First discovery");
        }

        // Discover current metadata
        DiscoveryResult current = metadataDiscoveryService.discoverMetadata(connectionId);

        // Compare
        List<SchemaChange> changes = new ArrayList<>();

        // Detect added tables
        // Detect removed tables
        // Detect modified tables
        // Detect added/removed/modified columns

        return new ChangeDetectionResult(changes);
    }

    @Data
    public static class SchemaChange {
        private ChangeType type; // ADDED, REMOVED, MODIFIED
        private EntityType entityType; // SCHEMA, TABLE, COLUMN, INDEX
        private String entityName;
        private String details;
        private LocalDateTime detectedAt;
    }
}
```

---

### 5. REST API Endpoints

```java
@RestController
@RequestMapping("/api/v1/metadata")
@Tag(name = "Metadata Discovery")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class MetadataDiscoveryController {

    private final MetadataDiscoveryService discoveryService;
    private final SchemaChangeDetectionService changeDetectionService;

    @PostMapping("/discover/{connectionId}")
    @Operation(summary = "Discover metadata for a connection")
    public ResponseEntity<DiscoveryResult> discover(@PathVariable Long connectionId) {
        DiscoveryResult result = discoveryService.discoverMetadata(connectionId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/sync/{connectionId}")
    @Operation(summary = "Sync and detect changes")
    public ResponseEntity<ChangeDetectionResult> sync(@PathVariable Long connectionId) {
        ChangeDetectionResult result = changeDetectionService.detectChanges(connectionId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/schemas/{connectionId}")
    @Operation(summary = "Get all schemas for a connection")
    public ResponseEntity<List<SchemaMetadataDTO>> getSchemas(@PathVariable Long connectionId) {
        // Implementation
    }

    @GetMapping("/tables/{connectionId}")
    @Operation(summary = "Get all tables for a connection")
    public ResponseEntity<List<TableMetadataDTO>> getTables(
            @PathVariable Long connectionId,
            @RequestParam(required = false) String schema) {
        // Implementation
    }

    @GetMapping("/table/{tableId}/columns")
    @Operation(summary = "Get columns for a table")
    public ResponseEntity<List<ColumnMetadataDTO>> getColumns(@PathVariable Long tableId) {
        // Implementation
    }
}
```

---

### 6. Scheduled Background Sync

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class MetadataSyncScheduler {

    private final DatabaseConnectionRepository connectionRepository;
    private final SchemaChangeDetectionService changeDetectionService;

    /**
     * Sync metadata every 6 hours
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000)
    public void syncAllConnections() {
        log.info("Starting scheduled metadata sync");

        List<DatabaseConnection> connections = connectionRepository
            .findByAutoSyncEnabledTrue();

        for (DatabaseConnection connection : connections) {
            try {
                changeDetectionService.detectChanges(connection.getId());
            } catch (Exception e) {
                log.error("Failed to sync connection: {}", connection.getId(), e);
            }
        }

        log.info("Completed scheduled metadata sync");
    }
}
```

---

## Implementation Timeline

| Task | Estimated Time |
|------|---------------|
| Create metadata entities | 1 hour |
| Implement discovery service | 2 hours |
| Add database-specific optimizations | 1 hour |
| Implement change detection | 1.5 hours |
| Create REST APIs | 1 hour |
| Add scheduled sync | 0.5 hours |
| Testing | 1 hour |
| **Total** | **8 hours** |

---

## Benefits

1. **Automatic cataloging** - No manual metadata entry
2. **Change tracking** - Know when schemas change
3. **Performance** - Efficient discovery using JDBC metadata
4. **Multi-database** - Works with all JDBC-compliant databases
5. **Background sync** - Keep metadata up-to-date automatically

---

## Next Steps

After implementing this, you can:
1. Build query builders using discovered metadata
2. Implement column-level lineage tracking
3. Add data profiling (min/max/avg/null counts)
4. Create ER diagrams from foreign key relationships
5. Build intelligent query suggestions

