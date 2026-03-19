package io.oneapi.sqlserver.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.oneapi.sdk.base.AbstractDatabaseSource;
import io.oneapi.sdk.database.DatabaseDriver;
import io.oneapi.sdk.database.JdbcDatabase;
import io.oneapi.sdk.jdbc.DataSourceFactory;
import io.oneapi.sdk.jdbc.JdbcUtils;
import io.oneapi.sdk.model.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SQL Server implementation of Source.
 * Supports SQL Server 2017 and later.
 */
public class SqlServerSource extends AbstractDatabaseSource<JDBCType, JdbcDatabase> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private JdbcDatabase database;

    public SqlServerSource() {
        super(DatabaseDriver.MSSQLSERVER.getDriverClassName());
    }

    @Override
    protected Set<String> getExcludedInternalNameSpaces() {
        return Set.of("sys", "INFORMATION_SCHEMA", "guest", "db_owner", "db_accessadmin",
                     "db_securityadmin", "db_ddladmin", "db_backupoperator", "db_datareader",
                     "db_datawriter", "db_denydatareader", "db_denydatawriter");
    }

    @Override
    protected Set<String> getExcludedViews() {
        return Collections.emptySet();
    }

    @Override
    public JsonNode toDatabaseConfig(JsonNode config) {
        ObjectNode dbConfig = MAPPER.createObjectNode();

        // SQL Server: jdbc:sqlserver://host:port;databaseName=dbname
        String jdbcUrl;
        if (config.has(JdbcUtils.JDBC_URL_KEY)) {
            jdbcUrl = config.get(JdbcUtils.JDBC_URL_KEY).asText();
        } else {
            String host = config.has(JdbcUtils.HOST_KEY) ?
                config.get(JdbcUtils.HOST_KEY).asText() : "localhost";
            int port = config.has(JdbcUtils.PORT_KEY) ?
                config.get(JdbcUtils.PORT_KEY).asInt() : 1433;
            String database = config.has(JdbcUtils.DATABASE_KEY) ?
                config.get(JdbcUtils.DATABASE_KEY).asText() : "master";

            jdbcUrl = String.format("jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false;trustServerCertificate=true",
                    host, port, database);
        }

        String username = config.has(JdbcUtils.USERNAME_KEY) ?
            config.get(JdbcUtils.USERNAME_KEY).asText() : "sa";
        String password = config.has(JdbcUtils.PASSWORD_KEY) ?
            config.get(JdbcUtils.PASSWORD_KEY).asText() : "";

        dbConfig.put(JdbcUtils.JDBC_URL_KEY, jdbcUrl);
        dbConfig.put(JdbcUtils.USERNAME_KEY, username);
        dbConfig.put(JdbcUtils.PASSWORD_KEY, password);

        return dbConfig;
    }

    @Override
    protected List<TableInfo<Field<JDBCType>>> discoverInternal(JdbcDatabase database) throws Exception {
        List<TableInfo<Field<JDBCType>>> allTables = new ArrayList<>();

        // Discover ALL non-system schemas
        String schemaQuery = "SELECT name as schema_name FROM sys.schemas " +
                           "WHERE name NOT IN ('sys', 'INFORMATION_SCHEMA', 'guest', 'db_owner', " +
                           "'db_accessadmin', 'db_securityadmin', 'db_ddladmin', 'db_backupoperator', " +
                           "'db_datareader', 'db_datawriter', 'db_denydatareader', 'db_denydatawriter')";

        Set<String> schemas = new HashSet<>();
        database.query(schemaQuery, Collections.emptyList(), rs -> {
            while (rs.next()) {
                schemas.add(rs.getString("schema_name"));
            }
            return Collections.emptyList();
        });

        // Discover tables from ALL schemas
        for (String schema : schemas) {
            try {
                List<TableInfo<Field<JDBCType>>> schemaTables = discoverInternal(database, schema);
                allTables.addAll(schemaTables);
            } catch (Exception e) {
                // Log warning but continue with other schemas
                System.err.println("Warning: Failed to discover schema '" + schema + "': " + e.getMessage());
            }
        }

        return allTables;
    }

    @Override
    protected List<TableInfo<Field<JDBCType>>> discoverInternal(JdbcDatabase database, String schema) throws Exception {
        List<TableInfo<Field<JDBCType>>> tables = new ArrayList<>();

        String query = "SELECT TABLE_SCHEMA, TABLE_NAME " +
                       "FROM INFORMATION_SCHEMA.TABLES " +
                       "WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE' " +
                       "ORDER BY TABLE_NAME";

        database.query(query, List.of(schema), rs -> {
            while (rs.next()) {
                String tableSchema = rs.getString("TABLE_SCHEMA");
                String tableName = rs.getString("TABLE_NAME");

                List<Field<JDBCType>> fields = discoverTableFields(database, tableSchema, tableName);
                List<String> incrementalFields = findIncrementalFields(fields);

                tables.add(new TableInfo<>(
                        tableSchema,
                        tableName,
                        fields,
                        new ArrayList<>(), // Primary keys discovered separately
                        incrementalFields
                ));
            }
            return Collections.emptyList();
        });

        return tables;
    }

    private List<Field<JDBCType>> discoverTableFields(JdbcDatabase database, String schema, String table) throws SQLException {
        List<Field<JDBCType>> fields = new ArrayList<>();

        String query = "SELECT COLUMN_NAME, DATA_TYPE " +
                       "FROM INFORMATION_SCHEMA.COLUMNS " +
                       "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
                       "ORDER BY ORDINAL_POSITION";

        database.query(query, List.of(schema, table), rs -> {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                String typeName = rs.getString("DATA_TYPE");
                JDBCType jdbcType = mapSqlServerTypeToJdbcType(typeName);
                fields.add(new Field<>(columnName, jdbcType));
            }
            return Collections.emptyList();
        });

        return fields;
    }

    private List<String> findIncrementalFields(List<Field<JDBCType>> fields) {
        return fields.stream()
                .filter(field -> isIncrementalType(field.getType()))
                .map(Field::getName)
                .collect(Collectors.toList());
    }

    private boolean isIncrementalType(JDBCType type) {
        return type == JDBCType.TIMESTAMP ||
               type == JDBCType.DATE ||
               type == JDBCType.BIGINT ||
               type == JDBCType.INTEGER;
    }

    private JDBCType mapSqlServerTypeToJdbcType(String sqlServerType) {
        return switch (sqlServerType.toLowerCase()) {
            case "int" -> JDBCType.INTEGER;
            case "bigint" -> JDBCType.BIGINT;
            case "smallint" -> JDBCType.SMALLINT;
            case "tinyint" -> JDBCType.TINYINT;
            case "bit" -> JDBCType.BOOLEAN;
            case "decimal", "numeric", "money", "smallmoney" -> JDBCType.NUMERIC;
            case "float" -> JDBCType.DOUBLE;
            case "real" -> JDBCType.REAL;
            case "varchar", "nvarchar", "char", "nchar" -> JDBCType.VARCHAR;
            case "text", "ntext" -> JDBCType.CLOB;
            case "binary", "varbinary", "image" -> JDBCType.BLOB;
            case "date" -> JDBCType.DATE;
            case "time" -> JDBCType.TIME;
            case "datetime", "datetime2", "smalldatetime" -> JDBCType.TIMESTAMP;
            case "datetimeoffset" -> JDBCType.TIMESTAMP_WITH_TIMEZONE;
            case "uniqueidentifier" -> JDBCType.OTHER;
            case "xml" -> JDBCType.SQLXML;
            default -> JDBCType.OTHER;
        };
    }

    @Override
    protected Map<String, List<String>> discoverPrimaryKeys(JdbcDatabase database, List<TableInfo<Field<JDBCType>>> tableInfos) {
        Map<String, List<String>> primaryKeys = new HashMap<>();

        for (TableInfo<Field<JDBCType>> tableInfo : tableInfos) {
            String fullyQualifiedName = tableInfo.getNameSpace() + "." + tableInfo.getName();

            try {
                String query = "SELECT COLUMN_NAME " +
                               "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                               "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
                               "AND CONSTRAINT_NAME IN (" +
                               "  SELECT CONSTRAINT_NAME " +
                               "  FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                               "  WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
                               "  AND CONSTRAINT_TYPE = 'PRIMARY KEY'" +
                               ") " +
                               "ORDER BY ORDINAL_POSITION";

                List<List<String>> result = database.query(
                        query,
                        List.of(tableInfo.getNameSpace(), tableInfo.getName(),
                               tableInfo.getNameSpace(), tableInfo.getName()),
                        rs -> {
                            List<String> cols = new ArrayList<>();
                            while (rs.next()) {
                                cols.add(rs.getString("COLUMN_NAME"));
                            }
                            return List.of(cols);
                        }
                );

                List<String> pks = result.isEmpty() ? new ArrayList<>() : result.get(0);

                if (!pks.isEmpty()) {
                    primaryKeys.put(fullyQualifiedName, pks);
                }
            } catch (SQLException e) {
                // Skip if error discovering primary keys
            }
        }

        return primaryKeys;
    }

    @Override
    protected String getQuoteString() {
        return "[";
    }

    @Override
    protected JdbcDatabase createDatabase(JsonNode config) throws Exception {
        JsonNode dbConfig = toDatabaseConfig(config);

        String jdbcUrl = dbConfig.get(JdbcUtils.JDBC_URL_KEY).asText();
        String username = dbConfig.has(JdbcUtils.USERNAME_KEY) ? dbConfig.get(JdbcUtils.USERNAME_KEY).asText() : null;
        String password = dbConfig.has(JdbcUtils.PASSWORD_KEY) ? dbConfig.get(JdbcUtils.PASSWORD_KEY).asText() : null;

        DataSource dataSource = DataSourceFactory.create(
                username,
                password,
                driverClassName,
                jdbcUrl,
                new HashMap<>()
        );

        database = new JdbcDatabase(dataSource);
        database.setSourceConfig(config);
        database.setDatabaseConfig(dbConfig);

        return database;
    }

    @Override
    public EntityRecordIterator<EntityRecord> read(JsonNode config, Domain domain, State state) throws Exception {
        JdbcDatabase db = createDatabase(config);

        // For simplicity, read first entity
        if (domain.getEntities().isEmpty()) {
            return new EmptyEntityRecordIterator();
        }

        DataEntity entity = domain.getEntities().get(0);
        String tableName = "[" + entity.getNamespace() + "].[" + entity.getName() + "]";

        String query = "SELECT * FROM " + tableName;

        Connection conn = db.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        return new SqlServerEntityRecordIterator(rs, stmt, conn, entity.getName(), entity.getNamespace());
    }

    @Override
    public void close() throws Exception {
        // Close database connections if needed
    }

    /**
     * Iterator implementation for SQL Server records.
     */
    private static class SqlServerEntityRecordIterator implements EntityRecordIterator<EntityRecord> {
        private final ResultSet resultSet;
        private final Statement statement;
        private final Connection connection;
        private final String entityName;
        private final String namespace;
        private final ResultSetMetaData metaData;
        private Boolean hasNext;

        public SqlServerEntityRecordIterator(ResultSet resultSet, Statement statement, Connection connection,
                                            String entityName, String namespace) throws SQLException {
            this.resultSet = resultSet;
            this.statement = statement;
            this.connection = connection;
            this.entityName = entityName;
            this.namespace = namespace;
            this.metaData = resultSet.getMetaData();
        }

        @Override
        public boolean hasNext() {
            if (hasNext == null) {
                try {
                    hasNext = resultSet.next();
                } catch (SQLException e) {
                    hasNext = false;
                }
            }
            return hasNext;
        }

        @Override
        public EntityRecord next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            try {
                Map<String, Object> data = new HashMap<>();

                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = resultSet.getObject(i);
                    data.put(columnName, value);
                }

                EntityRecord record = new EntityRecord();
                record.setEntityName(entityName);
                record.setNamespace(namespace);
                record.setData(data);
                record.setEmittedAt(System.currentTimeMillis());

                hasNext = null; // Reset for next iteration
                return record;
            } catch (SQLException e) {
                throw new RuntimeException("Error reading record", e);
            }
        }

        @Override
        public void close() throws Exception {
            if (resultSet != null) resultSet.close();
            if (statement != null) statement.close();
            if (connection != null) connection.close();
        }
    }

    /**
     * Empty iterator for when there are no entities.
     */
    private static class EmptyEntityRecordIterator implements EntityRecordIterator<EntityRecord> {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public EntityRecord next() {
            throw new NoSuchElementException();
        }

        @Override
        public void close() {
            // Nothing to close
        }
    }
}
