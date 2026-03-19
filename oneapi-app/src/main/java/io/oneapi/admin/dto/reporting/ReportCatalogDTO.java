package io.oneapi.admin.dto.reporting;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Report Catalog entity - for organizing reports and queries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportCatalogDTO {

    private Long id;

    @NotBlank(message = "Catalog name is required")
    @Size(max = 255, message = "Catalog name must not exceed 255 characters")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
