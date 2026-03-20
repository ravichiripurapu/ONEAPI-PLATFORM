package io.oneapi.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDTO {

    private Long id;
    private Long roleId;
    private String roleName;
    private Long sourceId;
    private String sourceName;
    private Long domainId;
    private String domainName;
    private Long entityId;
    private String entityName;
    private Long fieldId;
    private String fieldName;
    private String permissionLevel;  // SOURCE, DOMAIN, ENTITY, FIELD
    private LocalDateTime createdAt;
}
