package io.oneapi.admin.dto.reporting;

import io.oneapi.admin.entity.Report;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Report entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {

    private Long id;

    @NotBlank(message = "Report name is required")
    @Size(max = 255, message = "Report name must not exceed 255 characters")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotNull(message = "Query ID is required")
    private Long queryId;

    private String queryName; // For display purposes

    private Long sourceId;
    private String sourceName; // For display purposes

    @NotNull(message = "Output format is required")
    private Report.OutputFormat outputFormat = Report.OutputFormat.JSON;

    private String parameters; // JSON string of parameter definitions
    private String defaultParameters; // JSON string of default parameter values

    private Boolean isPublic = false;

    private Long executionCount = 0L;
    private LocalDateTime lastExecutedAt;
    private Long avgExecutionTimeMs;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
