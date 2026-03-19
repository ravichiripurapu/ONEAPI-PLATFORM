package io.oneapi.admin.repository.metadata;

import io.oneapi.admin.entity.metadata.FieldInfo;
import io.oneapi.admin.entity.metadata.EntityInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ColumnMetadata entity.
 * Provides CRUD operations and custom queries for database column metadata.
 */
@Repository
public interface FieldInfoRepository extends JpaRepository<FieldInfo, Long> {

    /**
     * Find column metadata by entity and column name.
     *
     * @param entity the entity metadata
     * @param columnName the column name
     * @return optional column metadata
     */
    Optional<FieldInfo> findByDataEntityAndColumnName(EntityInfo entity, String columnName);

    /**
     * Find all columns for a specific entity.
     *
     * @param entity the entity metadata
     * @return list of column metadata
     */
    List<FieldInfo> findByDataEntity(EntityInfo entity);

    /**
     * Find all columns by entity ID.
     *
     * @param entityId the entity ID
     * @return list of column metadata
     */
    List<FieldInfo> findByDataEntityId(Long entityId);

    /**
     * Find all columns by entity ID ordered by ordinal position.
     * This ensures columns are returned in their natural order as defined in the database.
     *
     * @param entityId the entity ID
     * @return list of column metadata ordered by position
     */
    List<FieldInfo> findByDataEntityIdOrderByOrdinalPosition(Long entityId);

    /**
     * Search columns by name pattern (case-insensitive).
     *
     * @param columnName the column name pattern
     * @return list of column metadata
     */
    List<FieldInfo> findByColumnNameContainingIgnoreCase(String columnName);

    /**
     * Find all primary key columns for a entity.
     *
     * @param entityId the entity ID
     * @return list of primary key column metadata
     */
    List<FieldInfo> findByDataEntityIdAndIsPrimaryKeyTrue(Long entityId);
}
