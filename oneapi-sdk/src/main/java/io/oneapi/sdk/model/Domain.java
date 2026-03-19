package io.oneapi.sdk.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a domain containing multiple data entities (tables/collections).
 * A domain typically corresponds to a schema or namespace in a data source.
 */
public class Domain {
    private List<DataEntity> entities = new ArrayList<>();

    public Domain() {
    }

    public Domain(List<DataEntity> entities) {
        this.entities = entities;
    }

    public List<DataEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<DataEntity> entities) {
        this.entities = entities;
    }
}
