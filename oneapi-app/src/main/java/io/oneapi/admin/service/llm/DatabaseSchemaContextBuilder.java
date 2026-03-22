package io.oneapi.admin.service.llm;

import io.oneapi.admin.entity.SourceInfo;
import io.oneapi.admin.entity.metadata.DomainInfo;
import io.oneapi.admin.entity.metadata.EntityInfo;
import io.oneapi.admin.entity.metadata.FieldInfo;
import io.oneapi.admin.repository.SourceInfoRepository;
import io.oneapi.admin.repository.metadata.DomainInfoRepository;
import io.oneapi.admin.repository.metadata.EntityInfoRepository;
import io.oneapi.admin.repository.metadata.FieldInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service to build database schema context for LLM from metadata.
 * This provides the LLM with accurate information about available tables, columns, and relationships.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseSchemaContextBuilder {

    private final SourceInfoRepository sourceInfoRepository;
    private final DomainInfoRepository domainInfoRepository;
    private final EntityInfoRepository entityInfoRepository;
    private final FieldInfoRepository fieldInfoRepository;

    /**
     * Builds a complete schema context for a datasource.
     *
     * @param datasourceId the datasource ID
     * @return database schema context
     */
    public DatabaseSchemaContext buildContext(Long datasourceId) {
        log.debug("Building schema context for datasource: {}", datasourceId);

        SourceInfo source = sourceInfoRepository.findById(datasourceId)
            .orElseThrow(() -> new IllegalArgumentException("Datasource not found: " + datasourceId));

        List<DomainInfo> domains = domainInfoRepository.findBySourceId(datasourceId);

        List<DatabaseSchemaContext.TableInfo> tables = new ArrayList<>();

        for (DomainInfo domain : domains) {
            List<EntityInfo> entities = entityInfoRepository.findByDomainId(domain.getId());

            for (EntityInfo entity : entities) {
                DatabaseSchemaContext.TableInfo tableInfo = buildTableInfo(domain, entity);
                tables.add(tableInfo);
            }
        }

        return DatabaseSchemaContext.builder()
            .databaseType(source.getType().toString())
            .databaseName(source.getName())
            .tables(tables)
            .build();
    }

    /**
     * Builds schema context with specific tables only (for focused queries).
     *
     * @param datasourceId the datasource ID
     * @param tableNames list of table names to include
     * @return filtered database schema context
     */
    public DatabaseSchemaContext buildContextForTables(Long datasourceId, List<String> tableNames) {
        log.debug("Building schema context for datasource {} with tables: {}", datasourceId, tableNames);

        DatabaseSchemaContext fullContext = buildContext(datasourceId);

        if (tableNames == null || tableNames.isEmpty()) {
            return fullContext;
        }

        List<DatabaseSchemaContext.TableInfo> filteredTables = fullContext.getTables().stream()
            .filter(table -> tableNames.stream()
                .anyMatch(name -> table.getTableName().equalsIgnoreCase(name)))
            .collect(Collectors.toList());

        return DatabaseSchemaContext.builder()
            .databaseType(fullContext.getDatabaseType())
            .databaseName(fullContext.getDatabaseName())
            .tables(filteredTables)
            .build();
    }

    /**
     * Builds a lightweight context with only table and column names (faster, less detailed).
     *
     * @param datasourceId the datasource ID
     * @return lightweight schema context
     */
    public DatabaseSchemaContext buildLightweightContext(Long datasourceId) {
        log.debug("Building lightweight schema context for datasource: {}", datasourceId);

        SourceInfo source = sourceInfoRepository.findById(datasourceId)
            .orElseThrow(() -> new IllegalArgumentException("Datasource not found: " + datasourceId));

        List<DomainInfo> domains = domainInfoRepository.findBySourceId(datasourceId);

        List<DatabaseSchemaContext.TableInfo> tables = new ArrayList<>();

        for (DomainInfo domain : domains) {
            List<EntityInfo> entities = entityInfoRepository.findByDomainId(domain.getId());

            for (EntityInfo entity : entities) {
                // Build lightweight version (without fetching all field details)
                List<DatabaseSchemaContext.ColumnInfo> columns = fieldInfoRepository
                    .findByDataEntityId(entity.getId())
                    .stream()
                    .map(this::toColumnInfo)
                    .collect(Collectors.toList());

                DatabaseSchemaContext.TableInfo tableInfo = DatabaseSchemaContext.TableInfo.builder()
                    .schemaName(domain.getSchemaName())
                    .tableName(entity.getTableName())
                    .columns(columns)
                    .estimatedRowCount(entity.getEstimatedRowCount())
                    .build();

                tables.add(tableInfo);
            }
        }

        return DatabaseSchemaContext.builder()
            .databaseType(source.getType().toString())
            .databaseName(source.getName())
            .tables(tables)
            .build();
    }

    // ========== Private Helper Methods ==========

    private DatabaseSchemaContext.TableInfo buildTableInfo(DomainInfo domain, EntityInfo entity) {
        List<FieldInfo> fields = fieldInfoRepository.findByDataEntityId(entity.getId());

        List<DatabaseSchemaContext.ColumnInfo> columns = fields.stream()
            .map(this::toColumnInfo)
            .collect(Collectors.toList());

        List<String> primaryKeys = fields.stream()
            .filter(FieldInfo::getIsPrimaryKey)
            .map(FieldInfo::getColumnName)
            .collect(Collectors.toList());

        // Note: Foreign key relationship details are not stored in FieldInfo
        // This would require additional metadata or JDBC introspection
        List<DatabaseSchemaContext.ForeignKeyInfo> foreignKeys = new ArrayList<>();

        return DatabaseSchemaContext.TableInfo.builder()
            .schemaName(domain.getSchemaName())
            .tableName(entity.getTableName())
            .description(entity.getTableComment())
            .columns(columns)
            .primaryKeys(primaryKeys)
            .foreignKeys(foreignKeys)
            .estimatedRowCount(entity.getEstimatedRowCount())
            .build();
    }

    private DatabaseSchemaContext.ColumnInfo toColumnInfo(FieldInfo field) {
        return DatabaseSchemaContext.ColumnInfo.builder()
            .columnName(field.getColumnName())
            .dataType(field.getDataType())
            .nullable(field.getNullable())
            .description(field.getColumnComment())
            .isPrimaryKey(field.getIsPrimaryKey())
            .isForeignKey(field.getIsForeignKey())
            .referencedTable(null)  // FK details not stored in current schema
            .referencedColumn(null) // FK details not stored in current schema
            .build();
    }
}
