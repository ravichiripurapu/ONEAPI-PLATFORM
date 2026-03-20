package io.oneapi.admin.controller.reporting;

import io.oneapi.admin.dto.reporting.ScheduleDTO;
import io.oneapi.admin.service.reporting.ScheduleService;
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
@RequestMapping("/api/v1/schedules")
@Tag(name = "Schedules", description = "Manage scheduled report and query executions")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    @Operation(summary = "Create schedule")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ScheduleDTO> create(@Valid @RequestBody ScheduleDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update schedule")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ScheduleDTO> update(@PathVariable Long id, @Valid @RequestBody ScheduleDTO dto) {
        return ResponseEntity.ok(scheduleService.update(id, dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get schedule by ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ScheduleDTO> getById(@PathVariable Long id) {
        return scheduleService.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "List all schedules")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ScheduleDTO>> getAll() {
        return ResponseEntity.ok(scheduleService.findAll());
    }

    @GetMapping("/my")
    @Operation(summary = "Get my schedules")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ScheduleDTO>> getMySchedules() {
        return ResponseEntity.ok(scheduleService.findByCreatedBy("admin")); // TODO: get from SecurityContext
    }

    @GetMapping("/report/{reportId}")
    @Operation(summary = "Get schedules by report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ScheduleDTO>> getByReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(scheduleService.findByReport(reportId));
    }

    @GetMapping("/query/{queryId}")
    @Operation(summary = "Get schedules by query")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ScheduleDTO>> getByQuery(@PathVariable Long queryId) {
        return ResponseEntity.ok(scheduleService.findByQuery(queryId));
    }

    @GetMapping("/enabled")
    @Operation(summary = "Get enabled schedules")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ScheduleDTO>> getEnabled() {
        return ResponseEntity.ok(scheduleService.findEnabled());
    }

    @PostMapping("/{id}/toggle")
    @Operation(summary = "Toggle schedule enabled status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ScheduleDTO> toggleEnable(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.toggleEnable(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete schedule")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
