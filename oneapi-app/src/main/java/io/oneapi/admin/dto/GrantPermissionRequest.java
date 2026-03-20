package io.oneapi.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for granting permissions.
 * Supports hierarchical permission levels:
 * - Source level: Only sourceId (access to entire source)
 * - Domain level: sourceId + domainId (access to all entities in domain)
 * - Entity level: sourceId + domainId + entityId (access to all fields in entity)
 * - Field level: All IDs (access to specific field only)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrantPermissionRequest {

    @NotNull(message = "Role ID is required")
    private Long roleId;

    // Hierarchical permission levels (nullable for wildcards)
    private Long sourceId;      // NULL = all sources
    private Long domainId;      // NULL = all domains in source
    private Long entityId;      // NULL = all entities in domain
    private Long fieldId;       // NULL = all fields in entity
}
