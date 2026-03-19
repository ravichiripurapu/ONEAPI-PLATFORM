package io.oneapi.admin.repository;

import io.oneapi.admin.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for the Schedule entity.
 */
@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByCreatedBy(String createdBy);

    List<Schedule> findByReportId(Long reportId);

    List<Schedule> findByQueryId(Long queryId);

    List<Schedule> findByEnabledTrue();

    @Query("SELECT s FROM Schedule s WHERE s.enabled = true AND s.nextRunAt <= :currentTime")
    List<Schedule> findSchedulesDueForExecution(LocalDateTime currentTime);

    List<Schedule> findByLastRunStatus(Schedule.RunStatus status);

    boolean existsByNameAndCreatedBy(String name, String createdBy);
}
