package io.oneapi.admin.model;

import java.util.List;
import java.util.Map;

/**
 * Request model for querying data.
 * Can be used for both starting a new query or fetching next page.
 */
public class QueryRequest {

    // For continuing existing query
    private String sessionKey;

    // For starting new query
    private Long datasourceId;
    private String tableName;
    private String schema;

    // NEW: For custom SQL execution
    private String sqlQuery;                // Custom SQL query to execute
    private Map<String, Object> parameters; // SQL parameters for substitution

    // Optional filters and selections
    private Map<String, Object> filters;    // WHERE conditions
    private List<String> columns;           // SELECT specific columns
    private String orderBy;                 // ORDER BY clause

    public QueryRequest() {
    }

    public QueryRequest(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public QueryRequest(Long datasourceId, String tableName) {
        this.datasourceId = datasourceId;
        this.tableName = tableName;
    }

    /**
     * Check if this is a continuation request (has session key).
     */
    public boolean isContinuation() {
        return sessionKey != null && !sessionKey.isEmpty();
    }

    /**
     * Check if this is a new table query request.
     */
    public boolean isNewQuery() {
        return datasourceId != null && tableName != null;
    }

    /**
     * Check if this is a table query (not SQL).
     */
    public boolean isTableQuery() {
        return tableName != null && sqlQuery == null;
    }

    /**
     * Check if this is a custom SQL query.
     */
    public boolean isSqlQuery() {
        return sqlQuery != null && tableName == null;
    }

    // Getters and Setters

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public Long getDatasourceId() {
        return datasourceId;
    }

    public void setDatasourceId(Long datasourceId) {
        this.datasourceId = datasourceId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public String getSqlQuery() {
        return sqlQuery;
    }

    public void setSqlQuery(String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}
