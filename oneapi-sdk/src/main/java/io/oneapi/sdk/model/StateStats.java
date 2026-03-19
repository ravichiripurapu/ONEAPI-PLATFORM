package io.oneapi.sdk.model;

/**
 * Statistics about the state of synchronization.
 */
public class StateStats {
    private Double recordCount;

    public StateStats() {
    }

    public StateStats(Double recordCount) {
        this.recordCount = recordCount;
    }

    public Double getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(Double recordCount) {
        this.recordCount = recordCount;
    }
}
