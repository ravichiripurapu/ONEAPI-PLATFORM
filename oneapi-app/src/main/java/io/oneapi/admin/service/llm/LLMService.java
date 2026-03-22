package io.oneapi.admin.service.llm;

import io.oneapi.admin.dto.reporting.NaturalLanguageQueryRequest;
import io.oneapi.admin.dto.reporting.NaturalLanguageQueryResponse;

/**
 * Service interface for LLM-based natural language to SQL conversion.
 * Implementations can use different LLM providers (OpenAI, Claude, local models, etc.)
 */
public interface LLMService {

    /**
     * Converts a natural language question to SQL.
     *
     * @param request the natural language query request with context
     * @param databaseSchema schema information to help generate accurate SQL
     * @return the generated SQL with explanation and confidence score
     */
    NaturalLanguageQueryResponse convertToSQL(
        NaturalLanguageQueryRequest request,
        DatabaseSchemaContext databaseSchema
    );

    /**
     * Explains a given SQL query in natural language.
     *
     * @param sql the SQL query to explain
     * @param databaseSchema schema context for better explanation
     * @return natural language explanation of the SQL
     */
    String explainSQL(String sql, DatabaseSchemaContext databaseSchema);

    /**
     * Validates if the generated SQL is likely to be correct.
     *
     * @param sql the SQL to validate
     * @param databaseSchema schema context for validation
     * @return validation result with potential issues
     */
    SQLValidationResult validateSQL(String sql, DatabaseSchemaContext databaseSchema);

    /**
     * Checks if the LLM service is available and configured.
     *
     * @return true if the service is ready to use
     */
    boolean isAvailable();

    /**
     * Call the LLM with a generic prompt.
     * Used for metadata enrichment and other non-SQL tasks.
     *
     * @param prompt the prompt to send to the LLM
     * @return the LLM's response
     */
    String callLLM(String prompt);

    /**
     * Get the provider name (e.g., "gemini-1.5-flash", "gpt-4o-mini", "mock").
     *
     * @return the provider name
     */
    String getProviderName();
}
