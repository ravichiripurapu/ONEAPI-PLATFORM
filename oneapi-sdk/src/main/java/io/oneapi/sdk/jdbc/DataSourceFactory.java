package io.oneapi.sdk.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;

/**
 * Factory for creating DataSource instances.
 */
public class DataSourceFactory {

    /**
     * Create a HikariCP DataSource.
     */
    public static DataSource create(
            String username,
            String password,
            String driverClassName,
            String jdbcUrl,
            Map<String, String> connectionProperties) {

        return create(username, password, driverClassName, jdbcUrl, connectionProperties, 60);
    }

    /**
     * Create a HikariCP DataSource with connection timeout.
     */
    public static DataSource create(
            String username,
            String password,
            String driverClassName,
            String jdbcUrl,
            Map<String, String> connectionProperties,
            long connectionTimeoutSeconds) {

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(connectionTimeoutSeconds * 1000);
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes

        if (connectionProperties != null) {
            Properties props = new Properties();
            props.putAll(connectionProperties);
            config.setDataSourceProperties(props);
        }

        return new HikariDataSource(config);
    }
}
