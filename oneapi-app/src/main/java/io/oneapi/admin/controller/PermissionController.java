package io.oneapi.admin.controller;

import io.oneapi.admin.dto.GrantPermissionRequest;
import io.oneapi.admin.dto.PermissionDTO;
import io.oneapi.admin.entity.Permission;
import io.oneapi.admin.repository.PermissionRepository;
import io.oneapi.admin.service.PermissionManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Tag(name = "Permission Management", description = "Grant and revoke permissions")
public class PermissionController {

    private final PermissionManagementService service;
    private final PermissionRepository repository;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Grant permission to role")
    public ResponseEntity<PermissionDTO> grantPermission(@Valid @RequestBody GrantPermissionRequest request) {
        Permission permission = service.grantPermission(
            request.getRoleId(),
            request.getSourceId(),
            request.getDomainId(),
            request.getEntityId(),
            request.getFieldId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(permission));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "List all permissions")
    public ResponseEntity<List<PermissionDTO>> getAllPermissions() {
        return ResponseEntity.ok(
            repository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList())
        );
    }

    @GetMapping("/role/{roleId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get permissions for role")
    public ResponseEntity<List<PermissionDTO>> getPermissionsByRole(@PathVariable Long roleId) {
        return ResponseEntity.ok(
            repository.findByRoleId(roleId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList())
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Revoke permission")
    public ResponseEntity<Void> revokePermission(@PathVariable Long id) {
        service.revokePermission(id);
        return ResponseEntity.noContent().build();
    }

    private PermissionDTO toDTO(Permission p) {
        PermissionDTO dto = new PermissionDTO();
        dto.setId(p.getId());
        dto.setRoleId(p.getRole().getId());
        dto.setRoleName(p.getRole().getName());
        dto.setSourceId(p.getSource() != null ? p.getSource().getId() : null);
        dto.setSourceName(p.getSource() != null ? p.getSource().getName() : null);
        dto.setDomainId(p.getDomain() != null ? p.getDomain().getId() : null);
        dto.setDomainName(p.getDomain() != null ? p.getDomain().getSchemaName() : null);
        dto.setEntityId(p.getEntity() != null ? p.getEntity().getId() : null);
        dto.setEntityName(p.getEntity() != null ? p.getEntity().getTableName() : null);
        dto.setFieldId(p.getField() != null ? p.getField().getId() : null);
        dto.setFieldName(p.getField() != null ? p.getField().getColumnName() : null);
        
        // Determine permission level
        if (p.getField() != null) dto.setPermissionLevel("FIELD");
        else if (p.getEntity() != null) dto.setPermissionLevel("ENTITY");
        else if (p.getDomain() != null) dto.setPermissionLevel("DOMAIN");
        else if (p.getSource() != null) dto.setPermissionLevel("SOURCE");
        else dto.setPermissionLevel("ALL");
        
        return dto;
    }
}
