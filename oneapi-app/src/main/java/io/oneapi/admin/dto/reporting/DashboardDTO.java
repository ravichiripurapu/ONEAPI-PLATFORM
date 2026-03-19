package io.oneapi.admin.dto.reporting;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for Dashboard entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {

    private Long id;

    @NotBlank(message = "Dashboard name is required")
    @Size(max = 255, message = "Dashboard name must not exceed 255 characters")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    private Long catalogId;
    private String catalogName; // For display purposes

    private Boolean isPublic = false;

    private Integer refreshIntervalSeconds;

    private String layout; // JSON string for grid layout configuration

    private List<WidgetDTO> widgets = new ArrayList<>();

    private Long viewCount = 0L;
    private LocalDateTime lastViewedAt;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
