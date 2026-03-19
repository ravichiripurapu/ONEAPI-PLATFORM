package io.oneapi.admin.controller.metadata;

import io.oneapi.admin.dto.metadata.FieldInfoDTO;
import io.oneapi.admin.dto.metadata.DomainInfoDTO;
import io.oneapi.admin.dto.metadata.EntityInfoDTO;
import io.oneapi.admin.entity.metadata.FieldInfo;
import io.oneapi.admin.entity.metadata.DomainInfo;
import io.oneapi.admin.entity.metadata.EntityInfo;
import io.oneapi.admin.mapper.MetadataMapper;
import io.oneapi.admin.repository.metadata.FieldInfoRepository;
import io.oneapi.admin.repository.metadata.DomainInfoRepository;
import io.oneapi.admin.repository.metadata.EntityInfoRepository;
import io.oneapi.admin.service.metadata.MetadataDiscoveryService;
import io.oneapi.admin.service.metadata.MetadataDiscoveryService.DiscoveryResult;
import io.oneapi.admin.service.security.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for metadata discovery and browsing.
 */
@RestController
@RequestMapping("/api/v1/metadata")
@Tag(name = "Metadata Discovery", description = "Discover and browse database metadata (schemas, tables, columns)")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
@Slf4j
public class MetadataDiscoveryController {

    private final MetadataDiscoveryService discoveryService;
    private final DomainInfoRepository domainRepository;
    private final EntityInfoRepository entityRepository;
    private final FieldInfoRepository fieldRepository;
    private final MetadataMapper mapper;
    private final SecurityService securityService;

    // ========== Discovery Operations ==========

    @PostMapping("/discover/{sourceId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Discover metadata for a database connection",
            description = "Triggers metadata discovery using OneAPI SDK connectors. " +
                    "Discovers schemas, tables, columns, and stores them in the metadata catalog.")
    public ResponseEntity<DiscoveryResult> discoverMetadata(@PathVariable Long sourceId) {
        log.info("Operation for source: {}", sourceId);
        DiscoveryResult result = discoveryService.discoverMetadata(sourceId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/sync/{sourceId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Re-sync metadata for a database connection",
            description = "Re-discovers metadata and detects changes (new schemas, tables, columns). Returns change summary.")
    public ResponseEntity<MetadataDiscoveryService.SyncResult> syncMetadata(@PathVariable Long sourceId) {
        log.info("Sync operation for source: {}", sourceId);
        MetadataDiscoveryService.SyncResult result = discoveryService.syncMetadata(sourceId);
        log.info("Sync completed for source {}: {}", sourceId, result.getChangesSummary());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/source/{sourceId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete all metadata for a connection",
            description = "Removes all discovered metadata (schemas, tables, columns) for a connection")
    public ResponseEntity<Void> deleteMetadata(@PathVariable Long sourceId) {
        log.info("Operation for source: {}", sourceId);
        discoveryService.deleteMetadata(sourceId);
        return ResponseEntity.noContent().build();
    }

    // ========== Schema Operations ==========

    @GetMapping("/domains/source/{sourceId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List all schemas for a connection",
            description = "Returns all discovered schemas for a database connection")
    public ResponseEntity<List<DomainInfoDTO>> getDomainsBySource(@PathVariable Long sourceId) {
        log.debug("Fetching for source: {}", sourceId);

        // Check source access
        if (!securityService.hasSourceAccess(sourceId)) {
            throw new SecurityException("Access denied to source: " + sourceId);
        }

        List<DomainInfo> schemas = domainRepository.findBySourceId(sourceId);
        List<DomainInfo> filteredSchemas = securityService.filterDomains(schemas);
        return ResponseEntity.ok(mapper.toDTOList(filteredSchemas));
    }

    @GetMapping("/domains/{domainId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get schema by ID",
            description = "Returns detailed information about a specific schema")
    public ResponseEntity<DomainInfoDTO> getDomainById(@PathVariable Long domainId) {
        log.debug("Fetching for domain: {}", domainId);

        DomainInfo domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new RuntimeException("Domain not found: " + domainId));

        // Check domain access
        if (!securityService.hasDomainAccess(domain.getSource().getId(), domainId)) {
            throw new SecurityException("Access denied to domain: " + domainId);
        }

        return ResponseEntity.ok(mapper.toDTO(domain));
    }

    // ========== Table Operations ==========

    @GetMapping("/entities/source/{sourceId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List all tables for a connection",
            description = "Returns all discovered tables across all schemas for a connection")
    public ResponseEntity<List<EntityInfoDTO>> getEntitiesBySource(@PathVariable Long sourceId) {
        log.debug("Fetching for source: {}", sourceId);

        // Check source access
        if (!securityService.hasSourceAccess(sourceId)) {
            throw new SecurityException("Access denied to source: " + sourceId);
        }

        List<EntityInfo> tables = entityRepository.findBySourceId(sourceId);
        List<EntityInfo> filteredTables = securityService.filterEntities(tables);
        return ResponseEntity.ok(mapper.toTableDTOList(filteredTables));
    }

    @GetMapping("/entities/domain/{domainId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List all tables for a schema",
            description = "Returns all discovered tables within a specific schema")
    public ResponseEntity<List<EntityInfoDTO>> getEntitiesByDomain(@PathVariable Long domainId) {
        log.debug("Fetching for domain: {}", domainId);

        DomainInfo domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new RuntimeException("Domain not found: " + domainId));

        // Check domain access
        if (!securityService.hasDomainAccess(domain.getSource().getId(), domainId)) {
            throw new SecurityException("Access denied to domain: " + domainId);
        }

        List<EntityInfo> tables = entityRepository.findByDomain(domain);
        List<EntityInfo> filteredTables = securityService.filterEntities(tables);
        return ResponseEntity.ok(mapper.toTableDTOList(filteredTables));
    }

    @GetMapping("/entities/{entityId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get table by ID",
            description = "Returns detailed information about a specific table")
    public ResponseEntity<EntityInfoDTO> getEntityById(@PathVariable Long entityId) {
        log.debug("Fetching for entity: {}", entityId);

        EntityInfo entity = entityRepository.findById(entityId)
                .orElseThrow(() -> new RuntimeException("Entity not found: " + entityId));

        // Check entity access
        if (!securityService.hasEntityAccess(
                entity.getDomain().getSource().getId(),
                entity.getDomain().getId(),
                entityId)) {
            throw new SecurityException("Access denied to entity: " + entityId);
        }

        return ResponseEntity.ok(mapper.toDTO(entity));
    }

    @GetMapping("/entities/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search tables by name",
            description = "Searches for tables matching a name pattern (case-insensitive, filtered by permissions)")
    public ResponseEntity<List<EntityInfoDTO>> searchEntitiesByName(
            @RequestParam String tableName,
            @RequestParam(required = false) Long sourceId) {
        log.debug("Searching tables with name containing: {}", tableName);

        // Check source access if sourceId provided
        if (sourceId != null && !securityService.hasSourceAccess(sourceId)) {
            throw new SecurityException("Access denied to source: " + sourceId);
        }

        List<EntityInfo> tables;
        if (sourceId != null) {
            tables = entityRepository.findBySourceIdAndTableNameContainingIgnoreCase(sourceId, tableName);
        } else {
            tables = entityRepository.findByTableNameContainingIgnoreCase(tableName);
        }

        // Apply security filtering
        List<EntityInfo> filteredTables = securityService.filterEntities(tables);
        return ResponseEntity.ok(mapper.toTableDTOList(filteredTables));
    }

    // ========== Column Operations ==========


    @GetMapping("/fields/{fieldId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get column by ID",
            description = "Returns detailed information about a specific column")
    public ResponseEntity<FieldInfoDTO> getFieldById(@PathVariable Long fieldId) {
        log.debug("Fetching for field: {}", fieldId);

        FieldInfo field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("Field not found: " + fieldId));

        // Check field access
        if (!securityService.hasFieldAccess(
                field.getDataEntity().getDomain().getSource().getId(),
                field.getDataEntity().getDomain().getId(),
                field.getDataEntity().getId(),
                fieldId)) {
            throw new SecurityException("Access denied to field: " + fieldId);
        }

        return ResponseEntity.ok(mapper.toDTO(field));
    }

    @GetMapping("/fields/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search columns by name",
            description = "Searches for columns matching a name pattern across all tables (filtered by permissions)")
    public ResponseEntity<List<FieldInfoDTO>> searchFieldsByName(@RequestParam String columnName) {
        log.debug("Searching columns with name containing: {}", columnName);

        List<FieldInfo> columns = fieldRepository.findByColumnNameContainingIgnoreCase(columnName);

        // Apply field-level security filtering (CRITICAL)
        List<FieldInfo> filteredColumns = securityService.filterFields(columns);
        return ResponseEntity.ok(mapper.toColumnDTOList(filteredColumns));
    }


    @GetMapping("/entities/{entityId}/fields")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List all fields for an entity",
            description = "Returns all discovered fields for an entity, ordered by position (filtered by user permissions)")
    public ResponseEntity<List<FieldInfoDTO>> getFieldsByEntity(@PathVariable Long entityId) {
        log.debug("Fetching fields for entity: {}", entityId);

        // Get entity to check access
        EntityInfo entity = entityRepository.findById(entityId)
                .orElseThrow(() -> new RuntimeException("Entity not found: " + entityId));

        // Check entity access
        if (!securityService.hasEntityAccess(
                entity.getDomain().getSource().getId(),
                entity.getDomain().getId(),
                entityId)) {
            throw new SecurityException("Access denied to entity: " + entityId);
        }

        // Get fields and apply field-level security filtering (CRITICAL)
        List<FieldInfo> fields = fieldRepository.findByDataEntityIdOrderByOrdinalPosition(entityId);
        List<FieldInfo> filteredFields = securityService.filterFields(fields);
        return ResponseEntity.ok(mapper.toColumnDTOList(filteredFields));
    }

    @GetMapping("/entities/{entityId}/primary-keys")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get primary key fields for an entity",
            description = "Returns only the primary key fields for an entity (filtered by user permissions)")
    public ResponseEntity<List<FieldInfoDTO>> getPrimaryKeyFields(@PathVariable Long entityId) {
        log.debug("Fetching primary key fields for entity: {}", entityId);

        // Get entity to check access
        EntityInfo entity = entityRepository.findById(entityId)
                .orElseThrow(() -> new RuntimeException("Entity not found: " + entityId));

        // Check entity access
        if (!securityService.hasEntityAccess(
                entity.getDomain().getSource().getId(),
                entity.getDomain().getId(),
                entityId)) {
            throw new SecurityException("Access denied to entity: " + entityId);
        }

        // Get primary key fields and apply field-level security filtering
        List<FieldInfo> fields = fieldRepository.findByDataEntityIdAndIsPrimaryKeyTrue(entityId);
        List<FieldInfo> filteredFields = securityService.filterFields(fields);
        return ResponseEntity.ok(mapper.toColumnDTOList(filteredFields));
    }
}
