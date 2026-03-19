package io.oneapi.admin.service.reporting;

import io.oneapi.admin.dto.reporting.WidgetDTO;
import io.oneapi.admin.entity.Dashboard;
import io.oneapi.admin.entity.Report;
import io.oneapi.admin.entity.SavedQuery;
import io.oneapi.admin.entity.Widget;
import io.oneapi.admin.mapper.ReportingMapper;
import io.oneapi.admin.repository.DashboardRepository;
import io.oneapi.admin.repository.ReportRepository;
import io.oneapi.admin.repository.SavedQueryRepository;
import io.oneapi.admin.repository.WidgetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing widgets.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class WidgetService {

    private final WidgetRepository widgetRepository;
    private final DashboardRepository dashboardRepository;
    private final SavedQueryRepository savedQueryRepository;
    private final ReportRepository reportRepository;
    private final ReportingMapper mapper;

    /**
     * Create a new widget.
     */
    public WidgetDTO create(WidgetDTO dto) {
        log.debug("Creating new widget: {}", dto.getName());

        Dashboard dashboard = dashboardRepository.findById(dto.getDashboardId())
            .orElseThrow(() -> new IllegalArgumentException("Dashboard not found with ID: " + dto.getDashboardId()));

        SavedQuery query = null;
        if (dto.getQueryId() != null) {
            query = savedQueryRepository.findById(dto.getQueryId())
                .orElseThrow(() -> new IllegalArgumentException("SavedQuery not found with ID: " + dto.getQueryId()));
        }

        Report report = null;
        if (dto.getReportId() != null) {
            report = reportRepository.findById(dto.getReportId())
                .orElseThrow(() -> new IllegalArgumentException("Report not found with ID: " + dto.getReportId()));
        }

        Widget entity = mapper.toEntity(dto, dashboard, query, report);
        Widget saved = widgetRepository.save(entity);
        log.info("Created widget with ID: {}", saved.getId());

        return mapper.toDTO(saved);
    }

    /**
     * Update an existing widget.
     */
    public WidgetDTO update(Long id, WidgetDTO dto) {
        log.debug("Updating widget ID: {}", id);

        Widget entity = widgetRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Widget not found with ID: " + id));

        Dashboard dashboard = dashboardRepository.findById(dto.getDashboardId())
            .orElseThrow(() -> new IllegalArgumentException("Dashboard not found with ID: " + dto.getDashboardId()));

        SavedQuery query = null;
        if (dto.getQueryId() != null) {
            query = savedQueryRepository.findById(dto.getQueryId())
                .orElseThrow(() -> new IllegalArgumentException("SavedQuery not found with ID: " + dto.getQueryId()));
        }

        Report report = null;
        if (dto.getReportId() != null) {
            report = reportRepository.findById(dto.getReportId())
                .orElseThrow(() -> new IllegalArgumentException("Report not found with ID: " + dto.getReportId()));
        }

        mapper.updateEntityFromDTO(dto, entity, dashboard, query, report);
        Widget updated = widgetRepository.save(entity);
        log.info("Updated widget ID: {}", id);

        return mapper.toDTO(updated);
    }

    /**
     * Get widget by ID.
     */
    @Transactional(readOnly = true)
    public Optional<WidgetDTO> findById(Long id) {
        log.debug("Finding widget by ID: {}", id);
        return widgetRepository.findById(id).map(mapper::toDTO);
    }

    /**
     * Get widgets by dashboard.
     */
    @Transactional(readOnly = true)
    public List<WidgetDTO> findByDashboard(Long dashboardId) {
        log.debug("Finding widgets for dashboard: {}", dashboardId);
        return mapper.widgetsToDTOs(widgetRepository.findByDashboardIdOrderByPositionYAscPositionXAsc(dashboardId));
    }

    /**
     * Get widgets by query.
     */
    @Transactional(readOnly = true)
    public List<WidgetDTO> findByQuery(Long queryId) {
        log.debug("Finding widgets for query: {}", queryId);
        return mapper.widgetsToDTOs(widgetRepository.findByQueryId(queryId));
    }

    /**
     * Get widgets by report.
     */
    @Transactional(readOnly = true)
    public List<WidgetDTO> findByReport(Long reportId) {
        log.debug("Finding widgets for report: {}", reportId);
        return mapper.widgetsToDTOs(widgetRepository.findByReportId(reportId));
    }

    /**
     * Get widgets by type.
     */
    @Transactional(readOnly = true)
    public List<WidgetDTO> findByType(Widget.WidgetType widgetType) {
        log.debug("Finding widgets by type: {}", widgetType);
        return mapper.widgetsToDTOs(widgetRepository.findByWidgetType(widgetType));
    }

    /**
     * Delete a widget.
     */
    public void delete(Long id) {
        log.debug("Deleting widget ID: {}", id);

        if (!widgetRepository.existsById(id)) {
            throw new IllegalArgumentException("Widget not found with ID: " + id);
        }

        widgetRepository.deleteById(id);
        log.info("Deleted widget ID: {}", id);
    }

    /**
     * Check if widget exists.
     */
    @Transactional(readOnly = true)
    public boolean exists(Long id) {
        return widgetRepository.existsById(id);
    }
}
