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
 * Schedule entity for cron-based scheduling of reports and queries.
 */
@Entity
@Table(name = "schedule")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Lob
    @Column
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id")
    private SavedQuery query;

    @Column(name = "cron_expression", nullable = false)
    private String cronExpression;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "output_format")
    @Enumerated(EnumType.STRING)
    private Report.OutputFormat outputFormat = Report.OutputFormat.JSON;

    @Lob
    @Column(name = "email_recipients")
    private String emailRecipients;

    @Column(name = "webhook_url")
    private String webhookUrl;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "last_run_status")
    @Enumerated(EnumType.STRING)
    private RunStatus lastRunStatus;

    @Lob
    @Column(name = "last_run_message")
    private String lastRunMessage;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @Column(name = "execution_count")
    private Long executionCount = 0L;

    @Column(name = "success_count")
    private Long successCount = 0L;

    @Column(name = "failure_count")
    private Long failureCount = 0L;

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

    public enum RunStatus {
        SUCCESS,
        FAILURE,
        RUNNING,
        SKIPPED
    }
}
