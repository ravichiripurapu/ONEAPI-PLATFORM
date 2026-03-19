package io.oneapi.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AuditLog entity for tracking all user actions and API calls.
 */
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_user_action", columnList = "user_login, action"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_login", nullable = false, length = 50)
    private String userLogin;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Action action;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "entity_name")
    private String entityName;

    @Column(length = 5000)
    private String details;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    public enum Action {
        CREATE,
        READ,
        UPDATE,
        DELETE,
        EXECUTE,
        LOGIN,
        LOGOUT,
        EXPORT,
        IMPORT,
        SHARE,
        DOWNLOAD
    }

    public enum Status {
        SUCCESS,
        FAILURE,
        PARTIAL
    }

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
