package io.oneapi.sdk.model;

import java.util.Map;

/**
 * Represents a single record from an entity/table.
 * Uses Map<String, Object> for data to provide natural integration with REST/GraphQL APIs.
 */
public class EntityRecord {
    private String entityName;
    private String namespace;
    private Map<String, Object> data;
    private Long emittedAt;

    public EntityRecord() {
    }

    public EntityRecord(String entityName, String namespace, Map<String, Object> data, Long emittedAt) {
        this.entityName = entityName;
        this.namespace = namespace;
        this.data = data;
        this.emittedAt = emittedAt;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Long getEmittedAt() {
        return emittedAt;
    }

    public void setEmittedAt(Long emittedAt) {
        this.emittedAt = emittedAt;
    }
}
