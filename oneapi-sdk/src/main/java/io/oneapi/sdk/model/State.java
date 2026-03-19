package io.oneapi.sdk.model;

/**
 * Represents the state of data synchronization.
 * Used for incremental syncs to track progress for every run.
 */
public class State {
    private StateStats sourceStats;

    public State() {
    }

    public State(StateStats sourceStats) {
        this.sourceStats = sourceStats;
    }

    public StateStats getSourceStats() {
        return sourceStats;
    }

    public void setSourceStats(StateStats sourceStats) {
        this.sourceStats = sourceStats;
    }
}
