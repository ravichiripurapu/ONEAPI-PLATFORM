package io.oneapi.admin.repository;

import io.oneapi.admin.entity.Catalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the Catalog entity.
 */
@Repository
public interface CatalogRepository extends JpaRepository<Catalog, Long> {

    Optional<Catalog> findByName(String name);

    List<Catalog> findByCreatedBy(String createdBy);

    List<Catalog> findByNameContainingIgnoreCase(String name);

    boolean existsByName(String name);
}
