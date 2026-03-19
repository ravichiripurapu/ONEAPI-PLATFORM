package io.oneapi.sdk.jdbc;

/**
 * JDBC utility constants and helper methods.
 */
public class JdbcUtils {

    public static final String HOST_KEY = "host";
    public static final String PORT_KEY = "port";
    public static final String DATABASE_KEY = "database";
    public static final String SCHEMAS_KEY = "schemas";
    public static final String USERNAME_KEY = "username";
    public static final String PASSWORD_KEY = "password";
    public static final String JDBC_URL_KEY = "jdbc_url";
    public static final String JDBC_URL_PARAMS_KEY = "jdbc_url_params";
    public static final String SSL_KEY = "ssl";
    public static final String SSL_MODE_KEY = "sslmode";

    private JdbcUtils() {
        // Utility class
    }

    /**
     * Build JDBC URL for PostgreSQL.
     */
    public static String buildPostgresJdbcUrl(String host, int port, String database, String params) {
        String baseUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
        if (params != null && !params.trim().isEmpty()) {
            return baseUrl + "?" + params;
        }
        return baseUrl;
    }
}
