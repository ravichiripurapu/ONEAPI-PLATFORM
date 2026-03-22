package io.oneapi.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a data source connection configuration.
 * Aligned with SDK terminology: Source → Domain → DataEntity → Field
 */
@Entity
@Table(name = "source_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DatabaseType type;

    // Host can be null for embedded databases like H2
    private String host;

    // Port can be null for embedded databases
    private Integer port;

    @Column(nullable = false)
    private String database;

    private String username;

    private String password;

    @Column(length = 2000)
    private String additionalParams;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean active = true;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum DatabaseType {
        POSTGRESQL,
        H2,
        MYSQL,
        ORACLE,
        MSSQL,
        MARIADB
    }
}
