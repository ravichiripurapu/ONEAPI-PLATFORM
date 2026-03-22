package io.oneapi.admin.service.llm.impl;

import io.oneapi.admin.dto.reporting.NaturalLanguageQueryRequest;
import io.oneapi.admin.dto.reporting.NaturalLanguageQueryResponse;
import io.oneapi.admin.service.llm.DatabaseSchemaContext;
import io.oneapi.admin.service.llm.LLMService;
import io.oneapi.admin.service.llm.SQLValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Smart mock implementation of LLM service.
 * Uses pattern matching and heuristics to generate SQL from natural language.
 * Good for testing and demonstration purposes.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "oneapi.llm.provider", havingValue = "mock", matchIfMissing = true)
public class SmartMockLLMService implements LLMService {

    @Override
    public NaturalLanguageQueryResponse convertToSQL(
            NaturalLanguageQueryRequest request,
            DatabaseSchemaContext schema) {

        log.info("Converting natural language to SQL (mock): {}", request.getQuestion());

        String question = request.getQuestion().toLowerCase();
        List<String> suggestedTables = identifyTablesFromQuestion(question, schema);

        String sql = generateSQLFromQuestion(question, suggestedTables, schema);
        String explanation = generateExplanation(question, sql);
        double confidence = calculateConfidence(question, suggestedTables);

        return NaturalLanguageQueryResponse.builder()
            .generatedSQL(sql)
            .explanation(explanation)
            .confidence(confidence)
            .suggestedTables(suggestedTables)
            .warnings(generateWarnings(sql))
            .build();
    }

    @Override
    public String explainSQL(String sql, DatabaseSchemaContext schema) {
        // Basic SQL explanation logic
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
            explanation += "and groups the results ";
        }
        if (sql.toUpperCase().contains("ORDER BY")) {
            explanation += "sorted in a specific order ";
        }

        return explanation.trim() + ".";
    }

    @Override
    public SQLValidationResult validateSQL(String sql, DatabaseSchemaContext schema) {
        List<SQLValidationResult.Issue> issues = new ArrayList<>();
        boolean isValid = true;

        // Basic validation
        if (!sql.trim().toUpperCase().startsWith("SELECT")) {
            issues.add(SQLValidationResult.Issue.builder()
                .severity(SQLValidationResult.Issue.Severity.ERROR)
                .message("Only SELECT queries are supported")
                .location("Query start")
                .build());
            isValid = false;
        }

        if (sql.toUpperCase().contains("DROP") ||
            sql.toUpperCase().contains("DELETE") ||
            sql.toUpperCase().contains("UPDATE")) {
            issues.add(SQLValidationResult.Issue.builder()
                .severity(SQLValidationResult.Issue.Severity.ERROR)
                .message("Dangerous operations (DROP, DELETE, UPDATE) are not allowed")
                .location("Query body")
                .build());
            isValid = false;
        }

        if (!sql.toUpperCase().contains("LIMIT")) {
            issues.add(SQLValidationResult.Issue.builder()
                .severity(SQLValidationResult.Issue.Severity.WARNING)
                .message("Consider adding a LIMIT clause for large datasets")
                .location("Query end")
                .build());
        }

        return SQLValidationResult.builder()
            .isValid(isValid)
            .confidence(0.85)
            .issues(issues)
            .suggestions(Arrays.asList(
                "Add indexes on filtered columns for better performance",
                "Consider caching this query if it runs frequently"
            ))
            .build();
    }

    @Override
    public boolean isAvailable() {
        return true; // Mock is always available
    }

    // ========== Private Helper Methods ==========

    private List<String> identifyTablesFromQuestion(String question, DatabaseSchemaContext schema) {
        List<String> tables = new ArrayList<>();

        for (DatabaseSchemaContext.TableInfo table : schema.getTables()) {
            String tableName = table.getTableName().toLowerCase();
            String singularForm = tableName.endsWith("s") ?
                tableName.substring(0, tableName.length() - 1) : tableName;

            if (question.contains(tableName) || question.contains(singularForm)) {
                tables.add(table.getSchemaName() + "." + table.getTableName());
            }
        }

        // Default to first table if none identified
        if (tables.isEmpty() && !schema.getTables().isEmpty()) {
            DatabaseSchemaContext.TableInfo firstTable = schema.getTables().get(0);
            tables.add(firstTable.getSchemaName() + "." + firstTable.getTableName());
        }

        return tables;
    }

    private String generateSQLFromQuestion(
            String question,
            List<String> tables,
            DatabaseSchemaContext schema) {

        StringBuilder sql = new StringBuilder();

        // Determine aggregation
        String aggregation = determineAggregation(question);

        // Build SELECT clause
        sql.append("SELECT ");
        if (aggregation != null) {
            sql.append(aggregation);
        } else {
            sql.append("*");
        }

        // Build FROM clause
        if (!tables.isEmpty()) {
            sql.append("\nFROM ").append(tables.get(0));

            // Add JOINs if multiple tables
            for (int i = 1; i < tables.size(); i++) {
                sql.append("\nJOIN ").append(tables.get(i))
                   .append(" ON ").append(tables.get(0).split("\\.")[1])
                   .append(".id = ").append(tables.get(i).split("\\.")[1]).append(".id");
            }
        }

        // Build WHERE clause
        String whereClause = determineWhereClause(question);
        if (whereClause != null) {
            sql.append("\nWHERE ").append(whereClause);
        }

        // Build GROUP BY if aggregation
        if (aggregation != null && aggregation.contains("GROUP BY")) {
            // GROUP BY is included in aggregation
        } else if (aggregation != null) {
            sql.append("\nGROUP BY ").append(determineGroupBy(question));
        }

        // Build ORDER BY
        String orderBy = determineOrderBy(question);
        if (orderBy != null) {
            sql.append("\nORDER BY ").append(orderBy);
        }

        // Add LIMIT
        Integer limit = determineLimit(question);
        sql.append("\nLIMIT ").append(limit != null ? limit : 100);

        sql.append(";");

        return sql.toString();
    }

    private String determineAggregation(String question) {
        if (question.contains("total") || question.contains("sum")) {
            return "SUM(amount) as total";
        }
        if (question.contains("count") || question.contains("how many")) {
            return "COUNT(*) as count";
        }
        if (question.contains("average") || question.contains("avg")) {
            return "AVG(amount) as average";
        }
        if (question.contains("maximum") || question.contains("max")) {
            return "MAX(amount) as maximum";
        }
        if (question.contains("minimum") || question.contains("min")) {
            return "MIN(amount) as minimum";
        }
        if (question.contains("group by") || question.contains("grouped by")) {
            return "column_name, COUNT(*) as count GROUP BY column_name";
        }
        return null;
    }

    private String determineWhereClause(String question) {
        List<String> conditions = new ArrayList<>();

        // Date filters
        if (question.contains("today")) {
            conditions.add("DATE(created_at) = CURRENT_DATE");
        } else if (question.contains("yesterday")) {
            conditions.add("DATE(created_at) = CURRENT_DATE - 1");
        } else if (question.contains("this week")) {
            conditions.add("created_at >= DATE_TRUNC('week', CURRENT_DATE)");
        } else if (question.contains("this month")) {
            conditions.add("created_at >= DATE_TRUNC('month', CURRENT_DATE)");
        } else if (question.contains("last") && question.contains("days")) {
            try {
                String[] parts = question.split("last\\s+");
                if (parts.length > 1) {
                    String numStr = parts[1].split("\\s+")[0];
                    int days = Integer.parseInt(numStr);
                    conditions.add("created_at >= CURRENT_DATE - " + days);
                }
            } catch (Exception e) {
                conditions.add("created_at >= CURRENT_DATE - 30");
            }
        }

        // Status filters
        if (question.contains("active")) {
            conditions.add("status = 'ACTIVE'");
        } else if (question.contains("inactive")) {
            conditions.add("status = 'INACTIVE'");
        } else if (question.contains("pending")) {
            conditions.add("status = 'PENDING'");
        }

        // Greater/less than
        if (question.contains("greater than") || question.contains("more than") || question.contains(">")) {
            conditions.add("amount > value");
        } else if (question.contains("less than") || question.contains("fewer than") || question.contains("<")) {
            conditions.add("amount < value");
        }

        return conditions.isEmpty() ? null : String.join(" AND ", conditions);
    }

    private String determineGroupBy(String question) {
        if (question.contains("by customer")) {
            return "customer_id";
        }
        if (question.contains("by date") || question.contains("by day")) {
            return "DATE(created_at)";
        }
        if (question.contains("by month")) {
            return "DATE_TRUNC('month', created_at)";
        }
        if (question.contains("by year")) {
            return "DATE_TRUNC('year', created_at)";
        }
        if (question.contains("by category") || question.contains("by type")) {
            return "category";
        }
        return "id";
    }

    private String determineOrderBy(String question) {
        if (question.contains("latest") || question.contains("newest") || question.contains("recent")) {
            return "created_at DESC";
        }
        if (question.contains("oldest")) {
            return "created_at ASC";
        }
        if (question.contains("highest") || question.contains("largest")) {
            return "amount DESC";
        }
        if (question.contains("lowest") || question.contains("smallest")) {
            return "amount ASC";
        }
        if (question.contains("alphabetically") || question.contains("alphabetical")) {
            return "name ASC";
        }
        return null;
    }

    private Integer determineLimit(String question) {
        try {
            if (question.contains("top ")) {
                String[] parts = question.split("top\\s+");
                if (parts.length > 1) {
                    String numStr = parts[1].split("\\s+")[0];
                    return Integer.parseInt(numStr);
                }
            }
            if (question.contains("first ")) {
                String[] parts = question.split("first\\s+");
                if (parts.length > 1) {
                    String numStr = parts[1].split("\\s+")[0];
                    return Integer.parseInt(numStr);
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return 100; // Default limit
    }

    private String generateExplanation(String question, String sql) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("This query ");

        if (sql.contains("COUNT")) {
            explanation.append("counts ");
        } else if (sql.contains("SUM")) {
            explanation.append("calculates the total ");
        } else if (sql.contains("AVG")) {
            explanation.append("calculates the average ");
        } else {
            explanation.append("retrieves ");
        }

        explanation.append("data based on your question: '").append(question).append("'. ");

        if (sql.contains("WHERE")) {
            explanation.append("It filters the results based on specific conditions. ");
        }

        if (sql.contains("JOIN")) {
            explanation.append("It combines data from multiple tables. ");
        }

        if (sql.contains("GROUP BY")) {
            explanation.append("The results are grouped for aggregation. ");
        }

        if (sql.contains("ORDER BY")) {
            explanation.append("The results are sorted in a specific order. ");
        }

        return explanation.toString().trim();
    }

    private double calculateConfidence(String question, List<String> tables) {
        double confidence = 0.7; // Base confidence

        // Increase if we found tables
        if (!tables.isEmpty()) {
            confidence += 0.1;
        }

        // Increase if question contains clear SQL keywords
        if (question.contains("show") || question.contains("get") ||
            question.contains("list") || question.contains("find")) {
            confidence += 0.05;
        }

        // Decrease if question is vague
        if (question.split("\\s+").length < 5) {
            confidence -= 0.1;
        }

        return Math.min(confidence, 1.0);
    }

    private List<String> generateWarnings(String sql) {
        List<String> warnings = new ArrayList<>();

        if (!sql.toUpperCase().contains("LIMIT")) {
            warnings.add("This query doesn't have a LIMIT clause. Results might be very large.");
        }

        if (sql.toUpperCase().contains("SELECT *")) {
            warnings.add("Using SELECT * might retrieve unnecessary columns. Consider specifying exact columns.");
        }

        if (!sql.toUpperCase().contains("WHERE") && !sql.toUpperCase().contains("LIMIT 10")) {
            warnings.add("This query retrieves all rows from the table. Consider adding WHERE clause for filtering.");
        }

        return warnings;
    }

    @Override
    public String callLLM(String prompt) {
        log.debug("Mock LLM called with prompt of length: {}", prompt.length());

        // For metadata enrichment, return mock JSON response
        if (prompt.contains("businessName") || prompt.contains("Table Information")) {
            return generateMockTableEnrichment();
        } else if (prompt.contains("columnName") || prompt.contains("Columns:")) {
            return generateMockColumnEnrichment();
        }

        return "Mock LLM response";
    }

    @Override
    public String getProviderName() {
        return "mock";
    }

    private String generateMockTableEnrichment() {
        return """
            {
              "businessName": "Customer Master",
              "businessDescription": "Master table containing customer information and profiles",
              "aliases": ["customers", "customer_data", "cust"],
              "keywords": ["customer", "client", "account", "user"],
              "category": "Customer",
              "confidence": 0.85
            }
            """;
    }

    private String generateMockColumnEnrichment() {
        return """
            [
              {
                "columnName": "cust_id",
                "businessName": "Customer ID",
                "businessDescription": "Unique identifier for the customer",
                "aliases": ["customer_id", "id"],
                "dataClassification": "Internal",
                "sampleValues": ["1001", "1002", "1003"],
                "valuePattern": "numeric"
              }
            ]
            """;
    }
}
