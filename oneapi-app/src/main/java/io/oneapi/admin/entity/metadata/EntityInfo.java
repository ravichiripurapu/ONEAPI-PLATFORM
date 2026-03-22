package io.oneapi.admin.entity.metadata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    // ========== AI-Generated Metadata Enrichment Fields ==========

    @Column(name = "business_name")
    private String businessName;

    @Column(name = "business_description", length = 1000)
    private String businessDescription;

    @Column(name = "table_aliases", length = 500)
    private String tableAliases; // JSON array of alternate names

    @Column(name = "table_keywords", length = 500)
    private String tableKeywords; // JSON array of keywords

    @Column(name = "business_category", length = 100)
    private String businessCategory;

    @Column(name = "metadata_confidence", precision = 3, scale = 2)
    private BigDecimal metadataConfidence;

    @Column(name = "metadata_generated_by", length = 50)
    private String metadataGeneratedBy; // e.g., "gemini-1.5-flash", "gpt-4o-mini"

    @Column(name = "metadata_generated_at")
    private LocalDateTime metadataGeneratedAt;

    @Column(name = "metadata_reviewed_by", length = 50)
    private String metadataReviewedBy;

    @Column(name = "metadata_reviewed_at")
    private LocalDateTime metadataReviewedAt;

    // ========== Relationships ==========

    @OneToMany(mappedBy = "dataEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<FieldInfo> fields = new HashSet<>();

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    // ========== Convenience Methods for JSON Fields ==========

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public List<String> getTableAliasesList() {
        if (tableAliases == null || tableAliases.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(tableAliases, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    public void setTableAliasesList(List<String> aliases) {
        try {
            this.tableAliases = OBJECT_MAPPER.writeValueAsString(aliases);
        } catch (JsonProcessingException e) {
            this.tableAliases = "[]";
        }
    }

    public List<String> getTableKeywordsList() {
        if (tableKeywords == null || tableKeywords.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(tableKeywords, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    public void setTableKeywordsList(List<String> keywords) {
        try {
            this.tableKeywords = OBJECT_MAPPER.writeValueAsString(keywords);
        } catch (JsonProcessingException e) {
            this.tableKeywords = "[]";
        }
    }

    /**
     * Check if this entity has been enriched with AI-generated metadata.
     */
    public boolean isEnriched() {
        return businessName != null && metadataGeneratedAt != null;
    }

    // ========== Enums ==========

    public enum TableType {
        TABLE,
        VIEW,
        MATERIALIZED_VIEW,
        SYSTEM_TABLE
    }
}
