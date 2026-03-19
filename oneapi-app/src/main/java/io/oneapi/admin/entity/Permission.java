package io.oneapi.admin.entity;

import io.oneapi.admin.entity.metadata.DomainInfo;
import io.oneapi.admin.entity.metadata.EntityInfo;
import io.oneapi.admin.entity.metadata.FieldInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Permission entity for hierarchical RBAC.
 * Uses NULL pattern for wildcards:
 * - Level 0 (Super Admin): All NULLs = access to everything
 * - Level 1 (Source): source_id set, rest NULL = all domains/entities/fields in source
 * - Level 2 (Domain): source_id + domain_id set = all entities/fields in domain
 * - Level 3 (Entity): source_id + domain_id + entity_id set = all fields in entity
 * - Level 4 (Field): All FKs set = specific field only
 */
@Entity
@Table(name = "permission", indexes = {
        @Index(name = "idx_permission_role_source", columnList = "role_id, source_id"),
        @Index(name = "idx_permission_role_domain", columnList = "role_id, domain_id"),
        @Index(name = "idx_permission_role_entity", columnList = "role_id, entity_id"),
        @Index(name = "idx_permission_role_field", columnList = "role_id, field_id"),
        @Index(name = "idx_permission_hierarchy", columnList = "role_id, source_id, domain_id, entity_id, field_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    // Nullable FKs create hierarchy (NULL = wildcard for all children)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private SourceInfo source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id")
    private DomainInfo domain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    private EntityInfo entity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id")
    private FieldInfo field;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_type", nullable = false, length = 20)
    private PermissionType permissionType = PermissionType.READ;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Permission types (currently only READ for reporting)
     */
    public enum PermissionType {
        READ
    }

    /**
     * Get permission level (0-4) based on which FKs are set
     */
    public int getLevel() {
        if (field != null) return 4;      // Field level
        if (entity != null) return 3;     // Entity level
        if (domain != null) return 2;     // Domain level
        if (source != null) return 1;     // Source level
        return 0;                         // Super admin level
    }
}
