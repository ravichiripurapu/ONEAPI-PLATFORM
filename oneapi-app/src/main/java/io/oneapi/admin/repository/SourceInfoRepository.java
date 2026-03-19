package io.oneapi.admin.repository;

import io.oneapi.admin.entity.SourceInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for database connections.
 */
@Repository
public interface SourceInfoRepository extends JpaRepository<SourceInfo, Long> {

    Optional<SourceInfo> findByName(String name);

    List<SourceInfo> findByActive(Boolean active);

    List<SourceInfo> findByType(SourceInfo.DatabaseType type);
}
