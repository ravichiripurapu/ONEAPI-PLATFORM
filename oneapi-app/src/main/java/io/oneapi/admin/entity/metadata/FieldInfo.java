package io.oneapi.admin.entity.metadata;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing a field (column/property) discovered from a data entity.
 * Aligned with SDK terminology: Source → Domain → DataEntity → Field
 */
@Entity
@Table(name = "field_info", indexes = {
    @Index(name = "idx_field_data_entity", columnList = "data_entity_id"),
    @Index(name = "idx_field_name", columnList = "column_name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class FieldInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = false)
    private EntityInfo dataEntity;

    @Column(name = "column_name", nullable = false)
    private String columnName;

    @Column(name = "data_type")
    private String dataType;

    @Column(name = "jdbc_type")
    private String jdbcType;

    @Column(name = "column_size")
    private Integer columnSize;

    @Column(name = "decimal_digits")
    private Integer decimalDigits;

    @Column(name = "nullable")
    private Boolean nullable = true;

    @Column(name = "default_value")
    private String defaultValue;

    @Lob
    @Column(name = "column_comment")
    private String columnComment;

    @Column(name = "is_primary_key")
    private Boolean isPrimaryKey = false;

    @Column(name = "is_foreign_key")
    private Boolean isForeignKey = false;

    @Column(name = "is_unique")
    private Boolean isUnique = false;

    @Column(name = "is_indexed")
    private Boolean isIndexed = false;

    @Column(name = "is_auto_increment")
    private Boolean isAutoIncrement = false;

    @Column(name = "ordinal_position")
    private Integer ordinalPosition;

    @Column(name = "discovered_at")
    private LocalDateTime discoveredAt;

    /**
     * Store the original JSON schema fragment from SDK
     */
    @Lob
    @Column(name = "json_schema_fragment")
    private String jsonSchemaFragment;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}
