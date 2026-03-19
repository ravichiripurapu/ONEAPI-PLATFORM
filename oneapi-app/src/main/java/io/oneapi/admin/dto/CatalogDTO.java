package io.oneapi.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing a database catalog with tables and views.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogDTO {
    private Long datasourceId;
    private String connectionName;
    private String databaseType;
    private LocalDateTime capturedAt;
    private List<TableDTO> tables;
    private List<ViewDTO> views;
    private CatalogStats stats;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CatalogStats {
        private Integer totalTables;
        private Integer totalViews;
        private Integer totalColumns;
        private Integer version;
    }
}
