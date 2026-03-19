package io.oneapi.admin.controller.reporting;

import io.oneapi.admin.dto.reporting.DashboardDTO;
import io.oneapi.admin.service.reporting.DashboardService;
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

@RestController
@RequestMapping("/api/v1/dashboards")
@Tag(name = "Dashboards", description = "Manage dashboard layouts with widgets and visualizations")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @PostMapping
    @Operation(summary = "Create dashboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardDTO> create(@Valid @RequestBody DashboardDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dashboardService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update dashboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardDTO> update(@PathVariable Long id, @Valid @RequestBody DashboardDTO dto) {
        return ResponseEntity.ok(dashboardService.update(id, dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dashboard by ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardDTO> getById(@PathVariable Long id) {
        return dashboardService.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "List all dashboards")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DashboardDTO>> getAll() {
        return ResponseEntity.ok(dashboardService.findAll());
    }

    @GetMapping("/my")
    @Operation(summary = "Get my dashboards")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DashboardDTO>> getMyDashboards() {
        return ResponseEntity.ok(dashboardService.findByCreatedBy("admin")); // TODO: get from SecurityContext
    }

    @GetMapping("/catalog/{catalogId}")
    @Operation(summary = "Get dashboards by catalog")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DashboardDTO>> getByCatalog(@PathVariable Long catalogId) {
        return ResponseEntity.ok(dashboardService.findByCatalog(catalogId));
    }

    @GetMapping("/public")
    @Operation(summary = "Get public dashboards")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DashboardDTO>> getPublicDashboards() {
        return ResponseEntity.ok(dashboardService.findPublicDashboards());
    }

    @GetMapping("/accessible")
    @Operation(summary = "Get accessible dashboards")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DashboardDTO>> getAccessibleDashboards() {
        return ResponseEntity.ok(dashboardService.findAccessibleDashboards("admin")); // TODO: get from SecurityContext
    }

    @GetMapping("/search")
    @Operation(summary = "Search dashboards")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DashboardDTO>> search(@RequestParam String q) {
        return ResponseEntity.ok(dashboardService.search("admin", q)); // TODO: get from SecurityContext
    }

    @PostMapping("/{id}/view")
    @Operation(summary = "Increment view count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> incrementViewCount(@PathVariable Long id) {
        dashboardService.incrementViewCount(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        dashboardService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
