package io.oneapi.admin.service.reporting;

import io.oneapi.admin.dto.reporting.ScheduleDTO;
import io.oneapi.admin.entity.Report;
import io.oneapi.admin.entity.SavedQuery;
import io.oneapi.admin.entity.Schedule;
import io.oneapi.admin.mapper.ReportingMapper;
import io.oneapi.admin.repository.ReportRepository;
import io.oneapi.admin.repository.SavedQueryRepository;
import io.oneapi.admin.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing report and query schedules.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ReportRepository reportRepository;
    private final SavedQueryRepository savedQueryRepository;
    private final ReportingMapper mapper;

    public ScheduleDTO create(ScheduleDTO dto) {
        log.debug("Creating new schedule: {}", dto.getName());

        Report report = null;
        if (dto.getReportId() != null) {
            report = reportRepository.findById(dto.getReportId())
                .orElseThrow(() -> new IllegalArgumentException("Report not found with ID: " + dto.getReportId()));
        }

        SavedQuery query = null;
        if (dto.getQueryId() != null) {
            query = savedQueryRepository.findById(dto.getQueryId())
                .orElseThrow(() -> new IllegalArgumentException("SavedQuery not found with ID: " + dto.getQueryId()));
        }

        // Either report or query must be provided
        if (report == null && query == null) {
            throw new IllegalArgumentException("Either reportId or queryId must be provided");
        }

        Schedule entity = mapper.toEntity(dto, report, query);
        Schedule saved = scheduleRepository.save(entity);
        log.info("Created schedule with ID: {}", saved.getId());

        return mapper.toDTO(saved);
    }

    public ScheduleDTO update(Long id, ScheduleDTO dto) {
        log.debug("Updating schedule ID: {}", id);

        Schedule entity = scheduleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found with ID: " + id));

        Report report = null;
        if (dto.getReportId() != null) {
            report = reportRepository.findById(dto.getReportId())
                .orElseThrow(() -> new IllegalArgumentException("Report not found with ID: " + dto.getReportId()));
        }

        SavedQuery query = null;
        if (dto.getQueryId() != null) {
            query = savedQueryRepository.findById(dto.getQueryId())
                .orElseThrow(() -> new IllegalArgumentException("SavedQuery not found with ID: " + dto.getQueryId()));
        }

        // Either report or query must be provided
        if (report == null && query == null) {
            throw new IllegalArgumentException("Either reportId or queryId must be provided");
        }

        mapper.updateEntityFromDTO(dto, entity, report, query);
        Schedule updated = scheduleRepository.save(entity);
        log.info("Updated schedule ID: {}", id);

        return mapper.toDTO(updated);
    }

    @Transactional(readOnly = true)
    public Optional<ScheduleDTO> findById(Long id) {
        log.debug("Finding schedule by ID: {}", id);
        return scheduleRepository.findById(id).map(mapper::toDTO);
    }

    @Transactional(readOnly = true)
    public List<ScheduleDTO> findAll() {
        log.debug("Finding all schedules");
        return mapper.schedulesToDTOs(scheduleRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<ScheduleDTO> findByCreatedBy(String username) {
        log.debug("Finding schedules created by: {}", username);
        return mapper.schedulesToDTOs(scheduleRepository.findByCreatedBy(username));
    }

    @Transactional(readOnly = true)
    public List<ScheduleDTO> findByReport(Long reportId) {
        log.debug("Finding schedules for report: {}", reportId);
        return mapper.schedulesToDTOs(scheduleRepository.findByReportId(reportId));
    }

    @Transactional(readOnly = true)
    public List<ScheduleDTO> findByQuery(Long queryId) {
        log.debug("Finding schedules for query: {}", queryId);
        return mapper.schedulesToDTOs(scheduleRepository.findByQueryId(queryId));
    }

    @Transactional(readOnly = true)
    public List<ScheduleDTO> findEnabled() {
        log.debug("Finding enabled schedules");
        return mapper.schedulesToDTOs(scheduleRepository.findByEnabledTrue());
    }

    @Transactional(readOnly = true)
    public List<ScheduleDTO> findDueForExecution() {
        log.debug("Finding schedules due for execution");
        return mapper.schedulesToDTOs(scheduleRepository.findSchedulesDueForExecution(LocalDateTime.now()));
    }

    public ScheduleDTO toggleEnable(Long id) {
        log.debug("Toggling enabled status for schedule ID: {}", id);

        Schedule entity = scheduleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found with ID: " + id));

        entity.setEnabled(!entity.getEnabled());
        Schedule updated = scheduleRepository.save(entity);
        log.info("Toggled schedule ID {} to enabled={}", id, updated.getEnabled());

        return mapper.toDTO(updated);
    }

    public void updateNextRun(Long id, LocalDateTime nextRunAt) {
        log.debug("Updating next run time for schedule ID: {}", id);

        Schedule entity = scheduleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found with ID: " + id));

        entity.setNextRunAt(nextRunAt);
        scheduleRepository.save(entity);
        log.info("Updated next run time for schedule ID {} to {}", id, nextRunAt);
    }

    public void recordRun(Long id, Schedule.RunStatus status, String message) {
        log.debug("Recording run for schedule ID: {}", id);

        Schedule entity = scheduleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found with ID: " + id));

        entity.setLastRunAt(LocalDateTime.now());
        entity.setLastRunStatus(status);
        entity.setLastRunMessage(message);
        entity.setExecutionCount(entity.getExecutionCount() + 1);

        if (status == Schedule.RunStatus.SUCCESS) {
            entity.setSuccessCount(entity.getSuccessCount() + 1);
        } else if (status == Schedule.RunStatus.FAILURE) {
            entity.setFailureCount(entity.getFailureCount() + 1);
        }

        scheduleRepository.save(entity);
        log.info("Recorded run for schedule ID {}: status={}, message={}", id, status, message);
    }

    public void delete(Long id) {
        log.debug("Deleting schedule ID: {}", id);

        if (!scheduleRepository.existsById(id)) {
            throw new IllegalArgumentException("Schedule not found with ID: " + id);
        }

        scheduleRepository.deleteById(id);
        log.info("Deleted schedule ID: {}", id);
    }

    @Transactional(readOnly = true)
    public boolean exists(Long id) {
        return scheduleRepository.existsById(id);
    }
}
