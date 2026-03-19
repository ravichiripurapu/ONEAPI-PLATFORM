package io.oneapi.sdk.model;

import java.util.Objects;

/**
 * Information about incremental field for tracking data sync progress.
 */
public class IncrementFieldInfo {

    private final String originalIncrementField;
    private final String originalIncrement;
    private final long originalIncrementRecordCount;

    private final String incrementField;
    private String increment;
    private long incrementRecordCount;

    public IncrementFieldInfo(String originalIncrementField,
                              String originalIncrement,
                              String incrementField,
                              String increment) {
        this(originalIncrementField, originalIncrement, 0L, incrementField, increment, 0L);
    }

    public IncrementFieldInfo(String originalIncrementField,
                              String originalIncrement,
                              long originalIncrementRecordCount,
                              String incrementField,
                              String increment,
                              long incrementRecordCount) {
        this.originalIncrementField = originalIncrementField;
        this.originalIncrement = originalIncrement;
        this.originalIncrementRecordCount = originalIncrementRecordCount;
        this.incrementField = incrementField;
        this.increment = increment;
        this.incrementRecordCount = incrementRecordCount;
    }

    public String getOriginalIncrementField() {
        return originalIncrementField;
    }

    public String getOriginalIncrement() {
        return originalIncrement;
    }

    public long getOriginalIncrementRecordCount() {
        return originalIncrementRecordCount;
    }

    public String getIncrementField() {
        return incrementField;
    }

    public String getIncrement() {
        return increment;
    }

    public long getIncrementRecordCount() {
        return incrementRecordCount;
    }

    public IncrementFieldInfo setIncrement(String increment) {
        this.increment = increment;
        return this;
    }

    public IncrementFieldInfo setIncrementRecordCount(long incrementRecordCount) {
        this.incrementRecordCount = incrementRecordCount;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IncrementFieldInfo that = (IncrementFieldInfo) o;
        return originalIncrementRecordCount == that.originalIncrementRecordCount &&
                incrementRecordCount == that.incrementRecordCount &&
                Objects.equals(originalIncrementField, that.originalIncrementField) &&
                Objects.equals(originalIncrement, that.originalIncrement) &&
                Objects.equals(incrementField, that.incrementField) &&
                Objects.equals(increment, that.increment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalIncrementField, originalIncrement, originalIncrementRecordCount,
                incrementField, increment, incrementRecordCount);
    }

    @Override
    public String toString() {
        return "IncrementFieldInfo{" +
                "originalIncrementField='" + originalIncrementField + '\'' +
                ", originalIncrement='" + originalIncrement + '\'' +
                ", originalIncrementRecordCount=" + originalIncrementRecordCount +
                ", incrementField='" + incrementField + '\'' +
                ", increment='" + increment + '\'' +
                ", incrementRecordCount=" + incrementRecordCount +
                '}';
    }
}
