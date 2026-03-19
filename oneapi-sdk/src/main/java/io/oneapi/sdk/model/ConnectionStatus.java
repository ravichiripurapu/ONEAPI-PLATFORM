package io.oneapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the status of a connection check.
 */
public class ConnectionStatus {

    private Status status;
    private String message;

    public ConnectionStatus() {
    }

    public ConnectionStatus(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public enum Status {
        SUCCEEDED("SUCCEEDED"),
        FAILED("FAILED");

        private final String value;
        private static final Map<String, Status> CONSTANTS = new HashMap<>();

        Status(String value) {
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
        public static Status fromValue(String value) {
            Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException("Unknown status: " + value);
            }
            return constant;
        }

        static {
            for (Status c : values()) {
                CONSTANTS.put(c.value, c);
            }
        }
    }
}
