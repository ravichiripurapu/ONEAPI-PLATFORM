package io.oneapi.admin.entity.metadata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "column_comment", length = 1000)
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

    // ========== AI-Generated Metadata Enrichment Fields ==========

    @Column(name = "business_name")
    private String businessName;

    @Column(name = "business_description", length = 500)
    private String businessDescription;

    @Column(name = "column_aliases", length = 500)
    private String columnAliases; // JSON array of alternate names

    @Column(name = "data_classification", length = 50)
    private String dataClassification; // e.g., "PII", "Sensitive", "Public"

    @Column(name = "sample_values", length = 500)
    private String sampleValues; // JSON array of example values

    @Column(name = "value_pattern", length = 200)
    private String valuePattern; // e.g., "email", "phone", "date", "uuid"

    // ========== Other Fields ==========

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    // ========== Convenience Methods for JSON Fields ==========

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public List<String> getColumnAliasesList() {
        if (columnAliases == null || columnAliases.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(columnAliases, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    public void setColumnAliasesList(List<String> aliases) {
        try {
            this.columnAliases = OBJECT_MAPPER.writeValueAsString(aliases);
        } catch (JsonProcessingException e) {
            this.columnAliases = "[]";
        }
    }

    public List<String> getSampleValuesList() {
        if (sampleValues == null || sampleValues.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(sampleValues, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    public void setSampleValuesList(List<String> samples) {
        try {
            this.sampleValues = OBJECT_MAPPER.writeValueAsString(samples);
        } catch (JsonProcessingException e) {
            this.sampleValues = "[]";
        }
    }
}
