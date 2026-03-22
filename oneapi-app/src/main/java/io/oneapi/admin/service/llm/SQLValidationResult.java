package io.oneapi.admin.service.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of SQL validation by LLM.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SQLValidationResult {

    private Boolean isValid;
    private Double confidence;
    private List<Issue> issues;
    private List<String> suggestions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Issue {
        private Severity severity;
        private String message;
        private String location; // e.g., "WHERE clause", "JOIN condition"

        public enum Severity {
            ERROR,    // Likely to cause SQL execution failure
            WARNING,  // Might cause unexpected results or performance issues
            INFO      // Suggestions for improvement
        }
    }
}
