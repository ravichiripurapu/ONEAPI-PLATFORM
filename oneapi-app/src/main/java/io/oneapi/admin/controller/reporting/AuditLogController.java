package io.oneapi.admin.controller.reporting;

import io.oneapi.admin.dto.reporting.AuditLogDTO;
import io.oneapi.admin.entity.AuditLog;
import io.oneapi.admin.service.reporting.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Audit Logs", description = "View audit logs and user activity (read-only, admin access)")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping("/{id}")
    @Operation(summary = "Get audit log by ID")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AuditLogDTO> getById(@PathVariable Long id) {
        return auditLogService.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "List all audit logs")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<AuditLogDTO>> getAll() {
        // Note: In production, this should be paginated to avoid memory issues
        // Consider adding pagination parameters
        return ResponseEntity.ok(auditLogService.findByDateRange(
            LocalDateTime.now().minusDays(30),
            LocalDateTime.now()
        ));
    }

    @GetMapping("/user/{username}")
    @Operation(summary = "Get audit logs for specific user")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<AuditLogDTO>> getByUser(@PathVariable String username) {
        return ResponseEntity.ok(auditLogService.findByUser(username));
    }

    @GetMapping("/action/{action}")
    @Operation(summary = "Get audit logs by action type")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<AuditLogDTO>> getByAction(@PathVariable AuditLog.Action action) {
        return ResponseEntity.ok(auditLogService.findByAction(action));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Get audit logs for specific entity")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<AuditLogDTO>> getByEntity(
        @PathVariable String entityType,
        @PathVariable Long entityId
    ) {
        return ResponseEntity.ok(auditLogService.findByEntity(entityType, entityId));
    }

    @GetMapping("/search")
    @Operation(summary = "Search audit logs by date range and optional filters")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<AuditLogDTO>> search(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
        @RequestParam(required = false) String username,
        @RequestParam(required = false) AuditLog.Action action,
        @RequestParam(required = false) AuditLog.Status status
    ) {
        List<AuditLogDTO> results;

        if (username != null) {
            results = auditLogService.findUserActivity(username, start, end);
        } else if (status != null) {
            // Filter by status then date range
            results = auditLogService.findByStatus(status);
            results = results.stream()
                .filter(log -> !log.getTimestamp().isBefore(start) && !log.getTimestamp().isAfter(end))
                .toList();
        } else if (action != null) {
            // Filter by action then date range
            results = auditLogService.findByAction(action);
            results = results.stream()
                .filter(log -> !log.getTimestamp().isBefore(start) && !log.getTimestamp().isAfter(end))
                .toList();
        } else {
            results = auditLogService.findByDateRange(start, end);
        }

        return ResponseEntity.ok(results);
    }
}
