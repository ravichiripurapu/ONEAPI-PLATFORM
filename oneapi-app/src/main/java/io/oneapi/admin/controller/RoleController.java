package io.oneapi.admin.controller;

import io.oneapi.admin.dto.CreateRoleRequest;
import io.oneapi.admin.dto.RoleDTO;
import io.oneapi.admin.dto.UpdateRoleRequest;
import io.oneapi.admin.service.RoleManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "Manage roles")
public class RoleController {

    private final RoleManagementService service;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Create new role")
    public ResponseEntity<RoleDTO> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createRole(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "List all roles")
    public ResponseEntity<List<RoleDTO>> getAllRoles() {
        return ResponseEntity.ok(service.getAllRoles());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get role by ID")
    public ResponseEntity<RoleDTO> getRoleById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getRoleById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Update role")
    public ResponseEntity<RoleDTO> updateRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(service.updateRole(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Delete role")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        service.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}
