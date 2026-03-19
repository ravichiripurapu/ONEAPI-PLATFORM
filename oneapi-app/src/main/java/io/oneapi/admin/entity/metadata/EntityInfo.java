package io.oneapi.admin.entity.metadata;

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
 * Entity representing a data entity (table/view/collection) discovered from a domain.
 * Aligned with SDK terminology: Source → Domain → DataEntity → Field
 */
@Entity
@Table(name = "entity_info", indexes = {
    @Index(name = "idx_data_entity_domain", columnList = "domain_id"),
    @Index(name = "idx_data_entity_name", columnList = "table_name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EntityInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", nullable = false)
    private DomainInfo domain;

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @Enumerated(EnumType.STRING)
    @Column(name = "table_type")
    private TableType tableType = TableType.TABLE;

    @Column(name = "estimated_row_count")
    private Long estimatedRowCount;

    @Column(name = "size_in_bytes")
    private Long sizeInBytes;

    @Column(name = "table_comment", length = 1000)
    private String tableComment;

    @Column(name = "discovered_at")
    private LocalDateTime discoveredAt;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @OneToMany(mappedBy = "dataEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<FieldInfo> fields = new HashSet<>();

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    public enum TableType {
        TABLE,
        VIEW,
        MATERIALIZED_VIEW,
        SYSTEM_TABLE
    }
}
