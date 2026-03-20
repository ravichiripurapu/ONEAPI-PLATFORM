package io.oneapi.admin.controller;

import io.oneapi.admin.dto.AssignRoleRequest;
import io.oneapi.admin.dto.UserRoleDTO;
import io.oneapi.admin.service.UserRoleManagementService;
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
@RequestMapping("/api/user-roles")
@RequiredArgsConstructor
@Tag(name = "User-Role Management", description = "Assign and revoke roles from users")
public class UserRoleController {

    private final UserRoleManagementService service;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Assign role to user")
    public ResponseEntity<UserRoleDTO> assignRole(@Valid @RequestBody AssignRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.assignRole(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "List all user-role assignments")
    public ResponseEntity<List<UserRoleDTO>> getAllUserRoles() {
        return ResponseEntity.ok(service.getAllUserRoles());
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get roles for specific user")
    public ResponseEntity<List<UserRoleDTO>> getUserRolesByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getUserRolesByUserId(userId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Revoke role from user")
    public ResponseEntity<Void> revokeUserRole(@PathVariable Long id) {
        service.revokeUserRole(id);
        return ResponseEntity.noContent().build();
    }
}
