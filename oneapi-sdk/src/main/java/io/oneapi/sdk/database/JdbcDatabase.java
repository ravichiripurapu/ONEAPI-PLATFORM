package io.oneapi.sdk.database;

import com.fasterxml.jackson.databind.JsonNode;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;

/**
 * JDBC database implementation for executing queries and managing connections.
 */
public class JdbcDatabase extends AbstractDatabase {

    private final DataSource dataSource;

    public JdbcDatabase(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        try (Connection conn = getConnection()) {
            return conn.getMetaData();
        }
    }

    /**
     * Execute a query and process results with a handler.
     */
    public <T> List<T> query(String sql, ResultSetHandler<T> handler) throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return handler.handle(rs);
        }
    }

    /**
     * Execute a query with parameters.
     */
    public <T> List<T> query(String sql, List<Object> params, ResultSetHandler<T> handler) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                return handler.handle(rs);
            }
        }
    }

    /**
     * Execute an update statement.
     */
    public int execute(String sql) throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }

    @FunctionalInterface
    public interface ResultSetHandler<T> {
        List<T> handle(ResultSet rs) throws SQLException;
    }
}
