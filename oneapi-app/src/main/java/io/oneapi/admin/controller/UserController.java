package io.oneapi.admin.controller;

import io.oneapi.admin.dto.CreateUserRequest;
import io.oneapi.admin.dto.UpdateUserRequest;
import io.oneapi.admin.dto.UserDTO;
import io.oneapi.admin.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for user management.
 * Requires ADMIN role for all operations.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Manage users")
public class UserController {

    private final UserManagementService service;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Create a new user")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createUser(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get all users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(service.getAllUsers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getUserById(id));
    }

    @GetMapping("/login/{login}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get user by login")
    public ResponseEntity<UserDTO> getUserByLogin(@PathVariable String login) {
        return ResponseEntity.ok(service.getUserByLogin(login));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Update a user")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(service.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Delete a user")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        service.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
