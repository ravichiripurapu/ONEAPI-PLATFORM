package io.oneapi.admin.model;

import io.oneapi.sdk.model.EntityRecord;
import io.oneapi.sdk.model.EntityRecordIterator;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a stateful query session for incremental data fetching.
 * Supports both in-memory (Caffeine) and distributed (Redis) caching.
 */
public class QuerySession implements Serializable {

    private static final long serialVersionUID = 1L;

    // Session identification
    private String sessionKey;              // UUID
    private String userId;                  // Who owns this session

    // Query parameters
    private Long datasourceId;              // Which database
    private String tableName;               // Which table (null for SQL queries)
    private String schema;                  // Optional schema
    private Map<String, Object> filters;    // Optional WHERE conditions

    // NEW: SQL query execution
    private String sqlQuery;                // Custom SQL query (null for table queries)
    private boolean isSqlQuery;             // Flag to distinguish SQL vs table queries

    // Pagination state
    private long offset;                    // Current position (for offset-based)
    private int pageSize;                   // Records per fetch
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime expiresAt;

    // Session state
    private boolean hasMore;                // Are there more records?
    private boolean closed;                 // Explicitly closed

    // Statistics
    private long totalFetched;              // Total records returned so far
    private int requestCount;               // Number of fetch requests

    // Iterator state (transient - only for Caffeine, not serialized to Redis)
    private transient EntityRecordIterator<EntityRecord> iterator; // For table queries
    private transient boolean iteratorActive; // Is iterator still open?

    // NEW: SQL iterator state (transient)
    private transient SQLResultIterator sqlIterator; // For SQL queries
    private transient boolean sqlIteratorActive; // Is SQL iterator still open?

    // Strategy flag
    private boolean useIteratorStrategy;    // true=Caffeine(iterator), false=Redis(offset)

    public QuerySession() {
    }

    public QuerySession(String sessionKey, String userId, Long datasourceId, String tableName) {
        this.sessionKey = sessionKey;
        this.userId = userId;
        this.datasourceId = datasourceId;
        this.tableName = tableName;
        this.createdAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
        this.offset = 0;
        this.totalFetched = 0;
        this.requestCount = 0;
        this.hasMore = true;
        this.closed = false;
        this.iteratorActive = false;
    }

    /**
     * Check if session is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Refresh the last accessed time and expiry.
     */
    public void touch(int ttlMinutes) {
        this.lastAccessedAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusMinutes(ttlMinutes);
    }

    /**
     * Mark session as closed and cleanup resources.
     */
    public void close() {
        this.closed = true;
        this.hasMore = false;
        closeIterator();
        closeSqlIterator();
    }

    /**
     * Close the iterator if active.
     */
    public void closeIterator() {
        if (iterator != null && iteratorActive) {
            try {
                iterator.close();
            } catch (Exception e) {
                // Log but don't throw
            }
            iteratorActive = false;
        }
    }

    // Getters and Setters

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public long getTotalFetched() {
        return totalFetched;
    }

    public void setTotalFetched(long totalFetched) {
        this.totalFetched = totalFetched;
    }

    public int getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(int requestCount) {
        this.requestCount = requestCount;
    }

    public EntityRecordIterator<EntityRecord> getIterator() {
        return iterator;
    }

    public void setIterator(EntityRecordIterator<EntityRecord> iterator) {
        this.iterator = iterator;
        this.iteratorActive = (iterator != null);
    }

    public boolean isIteratorActive() {
        return iteratorActive;
    }

    public void setIteratorActive(boolean iteratorActive) {
        this.iteratorActive = iteratorActive;
    }

    public boolean isUseIteratorStrategy() {
        return useIteratorStrategy;
    }

    public void setUseIteratorStrategy(boolean useIteratorStrategy) {
        this.useIteratorStrategy = useIteratorStrategy;
    }

    public void incrementRequestCount() {
        this.requestCount++;
    }

    public void addFetchedRecords(int count) {
        this.totalFetched += count;
    }

    // NEW: SQL query getters/setters

    public String getSqlQuery() {
        return sqlQuery;
    }

    public void setSqlQuery(String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }

    public boolean isSqlQuery() {
        return isSqlQuery;
    }

    public void setIsSqlQuery(boolean isSqlQuery) {
        this.isSqlQuery = isSqlQuery;
    }

    public SQLResultIterator getSqlIterator() {
        return sqlIterator;
    }

    public void setSqlIterator(SQLResultIterator sqlIterator) {
        this.sqlIterator = sqlIterator;
        this.sqlIteratorActive = (sqlIterator != null);
    }

    public boolean isSqlIteratorActive() {
        return sqlIteratorActive;
    }

    public void setSqlIteratorActive(boolean sqlIteratorActive) {
        this.sqlIteratorActive = sqlIteratorActive;
    }

    /**
     * Close the SQL iterator if active.
     */
    public void closeSqlIterator() {
        if (sqlIterator != null && sqlIteratorActive) {
            try {
                sqlIterator.close();
            } catch (Exception e) {
                // Log but don't throw
            }
            sqlIteratorActive = false;
        }
    }
}
