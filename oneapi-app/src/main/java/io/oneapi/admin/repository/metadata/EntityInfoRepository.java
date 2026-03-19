package io.oneapi.admin.repository.metadata;

import io.oneapi.admin.entity.metadata.DomainInfo;
import io.oneapi.admin.entity.metadata.EntityInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TableMetadata entity.
 * Provides CRUD operations and custom queries for database entity metadata.
 */
@Repository
public interface EntityInfoRepository extends JpaRepository<EntityInfo, Long> {

    /**
     * Find entity metadata by domain and entity name.
     *
     * @param domain the domain metadata
     * @param entityName the entity name
     * @return optional entity metadata
     */
    Optional<EntityInfo> findByDomainAndTableName(DomainInfo domain, String entityName);

    /**
     * Find all entitys for a specific domain.
     *
     * @param domain the domain metadata
     * @return list of entity metadata
     */
    List<EntityInfo> findByDomain(DomainInfo domain);

    /**
     * Find all entitys by domain ID.
     *
     * @param domainId the domain ID
     * @return list of entity metadata
     */
    List<EntityInfo> findByDomainId(Long domainId);

    /**
     * Find all entitys by source ID using a join query.
     * This retrieves all entitys across all domains for a given database source.
     *
     * @param sourceId the source ID
     * @return list of entity metadata
     */
    @Query("SELECT t FROM EntityInfo t JOIN t.domain s WHERE s.source.id = :sourceId")
    List<EntityInfo> findBySourceId(@Param("sourceId") Long sourceId);

    /**
     * Search entitys by name pattern (case-insensitive).
     *
     * @param entityName the entity name pattern
     * @return list of entity metadata
     */
    List<EntityInfo> findByTableNameContainingIgnoreCase(String entityName);

    /**
     * Search entitys by name pattern for a specific source (case-insensitive).
     *
     * @param sourceId the source ID
     * @param entityName the entity name pattern
     * @return list of entity metadata
     */
    @Query("SELECT t FROM EntityInfo t JOIN t.domain s WHERE s.source.id = :sourceId AND LOWER(t.tableName) LIKE LOWER(CONCAT('%', :entityName, '%'))")
    List<EntityInfo> findBySourceIdAndTableNameContainingIgnoreCase(
            @Param("sourceId") Long sourceId,
            @Param("entityName") String entityName);
}
