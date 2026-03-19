package io.oneapi.admin.dto.reporting;

import io.oneapi.admin.entity.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for AuditLog entity - read-only.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {

    private Long id;
    private String userLogin;
    private AuditLog.Action action;
    private String entityType;
    private Long entityId;
    private String entityName;
    private String details; // JSON string with additional details
    private String ipAddress;
    private String userAgent;
    private LocalDateTime timestamp;
    private Long executionTimeMs;
    private AuditLog.Status status;
    private String errorMessage;
}
