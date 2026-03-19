package io.oneapi.admin.controller.reporting;

import io.oneapi.admin.dto.reporting.ReportCatalogDTO;
import io.oneapi.admin.service.reporting.ReportCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing report catalogs.
 */
@RestController
@RequestMapping("/api/v1/catalogs")
@Tag(name = "Report Catalogs", description = "Manage report catalogs for organizing queries and reports")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class ReportCatalogController {

    private final ReportCatalogService catalogService;

    @PostMapping
    @Operation(summary = "Create a new catalog", description = "Creates a new catalog for organizing reports and queries")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ReportCatalogDTO> create(@Valid @RequestBody ReportCatalogDTO dto) {
        ReportCatalogDTO created = catalogService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a catalog", description = "Updates an existing catalog")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ReportCatalogDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ReportCatalogDTO dto) {
        ReportCatalogDTO updated = catalogService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get catalog by ID", description = "Retrieves a catalog by its ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportCatalogDTO> getById(@PathVariable Long id) {
        return catalogService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Get catalog by name", description = "Retrieves a catalog by its name")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportCatalogDTO> getByName(@PathVariable String name) {
        return catalogService.findByName(name)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "List all catalogs", description = "Retrieves all catalogs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportCatalogDTO>> getAll() {
        List<ReportCatalogDTO> catalogs = catalogService.findAll();
        return ResponseEntity.ok(catalogs);
    }

    @GetMapping("/my")
    @Operation(summary = "Get my catalogs", description = "Retrieves catalogs created by the current user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportCatalogDTO>> getMyCatalogs() {
        // TODO: Get current username from SecurityContext
        String currentUsername = "admin"; // Placeholder
        List<ReportCatalogDTO> catalogs = catalogService.findByCreatedBy(currentUsername);
        return ResponseEntity.ok(catalogs);
    }

    @GetMapping("/search")
    @Operation(summary = "Search catalogs", description = "Searches catalogs by name")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportCatalogDTO>> search(@RequestParam String q) {
        List<ReportCatalogDTO> catalogs = catalogService.search(q);
        return ResponseEntity.ok(catalogs);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a catalog", description = "Deletes a catalog")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        catalogService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/exists")
    @Operation(summary = "Check if catalog exists", description = "Checks if a catalog exists by ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> exists(@PathVariable Long id) {
        boolean exists = catalogService.exists(id);
        return ResponseEntity.ok(exists);
    }
}
