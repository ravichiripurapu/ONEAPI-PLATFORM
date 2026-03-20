package io.oneapi.admin.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.oneapi.admin.entity.SourceInfo;
import io.oneapi.h2.source.H2Source;
import io.oneapi.postgres.source.PostgresSource;
import io.oneapi.sdk.base.Source;
// import io.oneapi.sqlserver.source.SqlServerSource; // TODO: Implement SQL Server connector
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Factory for creating database connectors based on database type.
 */
@Slf4j
@Component
public class ConnectorFactory {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // JDBC URL patterns for different database types
    private static final Pattern POSTGRES_URL_PATTERN = Pattern.compile(
            "jdbc:postgresql://([^:/]+)(?::(\\d+))?/([^?]+).*");
    private static final Pattern MYSQL_URL_PATTERN = Pattern.compile(
            "jdbc:mysql://([^:/]+)(?::(\\d+))?/([^?]+).*");
    private static final Pattern SQLSERVER_URL_PATTERN = Pattern.compile(
            "jdbc:sqlserver://([^:/;]+)(?::(\\d+))?;.*databaseName=([^;]+).*");
    private static final Pattern H2_FILE_PATTERN = Pattern.compile(
            "jdbc:h2:file:(.+?)(?:;.*)?$");
    private static final Pattern H2_MEM_PATTERN = Pattern.compile(
            "jdbc:h2:mem:(.+?)(?:;.*)?$");

    /**
     * Create a Source connector for the given database type.
     */
    public Source createSource(SourceInfo.DatabaseType type) {
        log.debug("Creating source connector for database type: {}", type);
        return switch (type) {
            case POSTGRESQL -> new PostgresSource();
            case H2 -> new H2Source();
            case MSSQL -> throw new UnsupportedOperationException("SQL Server connector not yet implemented");
            case MYSQL -> throw new UnsupportedOperationException("MySQL connector not yet implemented");
            case ORACLE -> throw new UnsupportedOperationException("Oracle connector not yet implemented");
            case MARIADB -> throw new UnsupportedOperationException("MariaDB connector not yet implemented");
            default -> throw new IllegalArgumentException("Unsupported database type: " + type);
        };
    }

    /**
     * Create a Source connector for the given database connection.
     */
    public Source createSource(SourceInfo connection) {
        return createSource(connection.getType());
    }

    /**
     * Get Source connector for the given database connection.
     * Alias for createSource to match SDK naming convention.
     */
    public Source getSource(SourceInfo connection) {
        log.info("Getting source connector for connection: {} (type: {})",
                connection.getName(), connection.getType());
        return createSource(connection.getType());
    }

    /**
     * Convert Datasource to connector configuration JsonNode.
     * This creates the configuration format expected by SDK Source connectors.
     *
     * @param connection the database connection
     * @return JsonNode configuration for the connector
     */
    public JsonNode toConnectorConfig(SourceInfo connection) {
        log.debug("Converting database connection to connector config: {}", connection.getName());

        ObjectNode config = OBJECT_MAPPER.createObjectNode();

        try {
            switch (connection.getType()) {
                case POSTGRESQL -> buildPostgresConfig(config, connection);
                case H2 -> buildH2Config(config, connection);
                case MSSQL -> buildSqlServerConfig(config, connection);
                case MYSQL -> buildMySqlConfig(config, connection);
                default -> {
                    log.warn("Unsupported database type for config conversion: {}", connection.getType());
                    throw new UnsupportedOperationException(
                            "Configuration conversion not implemented for: " + connection.getType());
                }
            }

            log.debug("Successfully created connector config for: {}", connection.getName());
            return config;
        } catch (Exception e) {
            log.error("Error creating connector config for connection: {}", connection.getName(), e);
            throw new RuntimeException("Failed to create connector configuration", e);
        }
    }

    /**
     * Build PostgreSQL connector configuration.
     */
    private void buildPostgresConfig(ObjectNode config, SourceInfo connection) {
        config.put("host", connection.getHost());
        config.put("port", connection.getPort() != null ? connection.getPort() : 5432);
        config.put("database", connection.getDatabase());
        config.put("username", connection.getUsername());
        config.put("password", connection.getPassword());
    }

    /**
     * Build H2 connector configuration.
     */
    private void buildH2Config(ObjectNode config, SourceInfo connection) {
        // H2 can be TCP, file-based, or in-memory
        // Check if host and port are provided for TCP connection
        String database = connection.getDatabase();

        if (connection.getHost() != null && !connection.getHost().isEmpty() && connection.getPort() != null && connection.getPort() > 0) {
            // TCP mode: Remote H2 database server
            config.put("host", connection.getHost());
            config.put("port", connection.getPort());
            config.put("database", database != null ? database : "testdb");
        } else if (database != null && (database.startsWith("./") || database.startsWith("/"))) {
            // File-based H2
            config.put("mode", "file");
            config.put("filePath", database);
            config.put("database", extractDatabaseName(database));
        } else {
            // Memory-based H2 (default)
            config.put("mode", "mem");
            config.put("database", database != null ? database : "testdb");
        }

        config.put("username", connection.getUsername() != null ? connection.getUsername() : "sa");
        config.put("password", connection.getPassword() != null ? connection.getPassword() : "");
    }

    /**
     * Build SQL Server connector configuration.
     */
    private void buildSqlServerConfig(ObjectNode config, SourceInfo connection) {
        config.put("host", connection.getHost());
        config.put("port", connection.getPort() != null ? connection.getPort() : 1433);
        config.put("database", connection.getDatabase());
        config.put("username", connection.getUsername());
        config.put("password", connection.getPassword());
    }

    /**
     * Build MySQL connector configuration.
     */
    private void buildMySqlConfig(ObjectNode config, SourceInfo connection) {
        config.put("host", connection.getHost());
        config.put("port", connection.getPort() != null ? connection.getPort() : 3306);
        config.put("database", connection.getDatabase());
        config.put("username", connection.getUsername());
        config.put("password", connection.getPassword());
    }

    /**
     * Parse JDBC URL to extract connection components.
     * Returns a map with keys: host, port, database, mode, filePath
     *
     * @param jdbcUrl the JDBC URL to parse
     * @return map containing extracted connection parameters
     */
    private Map<String, String> parseJdbcUrl(String jdbcUrl) {
        log.debug("Parsing JDBC URL: {}", jdbcUrl);
        Map<String, String> params = new HashMap<>();

        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            log.warn("Empty or null JDBC URL provided");
            return params;
        }

        try {
            // PostgreSQL: jdbc:postgresql://host:port/database
            Matcher postgresMatcher = POSTGRES_URL_PATTERN.matcher(jdbcUrl);
            if (postgresMatcher.matches()) {
                params.put("host", postgresMatcher.group(1));
                params.put("port", postgresMatcher.group(2) != null ? postgresMatcher.group(2) : "5432");
                params.put("database", postgresMatcher.group(3));
                log.debug("Parsed as PostgreSQL URL");
                return params;
            }

            // MySQL: jdbc:mysql://host:port/database
            Matcher mysqlMatcher = MYSQL_URL_PATTERN.matcher(jdbcUrl);
            if (mysqlMatcher.matches()) {
                params.put("host", mysqlMatcher.group(1));
                params.put("port", mysqlMatcher.group(2) != null ? mysqlMatcher.group(2) : "3306");
                params.put("database", mysqlMatcher.group(3));
                log.debug("Parsed as MySQL URL");
                return params;
            }

            // SQL Server: jdbc:sqlserver://host:port;databaseName=database
            Matcher sqlServerMatcher = SQLSERVER_URL_PATTERN.matcher(jdbcUrl);
            if (sqlServerMatcher.matches()) {
                params.put("host", sqlServerMatcher.group(1));
                params.put("port", sqlServerMatcher.group(2) != null ? sqlServerMatcher.group(2) : "1433");
                params.put("database", sqlServerMatcher.group(3));
                log.debug("Parsed as SQL Server URL");
                return params;
            }

            // H2 file-based: jdbc:h2:file:./data/testdb
            Matcher h2FileMatcher = H2_FILE_PATTERN.matcher(jdbcUrl);
            if (h2FileMatcher.matches()) {
                String filePath = h2FileMatcher.group(1);
                params.put("mode", "file");
                params.put("filePath", filePath);
                params.put("database", extractDatabaseName(filePath));
                log.debug("Parsed as H2 file-based URL");
                return params;
            }

            // H2 memory-based: jdbc:h2:mem:testdb
            Matcher h2MemMatcher = H2_MEM_PATTERN.matcher(jdbcUrl);
            if (h2MemMatcher.matches()) {
                params.put("mode", "mem");
                params.put("database", h2MemMatcher.group(1));
                log.debug("Parsed as H2 memory-based URL");
                return params;
            }

            log.warn("Could not parse JDBC URL with known patterns: {}", jdbcUrl);
        } catch (Exception e) {
            log.error("Error parsing JDBC URL: {}", jdbcUrl, e);
        }

        return params;
    }

    /**
     * Extract database name from file path.
     * Handles paths like "./data/testdb" or "/var/lib/h2/testdb"
     */
    private String extractDatabaseName(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "testdb";
        }

        // Get the last segment of the path
        String[] parts = filePath.split("[/\\\\]");
        String lastPart = parts[parts.length - 1];

        // Remove file extensions if present
        int dotIndex = lastPart.lastIndexOf('.');
        if (dotIndex > 0) {
            return lastPart.substring(0, dotIndex);
        }

        return lastPart;
    }

    /**
     * Check if a database type is supported.
     */
    public boolean isSupported(SourceInfo.DatabaseType type) {
        return type == SourceInfo.DatabaseType.POSTGRESQL ||
               type == SourceInfo.DatabaseType.H2 ||
               type == SourceInfo.DatabaseType.MSSQL;
    }
}
