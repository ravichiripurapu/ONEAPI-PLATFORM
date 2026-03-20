package io.oneapi.admin.controller.reporting;

import io.oneapi.admin.dto.reporting.WidgetDTO;
import io.oneapi.admin.entity.Widget;
import io.oneapi.admin.service.reporting.WidgetService;
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
 * REST controller for managing widgets.
 */
@RestController
@RequestMapping("/api/v1/widgets")
@Tag(name = "Widgets", description = "Manage dashboard widgets")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class WidgetController {

    private final WidgetService widgetService;

    @PostMapping
    @Operation(summary = "Create a new widget", description = "Creates a new dashboard widget")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WidgetDTO> create(@Valid @RequestBody WidgetDTO dto) {
        WidgetDTO created = widgetService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a widget", description = "Updates an existing widget")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WidgetDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody WidgetDTO dto) {
        WidgetDTO updated = widgetService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get widget by ID", description = "Retrieves a widget by its ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WidgetDTO> getById(@PathVariable Long id) {
        return widgetService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/dashboard/{dashboardId}")
    @Operation(summary = "Get widgets by dashboard", description = "Retrieves all widgets for a specific dashboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WidgetDTO>> getByDashboard(@PathVariable Long dashboardId) {
        List<WidgetDTO> widgets = widgetService.findByDashboard(dashboardId);
        return ResponseEntity.ok(widgets);
    }

    @GetMapping("/query/{queryId}")
    @Operation(summary = "Get widgets by query", description = "Retrieves all widgets using a specific query")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WidgetDTO>> getByQuery(@PathVariable Long queryId) {
        List<WidgetDTO> widgets = widgetService.findByQuery(queryId);
        return ResponseEntity.ok(widgets);
    }

    @GetMapping("/report/{reportId}")
    @Operation(summary = "Get widgets by report", description = "Retrieves all widgets using a specific report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WidgetDTO>> getByReport(@PathVariable Long reportId) {
        List<WidgetDTO> widgets = widgetService.findByReport(reportId);
        return ResponseEntity.ok(widgets);
    }

    @GetMapping("/type/{widgetType}")
    @Operation(summary = "Get widgets by type", description = "Retrieves all widgets of a specific type")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WidgetDTO>> getByType(@PathVariable Widget.WidgetType widgetType) {
        List<WidgetDTO> widgets = widgetService.findByType(widgetType);
        return ResponseEntity.ok(widgets);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a widget", description = "Deletes a widget")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        widgetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
