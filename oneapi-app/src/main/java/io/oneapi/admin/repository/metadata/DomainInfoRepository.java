package io.oneapi.admin.repository.metadata;

import io.oneapi.admin.entity.SourceInfo;
import io.oneapi.admin.entity.metadata.DomainInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for DomainInfo entity.
 * Provides CRUD operations and custom queries for database domain metadata.
 */
@Repository
public interface DomainInfoRepository extends JpaRepository<DomainInfo, Long> {

    /**
     * Find domain metadata by source and domain name.
     *
     * @param source the database source
     * @param domainName the domain name
     * @return optional domain metadata
     */
    Optional<DomainInfo> findBySourceAndSchemaName(SourceInfo source, String domainName);

    /**
     * Find all domains for a specific database source.
     *
     * @param source the database source
     * @return list of domain metadata
     */
    List<DomainInfo> findBySource(SourceInfo source);

    /**
     * Find all domains by source ID.
     *
     * @param sourceId the source ID
     * @return list of domain metadata
     */
    List<DomainInfo> findBySourceId(Long sourceId);

    /**
     * Delete all domains associated with a source ID.
     *
     * @param sourceId the source ID
     */
    void deleteBySourceId(Long sourceId);
}
