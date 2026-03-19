package io.oneapi.admin.mapper;

import io.oneapi.admin.dto.metadata.FieldInfoDTO;
import io.oneapi.admin.dto.metadata.DomainInfoDTO;
import io.oneapi.admin.dto.metadata.EntityInfoDTO;
import io.oneapi.admin.entity.metadata.FieldInfo;
import io.oneapi.admin.entity.metadata.DomainInfo;
import io.oneapi.admin.entity.metadata.EntityInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between metadata entities and DTOs.
 */
@Component
public class MetadataMapper {

    // ========== DomainInfo ==========

    public DomainInfoDTO toDTO(DomainInfo entity) {
        if (entity == null) {
            return null;
        }

        DomainInfoDTO dto = new DomainInfoDTO();
        dto.setId(entity.getId());
        dto.setDatasourceId(entity.getSource() != null ? entity.getSource().getId() : null);
        dto.setConnectionName(entity.getSource() != null ? entity.getSource().getName() : null);
        dto.setCatalogName(entity.getCatalogName());
        dto.setSchemaName(entity.getSchemaName());
        dto.setTableCount(entity.getTableCount());
        dto.setViewCount(entity.getViewCount());
        dto.setDiscoveredAt(entity.getDiscoveredAt());
        dto.setLastSyncedAt(entity.getLastSyncedAt());
        dto.setCreatedDate(entity.getCreatedDate());

        return dto;
    }

    public List<DomainInfoDTO> toDTOList(List<DomainInfo> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ========== EntityInfo ==========

    public EntityInfoDTO toDTO(EntityInfo entity) {
        if (entity == null) {
            return null;
        }

        EntityInfoDTO dto = new EntityInfoDTO();
        dto.setId(entity.getId());
        dto.setSchemaId(entity.getDomain() != null ? entity.getDomain().getId() : null);
        dto.setSchemaName(entity.getDomain() != null ? entity.getDomain().getSchemaName() : null);
        dto.setCatalogName(entity.getDomain() != null ? entity.getDomain().getCatalogName() : null);
        dto.setTableName(entity.getTableName());
        dto.setTableType(entity.getTableType() != null ? entity.getTableType().name() : null);
        dto.setTableComment(entity.getTableComment());
        dto.setEstimatedRowCount(entity.getEstimatedRowCount());
        dto.setSizeInBytes(entity.getSizeInBytes());
        dto.setDiscoveredAt(entity.getDiscoveredAt());
        dto.setLastSyncedAt(entity.getLastSyncedAt());
        dto.setCreatedDate(entity.getCreatedDate());

        return dto;
    }

    public List<EntityInfoDTO> toTableDTOList(List<EntityInfo> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ========== FieldInfo ==========

    public FieldInfoDTO toDTO(FieldInfo entity) {
        if (entity == null) {
            return null;
        }

        FieldInfoDTO dto = new FieldInfoDTO();
        dto.setId(entity.getId());
        dto.setTableId(entity.getDataEntity() != null ? entity.getDataEntity().getId() : null);
        dto.setTableName(entity.getDataEntity() != null ? entity.getDataEntity().getTableName() : null);
        dto.setSchemaName(entity.getDataEntity() != null && entity.getDataEntity().getDomain() != null
                ? entity.getDataEntity().getDomain().getSchemaName() : null);
        dto.setColumnName(entity.getColumnName());
        dto.setDataType(entity.getDataType());
        dto.setJdbcType(entity.getJdbcType());
        dto.setColumnSize(entity.getColumnSize());
        dto.setDecimalDigits(entity.getDecimalDigits());
        dto.setNullable(entity.getNullable());
        dto.setDefaultValue(entity.getDefaultValue());
        dto.setColumnComment(entity.getColumnComment());
        dto.setIsPrimaryKey(entity.getIsPrimaryKey());
        dto.setIsForeignKey(entity.getIsForeignKey());
        dto.setIsUnique(entity.getIsUnique());
        dto.setIsIndexed(entity.getIsIndexed());
        dto.setIsAutoIncrement(entity.getIsAutoIncrement());
        dto.setOrdinalPosition(entity.getOrdinalPosition());
        dto.setDiscoveredAt(entity.getDiscoveredAt());
        dto.setCreatedDate(entity.getCreatedDate());

        return dto;
    }

    public List<FieldInfoDTO> toColumnDTOList(List<FieldInfo> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
