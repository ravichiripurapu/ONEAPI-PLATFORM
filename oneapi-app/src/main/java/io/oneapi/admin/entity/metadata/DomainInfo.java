package io.oneapi.admin.entity.metadata;

import io.oneapi.admin.entity.SourceInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a domain (schema/namespace) discovered from a data source.
 * Aligned with SDK terminology: Source → Domain → DataEntity → Field
 */
@Entity
@Table(name = "domain_info", indexes = {
    @Index(name = "idx_domain_source", columnList = "source_id"),
    @Index(name = "idx_domain_name", columnList = "schema_name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DomainInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private SourceInfo source;

    @Column(name = "catalog_name")
    private String catalogName;

    @Column(name = "schema_name", nullable = false)
    private String schemaName;

    @Column(name = "table_count")
    private Integer tableCount = 0;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "discovered_at")
    private LocalDateTime discoveredAt;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @OneToMany(mappedBy = "domain", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EntityInfo> dataEntities = new HashSet<>();

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;
}
