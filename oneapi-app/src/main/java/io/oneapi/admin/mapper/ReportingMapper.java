package io.oneapi.admin.mapper;

import io.oneapi.admin.dto.reporting.*;
import io.oneapi.admin.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper utility for converting between reporting entities and DTOs.
 */
@Component
public class ReportingMapper {

    // ========== SavedQuery Mappings ==========

    public SavedQueryDTO toDTO(SavedQuery entity) {
        if (entity == null) return null;

        SavedQueryDTO dto = new SavedQueryDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setQueryText(entity.getQueryText());
        dto.setDatasourceId(entity.getSource() != null ? entity.getSource().getId() : null);
        dto.setConnectionName(entity.getSource() != null ? entity.getSource().getName() : null);
        dto.setIsPublic(entity.getIsPublic());
        dto.setIsFavorite(entity.getIsFavorite());
        dto.setExecutionCount(entity.getExecutionCount());
        dto.setLastExecutedAt(entity.getLastExecutedAt());
        dto.setAvgExecutionTimeMs(entity.getAvgExecutionTimeMs());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setLastModifiedDate(entity.getLastModifiedDate());
        return dto;
    }

    public SavedQuery toEntity(SavedQueryDTO dto, SourceInfo connection) {
        if (dto == null) return null;

        SavedQuery entity = new SavedQuery();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setQueryText(dto.getQueryText());
        entity.setSource(connection);
        entity.setIsPublic(dto.getIsPublic());
        entity.setIsFavorite(dto.getIsFavorite());
        return entity;
    }

    public void updateEntityFromDTO(SavedQueryDTO dto, SavedQuery entity, SourceInfo connection) {
        if (dto == null || entity == null) return;
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setQueryText(dto.getQueryText());
        entity.setSource(connection);
        entity.setIsPublic(dto.getIsPublic());
        entity.setIsFavorite(dto.getIsFavorite());
    }

    // ========== Report Mappings ==========

    public ReportDTO toDTO(Report entity) {
        if (entity == null) return null;

        ReportDTO dto = new ReportDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setQueryId(entity.getQuery() != null ? entity.getQuery().getId() : null);
        dto.setQueryName(entity.getQuery() != null ? entity.getQuery().getName() : null);
        dto.setSourceId(entity.getSource() != null ? entity.getSource().getId() : null);
        dto.setSourceName(entity.getSource() != null ? entity.getSource().getName() : null);
        dto.setOutputFormat(entity.getOutputFormat());
        dto.setParameters(entity.getParameters());
        dto.setDefaultParameters(entity.getDefaultParameters());
        dto.setIsPublic(entity.getIsPublic());
        dto.setExecutionCount(entity.getExecutionCount());
        dto.setLastExecutedAt(entity.getLastExecutedAt());
        dto.setAvgExecutionTimeMs(entity.getAvgExecutionTimeMs());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setLastModifiedDate(entity.getLastModifiedDate());
        return dto;
    }

    public Report toEntity(ReportDTO dto, SavedQuery query, SourceInfo source) {
        if (dto == null) return null;

        Report entity = new Report();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setQuery(query);
        entity.setSource(source);
        entity.setOutputFormat(dto.getOutputFormat());
        entity.setParameters(dto.getParameters());
        entity.setDefaultParameters(dto.getDefaultParameters());
        entity.setIsPublic(dto.getIsPublic());
        return entity;
    }

    public void updateEntityFromDTO(ReportDTO dto, Report entity, SavedQuery query, SourceInfo source) {
        if (dto == null || entity == null) return;
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setQuery(query);
        entity.setSource(source);
        entity.setOutputFormat(dto.getOutputFormat());
        entity.setParameters(dto.getParameters());
        entity.setDefaultParameters(dto.getDefaultParameters());
        entity.setIsPublic(dto.getIsPublic());
    }

    // ========== Dashboard Mappings ==========

    public DashboardDTO toDTO(Dashboard entity) {
        if (entity == null) return null;

        DashboardDTO dto = new DashboardDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setSourceId(entity.getSource() != null ? entity.getSource().getId() : null);
        dto.setSourceName(entity.getSource() != null ? entity.getSource().getName() : null);
        dto.setIsPublic(entity.getIsPublic());
        dto.setRefreshIntervalSeconds(entity.getRefreshIntervalSeconds());
        dto.setLayout(entity.getLayout());
        dto.setViewCount(entity.getViewCount());
        dto.setLastViewedAt(entity.getLastViewedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setLastModifiedDate(entity.getLastModifiedDate());

        // Map widgets
        if (entity.getWidgets() != null) {
            dto.setWidgets(entity.getWidgets().stream()
                .map(this::toDTO)
                .collect(Collectors.toList()));
        }

        return dto;
    }

    public Dashboard toEntity(DashboardDTO dto, SourceInfo source) {
        if (dto == null) return null;

        Dashboard entity = new Dashboard();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setSource(source);
        entity.setIsPublic(dto.getIsPublic());
        entity.setRefreshIntervalSeconds(dto.getRefreshIntervalSeconds());
        entity.setLayout(dto.getLayout());
        return entity;
    }

    public void updateEntityFromDTO(DashboardDTO dto, Dashboard entity, SourceInfo source) {
        if (dto == null || entity == null) return;
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setSource(source);
        entity.setIsPublic(dto.getIsPublic());
        entity.setRefreshIntervalSeconds(dto.getRefreshIntervalSeconds());
        entity.setLayout(dto.getLayout());
    }

    // ========== Widget Mappings ==========

    public WidgetDTO toDTO(Widget entity) {
        if (entity == null) return null;

        WidgetDTO dto = new WidgetDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setDashboardId(entity.getDashboard() != null ? entity.getDashboard().getId() : null);
        dto.setQueryId(entity.getQuery() != null ? entity.getQuery().getId() : null);
        dto.setQueryName(entity.getQuery() != null ? entity.getQuery().getName() : null);
        dto.setReportId(entity.getReport() != null ? entity.getReport().getId() : null);
        dto.setReportName(entity.getReport() != null ? entity.getReport().getName() : null);
        dto.setWidgetType(entity.getWidgetType());
        dto.setPositionX(entity.getPositionX());
        dto.setPositionY(entity.getPositionY());
        dto.setWidth(entity.getWidth());
        dto.setHeight(entity.getHeight());
        dto.setConfiguration(entity.getConfiguration());
        dto.setRefreshIntervalSeconds(entity.getRefreshIntervalSeconds());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setLastModifiedDate(entity.getLastModifiedDate());
        return dto;
    }

    public Widget toEntity(WidgetDTO dto, Dashboard dashboard, SavedQuery query, Report report) {
        if (dto == null) return null;

        Widget entity = new Widget();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setDashboard(dashboard);
        entity.setQuery(query);
        entity.setReport(report);
        entity.setWidgetType(dto.getWidgetType());
        entity.setPositionX(dto.getPositionX());
        entity.setPositionY(dto.getPositionY());
        entity.setWidth(dto.getWidth());
        entity.setHeight(dto.getHeight());
        entity.setConfiguration(dto.getConfiguration());
        entity.setRefreshIntervalSeconds(dto.getRefreshIntervalSeconds());
        return entity;
    }

    public void updateEntityFromDTO(WidgetDTO dto, Widget entity, Dashboard dashboard, SavedQuery query, Report report) {
        if (dto == null || entity == null) return;
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setDashboard(dashboard);
        entity.setQuery(query);
        entity.setReport(report);
        entity.setWidgetType(dto.getWidgetType());
        entity.setPositionX(dto.getPositionX());
        entity.setPositionY(dto.getPositionY());
        entity.setWidth(dto.getWidth());
        entity.setHeight(dto.getHeight());
        entity.setConfiguration(dto.getConfiguration());
        entity.setRefreshIntervalSeconds(dto.getRefreshIntervalSeconds());
    }

    // ========== Schedule Mappings ==========

    public ScheduleDTO toDTO(Schedule entity) {
        if (entity == null) return null;

        ScheduleDTO dto = new ScheduleDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setReportId(entity.getReport() != null ? entity.getReport().getId() : null);
        dto.setReportName(entity.getReport() != null ? entity.getReport().getName() : null);
        dto.setQueryId(entity.getQuery() != null ? entity.getQuery().getId() : null);
        dto.setQueryName(entity.getQuery() != null ? entity.getQuery().getName() : null);
        dto.setCronExpression(entity.getCronExpression());
        dto.setEnabled(entity.getEnabled());
        dto.setOutputFormat(entity.getOutputFormat());
        dto.setEmailRecipients(entity.getEmailRecipients());
        dto.setWebhookUrl(entity.getWebhookUrl());
        dto.setLastRunAt(entity.getLastRunAt());
        dto.setLastRunStatus(entity.getLastRunStatus());
        dto.setLastRunMessage(entity.getLastRunMessage());
        dto.setNextRunAt(entity.getNextRunAt());
        dto.setExecutionCount(entity.getExecutionCount());
        dto.setSuccessCount(entity.getSuccessCount());
        dto.setFailureCount(entity.getFailureCount());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setLastModifiedDate(entity.getLastModifiedDate());
        return dto;
    }

    public Schedule toEntity(ScheduleDTO dto, Report report, SavedQuery query) {
        if (dto == null) return null;

        Schedule entity = new Schedule();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setReport(report);
        entity.setQuery(query);
        entity.setCronExpression(dto.getCronExpression());
        entity.setEnabled(dto.getEnabled());
        entity.setOutputFormat(dto.getOutputFormat());
        entity.setEmailRecipients(dto.getEmailRecipients());
        entity.setWebhookUrl(dto.getWebhookUrl());
        return entity;
    }

    public void updateEntityFromDTO(ScheduleDTO dto, Schedule entity, Report report, SavedQuery query) {
        if (dto == null || entity == null) return;
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setReport(report);
        entity.setQuery(query);
        entity.setCronExpression(dto.getCronExpression());
        entity.setEnabled(dto.getEnabled());
        entity.setOutputFormat(dto.getOutputFormat());
        entity.setEmailRecipients(dto.getEmailRecipients());
        entity.setWebhookUrl(dto.getWebhookUrl());
    }

    // ========== AuditLog Mappings ==========

    public AuditLogDTO toDTO(AuditLog entity) {
        if (entity == null) return null;

        return new AuditLogDTO(
            entity.getId(),
            entity.getUserLogin(),
            entity.getAction(),
            entity.getEntityType(),
            entity.getEntityId(),
            entity.getEntityName(),
            entity.getDetails(),
            entity.getIpAddress(),
            entity.getUserAgent(),
            entity.getTimestamp(),
            entity.getExecutionTimeMs(),
            entity.getStatus(),
            entity.getErrorMessage()
        );
    }

    // AuditLog is read-only, no toEntity method needed

    // ========== List Conversions ==========

    public List<SavedQueryDTO> queriesToDTOs(List<SavedQuery> entities) {
        return entities.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<ReportDTO> reportsToDTOs(List<Report> entities) {
        return entities.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<DashboardDTO> dashboardsToDTOs(List<Dashboard> entities) {
        return entities.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<WidgetDTO> widgetsToDTOs(List<Widget> entities) {
        return entities.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<ScheduleDTO> schedulesToDTOs(List<Schedule> entities) {
        return entities.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<AuditLogDTO> auditLogsToDTOs(List<AuditLog> entities) {
        return entities.stream().map(this::toDTO).collect(Collectors.toList());
    }
}
