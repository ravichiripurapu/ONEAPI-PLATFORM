package io.oneapi.admin.dto.reporting;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for natural language query request.
 * Converts a plain English question into SQL.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NaturalLanguageQueryRequest {

    @NotBlank(message = "Question is required")
    @Size(max = 1000, message = "Question must not exceed 1000 characters")
    private String question;

    @NotNull(message = "Datasource ID is required")
    private Long datasourceId;

    /**
     * Optional context to help the LLM generate better SQL.
     */
    private QueryContext context;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryContext {
        /**
         * Specific tables to focus on (optional).
         * If provided, the LLM will prioritize these tables.
         */
        private List<String> tables;

        /**
         * Previous queries or conversation history (optional).
         * Helps maintain context in multi-turn conversations.
         */
        private List<String> previousQueries;

        /**
         * Additional hints or requirements (optional).
         * E.g., "Use LEFT JOIN", "Include only active records"
         */
        private String hint;
    }
}
