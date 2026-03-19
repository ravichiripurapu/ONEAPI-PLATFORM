package io.oneapi.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing a database view with its columns.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViewDTO {
    private String name;
    private String schema;
    private List<ColumnDTO> columns;
}
