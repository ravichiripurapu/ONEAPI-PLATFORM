package io.oneapi.admin.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response model for query data.
 * Contains records and metadata about the query session.
 */
public class QueryResponse {

    private String sessionKey;              // For next request (null if complete)
    private List<Map<String, Object>> records;
    private QueryMetadata metadata;

    public QueryResponse() {
    }

    public QueryResponse(String sessionKey, List<Map<String, Object>> records, QueryMetadata metadata) {
        this.sessionKey = sessionKey;
        this.records = records;
        this.metadata = metadata;
    }

    // Getters and Setters

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public List<Map<String, Object>> getRecords() {
        return records;
    }

    public void setRecords(List<Map<String, Object>> records) {
        this.records = records;
    }

    public QueryMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(QueryMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Metadata about the query and session.
     */
    public static class QueryMetadata {
        private int recordCount;            // Records in this response
        private long totalFetched;          // Total records fetched so far
        private boolean hasMore;            // More records available?
        private LocalDateTime expiresAt;    // When session expires
        private int pageSize;               // Records per page
        private int requestCount;           // Number of requests made

        public QueryMetadata() {
        }

        public QueryMetadata(int recordCount, long totalFetched, boolean hasMore,
                             LocalDateTime expiresAt, int pageSize, int requestCount) {
            this.recordCount = recordCount;
            this.totalFetched = totalFetched;
            this.hasMore = hasMore;
            this.expiresAt = expiresAt;
            this.pageSize = pageSize;
            this.requestCount = requestCount;
        }

        // Getters and Setters

        public int getRecordCount() {
            return recordCount;
        }

        public void setRecordCount(int recordCount) {
            this.recordCount = recordCount;
        }

        public long getTotalFetched() {
            return totalFetched;
        }

        public void setTotalFetched(long totalFetched) {
            this.totalFetched = totalFetched;
        }

        public boolean isHasMore() {
            return hasMore;
        }

        public void setHasMore(boolean hasMore) {
            this.hasMore = hasMore;
        }

        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public int getRequestCount() {
            return requestCount;
        }

        public void setRequestCount(int requestCount) {
            this.requestCount = requestCount;
        }
    }
}
