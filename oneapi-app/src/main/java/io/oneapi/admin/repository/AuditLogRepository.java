package io.oneapi.admin.repository;

import io.oneapi.admin.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for the AuditLog entity.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserLogin(String userLogin);

    Page<AuditLog> findByUserLogin(String userLogin, Pageable pageable);

    List<AuditLog> findByAction(AuditLog.Action action);

    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    Page<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    List<AuditLog> findByStatus(AuditLog.Status status);

    @Query("SELECT a FROM AuditLog a WHERE a.userLogin = :userLogin " +
           "AND a.timestamp BETWEEN :start AND :end ORDER BY a.timestamp DESC")
    List<AuditLog> findUserActivityInPeriod(
        @Param("userLogin") String userLogin,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType " +
           "AND a.entityId = :entityId ORDER BY a.timestamp DESC")
    List<AuditLog> findEntityHistory(
        @Param("entityType") String entityType,
        @Param("entityId") Long entityId
    );

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.userLogin = :userLogin " +
           "AND a.action = :action AND a.timestamp >= :since")
    Long countUserActions(
        @Param("userLogin") String userLogin,
        @Param("action") AuditLog.Action action,
        @Param("since") LocalDateTime since
    );
}
