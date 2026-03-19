package io.oneapi.sdk.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines the mode of data synchronization.
 */
public enum SyncMode {
    FULL_REFRESH("full_refresh"),
    INCREMENTAL("incremental");

    private final String value;
    private static final Map<String, SyncMode> CONSTANTS = new HashMap<>();

    SyncMode(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    @JsonValue
    public String value() {
        return this.value;
    }

    @JsonCreator
    public static SyncMode fromValue(String value) {
        SyncMode constant = CONSTANTS.get(value);
        if (constant == null) {
            throw new IllegalArgumentException("Unknown sync mode: " + value);
        }
        return constant;
    }

    static {
        for (SyncMode mode : values()) {
            CONSTANTS.put(mode.value, mode);
        }
    }
}
