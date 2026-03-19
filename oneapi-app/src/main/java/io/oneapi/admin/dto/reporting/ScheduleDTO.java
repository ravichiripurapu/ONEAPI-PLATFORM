package io.oneapi.admin.dto.reporting;

import io.oneapi.admin.entity.Report;
import io.oneapi.admin.entity.Schedule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Schedule entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDTO {

    private Long id;

    @NotBlank(message = "Schedule name is required")
    @Size(max = 255, message = "Schedule name must not exceed 255 characters")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    private Long reportId;
    private String reportName; // For display purposes

    private Long queryId;
    private String queryName; // For display purposes

    @NotBlank(message = "Cron expression is required")
    private String cronExpression;

    private Boolean enabled = true;

    private Report.OutputFormat outputFormat = Report.OutputFormat.JSON;

    private String emailRecipients; // Comma-separated email addresses

    private String webhookUrl;

    private LocalDateTime lastRunAt;
    private Schedule.RunStatus lastRunStatus;
    private String lastRunMessage;

    private LocalDateTime nextRunAt;

    private Long executionCount = 0L;
    private Long successCount = 0L;
    private Long failureCount = 0L;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
