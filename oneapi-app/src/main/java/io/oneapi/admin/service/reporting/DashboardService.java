package io.oneapi.admin.service.reporting;

import io.oneapi.admin.dto.reporting.DashboardDTO;
import io.oneapi.admin.entity.Catalog;
import io.oneapi.admin.entity.Dashboard;
import io.oneapi.admin.mapper.ReportingMapper;
import io.oneapi.admin.repository.CatalogRepository;
import io.oneapi.admin.repository.DashboardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing dashboards.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final CatalogRepository catalogRepository;
    private final ReportingMapper mapper;

    public DashboardDTO create(DashboardDTO dto) {
        log.debug("Creating new dashboard: {}", dto.getName());

        Catalog catalog = null;
        if (dto.getCatalogId() != null) {
            catalog = catalogRepository.findById(dto.getCatalogId())
                .orElseThrow(() -> new IllegalArgumentException("Catalog not found with ID: " + dto.getCatalogId()));
        }

        Dashboard entity = mapper.toEntity(dto, catalog);
        Dashboard saved = dashboardRepository.save(entity);
        log.info("Created dashboard with ID: {}", saved.getId());

        return mapper.toDTO(saved);
    }

    public DashboardDTO update(Long id, DashboardDTO dto) {
        log.debug("Updating dashboard ID: {}", id);

        Dashboard entity = dashboardRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Dashboard not found with ID: " + id));

        Catalog catalog = null;
        if (dto.getCatalogId() != null) {
            catalog = catalogRepository.findById(dto.getCatalogId())
                .orElseThrow(() -> new IllegalArgumentException("Catalog not found with ID: " + dto.getCatalogId()));
        }

        mapper.updateEntityFromDTO(dto, entity, catalog);
        Dashboard updated = dashboardRepository.save(entity);
        log.info("Updated dashboard ID: {}", id);

        return mapper.toDTO(updated);
    }

    @Transactional(readOnly = true)
    public Optional<DashboardDTO> findById(Long id) {
        log.debug("Finding dashboard by ID: {}", id);
        return dashboardRepository.findById(id).map(mapper::toDTO);
    }

    @Transactional(readOnly = true)
    public List<DashboardDTO> findAll() {
        log.debug("Finding all dashboards");
        return mapper.dashboardsToDTOs(dashboardRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<DashboardDTO> findByCreatedBy(String username) {
        log.debug("Finding dashboards created by: {}", username);
        return mapper.dashboardsToDTOs(dashboardRepository.findByCreatedBy(username));
    }

    @Transactional(readOnly = true)
    public List<DashboardDTO> findByCatalog(Long catalogId) {
        log.debug("Finding dashboards for catalog: {}", catalogId);
        return mapper.dashboardsToDTOs(dashboardRepository.findByCatalogId(catalogId));
    }

    @Transactional(readOnly = true)
    public List<DashboardDTO> findPublicDashboards() {
        log.debug("Finding public dashboards");
        return mapper.dashboardsToDTOs(dashboardRepository.findByIsPublicTrue());
    }

    @Transactional(readOnly = true)
    public List<DashboardDTO> findAccessibleDashboards(String username) {
        log.debug("Finding accessible dashboards for user: {}", username);
        return mapper.dashboardsToDTOs(dashboardRepository.findAccessibleDashboards(username));
    }

    @Transactional(readOnly = true)
    public List<DashboardDTO> search(String username, String searchTerm) {
        log.debug("Searching dashboards for user {} with term: {}", username, searchTerm);
        return mapper.dashboardsToDTOs(dashboardRepository.searchDashboards(username, searchTerm));
    }

    public void incrementViewCount(Long id) {
        log.debug("Incrementing view count for dashboard ID: {}", id);

        Dashboard entity = dashboardRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Dashboard not found with ID: " + id));

        entity.setViewCount(entity.getViewCount() + 1);
        entity.setLastViewedAt(LocalDateTime.now());

        dashboardRepository.save(entity);
        log.info("Incremented view count for dashboard ID {}: count={}", id, entity.getViewCount());
    }

    public void delete(Long id) {
        log.debug("Deleting dashboard ID: {}", id);

        if (!dashboardRepository.existsById(id)) {
            throw new IllegalArgumentException("Dashboard not found with ID: " + id);
        }

        dashboardRepository.deleteById(id);
        log.info("Deleted dashboard ID: {}", id);
    }

    @Transactional(readOnly = true)
    public boolean exists(Long id) {
        return dashboardRepository.existsById(id);
    }
}
