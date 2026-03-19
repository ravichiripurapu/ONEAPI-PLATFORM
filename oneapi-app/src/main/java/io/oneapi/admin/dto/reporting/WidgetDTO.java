package io.oneapi.admin.dto.reporting;

import io.oneapi.admin.entity.Widget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Widget entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WidgetDTO {

    private Long id;

    @NotBlank(message = "Widget name is required")
    @Size(max = 255, message = "Widget name must not exceed 255 characters")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotNull(message = "Dashboard ID is required")
    private Long dashboardId;

    private Long queryId;
    private String queryName; // For display purposes

    private Long reportId;
    private String reportName; // For display purposes

    @NotNull(message = "Widget type is required")
    private Widget.WidgetType widgetType;

    private Integer positionX;
    private Integer positionY;
    private Integer width;
    private Integer height;

    private String configuration; // JSON string for chart/widget configuration

    private Integer refreshIntervalSeconds;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
