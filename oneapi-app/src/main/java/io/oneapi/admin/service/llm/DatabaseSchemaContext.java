package io.oneapi.admin.service.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Context object containing database schema information for LLM.
 * This helps the LLM generate accurate SQL by knowing available tables, columns, and relationships.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatabaseSchemaContext {

    private String databaseType; // PostgreSQL, MySQL, Snowflake, etc.
    private String databaseName;
    private List<TableInfo> tables;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TableInfo {
        private String schemaName;
        private String tableName;
        private String description;
        private List<ColumnInfo> columns;
        private List<String> primaryKeys;
        private List<ForeignKeyInfo> foreignKeys;
        private Long estimatedRowCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ColumnInfo {
        private String columnName;
        private String dataType;
        private Boolean nullable;
        private String description;
        private Boolean isPrimaryKey;
        private Boolean isForeignKey;
        private String referencedTable;
        private String referencedColumn;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ForeignKeyInfo {
        private String columnName;
        private String referencedSchema;
        private String referencedTable;
        private String referencedColumn;
    }

    /**
     * Converts the schema context to a formatted string for LLM prompts.
     */
    public String toFormattedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Database Type: ").append(databaseType).append("\n");
        sb.append("Database: ").append(databaseName).append("\n\n");
        sb.append("Available Tables:\n\n");

        for (TableInfo table : tables) {
            sb.append(table.schemaName).append(".").append(table.tableName).append("\n");
            if (table.description != null) {
                sb.append("  Description: ").append(table.description).append("\n");
            }
            if (table.estimatedRowCount != null) {
                sb.append("  Rows: ~").append(table.estimatedRowCount).append("\n");
            }
            sb.append("  Columns:\n");
            for (ColumnInfo col : table.columns) {
                sb.append("    - ").append(col.columnName)
                  .append(" (").append(col.dataType).append(")");
                if (col.isPrimaryKey) {
                    sb.append(" [PRIMARY KEY]");
                }
                if (col.isForeignKey) {
                    sb.append(" [FK -> ").append(col.referencedTable).append("]");
                }
                if (col.description != null) {
                    sb.append(" - ").append(col.description);
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
