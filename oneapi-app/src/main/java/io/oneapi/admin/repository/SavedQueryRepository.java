package io.oneapi.admin.repository;

import io.oneapi.admin.entity.SavedQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the SavedQuery entity.
 */
@Repository
public interface SavedQueryRepository extends JpaRepository<SavedQuery, Long> {

    List<SavedQuery> findByCreatedBy(String createdBy);

    List<SavedQuery> findBySourceId(Long sourceId);

    List<SavedQuery> findByCatalogId(Long catalogId);

    List<SavedQuery> findByIsPublicTrue();

    List<SavedQuery> findByIsFavoriteTrueAndCreatedBy(String createdBy);

    @Query("SELECT q FROM SavedQuery q WHERE q.isPublic = true OR q.createdBy = :username")
    List<SavedQuery> findAccessibleQueries(@Param("username") String username);

    @Query("SELECT q FROM SavedQuery q WHERE (q.isPublic = true OR q.createdBy = :username) " +
           "AND LOWER(q.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<SavedQuery> searchQueries(@Param("username") String username, @Param("searchTerm") String searchTerm);

    Optional<SavedQuery> findByIdAndCreatedBy(Long id, String createdBy);

    boolean existsByNameAndCreatedBy(String name, String createdBy);
}
