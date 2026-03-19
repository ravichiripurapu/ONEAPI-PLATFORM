package io.oneapi.admin.dto.metadata;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for ColumnMetadata entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldInfoDTO {

    private Long id;
    private Long tableId;
    private String tableName;
    private String schemaName;
    private String columnName;
    private String dataType;
    private String jdbcType;
    private Integer columnSize;
    private Integer decimalDigits;
    private Boolean nullable;
    private String defaultValue;
    private String columnComment;
    private Boolean isPrimaryKey;
    private Boolean isForeignKey;
    private Boolean isUnique;
    private Boolean isIndexed;
    private Boolean isAutoIncrement;
    private Integer ordinalPosition;
    private LocalDateTime discoveredAt;
    private LocalDateTime createdDate;

    // Optional: Include JSON schema fragment for debugging/inspection
    private String jsonSchemaFragment;
}
