package io.oneapi.sdk.database;

/**
 * Enum of supported database drivers with their JDBC URLs.
 */
public enum DatabaseDriver {

    POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql://%s:%d/%s"),
    MYSQL("com.mysql.cj.jdbc.Driver", "jdbc:mysql://%s:%d/%s"),
    H2("org.h2.Driver", "jdbc:h2:mem:%s"),
    ORACLE("oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@%s:%d/%s"),
    MSSQLSERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://%s:%d/%s"),
    MARIADB("org.mariadb.jdbc.Driver", "jdbc:mariadb://%s:%d/%s"),
    DB2("com.ibm.db2.jcc.DB2Driver", "jdbc:db2://%s:%d/%s"),
    REDSHIFT("com.amazon.redshift.jdbc.Driver", "jdbc:redshift://%s:%d/%s"),
    SNOWFLAKE("net.snowflake.client.jdbc.SnowflakeDriver", "jdbc:snowflake://%s/");

    private final String driverClassName;
    private final String urlFormatString;

    DatabaseDriver(String driverClassName, String urlFormatString) {
        this.driverClassName = driverClassName;
        this.urlFormatString = urlFormatString;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getUrlFormatString() {
        return urlFormatString;
    }

    /**
     * Finds the DatabaseDriver that matches the provided driver class name.
     *
     * @param driverClassName The driver class name
     * @return The matching DatabaseDriver or null if not found
     */
    public static DatabaseDriver findByDriverClassName(String driverClassName) {
        for (DatabaseDriver candidate : values()) {
            if (candidate.getDriverClassName().equalsIgnoreCase(driverClassName)) {
                return candidate;
            }
        }
        return null;
    }
}
