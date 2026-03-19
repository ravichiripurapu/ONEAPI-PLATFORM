package io.oneapi.sdk.model;

import java.util.List;
import java.util.Objects;

/**
 * Represents a field/column with its name and type.
 */
public class Field<T> {
    private final String name;
    private final T type;
    private final List<Field<T>> properties;

    public Field(String name, T type) {
        this.name = name;
        this.type = type;
        this.properties = null;
    }

    public Field(String name, T type, List<Field<T>> properties) {
        this.name = name;
        this.type = type;
        this.properties = properties;
    }

    public String getName() {
        return this.name;
    }

    public T getType() {
        return this.type;
    }

    public List<Field<T>> getProperties() {
        return this.properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Field<?> field = (Field<?>) o;
        return name.equals(field.name) && type.equals(field.type) && Objects.equals(properties, field.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, properties);
    }

    @Override
    public String toString() {
        return String.format("Field{name='%s', type=%s, properties=%s}", name, type, properties);
    }
}
