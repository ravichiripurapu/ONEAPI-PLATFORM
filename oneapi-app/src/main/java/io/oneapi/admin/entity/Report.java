package io.oneapi.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Report entity for storing report definitions with parameters and output formats.
 */
@Entity
@Table(name = "report")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Lob
    @Column
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id", nullable = false)
    private SavedQuery query;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_id")
    private Catalog catalog;

    @Column(name = "output_format", nullable = false)
    @Enumerated(EnumType.STRING)
    private OutputFormat outputFormat = OutputFormat.JSON;

    @Lob
    @Column
    private String parameters;

    @Lob
    @Column(name = "default_parameters")
    private String defaultParameters;

    @Column(nullable = false)
    private Boolean isPublic = false;

    @Column(name = "execution_count")
    private Long executionCount = 0L;

    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;

    @Column(name = "avg_execution_time_ms")
    private Long avgExecutionTimeMs;

    @CreatedBy
    @Column(name = "created_by", length = 50)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 50)
    private String lastModifiedBy;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    public enum OutputFormat {
        JSON,
        CSV,
        EXCEL,
        PDF,
        HTML
    }
}
