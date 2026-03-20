package io.oneapi.admin.dto.metadata;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for SchemaMetadata entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DomainInfoDTO {

    private Long id;
    private Long datasourceId;
    private String connectionName;
    private String schemaName;
    private Integer tableCount;
    private Integer viewCount;
    private LocalDateTime discoveredAt;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime createdDate;
}
