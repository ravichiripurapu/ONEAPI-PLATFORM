package io.oneapi.admin.repository;

import io.oneapi.admin.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the Report entity.
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByCreatedBy(String createdBy);

    List<Report> findByQueryId(Long queryId);

    List<Report> findBySourceId(Long sourceId);

    List<Report> findByIsPublicTrue();

    @Query("SELECT r FROM Report r WHERE r.isPublic = true OR r.createdBy = :username")
    List<Report> findAccessibleReports(@Param("username") String username);

    @Query("SELECT r FROM Report r WHERE (r.isPublic = true OR r.createdBy = :username) " +
           "AND LOWER(r.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Report> searchReports(@Param("username") String username, @Param("searchTerm") String searchTerm);

    Optional<Report> findByIdAndCreatedBy(Long id, String createdBy);

    boolean existsByNameAndCreatedBy(String name, String createdBy);

    List<Report> findByOutputFormat(Report.OutputFormat outputFormat);
}
