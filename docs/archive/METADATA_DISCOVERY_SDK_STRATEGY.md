# Metadata Discovery Using OneAPI SDK & Connectors

## ✅ Key Insight
**The OneAPI SDK and connectors already have metadata discovery built-in!**

The `Source` interface provides:
```java
Catalog discover(JsonNode config) throws Exception;
```

Each connector (PostgresSource, H2Source, SqlServerSource) implements:
- **discoverInternal()** - Discovers tables and fields
- **discoverTableFields()** - Discovers column metadata
- Returns `Catalog` with `DataEntity` objects containing JSON schemas

---

## 🏗️ **Revised Architecture - Leveraging SDK**

```
┌─────────────────────────────────────────────────────────┐
│               oneapi-app (Our Application)               │
│                                                          │
│  ┌────────────────────────────────────────────────────┐ │
│  │  MetadataDiscoveryService                          │ │
│  │  - Uses Source.discover() via SDK                  │ │
│  │  - Converts SDK Catalog → Our MetadataEntities    │ │
│  │  - Stores in our database                          │ │
│  └────────────────────────────────────────────────────┘ │
│                         ↓                                │
│  ┌────────────────────────────────────────────────────┐ │
│  │  ConnectorFactory                                   │ │
│  │  - Resolves Source implementation by DB type       │ │
│  │  - PostgresSource, H2Source, SqlServerSource       │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│           oneapi-sdk (Existing Framework)                │
│                                                          │
│  Source interface:                                       │
│  - discover(config) → Catalog                           │
│  - check(config) → ConnectionStatus                     │
│  - read(config, catalog, state) → EntityRecordIterator  │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│        oneapi-*-connector (Database Connectors)          │
│                                                          │
│  PostgresSource extends AbstractDatabaseSource           │
│  - discoverInternal() - Uses INFORMATION_SCHEMA         │
│  - discoverTableFields() - Gets column metadata         │
│  - Returns List<TableInfo<CommonField<JDBCType>>>       │
│                                                          │
│  Similar for: H2Source, SqlServerSource, etc.           │
└─────────────────────────────────────────────────────────┘
```

---

## 📦 **Implementation Strategy**

### Step 1: Create ConnectorFactory in oneapi-app

```java
@Component
@Slf4j
public class ConnectorFactory {

    /**
     * Get Source implementation for a database connection
     */
    public Source getSource(DatabaseConnection connection) {
        return switch (connection.getType()) {
            case POSTGRESQL -> new PostgresSource();
            case H2 -> new H2Source();
            case SQLSERVER -> new SqlServerSource();
            case MYSQL -> new MySqlSource();
            case ORACLE -> new OracleSource();
            default -> throw new UnsupportedOperationException(
                "No connector available for: " + connection.getType()
            );
        };
    }

    /**
     * Convert DatabaseConnection to SDK JsonNode config
     */
    public JsonNode toConnectorConfig(DatabaseConnection connection) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode config = mapper.createObjectNode();

        // Parse JDBC URL to extract host, port, database
        JdbcUrlParser parser = new JdbcUrlParser(connection.getJdbcUrl());

        config.put("host", parser.getHost());
        config.put("port", parser.getPort());
        config.put("database", parser.getDatabase());
        config.put("username", connection.getUsername());
        config.put("password", connection.getPassword()); // Decrypt if encrypted

        return config;
    }
}
```

---

### Step 2: Create MetadataDiscoveryService (SDK-Based)

```java
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MetadataDiscoveryService {

    private final DatabaseConnectionRepository connectionRepository;
    private final SchemaMetadataRepository schemaRepository;
    private final TableMetadataRepository tableRepository;
    private final ColumnMetadataRepository columnRepository;
    private final ConnectorFactory connectorFactory;
    private final ObjectMapper objectMapper;

    /**
     * Discover metadata using SDK connectors
     */
    public DiscoveryResult discoverMetadata(Long connectionId) {
        log.info("Starting SDK-based metadata discovery for connection: {}", connectionId);

        DatabaseConnection connection = connectionRepository.findById(connectionId)
            .orElseThrow(() -> new IllegalArgumentException("Connection not found"));

        try {
            // 1. Get appropriate Source connector
            Source source = connectorFactory.getSource(connection);

            // 2. Convert connection to SDK config
            JsonNode config = connectorFactory.toConnectorConfig(connection);

            // 3. Call SDK discover() method
            Catalog sdkCatalog = source.discover(config);

            // 4. Convert SDK Catalog to our database entities
            DiscoveryResult result = convertAndStore(connection, sdkCatalog);

            log.info("Discovery completed: {} schemas, {} tables discovered",
                result.getSchemaCount(), result.getTableCount());

            return result;

        } catch (Exception e) {
            log.error("Metadata discovery failed", e);
            throw new RuntimeException("Discovery failed: " + e.getMessage(), e);
        }
    }

    /**
     * Convert SDK Catalog to our metadata entities and store in DB
     */
    private DiscoveryResult convertAndStore(
            DatabaseConnection connection,
            Catalog sdkCatalog) throws Exception {

        int schemaCount = 0;
        int tableCount = 0;
        int columnCount = 0;

        // Group entities by namespace (schema)
        Map<String, List<DataEntity>> entitiesBySchema = sdkCatalog.getEntities()
            .stream()
            .collect(Collectors.groupingBy(DataEntity::getNamespace));

        for (Map.Entry<String, List<DataEntity>> entry : entitiesBySchema.entrySet()) {
            String schemaName = entry.getKey();
            List<DataEntity> tables = entry.getValue();

            // Create or update SchemaMetadata
            SchemaMetadata schema = schemaRepository
                .findByConnectionAndSchemaName(connection, schemaName)
                .orElse(new SchemaMetadata());

            schema.setConnection(connection);
            schema.setSchemaName(schemaName);
            schema.setTableCount(tables.size());
            schema.setLastSyncedAt(LocalDateTime.now());

            if (schema.getId() == null) {
                schema.setDiscoveredAt(LocalDateTime.now());
            }

            schema = schemaRepository.save(schema);
            schemaCount++;

            // Process each table/entity
            for (DataEntity entity : tables) {
                TableMetadata table = processTable(schema, entity);
                tableCount++;
                columnCount += processColumns(table, entity);
            }
        }

        return new DiscoveryResult(schemaCount, tableCount, columnCount);
    }

    /**
     * Convert SDK DataEntity to our TableMetadata
     */
    private TableMetadata processTable(SchemaMetadata schema, DataEntity entity) {
        TableMetadata table = tableRepository
            .findBySchemaAndTableName(schema, entity.getName())
            .orElse(new TableMetadata());

        table.setSchema(schema);
        table.setTableName(entity.getName());
        table.setTableType(TableType.TABLE); // Default, could enhance
        table.setLastSyncedAt(LocalDateTime.now());

        if (table.getId() == null) {
            table.setDiscoveredAt(LocalDateTime.now());
        }

        return tableRepository.save(table);
    }

    /**
     * Extract columns from SDK DataEntity JSON schema
     */
    private int processColumns(TableMetadata table, DataEntity entity) throws Exception {
        JsonNode jsonSchema = entity.getJsonSchema();

        if (jsonSchema == null || !jsonSchema.has("properties")) {
            return 0;
        }

        JsonNode properties = jsonSchema.get("properties");
        Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();

        int columnCount = 0;
        int position = 1;

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String columnName = field.getKey();
            JsonNode columnSchema = field.getValue();

            ColumnMetadata column = columnRepository
                .findByTableAndColumnName(table, columnName)
                .orElse(new ColumnMetadata());

            column.setTable(table);
            column.setColumnName(columnName);
            column.setOrdinalPosition(position++);

            // Extract type from JSON schema
            if (columnSchema.has("type")) {
                String type = columnSchema.get("type").asText();
                column.setDataType(type);
            }

            // Extract JDBC type if available
            if (columnSchema.has("airbyte_type")) {
                column.setJdbcType(columnSchema.get("airbyte_type").asText());
            }

            // Check if part of primary key
            if (entity.getPrimaryKeys() != null) {
                boolean isPK = entity.getPrimaryKeys().stream()
                    .flatMap(List::stream)
                    .anyMatch(pk -> pk.equals(columnName));
                column.setIsPrimaryKey(isPK);
            }

            // Check if incremental field
            if (entity.getIncrementalFields() != null) {
                column.setIsAutoIncrement(
                    entity.getIncrementalFields().contains(columnName)
                );
            }

            if (column.getId() == null) {
                column.setDiscoveredAt(LocalDateTime.now());
            }

            columnRepository.save(column);
            columnCount++;
        }

        return columnCount;
    }
}
```

---

### Step 3: Simplified Data Model (Reuse SDK Concepts)

Since the SDK already provides rich metadata, we only need to store:

```java
// Simplified entities - no need for IndexMetadata, ForeignKeyMetadata yet
// We can add those later if needed

@Entity
@Table(name = "schema_metadata")
public class SchemaMetadata {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    private DatabaseConnection connection;

    private String schemaName;
    private String catalogName;

    private Integer tableCount;
    private Integer viewCount;

    private LocalDateTime discoveredAt;
    private LocalDateTime lastSyncedAt;

    // Store original SDK Catalog as JSON for full fidelity
    @Column(columnDefinition = "TEXT")
    private String sdkCatalogJson;
}

@Entity
@Table(name = "table_metadata")
public class TableMetadata {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    private SchemaMetadata schema;

    private String tableName;
    private TableType tableType;

    private LocalDateTime discoveredAt;
    private LocalDateTime lastSyncedAt;

    // Store original SDK DataEntity as JSON
    @Column(columnDefinition = "TEXT")
    private String sdkEntityJson;
}

@Entity
@Table(name = "column_metadata")
public class ColumnMetadata {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    private TableMetadata table;

    private String columnName;
    private String dataType;
    private String jdbcType;

    private Boolean isPrimaryKey;
    private Boolean isAutoIncrement;
    private Boolean nullable;

    private Integer ordinalPosition;

    private LocalDateTime discoveredAt;

    // Store original JSON schema fragment
    @Column(columnDefinition = "TEXT")
    private String jsonSchemaFragment;
}
```

---

### Step 4: REST API (Same as Before)

```java
@RestController
@RequestMapping("/api/v1/metadata")
@Tag(name = "Metadata Discovery")
@RequiredArgsConstructor
public class MetadataDiscoveryController {

    private final MetadataDiscoveryService discoveryService;

    @PostMapping("/discover/{connectionId}")
    @Operation(summary = "Discover metadata using SDK connectors")
    public ResponseEntity<DiscoveryResult> discover(@PathVariable Long connectionId) {
        DiscoveryResult result = discoveryService.discoverMetadata(connectionId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/schemas/{connectionId}")
    @Operation(summary = "Get discovered schemas")
    public ResponseEntity<List<SchemaMetadataDTO>> getSchemas(
            @PathVariable Long connectionId) {
        // Implementation
    }

    @GetMapping("/tables/{connectionId}")
    @Operation(summary = "Get discovered tables")
    public ResponseEntity<List<TableMetadataDTO>> getTables(
            @PathVariable Long connectionId,
            @RequestParam(required = false) String schema) {
        // Implementation
    }
}
```

---

## 🎯 **Benefits of Using SDK**

✅ **Already implemented** - Discovery logic exists in connectors
✅ **Database-specific optimizations** - Each connector knows its DB
✅ **Consistent interface** - Same API across all database types
✅ **JSON Schema support** - Rich metadata via JSON Schema
✅ **Tested** - Connectors already used for data sync
✅ **Extensible** - Easy to add new database types
✅ **No JDBC complexity** - SDK handles all driver details

---

## ⚡ **Implementation Timeline**

| Task | Time | Description |
|------|------|-------------|
| **1. ConnectorFactory** | 1 hour | Create factory to instantiate Source connectors |
| **2. Simplified entities** | 1 hour | Create 3 entities (Schema, Table, Column) |
| **3. MetadataDiscoveryService** | 2 hours | Implement using SDK discover() |
| **4. Conversion logic** | 1.5 hours | Convert SDK Catalog → Our entities |
| **5. REST API** | 1 hour | Create controllers and DTOs |
| **6. Testing** | 1.5 hours | Test with H2, Postgres |
| **Total** | **8 hours** | Complete implementation |

---

## 🔄 **Flow Diagram**

```
User → REST API → MetadataDiscoveryService
                         ↓
                  ConnectorFactory
                         ↓
            [Resolve Source by DB type]
                         ↓
       PostgresSource / H2Source / SqlServerSource
                         ↓
              Source.discover(config)
                         ↓
            [Query INFORMATION_SCHEMA]
                         ↓
              Return SDK Catalog
            (List of DataEntity with JSON Schemas)
                         ↓
         Convert to our entities:
         - SchemaMetadata
         - TableMetadata
         - ColumnMetadata
                         ↓
              Save to database
                         ↓
         Return DiscoveryResult to user
```

---

## 📝 **Example Usage**

```java
// Discover metadata for PostgreSQL connection
POST /api/v1/metadata/discover/1

Response:
{
  "schemaCount": 2,
  "tableCount": 15,
  "columnCount": 87,
  "discoveredAt": "2026-03-18T10:00:00",
  "schemas": [
    {
      "name": "public",
      "tables": ["users", "orders", "products"]
    },
    {
      "name": "analytics",
      "tables": ["events", "metrics"]
    }
  ]
}

// Get tables for a schema
GET /api/v1/metadata/tables/1?schema=public

Response:
[
  {
    "id": 1,
    "schemaName": "public",
    "tableName": "users",
    "columnCount": 8,
    "primaryKeys": ["id"],
    "discoveredAt": "2026-03-18T10:00:00"
  },
  ...
]

// Get columns for a table
GET /api/v1/metadata/table/1/columns

Response:
[
  {
    "columnName": "id",
    "dataType": "integer",
    "isPrimaryKey": true,
    "position": 1
  },
  {
    "columnName": "email",
    "dataType": "string",
    "isPrimaryKey": false,
    "position": 2
  },
  ...
]
```

---

## 🚀 **Next Steps After Discovery**

1. **Query Builder** - Use metadata to build SQL queries visually
2. **Data Lineage** - Track which queries read/write which tables
3. **Automatic ER Diagrams** - Generate from primary/foreign keys
4. **Smart Suggestions** - Suggest joins based on column names
5. **Data Profiling** - Use discovered metadata to profile data
6. **Column-level Security** - Apply permissions based on metadata

---

## Summary

**Using the OneAPI SDK and connectors is the RIGHT approach:**

1. ✅ Discovery logic already exists
2. ✅ Database-specific optimizations built-in
3. ✅ Consistent across all database types
4. ✅ JSON Schema support for rich metadata
5. ✅ Less code to write and maintain
6. ✅ Leverages existing, tested infrastructure

We just need to:
- Create ConnectorFactory to instantiate the right Source
- Call `source.discover(config)` to get metadata
- Convert SDK's `Catalog` to our database entities
- Expose via REST API

**Much simpler and more robust than writing custom JDBC code!**

