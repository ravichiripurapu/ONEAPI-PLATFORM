package io.oneapi.admin.service.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.oneapi.admin.entity.metadata.EntityInfo;
import io.oneapi.admin.entity.metadata.FieldInfo;
import io.oneapi.admin.repository.metadata.EntityInfoRepository;
import io.oneapi.admin.repository.metadata.FieldInfoRepository;
import io.oneapi.admin.service.llm.LLMService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for enriching metadata with AI-generated business-friendly names and descriptions.
 * Uses LLM to transform technical database names (e.g., "cust_mstr") into
 * business-friendly names (e.g., "Customer Master").
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MetadataEnrichmentService {

    private final LLMService llmService;
    private final EntityInfoRepository entityRepository;
    private final FieldInfoRepository fieldRepository;
    private final ObjectMapper objectMapper;

    @Value("${oneapi.metadata.enrichment.batch-size:10}")
    private int batchSize;

    @Value("${oneapi.metadata.enrichment.enabled:true}")
    private boolean enrichmentEnabled;

    /**
     * Asynchronously enrich all tables for a datasource.
     * This method returns immediately and processes enrichment in the background.
     *
     * @param datasourceId the datasource ID
     * @return CompletableFuture that completes when enrichment is done
     */
    @Async("enrichmentExecutor")
    public CompletableFuture<EnrichmentResult> enrichDatasourceMetadataAsync(Long datasourceId) {
        if (!enrichmentEnabled) {
            log.info("Metadata enrichment is disabled, skipping datasource {}", datasourceId);
            return CompletableFuture.completedFuture(new EnrichmentResult(0, 0, 0));
        }

        if (!llmService.isAvailable()) {
            log.warn("LLM service not available, skipping enrichment for datasource {}", datasourceId);
            return CompletableFuture.completedFuture(new EnrichmentResult(0, 0, 0));
        }

        log.info("Starting async metadata enrichment for datasource: {}", datasourceId);
        long startTime = System.currentTimeMillis();

        try {
            // Get all tables for this datasource that haven't been enriched
            List<EntityInfo> tables = entityRepository.findBySourceId(datasourceId);
            List<EntityInfo> unenrichedTables = tables.stream()
                .filter(table -> !table.isEnriched())
                .toList();

            log.info("Found {} unenriched tables for datasource {}", unenrichedTables.size(), datasourceId);

            int enrichedTables = 0;
            int enrichedColumns = 0;
            int errors = 0;

            // Process in batches to avoid overwhelming LLM API
            for (int i = 0; i < unenrichedTables.size(); i += batchSize) {
                int end = Math.min(i + batchSize, unenrichedTables.size());
                List<EntityInfo> batch = unenrichedTables.subList(i, end);

                log.debug("Processing batch {}-{} of {}", i + 1, end, unenrichedTables.size());

                for (EntityInfo table : batch) {
                    try {
                        enrichTableMetadata(table.getId());
                        enrichedTables++;

                        // Enrich columns for this table
                        int columnCount = enrichColumnMetadata(table.getId());
                        enrichedColumns += columnCount;

                    } catch (Exception e) {
                        log.error("Failed to enrich table {}: {}", table.getTableName(), e.getMessage(), e);
                        errors++;
                    }
                }

                // Small delay between batches to respect rate limits
                if (end < unenrichedTables.size()) {
                    Thread.sleep(500);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Completed metadata enrichment for datasource {}: {} tables, {} columns, {} errors in {}ms",
                datasourceId, enrichedTables, enrichedColumns, errors, duration);

            return CompletableFuture.completedFuture(
                new EnrichmentResult(enrichedTables, enrichedColumns, errors)
            );

        } catch (Exception e) {
            log.error("Metadata enrichment failed for datasource {}: {}", datasourceId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Enrich metadata for a single table using LLM.
     */
    @Transactional
    public void enrichTableMetadata(Long entityId) {
        EntityInfo entity = entityRepository.findById(entityId)
            .orElseThrow(() -> new IllegalArgumentException("Entity not found: " + entityId));

        if (entity.isEnriched()) {
            log.debug("Table {} already enriched, skipping", entity.getTableName());
            return;
        }

        log.debug("Enriching table metadata: {}.{}",
            entity.getDomain().getSchemaName(), entity.getTableName());

        try {
            // Build LLM prompt for table enrichment
            String prompt = buildTableEnrichmentPrompt(entity);

            // Call LLM
            String llmResponse = llmService.callLLM(prompt);

            // Parse LLM response
            TableEnrichment enrichment = parseTableEnrichment(llmResponse);

            // Update entity with enriched metadata
            entity.setBusinessName(enrichment.getBusinessName());
            entity.setBusinessDescription(enrichment.getBusinessDescription());
            entity.setTableAliasesList(enrichment.getAliases());
            entity.setTableKeywordsList(enrichment.getKeywords());
            entity.setBusinessCategory(enrichment.getCategory());
            entity.setMetadataConfidence(enrichment.getConfidence());
            entity.setMetadataGeneratedBy(llmService.getProviderName());
            entity.setMetadataGeneratedAt(LocalDateTime.now());

            entityRepository.save(entity);

            log.debug("Enriched table {}: businessName={}, confidence={}",
                entity.getTableName(), enrichment.getBusinessName(), enrichment.getConfidence());

        } catch (Exception e) {
            log.error("Failed to enrich table {}: {}", entity.getTableName(), e.getMessage());
            throw new RuntimeException("Table enrichment failed", e);
        }
    }

    /**
     * Enrich column metadata for all columns in a table.
     *
     * @return number of columns enriched
     */
    @Transactional
    public int enrichColumnMetadata(Long entityId) {
        List<FieldInfo> columns = fieldRepository.findByDataEntityIdOrderByOrdinalPosition(entityId);

        if (columns.isEmpty()) {
            return 0;
        }

        EntityInfo entity = entityRepository.findById(entityId)
            .orElseThrow(() -> new IllegalArgumentException("Entity not found: " + entityId));

        log.debug("Enriching {} columns for table {}", columns.size(), entity.getTableName());

        try {
            // Build LLM prompt for column enrichment
            String prompt = buildColumnEnrichmentPrompt(entity, columns);

            // Call LLM
            String llmResponse = llmService.callLLM(prompt);

            // Parse LLM response
            List<ColumnEnrichment> enrichments = parseColumnEnrichments(llmResponse);

            // Match enrichments to columns and update
            int enrichedCount = 0;
            for (FieldInfo column : columns) {
                ColumnEnrichment enrichment = findEnrichmentForColumn(column.getColumnName(), enrichments);
                if (enrichment != null) {
                    column.setBusinessName(enrichment.getBusinessName());
                    column.setBusinessDescription(enrichment.getBusinessDescription());
                    column.setColumnAliasesList(enrichment.getAliases());
                    column.setDataClassification(enrichment.getDataClassification());
                    column.setSampleValuesList(enrichment.getSampleValues());
                    column.setValuePattern(enrichment.getValuePattern());

                    fieldRepository.save(column);
                    enrichedCount++;
                }
            }

            log.debug("Enriched {} columns for table {}", enrichedCount, entity.getTableName());
            return enrichedCount;

        } catch (Exception e) {
            log.error("Failed to enrich columns for table {}: {}", entity.getTableName(), e.getMessage());
            return 0;
        }
    }

    /**
     * Build LLM prompt for table enrichment.
     */
    private String buildTableEnrichmentPrompt(EntityInfo entity) {
        return String.format("""
            You are a database metadata expert. Given a database table name, generate business-friendly metadata.

            Table Information:
            - Schema: %s
            - Table Name: %s
            - Database Type: %s
            - Table Comment: %s

            Task: Generate business-friendly metadata for this table.

            Return your response in JSON format with these fields:
            {
              "businessName": "Human-readable business name (e.g., 'Customer Master' for 'cust_mstr')",
              "businessDescription": "Brief description of what this table stores (1-2 sentences)",
              "aliases": ["List of alternate names or abbreviations"],
              "keywords": ["List of business domain keywords"],
              "category": "Business category (e.g., 'Sales', 'Finance', 'Customer', 'Product', 'Transaction')",
              "confidence": 0.95
            }

            Guidelines:
            - If the table name is already clear (e.g., 'customers'), use it as-is
            - For abbreviated names (e.g., 'cust_mstr', 'acct_txn'), expand to full business terms
            - Include common abbreviations in aliases
            - Keywords should be business terms, not technical terms
            - Confidence should be 0.7-1.0 (higher if table name is clear)

            Return ONLY the JSON object, no additional text.
            """,
            entity.getDomain().getSchemaName(),
            entity.getTableName(),
            entity.getDomain().getSource().getType(),
            entity.getTableComment() != null ? entity.getTableComment() : "N/A"
        );
    }

    /**
     * Build LLM prompt for column enrichment.
     */
    private String buildColumnEnrichmentPrompt(EntityInfo entity, List<FieldInfo> columns) {
        StringBuilder columnsInfo = new StringBuilder();
        for (FieldInfo column : columns) {
            columnsInfo.append(String.format("  - %s (%s, %s, %s)\n",
                column.getColumnName(),
                column.getDataType(),
                column.getIsPrimaryKey() ? "PK" : "Regular",
                column.getNullable() ? "Nullable" : "Not Null"
            ));
        }

        return String.format("""
            You are a database metadata expert. Given a table's columns, generate business-friendly metadata for each column.

            Table Information:
            - Business Name: %s
            - Technical Name: %s
            - Schema: %s

            Columns:
%s

            Task: Generate business-friendly metadata for each column.

            Return your response as a JSON array with one object per column:
            [
              {
                "columnName": "exact_column_name_from_above",
                "businessName": "Human-readable name (e.g., 'Customer ID' for 'cust_id')",
                "businessDescription": "Brief description of what this column stores",
                "aliases": ["List of alternate names"],
                "dataClassification": "PII|Sensitive|Internal|Public",
                "sampleValues": ["example1", "example2"],
                "valuePattern": "email|phone|date|uuid|numeric|text"
              }
            ]

            Guidelines:
            - For abbreviated column names, expand to full business terms
            - Data classification: Use PII for personally identifiable info (email, phone, SSN)
            - Sample values should be realistic but fake/generic
            - Value pattern should describe the data format
            - Include all columns from the input

            Return ONLY the JSON array, no additional text.
            """,
            entity.getBusinessName() != null ? entity.getBusinessName() : entity.getTableName(),
            entity.getTableName(),
            entity.getDomain().getSchemaName(),
            columnsInfo.toString()
        );
    }

    /**
     * Parse LLM response into TableEnrichment object.
     */
    private TableEnrichment parseTableEnrichment(String llmResponse) {
        try {
            // Extract JSON from response (in case LLM adds extra text)
            String jsonResponse = extractJson(llmResponse);

            JsonNode node = objectMapper.readTree(jsonResponse);

            TableEnrichment enrichment = new TableEnrichment();
            enrichment.setBusinessName(node.get("businessName").asText());
            enrichment.setBusinessDescription(node.get("businessDescription").asText());
            enrichment.setAliases(objectMapper.convertValue(node.get("aliases"), List.class));
            enrichment.setKeywords(objectMapper.convertValue(node.get("keywords"), List.class));
            enrichment.setCategory(node.get("category").asText());
            enrichment.setConfidence(BigDecimal.valueOf(node.get("confidence").asDouble()));

            return enrichment;

        } catch (Exception e) {
            log.error("Failed to parse table enrichment response: {}", llmResponse, e);
            throw new RuntimeException("Failed to parse LLM response", e);
        }
    }

    /**
     * Parse LLM response into list of ColumnEnrichment objects.
     */
    private List<ColumnEnrichment> parseColumnEnrichments(String llmResponse) {
        try {
            String jsonResponse = extractJson(llmResponse);
            JsonNode arrayNode = objectMapper.readTree(jsonResponse);

            List<ColumnEnrichment> enrichments = new ArrayList<>();
            for (JsonNode node : arrayNode) {
                ColumnEnrichment enrichment = new ColumnEnrichment();
                enrichment.setColumnName(node.get("columnName").asText());
                enrichment.setBusinessName(node.get("businessName").asText());
                enrichment.setBusinessDescription(node.get("businessDescription").asText());
                enrichment.setAliases(objectMapper.convertValue(node.get("aliases"), List.class));
                enrichment.setDataClassification(node.get("dataClassification").asText());
                enrichment.setSampleValues(objectMapper.convertValue(node.get("sampleValues"), List.class));
                enrichment.setValuePattern(node.get("valuePattern").asText());

                enrichments.add(enrichment);
            }

            return enrichments;

        } catch (Exception e) {
            log.error("Failed to parse column enrichment response: {}", llmResponse, e);
            return new ArrayList<>();
        }
    }

    /**
     * Extract JSON from LLM response (handles cases where LLM adds extra text).
     */
    private String extractJson(String response) {
        // Remove markdown code blocks if present
        response = response.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "");

        // Find first { or [
        int start = Math.min(
            response.indexOf("{") != -1 ? response.indexOf("{") : Integer.MAX_VALUE,
            response.indexOf("[") != -1 ? response.indexOf("[") : Integer.MAX_VALUE
        );

        // Find last } or ]
        int end = Math.max(
            response.lastIndexOf("}"),
            response.lastIndexOf("]")
        );

        if (start < end) {
            return response.substring(start, end + 1);
        }

        return response.trim();
    }

    /**
     * Find column enrichment by column name.
     */
    private ColumnEnrichment findEnrichmentForColumn(String columnName, List<ColumnEnrichment> enrichments) {
        return enrichments.stream()
            .filter(e -> e.getColumnName().equalsIgnoreCase(columnName))
            .findFirst()
            .orElse(null);
    }

    // ========== DTOs ==========

    @Data
    public static class TableEnrichment {
        private String businessName;
        private String businessDescription;
        private List<String> aliases;
        private List<String> keywords;
        private String category;
        private BigDecimal confidence;
    }

    @Data
    public static class ColumnEnrichment {
        private String columnName;
        private String businessName;
        private String businessDescription;
        private List<String> aliases;
        private String dataClassification;
        private List<String> sampleValues;
        private String valuePattern;
    }

    @Data
    public static class EnrichmentResult {
        private final int tablesEnriched;
        private final int columnsEnriched;
        private final int errors;
    }
}
