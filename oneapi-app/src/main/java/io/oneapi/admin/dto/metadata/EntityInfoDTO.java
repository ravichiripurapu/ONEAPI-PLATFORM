package io.oneapi.admin.dto.metadata;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for TableMetadata entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityInfoDTO {

    private Long id;
    private Long schemaId;
    private String schemaName;
    private String catalogName;
    private String tableName;
    private String tableType; // TABLE, VIEW, MATERIALIZED_VIEW, SYSTEM_TABLE
    private String tableComment;
    private Long estimatedRowCount;
    private Long sizeInBytes;
    private LocalDateTime discoveredAt;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime createdDate;

    // Optional: Include SDK entity JSON for debugging/inspection
    private String sdkEntityJson;
}
