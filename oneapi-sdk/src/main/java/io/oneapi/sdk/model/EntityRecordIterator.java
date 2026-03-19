package io.oneapi.sdk.model;

import java.util.Iterator;

/**
 * Iterator for EntityRecords that extends AutoCloseable for resource management.
 */
public interface EntityRecordIterator<T> extends Iterator<T>, AutoCloseable {
}
