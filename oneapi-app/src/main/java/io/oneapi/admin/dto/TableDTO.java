package io.oneapi.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing a database table with its columns.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableDTO {
    private String name;
    private String schema;
    private List<ColumnDTO> columns;
    private List<String> primaryKeys;
    private List<String> incrementalFields;
    private Boolean hasIncrementalField;
}
