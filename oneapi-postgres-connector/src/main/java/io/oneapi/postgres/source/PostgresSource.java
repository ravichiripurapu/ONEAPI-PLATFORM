package io.oneapi.postgres.source;

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
 * PostgreSQL implementation of Source.
 */
public class PostgresSource extends AbstractDatabaseSource<JDBCType, JdbcDatabase> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private JdbcDatabase database;

    public PostgresSource() {
        super(DatabaseDriver.POSTGRESQL.getDriverClassName());
    }

    @Override
    protected Set<String> getExcludedInternalNameSpaces() {
        return Set.of("information_schema", "pg_catalog", "pg_toast");
    }

    @Override
    protected Set<String> getExcludedViews() {
        return Set.of("pg_stat_statements", "pg_available_extensions");
    }

    @Override
    public JsonNode toDatabaseConfig(JsonNode config) {
        ObjectNode dbConfig = MAPPER.createObjectNode();

        String host = config.has(JdbcUtils.HOST_KEY) ? config.get(JdbcUtils.HOST_KEY).asText() : "localhost";
        int port = config.has(JdbcUtils.PORT_KEY) ? config.get(JdbcUtils.PORT_KEY).asInt() : 5432;
        String database = config.get(JdbcUtils.DATABASE_KEY).asText();
        String username = config.has(JdbcUtils.USERNAME_KEY) ? config.get(JdbcUtils.USERNAME_KEY).asText() : null;
        String password = config.has(JdbcUtils.PASSWORD_KEY) ? config.get(JdbcUtils.PASSWORD_KEY).asText() : null;

        String jdbcUrl = JdbcUtils.buildPostgresJdbcUrl(host, port, database, null);

        dbConfig.put(JdbcUtils.JDBC_URL_KEY, jdbcUrl);
        if (username != null) dbConfig.put(JdbcUtils.USERNAME_KEY, username);
        if (password != null) dbConfig.put(JdbcUtils.PASSWORD_KEY, password);

        return dbConfig;
    }

    @Override
    protected List<TableInfo<Field<JDBCType>>> discoverInternal(JdbcDatabase database) throws Exception {
        List<TableInfo<Field<JDBCType>>> allTables = new ArrayList<>();

        // Discover ALL non-system schemas
        String schemaQuery = "SELECT schema_name FROM information_schema.schemata " +
                           "WHERE schema_name NOT IN ('pg_catalog', 'information_schema', 'pg_toast') " +
                           "AND schema_name NOT LIKE 'pg_temp_%' " +
                           "AND schema_name NOT LIKE 'pg_toast_temp_%'";

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

        String query = "SELECT table_schema, table_name " +
                       "FROM information_schema.tables " +
                       "WHERE table_schema = ? AND table_type = 'BASE TABLE' " +
                       "ORDER BY table_name";

        database.query(query, List.of(schema), rs -> {
            while (rs.next()) {
                String tableSchema = rs.getString("table_schema");
                String tableName = rs.getString("table_name");

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

        String query = "SELECT column_name, data_type, udt_name " +
                       "FROM information_schema.columns " +
                       "WHERE table_schema = ? AND table_name = ? " +
                       "ORDER BY ordinal_position";

        database.query(query, List.of(schema, table), rs -> {
            while (rs.next()) {
                String columnName = rs.getString("column_name");
                String dataType = rs.getString("data_type");
                JDBCType jdbcType = mapPostgresTypeToJdbcType(dataType);
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
               type == JDBCType.TIMESTAMP_WITH_TIMEZONE ||
               type == JDBCType.DATE ||
               type == JDBCType.BIGINT ||
               type == JDBCType.INTEGER;
    }

    private JDBCType mapPostgresTypeToJdbcType(String postgresType) {
        return switch (postgresType.toLowerCase()) {
            case "integer", "int", "int4" -> JDBCType.INTEGER;
            case "bigint", "int8" -> JDBCType.BIGINT;
            case "smallint", "int2" -> JDBCType.SMALLINT;
            case "numeric", "decimal" -> JDBCType.NUMERIC;
            case "real", "float4" -> JDBCType.REAL;
            case "double precision", "float8" -> JDBCType.DOUBLE;
            case "character varying", "varchar" -> JDBCType.VARCHAR;
            case "character", "char" -> JDBCType.CHAR;
            case "text" -> JDBCType.VARCHAR;
            case "boolean", "bool" -> JDBCType.BOOLEAN;
            case "date" -> JDBCType.DATE;
            case "timestamp without time zone", "timestamp" -> JDBCType.TIMESTAMP;
            case "timestamp with time zone", "timestamptz" -> JDBCType.TIMESTAMP_WITH_TIMEZONE;
            case "time without time zone", "time" -> JDBCType.TIME;
            case "time with time zone", "timetz" -> JDBCType.TIME_WITH_TIMEZONE;
            case "bytea" -> JDBCType.BINARY;
            case "uuid" -> JDBCType.OTHER;
            case "json", "jsonb" -> JDBCType.OTHER;
            default -> JDBCType.OTHER;
        };
    }

    @Override
    protected Map<String, List<String>> discoverPrimaryKeys(JdbcDatabase database, List<TableInfo<Field<JDBCType>>> tableInfos) {
        Map<String, List<String>> primaryKeys = new HashMap<>();

        for (TableInfo<Field<JDBCType>> tableInfo : tableInfos) {
            String fullyQualifiedName = tableInfo.getNameSpace() + "." + tableInfo.getName();

            try {
                String query = "SELECT a.attname AS column_name " +
                               "FROM pg_index i " +
                               "JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY(i.indkey) " +
                               "WHERE i.indrelid = ?::regclass AND i.indisprimary";

                List<List<String>> result = database.query(
                        query,
                        List.of(fullyQualifiedName),
                        rs -> {
                            List<String> cols = new ArrayList<>();
                            while (rs.next()) {
                                cols.add(rs.getString("column_name"));
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
        return "\"";
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
        String tableName = entity.getNamespace() + "." + getQuoteString() + entity.getName() + getQuoteString();

        String query = "SELECT * FROM " + tableName;

        Connection conn = db.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        return new PostgresEntityRecordIterator(rs, stmt, conn, entity.getName(), entity.getNamespace());
    }

    @Override
    public void close() throws Exception {
        // Close database connections if needed
    }

    /**
     * Iterator implementation for Postgres records.
     */
    private static class PostgresEntityRecordIterator implements EntityRecordIterator<EntityRecord> {
        private final ResultSet resultSet;
        private final Statement statement;
        private final Connection connection;
        private final String entityName;
        private final String namespace;
        private final ResultSetMetaData metaData;
        private Boolean hasNext;

        public PostgresEntityRecordIterator(ResultSet resultSet, Statement statement, Connection connection,
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
