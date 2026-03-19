package io.oneapi.admin.controller.reporting;

import io.oneapi.admin.dto.reporting.ReportDTO;
import io.oneapi.admin.service.reporting.ReportService;
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
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Manage report definitions with parameters and output formats")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @Operation(summary = "Create report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportDTO> create(@Valid @RequestBody ReportDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportDTO> update(@PathVariable Long id, @Valid @RequestBody ReportDTO dto) {
        return ResponseEntity.ok(reportService.update(id, dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get report by ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportDTO> getById(@PathVariable Long id) {
        return reportService.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "List all reports")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportDTO>> getAll() {
        return ResponseEntity.ok(reportService.findAll());
    }

    @GetMapping("/my")
    @Operation(summary = "Get my reports")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportDTO>> getMyReports() {
        return ResponseEntity.ok(reportService.findByCreatedBy("admin")); // TODO: get from SecurityContext
    }

    @GetMapping("/query/{queryId}")
    @Operation(summary = "Get reports by query")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportDTO>> getByQuery(@PathVariable Long queryId) {
        return ResponseEntity.ok(reportService.findByQuery(queryId));
    }

    @GetMapping("/catalog/{catalogId}")
    @Operation(summary = "Get reports by catalog")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportDTO>> getByCatalog(@PathVariable Long catalogId) {
        return ResponseEntity.ok(reportService.findByCatalog(catalogId));
    }

    @GetMapping("/public")
    @Operation(summary = "Get public reports")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportDTO>> getPublicReports() {
        return ResponseEntity.ok(reportService.findPublicReports());
    }

    @GetMapping("/accessible")
    @Operation(summary = "Get accessible reports")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportDTO>> getAccessibleReports() {
        return ResponseEntity.ok(reportService.findAccessibleReports("admin")); // TODO: get from SecurityContext
    }

    @GetMapping("/search")
    @Operation(summary = "Search reports")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportDTO>> search(@RequestParam String q) {
        return ResponseEntity.ok(reportService.search("admin", q)); // TODO: get from SecurityContext
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "Record execution")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> recordExecution(@PathVariable Long id, @RequestParam long executionTimeMs) {
        reportService.recordExecution(id, executionTimeMs);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reportService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
