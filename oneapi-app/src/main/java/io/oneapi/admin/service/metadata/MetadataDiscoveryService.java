package io.oneapi.admin.service.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.oneapi.admin.connector.ConnectorFactory;
import io.oneapi.admin.entity.SourceInfo;
import io.oneapi.admin.entity.metadata.FieldInfo;
import io.oneapi.admin.entity.metadata.DomainInfo;
import io.oneapi.admin.entity.metadata.EntityInfo;
import io.oneapi.admin.repository.SourceInfoRepository;
import io.oneapi.admin.repository.metadata.FieldInfoRepository;
import io.oneapi.admin.repository.metadata.DomainInfoRepository;
import io.oneapi.admin.repository.metadata.EntityInfoRepository;
import io.oneapi.sdk.base.Source;
import io.oneapi.sdk.model.Domain;
import io.oneapi.sdk.model.DataEntity;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for discovering database metadata using OneAPI SDK connectors.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MetadataDiscoveryService {

    private final SourceInfoRepository sourceRepository;
    private final DomainInfoRepository domainRepository;
    private final EntityInfoRepository entityRepository;
    private final FieldInfoRepository fieldRepository;
    private final ConnectorFactory connectorFactory;
    private final ObjectMapper objectMapper;
    private final MetadataEnrichmentService enrichmentService;

    @Value("${oneapi.metadata.enrichment.enabled:true}")
    private boolean enrichmentEnabled;

    /**
     * Discover metadata for a database connection using SDK connectors.
     */
    public DiscoveryResult discoverMetadata(Long datasourceId) {
        log.info("Starting SDK-based metadata discovery for connection: {}", datasourceId);

        SourceInfo connection = sourceRepository.findById(datasourceId)
            .orElseThrow(() -> new IllegalArgumentException("Connection not found with ID: " + datasourceId));

        try {
            // 1. Get appropriate Source connector based on database type
            Source source = connectorFactory.getSource(connection);
            log.debug("Using connector: {}", source.getClass().getSimpleName());

            // 2. Convert Datasource to SDK config format
            JsonNode config = connectorFactory.toConnectorConfig(connection);
            log.debug("Generated connector config for: {}", connection.getName());

            // 3. Call SDK discover() method to get metadata
            Domain sdkDomain = source.discover(config);
            log.info("SDK discovery completed. Found {} entities", sdkDomain.getEntities().size());

            // 4. Convert SDK Domain to our database entities and store
            DiscoveryResult result = convertAndStore(connection, sdkDomain);

            log.info("Metadata discovery completed: {} schemas, {} tables, {} columns",
                result.getSchemaCount(), result.getTableCount(), result.getColumnCount());

            // Phase 2: Trigger async metadata enrichment (non-blocking)
            if (enrichmentEnabled) {
                log.info("Triggering async metadata enrichment for datasource: {}", datasourceId);
                enrichmentService.enrichDatasourceMetadataAsync(datasourceId);
            } else {
                log.info("Metadata enrichment is disabled");
            }

            return result;

        } catch (Exception e) {
            log.error("Metadata discovery failed for connection {}: {}", datasourceId, e.getMessage(), e);
            throw new RuntimeException("Discovery failed: " + e.getMessage(), e);
        }
    }

    /**
     * Convert SDK Domain to our metadata entities and persist to database.
     */
    private DiscoveryResult convertAndStore(SourceInfo connection, Domain sdkDomain) throws Exception {
        int schemaCount = 0;
        int tableCount = 0;
        int columnCount = 0;

        LocalDateTime now = LocalDateTime.now();

        // Group entities by namespace (domain)
        Map<String, List<DataEntity>> entitiesBySchema = sdkDomain.getEntities()
            .stream()
            .collect(Collectors.groupingBy(
                entity -> entity.getNamespace() != null ? entity.getNamespace() : "default"
            ));

        log.debug("Processing {} schemas", entitiesBySchema.size());

        for (Map.Entry<String, List<DataEntity>> entry : entitiesBySchema.entrySet()) {
            String schemaName = entry.getKey();
            List<DataEntity> tables = entry.getValue();

            // Create or update SchemaMetadata
            DomainInfo domain = domainRepository
                .findBySourceAndSchemaName(connection, schemaName)
                .orElse(new DomainInfo());

            boolean isNewSchema = domain.getId() == null;

            domain.setSource(connection);
            domain.setSchemaName(schemaName);
            domain.setTableCount(tables.size());
            domain.setLastSyncedAt(now);

            if (isNewSchema) {
                domain.setDiscoveredAt(now);
                log.debug("Discovered new schema: {}", schemaName);
            }

            // Note: SDK domain JSON storage removed to simplify schema

            domain = domainRepository.save(domain);
            schemaCount++;

            // Process each table/entity
            for (DataEntity entity : tables) {
                EntityInfo table = processTable(domain, entity, now);
                tableCount++;
                columnCount += processColumns(table, entity, now);
            }
        }

        return new DiscoveryResult(schemaCount, tableCount, columnCount, now);
    }

    /**
     * Convert SDK DataEntity to our EntityInfo entity.
     */
    private EntityInfo processTable(DomainInfo domain, DataEntity entity, LocalDateTime now)
            throws Exception {

        EntityInfo table = entityRepository
            .findByDomainAndTableName(domain, entity.getName())
            .orElse(new EntityInfo());

        boolean isNewTable = table.getId() == null;

        table.setDomain(domain);
        table.setTableName(entity.getName());
        table.setTableType(EntityInfo.TableType.TABLE); // Default, could enhance based on entity type
        table.setLastSyncedAt(now);

        if (isNewTable) {
            table.setDiscoveredAt(now);
            log.debug("Discovered new table: {}.{}", domain.getSchemaName(), entity.getName());
        }

        // Note: SDK entity JSON storage removed to simplify schema

        return entityRepository.save(table);
    }

    /**
     * Extract columns from SDK DataEntity fields and create FieldInfo entities.
     *
     * This method directly accesses the fields list from DataEntity, eliminating
     * the need for JSON Schema parsing and providing type-safe access to field metadata.
     */
    private int processColumns(EntityInfo table, DataEntity entity, LocalDateTime now) throws Exception {
        List<io.oneapi.sdk.model.Field<?>> fields = entity.getFields();

        if (fields == null || fields.isEmpty()) {
            log.warn("No fields found for table: {}", table.getTableName());
            return 0;
        }

        int columnCount = 0;
        int position = 1;

        // Get primary key columns for lookup
        Set<String> primaryKeyColumns = getPrimaryKeyColumns(entity);

        // Get incremental fields for lookup
        Set<String> incrementalFieldNames = new HashSet<>(
            entity.getIncrementalFields() != null ? entity.getIncrementalFields() : Collections.emptyList()
        );

        for (io.oneapi.sdk.model.Field<?> field : fields) {
            String columnName = field.getName();
            Object type = field.getType();

            FieldInfo column = fieldRepository
                .findByDataEntityAndColumnName(table, columnName)
                .orElse(new FieldInfo());

            boolean isNewColumn = column.getId() == null;

            column.setDataEntity(table);
            column.setColumnName(columnName);
            column.setOrdinalPosition(position++);

            // Direct type access - no JSON parsing needed!
            if (type != null) {
                column.setDataType(type.toString());
                column.setJdbcType(type.toString());
            }

            // Check if primary key
            column.setIsPrimaryKey(primaryKeyColumns.contains(columnName));

            // Check if incremental field (often indicates auto-increment or timestamp)
            column.setIsAutoIncrement(incrementalFieldNames.contains(columnName));

            if (isNewColumn) {
                column.setDiscoveredAt(now);
                log.trace("Discovered column: {}.{}.{}",
                    table.getDomain().getSchemaName(), table.getTableName(), columnName);
            }

            fieldRepository.save(column);
            columnCount++;
        }

        log.debug("Processed {} fields for table: {}", columnCount, table.getTableName());
        return columnCount;
    }

    /**
     * Extract primary key column names from DataEntity.
     */
    private Set<String> getPrimaryKeyColumns(DataEntity entity) {
        if (entity.getPrimaryKeys() == null || entity.getPrimaryKeys().isEmpty()) {
            return Collections.emptySet();
        }

        return entity.getPrimaryKeys().stream()
            .flatMap(List::stream)
            .collect(Collectors.toSet());
    }

    /**
     * Sync metadata (re-discover) for a connection with change detection.
     * This method detects what changed since last discovery.
     */
    public SyncResult syncMetadata(Long datasourceId) {
        log.info("Syncing metadata for connection: {}", datasourceId);

        // Get existing metadata counts before sync
        int existingSchemasCount = domainRepository.findBySourceId(datasourceId).size();
        int existingTablesCount = entityRepository.findBySourceId(datasourceId).size();
        long existingColumnsCount = fieldRepository.findAll().stream()
            .filter(f -> f.getDataEntity().getDomain().getSource().getId().equals(datasourceId))
            .count();

        log.info("Existing metadata: {} schemas, {} tables, {} columns",
            existingSchemasCount, existingTablesCount, existingColumnsCount);

        // Get list of existing table IDs before sync (to detect new tables)
        Set<Long> existingTableIds = entityRepository.findBySourceId(datasourceId)
            .stream()
            .map(EntityInfo::getId)
            .collect(Collectors.toSet());

        // Perform discovery (which updates existing or creates new)
        DiscoveryResult discoveryResult = discoverMetadata(datasourceId);

        // Calculate differences
        int newSchemas = discoveryResult.getSchemaCount() - existingSchemasCount;
        int newTables = discoveryResult.getTableCount() - existingTablesCount;
        int newColumns = discoveryResult.getColumnCount() - (int)existingColumnsCount;

        SyncResult result = new SyncResult(
            discoveryResult.getSchemaCount(),
            discoveryResult.getTableCount(),
            discoveryResult.getColumnCount(),
            newSchemas,
            newTables,
            newColumns,
            discoveryResult.getDiscoveredAt()
        );

        log.info("Sync completed. Changes: {} new schemas, {} new tables, {} new columns",
            newSchemas, newTables, newColumns);

        // Enrich tables that don't have enrichment data yet (async)
        if (enrichmentEnabled) {
            // Get all tables for this datasource
            List<EntityInfo> allTables = entityRepository.findBySourceId(datasourceId);

            // Filter to find tables without enrichment
            List<Long> unenrichedTableIds = allTables.stream()
                .filter(table -> !table.isEnriched())
                .map(EntityInfo::getId)
                .toList();

            if (!unenrichedTableIds.isEmpty()) {
                log.info("Triggering async enrichment for {} unenriched tables during resync",
                    unenrichedTableIds.size());
                enrichUnenrichedTablesAsync(unenrichedTableIds);
            } else {
                log.info("All tables are already enriched, skipping enrichment");
            }
        }

        return result;
    }

    /**
     * Enrich only unenriched tables (for sync operation).
     * This enriches tables that don't have enrichment data yet.
     */
    private void enrichUnenrichedTablesAsync(List<Long> tableIds) {
        if (tableIds.isEmpty()) {
            return;
        }

        log.info("Enriching {} unenriched tables", tableIds.size());

        // Process each table asynchronously
        for (Long tableId : tableIds) {
            try {
                enrichmentService.enrichTableMetadata(tableId);
                enrichmentService.enrichColumnMetadata(tableId);
            } catch (Exception e) {
                log.error("Failed to enrich table {}: {}", tableId, e.getMessage());
            }
        }
    }

    /**
     * Delete all metadata for a connection.
     */
    public void deleteMetadata(Long datasourceId) {
        log.info("Deleting all metadata for connection: {}", datasourceId);
        domainRepository.deleteBySourceId(datasourceId);
        log.info("Deleted metadata for connection: {}", datasourceId);
    }

    /**
     * Result of metadata discovery operation.
     */
    @Data
    public static class DiscoveryResult {
        private final int schemaCount;
        private final int tableCount;
        private final int columnCount;
        private final LocalDateTime discoveredAt;
    }

    /**
     * Result of metadata sync operation with change detection.
     */
    @Data
    public static class SyncResult {
        private final int totalSchemas;
        private final int totalTables;
        private final int totalColumns;
        private final int newSchemas;
        private final int newTables;
        private final int newColumns;
        private final LocalDateTime syncedAt;

        public boolean hasChanges() {
            return newSchemas > 0 || newTables > 0 || newColumns > 0;
        }

        public String getChangesSummary() {
            if (!hasChanges()) {
                return "No changes detected";
            }
            List<String> changes = new ArrayList<>();
            if (newSchemas > 0) changes.add(newSchemas + " new schema(s)");
            if (newTables > 0) changes.add(newTables + " new table(s)");
            if (newColumns > 0) changes.add(newColumns + " new column(s)");
            return String.join(", ", changes);
        }
    }
}
