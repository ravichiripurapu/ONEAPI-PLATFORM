package io.oneapi.admin.model;

import io.oneapi.sdk.database.JdbcDatabase;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;

/**
 * Iterator for SQL query results that supports pagination.
 * Similar to EntityRecordIterator but for custom SQL queries.
 * Wraps a JDBC ResultSet and allows streaming through large result sets.
 */
@Slf4j
public class SQLResultIterator implements AutoCloseable {

    private final JdbcDatabase database;
    private final String sql;
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;
    private ResultSetMetaData metadata;
    private List<String> columnNames;
    private boolean hasNext;
    private boolean closed = false;

    public SQLResultIterator(JdbcDatabase database, String sql) throws SQLException {
        this.database = database;
        this.sql = sql;
        initialize();
    }

    private void initialize() throws SQLException {
        log.debug("Initializing SQLResultIterator for query: {}",
                  sql.substring(0, Math.min(100, sql.length())));

        this.connection = database.getConnection();
        this.statement = connection.createStatement(
            ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_READ_ONLY
        );

        // Set fetch size for memory efficiency
        statement.setFetchSize(100);

        // Set query timeout (30 seconds)
        statement.setQueryTimeout(30);

        this.resultSet = statement.executeQuery(sql);
        this.metadata = resultSet.getMetaData();

        // Cache column names
        int columnCount = metadata.getColumnCount();
        this.columnNames = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            columnNames.add(metadata.getColumnName(i));
        }

        // Check if there's at least one row
        this.hasNext = resultSet.next();

        log.debug("SQLResultIterator initialized with {} columns", columnNames.size());
    }

    public boolean hasNext() {
        return hasNext && !closed;
    }

    public Map<String, Object> next() throws SQLException {
        if (!hasNext || closed) {
            throw new NoSuchElementException("No more results");
        }

        Map<String, Object> record = new LinkedHashMap<>();

        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);
            Object value = resultSet.getObject(i + 1);
            record.put(columnName, value);
        }

        // Advance to next row
        hasNext = resultSet.next();

        return record;
    }

    public List<String> getColumnNames() {
        return Collections.unmodifiableList(columnNames);
    }

    public int getColumnCount() {
        return columnNames.size();
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        log.debug("Closing SQLResultIterator");

        try {
            if (resultSet != null && !resultSet.isClosed()) {
                resultSet.close();
            }
        } catch (SQLException e) {
            log.warn("Error closing ResultSet", e);
        }

        try {
            if (statement != null && !statement.isClosed()) {
                statement.close();
            }
        } catch (SQLException e) {
            log.warn("Error closing Statement", e);
        }

        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            log.warn("Error closing Connection", e);
        }

        closed = true;
    }
}
