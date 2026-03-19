package io.oneapi.sdk.base;

import com.fasterxml.jackson.databind.JsonNode;
import io.oneapi.sdk.database.AbstractDatabase;
import io.oneapi.sdk.enums.SyncMode;
import io.oneapi.sdk.model.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Abstract base class for database sources.
 * Provides common functionality for discovering tables and managing connections.
 *
 * @param <DataType> The database-specific data type
 * @param <Database> The database implementation type
 */
public abstract class AbstractDatabaseSource<DataType, Database extends AbstractDatabase>
        implements Source, AutoCloseable {

    protected final String driverClassName;

    protected AbstractDatabaseSource(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    @Override
    public ConnectionStatus check(JsonNode config) throws Exception {
        try {
            Database database = createDatabase(config);
            // Test connection by attempting to get metadata or execute a simple query
            return new ConnectionStatus(ConnectionStatus.Status.SUCCEEDED, "Connection successful");
        } catch (Exception e) {
            return new ConnectionStatus(ConnectionStatus.Status.FAILED, "Connection failed: " + e.getMessage());
        } finally {
            close();
        }
    }

    @Override
    public Domain discover(JsonNode config) throws Exception {
        try {
            Database database = createDatabase(config);
            List<TableInfo<Field<DataType>>> tableInfos = discoverWithoutSystemTables(database);
            Map<String, List<String>> tableToPrimaryKeys = discoverPrimaryKeys(database, tableInfos);
            return convertTableInfosToDomain(tableInfos, tableToPrimaryKeys);
        } finally {
            close();
        }
    }

    /**
     * Discovers tables excluding system tables and views.
     */
    protected List<TableInfo<Field<DataType>>> discoverWithoutSystemTables(Database database)
            throws Exception {
        Set<String> systemNameSpaces = getExcludedInternalNameSpaces();
        Set<String> systemViews = getExcludedViews();
        List<TableInfo<Field<DataType>>> discoveredTables = discoverInternal(database);

        if (systemNameSpaces == null || systemNameSpaces.isEmpty()) {
            return discoveredTables;
        }

        return discoveredTables.stream()
                .filter(table -> !systemNameSpaces.contains(table.getNameSpace()) &&
                        !systemViews.contains(table.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Converts TableInfo list to Domain.
     *
     * This method directly transfers field information from TableInfo to DataEntity,
     * eliminating the need for JSON Schema serialization/deserialization.
     */
    protected Domain convertTableInfosToDomain(
            List<TableInfo<Field<DataType>>> tableInfos,
            Map<String, List<String>> primaryKeys) {

        List<DataEntity> entities = tableInfos.stream()
                .map(tableInfo -> {
                    String fullyQualifiedName = tableInfo.getNameSpace() + "." + tableInfo.getName();
                    DataEntity entity = new DataEntity();
                    entity.setName(tableInfo.getName());
                    entity.setNamespace(tableInfo.getNameSpace());
                    entity.setSyncMode(SyncMode.FULL_REFRESH);

                    // Set fields directly - no JSON conversion needed!
                    if (tableInfo.getFields() != null && !tableInfo.getFields().isEmpty()) {
                        entity.setFields((List<Field<?>>) (List<?>) tableInfo.getFields());
                    }

                    // Set primary keys
                    List<String> pks = primaryKeys.getOrDefault(fullyQualifiedName, Collections.emptyList());
                    if (!pks.isEmpty()) {
                        entity.setPrimaryKeys(List.of(pks));
                    }

                    // Set incremental fields
                    if (tableInfo.getIncrementalFields() != null && !tableInfo.getIncrementalFields().isEmpty()) {
                        entity.setIncrementalFields(tableInfo.getIncrementalFields());
                        entity.setHasIncrementalField(true);
                        entity.setSyncMode(SyncMode.INCREMENTAL);
                    }

                    return entity;
                })
                .collect(Collectors.toList());

        return new Domain(entities);
    }

    @Override
    public EntityRecordIterator<EntityRecord> read(JsonNode config, Domain domain, State state) throws Exception {
        // To be implemented by subclasses
        throw new UnsupportedOperationException("read() must be implemented by subclass");
    }

    // Abstract methods to be implemented by subclasses

    /**
     * Returns the set of system/internal namespaces to exclude from discovery.
     */
    protected abstract Set<String> getExcludedInternalNameSpaces();

    /**
     * Returns the set of system views to exclude from discovery.
     */
    protected Set<String> getExcludedViews() {
        return Collections.emptySet();
    }

    /**
     * Converts source config to database-specific config.
     */
    public abstract JsonNode toDatabaseConfig(JsonNode config);

    /**
     * Discovers all tables/collections in the database.
     */
    protected abstract List<TableInfo<Field<DataType>>> discoverInternal(Database database)
            throws Exception;

    /**
     * Discovers all tables/collections in a specific schema.
     */
    protected abstract List<TableInfo<Field<DataType>>> discoverInternal(Database database, String schema)
            throws Exception;

    /**
     * Discovers primary keys for the given tables.
     */
    protected abstract Map<String, List<String>> discoverPrimaryKeys(
            Database database,
            List<TableInfo<Field<DataType>>> tableInfos);

    /**
     * Returns the quote string for identifiers (e.g., " for PostgreSQL, ` for MySQL).
     */
    protected abstract String getQuoteString();

    /**
     * Creates a database instance from config.
     */
    protected abstract Database createDatabase(JsonNode config) throws Exception;

    @Override
    public void close() throws Exception {
        // Override to clean up resources
    }
}
