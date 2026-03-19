package io.oneapi.sdk.model;

import io.oneapi.sdk.enums.SyncMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a data entity (table, collection, etc.) with its schema and metadata.
 *
 * This is the core SDK model that carries complete field/column information.
 * The fields list provides direct, type-safe access to column metadata without
 * requiring JSON serialization/deserialization.
 */
public class DataEntity {
    private String name;
    private String namespace;
    private List<Field<?>> fields = new ArrayList<>();
    private SyncMode syncMode;
    private Boolean hasIncrementalField;
    private List<String> incrementalFields = new ArrayList<>();
    private List<List<String>> primaryKeys = new ArrayList<>();

    public DataEntity() {
    }

    public DataEntity(String name, String namespace, List<Field<?>> fields, SyncMode syncMode) {
        this.name = name;
        this.namespace = namespace;
        this.fields = fields;
        this.syncMode = syncMode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public List<Field<?>> getFields() {
        return fields;
    }

    public void setFields(List<Field<?>> fields) {
        this.fields = fields;
    }

    public SyncMode getSyncMode() {
        return syncMode;
    }

    public void setSyncMode(SyncMode syncMode) {
        this.syncMode = syncMode;
    }

    public Boolean getHasIncrementalField() {
        return hasIncrementalField;
    }

    public void setHasIncrementalField(Boolean hasIncrementalField) {
        this.hasIncrementalField = hasIncrementalField;
    }

    public List<String> getIncrementalFields() {
        return incrementalFields;
    }

    public void setIncrementalFields(List<String> incrementalFields) {
        this.incrementalFields = incrementalFields;
    }

    public List<List<String>> getPrimaryKeys() {
        return primaryKeys;
    }

    public void setPrimaryKeys(List<List<String>> primaryKeys) {
        this.primaryKeys = primaryKeys;
    }
}
