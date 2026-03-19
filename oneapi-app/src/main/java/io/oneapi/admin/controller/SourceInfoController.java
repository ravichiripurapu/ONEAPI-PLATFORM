package io.oneapi.admin.controller;

import io.oneapi.admin.dto.ConnectionTestResult;
import io.oneapi.admin.dto.SourceInfoDTO;
import io.oneapi.admin.service.SourceInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for database connection management.
 */
@RestController
@RequestMapping("/api/sources")
@RequiredArgsConstructor
@Tag(name = "Sources", description = "Manage data sources")
public class SourceInfoController {

    private final SourceInfoService service;

    @PostMapping
    @Operation(summary = "Create a new database connection")
    public ResponseEntity<SourceInfoDTO> createConnection(
            @Valid @RequestBody SourceInfoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createConnection(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing database connection")
    public ResponseEntity<SourceInfoDTO> updateConnection(
            @PathVariable Long id,
            @Valid @RequestBody SourceInfoDTO dto) {
        return ResponseEntity.ok(service.updateConnection(id, dto));
    }

    @GetMapping
    @Operation(summary = "Get all database connections")
    public ResponseEntity<List<SourceInfoDTO>> getAllConnections() {
        return ResponseEntity.ok(service.getAllConnections());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get database connection by ID")
    public ResponseEntity<SourceInfoDTO> getConnectionById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getConnectionById(id));
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Get database connection by name")
    public ResponseEntity<SourceInfoDTO> getConnectionByName(@PathVariable String name) {
        return ResponseEntity.ok(service.getConnectionByName(name));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a database connection")
    public ResponseEntity<Void> deleteConnection(@PathVariable Long id) {
        service.deleteConnection(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "Test database connection")
    public ResponseEntity<ConnectionTestResult> testConnection(@PathVariable Long id) {
        return ResponseEntity.ok(service.testConnection(id));
    }

    @PostMapping("/test")
    @Operation(summary = "Test database connection without saving")
    public ResponseEntity<ConnectionTestResult> testConnection(
            @Valid @RequestBody SourceInfoDTO dto) {
        return ResponseEntity.ok(service.testConnection(dto));
    }
}
