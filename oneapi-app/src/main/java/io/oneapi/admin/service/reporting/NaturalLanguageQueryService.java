package io.oneapi.admin.service.reporting;

import io.oneapi.admin.dto.reporting.NaturalLanguageQueryRequest;
import io.oneapi.admin.dto.reporting.NaturalLanguageQueryResponse;
import io.oneapi.admin.service.llm.DatabaseSchemaContext;
import io.oneapi.admin.service.llm.DatabaseSchemaContextBuilder;
import io.oneapi.admin.service.llm.LLMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for handling natural language to SQL conversion.
 * Orchestrates the LLM service and schema context builder.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NaturalLanguageQueryService {

    private final LLMService llmService;
    private final DatabaseSchemaContextBuilder schemaContextBuilder;

    /**
     * Converts a natural language question to SQL.
     *
     * @param request the natural language query request
     * @return the generated SQL with explanation and metadata
     */
    public NaturalLanguageQueryResponse convertQuestionToSQL(NaturalLanguageQueryRequest request) {
        log.info("Converting natural language to SQL for datasource {}: {}",
            request.getDatasourceId(), request.getQuestion());

        // Check if LLM service is available
        if (!llmService.isAvailable()) {
            log.warn("LLM service is not available, returning error response");
            return NaturalLanguageQueryResponse.builder()
                .generatedSQL("-- LLM service is not available")
                .explanation("The natural language query service is currently unavailable. " +
                           "Please contact your administrator or use the SQL editor directly.")
                .confidence(0.0)
                .warnings(List.of("LLM service not configured or unavailable"))
                .build();
        }

        // Build database schema context
        DatabaseSchemaContext schemaContext;
        if (request.getContext() != null && request.getContext().getTables() != null) {
            // Use focused context if specific tables are requested
            schemaContext = schemaContextBuilder.buildContextForTables(
                request.getDatasourceId(),
                request.getContext().getTables()
            );
        } else {
            // Use full context
            schemaContext = schemaContextBuilder.buildLightweightContext(
                request.getDatasourceId()
            );
        }

        log.debug("Schema context built with {} tables", schemaContext.getTables().size());

        // Convert to SQL using LLM
        NaturalLanguageQueryResponse response = llmService.convertToSQL(request, schemaContext);

        log.info("SQL generated with confidence: {}", response.getConfidence());
        log.debug("Generated SQL: {}", response.getGeneratedSQL());

        return response;
    }

    /**
     * Explains a given SQL query in natural language.
     *
     * @param sql the SQL query to explain
     * @param datasourceId the datasource ID for context
     * @return natural language explanation
     */
    public String explainSQL(String sql, Long datasourceId) {
        log.info("Explaining SQL for datasource {}", datasourceId);

        if (!llmService.isAvailable()) {
            return "SQL explanation service is currently unavailable.";
        }

        DatabaseSchemaContext schemaContext = schemaContextBuilder.buildLightweightContext(datasourceId);
        return llmService.explainSQL(sql, schemaContext);
    }

    /**
     * Checks if the natural language query service is available.
     *
     * @return true if the service is ready to use
     */
    public boolean isServiceAvailable() {
        return llmService.isAvailable();
    }
}
