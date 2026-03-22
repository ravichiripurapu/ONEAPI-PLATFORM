package io.oneapi.admin.dto.reporting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for natural language query response.
 * Contains the generated SQL and metadata about the generation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NaturalLanguageQueryResponse {

    /**
     * The generated SQL query.
     */
    private String generatedSQL;

    /**
     * Human-readable explanation of what the query does.
     */
    private String explanation;

    /**
     * Confidence score (0.0 to 1.0) indicating how confident
     * the LLM is about the generated SQL.
     */
    private Double confidence;

    /**
     * Tables that were identified and used in the query.
     */
    private List<String> suggestedTables;

    /**
     * Warnings or notes about the generated query.
     * E.g., "This query might be slow on large datasets"
     */
    private List<String> warnings;

    /**
     * Alternative queries the LLM considered.
     */
    private List<AlternativeQuery> alternatives;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlternativeQuery {
        private String sql;
        private String reasoning;
        private Double confidence;
    }
}
