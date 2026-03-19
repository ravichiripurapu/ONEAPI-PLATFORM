package io.oneapi.admin.dto.reporting;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for SavedQuery entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavedQueryDTO {

    private Long id;

    @NotBlank(message = "Query name is required")
    @Size(max = 255, message = "Query name must not exceed 255 characters")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotBlank(message = "Query text is required")
    private String queryText;

    @NotNull(message = "Connection ID is required")
    private Long datasourceId;

    private String connectionName; // For display purposes

    private Long catalogId;
    private String catalogName; // For display purposes

    private Boolean isPublic = false;
    private Boolean isFavorite = false;

    private Long executionCount = 0L;
    private LocalDateTime lastExecutedAt;
    private Long avgExecutionTimeMs;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
