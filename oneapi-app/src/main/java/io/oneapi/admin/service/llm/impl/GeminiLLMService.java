package io.oneapi.admin.service.llm.impl;

import io.oneapi.admin.dto.reporting.NaturalLanguageQueryRequest;
import io.oneapi.admin.dto.reporting.NaturalLanguageQueryResponse;
import io.oneapi.admin.service.llm.DatabaseSchemaContext;
import io.oneapi.admin.service.llm.LLMService;
import io.oneapi.admin.service.llm.SQLValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Google Gemini LLM service implementation.
 * Uses Gemini API for natural language to SQL conversion.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "oneapi.llm.provider", havingValue = "gemini")
public class GeminiLLMService implements LLMService {

    @Value("${oneapi.llm.gemini.api-key}")
    private String apiKey;

    @Value("${oneapi.llm.gemini.model:gemini-1.5-flash}")
    private String model;

    @Value("${oneapi.llm.gemini.temperature:0.1}")
    private double temperature;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private static final String SYSTEM_PROMPT = """
        You are a SQL query generator expert. Convert natural language questions into valid SQL queries.

        Rules:
        1. Use ONLY tables and columns that exist in the provided database schema
        2. Infer JOINs from foreign key relationships in the schema
        3. Use appropriate aggregations (COUNT, SUM, AVG, MIN, MAX) when asked
        4. Add date/time filters when time-based questions are asked
        5. Always include a LIMIT clause (default 100) to prevent large result sets
        6. Return ONLY valid SQL for the specified database type
        7. Do NOT include explanations, markdown formatting, or code blocks
        8. Return the raw SQL query only
        9. Use proper SQL syntax with line breaks for readability
        10. Ensure all column references are properly qualified with table names or aliases
        """;

    @Override
    public NaturalLanguageQueryResponse convertToSQL(
            NaturalLanguageQueryRequest request,
            DatabaseSchemaContext schema) {

        log.info("Converting natural language to SQL using Gemini: {}", request.getQuestion());

        try {
            String prompt = buildPrompt(request.getQuestion(), schema, request.getContext());
            String generatedSQL = callGeminiAPI(prompt);

            // Clean up the response
            generatedSQL = cleanSQLResponse(generatedSQL);

            String explanation = generateExplanation(generatedSQL, request.getQuestion());
            double confidence = calculateConfidence(generatedSQL, schema);

            return NaturalLanguageQueryResponse.builder()
                .generatedSQL(generatedSQL)
                .explanation(explanation)
                .confidence(confidence)
                .suggestedTables(extractTableNames(generatedSQL))
                .warnings(generateWarnings(generatedSQL))
                .build();

        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            throw new RuntimeException("Failed to generate SQL using Gemini: " + e.getMessage(), e);
        }
    }

    @Override
    public String explainSQL(String sql, DatabaseSchemaContext schema) {
        try {
            String prompt = String.format("""
                Explain what this SQL query does in simple terms:

                SQL Query:
                %s

                Database Schema:
                %s

                Provide a clear, concise explanation in 2-3 sentences.
                """, sql, schema.toFormattedString());

            return callGeminiAPI(prompt);
        } catch (Exception e) {
            log.error("Error explaining SQL with Gemini", e);
            return "Unable to generate explanation: " + e.getMessage();
        }
    }

    @Override
    public SQLValidationResult validateSQL(String sql, DatabaseSchemaContext schema) {
        // Basic validation - could be enhanced with Gemini API
        List<SQLValidationResult.Issue> issues = new ArrayList<>();
        boolean isValid = true;

        // Check for dangerous operations
        if (sql.toUpperCase().matches(".*(DROP|DELETE|TRUNCATE|UPDATE).*")) {
            issues.add(SQLValidationResult.Issue.builder()
                .severity(SQLValidationResult.Issue.Severity.ERROR)
                .message("Dangerous operations (DROP, DELETE, UPDATE, TRUNCATE) are not allowed")
                .location("Query body")
                .build());
            isValid = false;
        }

        // Check for LIMIT clause
        if (!sql.toUpperCase().contains("LIMIT")) {
            issues.add(SQLValidationResult.Issue.builder()
                .severity(SQLValidationResult.Issue.Severity.WARNING)
                .message("Consider adding a LIMIT clause for large datasets")
                .location("Query end")
                .build());
        }

        return SQLValidationResult.builder()
            .isValid(isValid)
            .confidence(0.90)
            .issues(issues)
            .suggestions(Arrays.asList(
                "Add indexes on filtered columns for better performance",
                "Consider caching this query if it runs frequently"
            ))
            .build();
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    // ========== Private Helper Methods ==========

    private String buildPrompt(String question, DatabaseSchemaContext schema,
                               NaturalLanguageQueryRequest.QueryContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(SYSTEM_PROMPT).append("\n\n");
        prompt.append("Natural Language Question:\n");
        prompt.append(question).append("\n\n");
        prompt.append("Database Schema:\n");
        prompt.append(schema.toFormattedString()).append("\n\n");

        if (context != null && context.getHint() != null && !context.getHint().isBlank()) {
            prompt.append("Additional Context:\n");
            prompt.append(context.getHint()).append("\n\n");
        }

        prompt.append("Generate the SQL query now:");

        return prompt.toString();
    }

    private String callGeminiAPI(String prompt) {
        String url = String.format(GEMINI_API_URL, model, apiKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();

        // Build contents array
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        List<Map<String, String>> parts = new ArrayList<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", prompt);
        parts.add(part);
        content.put("parts", parts);
        contents.add(content);
        requestBody.put("contents", contents);

        // Add generation config
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", temperature);
        generationConfig.put("maxOutputTokens", 2048);
        requestBody.put("generationConfig", generationConfig);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

        if (response != null && response.containsKey("candidates")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (!candidates.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> candidate = candidates.get(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> contentResponse = (Map<String, Object>) candidate.get("content");
                @SuppressWarnings("unchecked")
                List<Map<String, String>> partsResponse = (List<Map<String, String>>) contentResponse.get("parts");
                if (!partsResponse.isEmpty()) {
                    return partsResponse.get(0).get("text");
                }
            }
        }

        throw new RuntimeException("No response from Gemini API");
    }

    private String cleanSQLResponse(String sql) {
        // Remove markdown code blocks
        sql = sql.replaceAll("```sql\\s*", "");
        sql = sql.replaceAll("```\\s*", "");

        // Remove extra whitespace
        sql = sql.trim();

        // Ensure it ends with semicolon
        if (!sql.endsWith(";")) {
            sql += ";";
        }

        return sql;
    }

    private String generateExplanation(String sql, String originalQuestion) {
        String explanation = "This SQL query ";

        if (sql.toUpperCase().contains("SELECT")) {
            explanation += "retrieves data ";
        }
        if (sql.toUpperCase().contains("JOIN")) {
            explanation += "by joining multiple tables ";
        }
        if (sql.toUpperCase().contains("WHERE")) {
            explanation += "with specific filtering conditions ";
        }
        if (sql.toUpperCase().contains("GROUP BY")) {
            explanation += "grouped by specific columns ";
        }
        if (sql.toUpperCase().contains("ORDER BY")) {
            explanation += "sorted in a specific order ";
        }
        if (sql.toUpperCase().contains("LIMIT")) {
            Pattern limitPattern = Pattern.compile("LIMIT\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = limitPattern.matcher(sql);
            if (matcher.find()) {
                explanation += "limited to " + matcher.group(1) + " rows ";
            }
        }

        explanation += "to answer: \"" + originalQuestion + "\"";

        return explanation.trim() + ".";
    }

    private double calculateConfidence(String sql, DatabaseSchemaContext schema) {
        double confidence = 0.85; // Base confidence

        // Increase confidence if SQL contains proper table references
        long tableCount = schema.getTables().stream()
            .filter(table -> sql.toUpperCase().contains(table.getTableName().toUpperCase()))
            .count();

        if (tableCount > 0) {
            confidence += 0.05;
        }

        // Increase confidence if SQL has proper structure
        if (sql.toUpperCase().contains("SELECT") &&
            sql.toUpperCase().contains("FROM") &&
            sql.toUpperCase().contains("LIMIT")) {
            confidence += 0.05;
        }

        // Decrease confidence if SQL is very short (might be incomplete)
        if (sql.length() < 50) {
            confidence -= 0.10;
        }

        return Math.min(0.95, Math.max(0.60, confidence));
    }

    private List<String> extractTableNames(String sql) {
        List<String> tables = new ArrayList<>();
        Pattern pattern = Pattern.compile("FROM\\s+(\\w+\\.\\w+|\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            tables.add(matcher.group(1));
        }

        pattern = Pattern.compile("JOIN\\s+(\\w+\\.\\w+|\\w+)", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(sql);
        while (matcher.find()) {
            tables.add(matcher.group(1));
        }

        return tables;
    }

    private List<String> generateWarnings(String sql) {
        List<String> warnings = new ArrayList<>();

        if (!sql.toUpperCase().contains("LIMIT")) {
            warnings.add("Query does not have a LIMIT clause - may return large result sets");
        }

        if (sql.toUpperCase().contains("SELECT *")) {
            warnings.add("Using SELECT * - consider specifying only needed columns for better performance");
        }

        if (sql.toUpperCase().matches(".*WHERE.*LIKE\\s+'%.*")) {
            warnings.add("LIKE pattern starts with wildcard - may be slow on large datasets");
        }

        return warnings;
    }

    @Override
    public String callLLM(String prompt) {
        log.debug("Calling Gemini API with prompt of length: {}", prompt.length());

        try {
            String response = callGeminiAPI(prompt);
            return response;
        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return model; // e.g., "gemini-1.5-flash"
    }
}
