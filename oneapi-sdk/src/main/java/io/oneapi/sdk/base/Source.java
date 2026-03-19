package io.oneapi.sdk.base;

import com.fasterxml.jackson.databind.JsonNode;
import io.oneapi.sdk.model.Domain;
import io.oneapi.sdk.model.ConnectionStatus;
import io.oneapi.sdk.model.EntityRecord;
import io.oneapi.sdk.model.EntityRecordIterator;
import io.oneapi.sdk.model.State;

/**
 * Source interface for API connectors (Oracle, Postgres, etc.)
 * This interface defines the core operations for database/API sources.
 */
public interface Source {

    /**
     * Check if the connection to the source can be established with the provided configuration.
     *
     * @param config JSON configuration for the connection
     * @return ConnectionStatus indicating success or failure
     * @throws Exception if connection check fails
     */
    ConnectionStatus check(JsonNode config) throws Exception;

    /**
     * Discover the schema/domain from the source.
     *
     * @param config JSON configuration for the connection
     * @return Domain containing discovered entities and their schemas
     * @throws Exception if discovery fails
     */
    Domain discover(JsonNode config) throws Exception;

    /**
     * Read data from the source based on the provided configuration, domain, and state.
     *
     * @param config  JSON configuration for the connection
     * @param domain Domain defining what entities to read
     * @param state   State containing information about previous reads (for incremental sync)
     * @return Iterator of EntityRecords
     * @throws Exception if read operation fails
     */
    EntityRecordIterator<EntityRecord> read(JsonNode config, Domain domain, State state) throws Exception;
}
