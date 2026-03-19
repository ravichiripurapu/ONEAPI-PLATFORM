package io.oneapi.sdk.model;

import java.util.List;

/**
 * Represents table/collection information including fields and keys.
 */
public class TableInfo<T> {

    private final String nameSpace;
    private final String name;
    private final List<T> fields;
    private final List<String> primaryKeys;
    private final List<String> incrementalFields;

    public TableInfo(String nameSpace, String name, List<T> fields, List<String> primaryKeys, List<String> incrementalFields) {
        this.nameSpace = nameSpace;
        this.name = name;
        this.fields = fields;
        this.primaryKeys = primaryKeys;
        this.incrementalFields = incrementalFields;
    }

    public String getNameSpace() {
        return nameSpace;
    }

    public String getName() {
        return name;
    }

    public List<T> getFields() {
        return fields;
    }

    public List<String> getPrimaryKeys() {
        return primaryKeys;
    }

    public List<String> getIncrementalFields() {
        return incrementalFields;
    }
}
