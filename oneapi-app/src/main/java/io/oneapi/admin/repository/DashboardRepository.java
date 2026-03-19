package io.oneapi.admin.repository;

import io.oneapi.admin.entity.Dashboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the Dashboard entity.
 */
@Repository
public interface DashboardRepository extends JpaRepository<Dashboard, Long> {

    List<Dashboard> findByCreatedBy(String createdBy);

    List<Dashboard> findBySourceId(Long sourceId);

    List<Dashboard> findByIsPublicTrue();

    @Query("SELECT d FROM Dashboard d WHERE d.isPublic = true OR d.createdBy = :username")
    List<Dashboard> findAccessibleDashboards(@Param("username") String username);

    @Query("SELECT d FROM Dashboard d WHERE (d.isPublic = true OR d.createdBy = :username) " +
           "AND LOWER(d.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Dashboard> searchDashboards(@Param("username") String username, @Param("searchTerm") String searchTerm);

    Optional<Dashboard> findByIdAndCreatedBy(Long id, String createdBy);

    boolean existsByNameAndCreatedBy(String name, String createdBy);
}
