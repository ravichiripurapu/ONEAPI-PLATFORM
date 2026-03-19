package io.oneapi.admin.service.reporting;

import io.oneapi.admin.dto.reporting.ReportDTO;
import io.oneapi.admin.entity.Report;
import io.oneapi.admin.entity.SavedQuery;
import io.oneapi.admin.entity.SourceInfo;
import io.oneapi.admin.mapper.ReportingMapper;
import io.oneapi.admin.repository.ReportRepository;
import io.oneapi.admin.repository.SavedQueryRepository;
import io.oneapi.admin.repository.SourceInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing reports.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;
    private final SavedQueryRepository savedQueryRepository;
    private final SourceInfoRepository sourceInfoRepository;
    private final ReportingMapper mapper;

    public ReportDTO create(ReportDTO dto) {
        log.debug("Creating new report: {}", dto.getName());

        SavedQuery query = savedQueryRepository.findById(dto.getQueryId())
            .orElseThrow(() -> new IllegalArgumentException("SavedQuery not found with ID: " + dto.getQueryId()));

        SourceInfo source = null;
        if (dto.getSourceId() != null) {
            source = sourceInfoRepository.findById(dto.getSourceId())
                .orElseThrow(() -> new IllegalArgumentException("Source not found with ID: " + dto.getSourceId()));
        }

        Report entity = mapper.toEntity(dto, query, source);
        Report saved = reportRepository.save(entity);
        log.info("Created report with ID: {}", saved.getId());

        return mapper.toDTO(saved);
    }

    public ReportDTO update(Long id, ReportDTO dto) {
        log.debug("Updating report ID: {}", id);

        Report entity = reportRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Report not found with ID: " + id));

        SavedQuery query = savedQueryRepository.findById(dto.getQueryId())
            .orElseThrow(() -> new IllegalArgumentException("SavedQuery not found with ID: " + dto.getQueryId()));

        SourceInfo source = null;
        if (dto.getSourceId() != null) {
            source = sourceInfoRepository.findById(dto.getSourceId())
                .orElseThrow(() -> new IllegalArgumentException("Source not found with ID: " + dto.getSourceId()));
        }

        mapper.updateEntityFromDTO(dto, entity, query, source);
        Report updated = reportRepository.save(entity);
        log.info("Updated report ID: {}", id);

        return mapper.toDTO(updated);
    }

    @Transactional(readOnly = true)
    public Optional<ReportDTO> findById(Long id) {
        log.debug("Finding report by ID: {}", id);
        return reportRepository.findById(id).map(mapper::toDTO);
    }

    @Transactional(readOnly = true)
    public List<ReportDTO> findAll() {
        log.debug("Finding all reports");
        return mapper.reportsToDTOs(reportRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<ReportDTO> findByCreatedBy(String username) {
        log.debug("Finding reports created by: {}", username);
        return mapper.reportsToDTOs(reportRepository.findByCreatedBy(username));
    }

    @Transactional(readOnly = true)
    public List<ReportDTO> findByQuery(Long queryId) {
        log.debug("Finding reports for query: {}", queryId);
        return mapper.reportsToDTOs(reportRepository.findByQueryId(queryId));
    }

    @Transactional(readOnly = true)
    public List<ReportDTO> findBySource(Long sourceId) {
        log.debug("Finding reports for source: {}", sourceId);
        return mapper.reportsToDTOs(reportRepository.findBySourceId(sourceId));
    }

    @Transactional(readOnly = true)
    public List<ReportDTO> findPublicReports() {
        log.debug("Finding public reports");
        return mapper.reportsToDTOs(reportRepository.findByIsPublicTrue());
    }

    @Transactional(readOnly = true)
    public List<ReportDTO> findAccessibleReports(String username) {
        log.debug("Finding accessible reports for user: {}", username);
        return mapper.reportsToDTOs(reportRepository.findAccessibleReports(username));
    }

    @Transactional(readOnly = true)
    public List<ReportDTO> search(String username, String searchTerm) {
        log.debug("Searching reports for user {} with term: {}", username, searchTerm);
        return mapper.reportsToDTOs(reportRepository.searchReports(username, searchTerm));
    }

    public void recordExecution(Long id, long executionTimeMs) {
        log.debug("Recording execution for report ID: {}", id);

        Report entity = reportRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Report not found with ID: " + id));

        Long currentCount = entity.getExecutionCount();
        Long currentAvg = entity.getAvgExecutionTimeMs();

        long newAvg = currentAvg == null ? executionTimeMs :
            ((currentAvg * currentCount) + executionTimeMs) / (currentCount + 1);

        entity.setExecutionCount(currentCount + 1);
        entity.setAvgExecutionTimeMs(newAvg);
        entity.setLastExecutedAt(LocalDateTime.now());

        reportRepository.save(entity);
        log.info("Recorded execution for report ID {}: count={}, avg={}ms", id, currentCount + 1, newAvg);
    }

    public void delete(Long id) {
        log.debug("Deleting report ID: {}", id);

        if (!reportRepository.existsById(id)) {
            throw new IllegalArgumentException("Report not found with ID: " + id);
        }

        reportRepository.deleteById(id);
        log.info("Deleted report ID: {}", id);
    }

    @Transactional(readOnly = true)
    public boolean exists(Long id) {
        return reportRepository.existsById(id);
    }
}
