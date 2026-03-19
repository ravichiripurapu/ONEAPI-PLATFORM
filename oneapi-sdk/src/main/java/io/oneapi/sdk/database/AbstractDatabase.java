package io.oneapi.sdk.database;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Base class for database implementations.
 * Holds source and database configuration.
 */
public class AbstractDatabase {
    private JsonNode sourceConfig;
    private JsonNode databaseConfig;

    public JsonNode getSourceConfig() {
        return sourceConfig;
    }

    public void setSourceConfig(JsonNode sourceConfig) {
        this.sourceConfig = sourceConfig;
    }

    public JsonNode getDatabaseConfig() {
        return databaseConfig;
    }

    public void setDatabaseConfig(JsonNode databaseConfig) {
        this.databaseConfig = databaseConfig;
    }
}
