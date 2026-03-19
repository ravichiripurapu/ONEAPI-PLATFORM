package io.oneapi.admin.service.reporting;

import io.oneapi.admin.dto.reporting.ReportCatalogDTO;
import io.oneapi.admin.entity.Catalog;
import io.oneapi.admin.mapper.ReportingMapper;
import io.oneapi.admin.repository.CatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing report catalogs.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ReportCatalogService {

    private final CatalogRepository catalogRepository;
    private final ReportingMapper mapper;

    /**
     * Create a new catalog.
     */
    public ReportCatalogDTO create(ReportCatalogDTO dto) {
        log.debug("Creating new catalog: {}", dto.getName());

        if (catalogRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Catalog with name '" + dto.getName() + "' already exists");
        }

        Catalog entity = mapper.toEntity(dto);
        Catalog saved = catalogRepository.save(entity);
        log.info("Created catalog with ID: {}", saved.getId());

        return mapper.toDTO(saved);
    }

    /**
     * Update an existing catalog.
     */
    public ReportCatalogDTO update(Long id, ReportCatalogDTO dto) {
        log.debug("Updating catalog ID: {}", id);

        Catalog entity = catalogRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Catalog not found with ID: " + id));

        // Check for name conflicts (excluding current entity)
        if (!entity.getName().equals(dto.getName()) && catalogRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Catalog with name '" + dto.getName() + "' already exists");
        }

        mapper.updateEntityFromDTO(dto, entity);
        Catalog updated = catalogRepository.save(entity);
        log.info("Updated catalog ID: {}", id);

        return mapper.toDTO(updated);
    }

    /**
     * Get catalog by ID.
     */
    @Transactional(readOnly = true)
    public Optional<ReportCatalogDTO> findById(Long id) {
        log.debug("Finding catalog by ID: {}", id);
        return catalogRepository.findById(id).map(mapper::toDTO);
    }

    /**
     * Get catalog by name.
     */
    @Transactional(readOnly = true)
    public Optional<ReportCatalogDTO> findByName(String name) {
        log.debug("Finding catalog by name: {}", name);
        return catalogRepository.findByName(name).map(mapper::toDTO);
    }

    /**
     * Get all catalogs.
     */
    @Transactional(readOnly = true)
    public List<ReportCatalogDTO> findAll() {
        log.debug("Finding all catalogs");
        return mapper.catalogsToDTOs(catalogRepository.findAll());
    }

    /**
     * Get catalogs created by specific user.
     */
    @Transactional(readOnly = true)
    public List<ReportCatalogDTO> findByCreatedBy(String username) {
        log.debug("Finding catalogs created by: {}", username);
        return mapper.catalogsToDTOs(catalogRepository.findByCreatedBy(username));
    }

    /**
     * Search catalogs by name.
     */
    @Transactional(readOnly = true)
    public List<ReportCatalogDTO> search(String searchTerm) {
        log.debug("Searching catalogs with term: {}", searchTerm);
        return mapper.catalogsToDTOs(catalogRepository.findByNameContainingIgnoreCase(searchTerm));
    }

    /**
     * Delete a catalog.
     */
    public void delete(Long id) {
        log.debug("Deleting catalog ID: {}", id);

        if (!catalogRepository.existsById(id)) {
            throw new IllegalArgumentException("Catalog not found with ID: " + id);
        }

        catalogRepository.deleteById(id);
        log.info("Deleted catalog ID: {}", id);
    }

    /**
     * Check if catalog exists.
     */
    @Transactional(readOnly = true)
    public boolean exists(Long id) {
        return catalogRepository.existsById(id);
    }

    /**
     * Check if catalog name exists.
     */
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return catalogRepository.existsByName(name);
    }
}
