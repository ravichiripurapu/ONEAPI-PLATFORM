package io.oneapi.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a database column.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnDTO {
    private String name;
    private String dataType;
    private Boolean nullable;
    private String defaultValue;
    private Integer maxLength;
    private Integer precision;
    private Integer scale;
}
