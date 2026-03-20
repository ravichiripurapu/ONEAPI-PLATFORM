package io.oneapi.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new role
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoleRequest {

    @NotBlank(message = "Role name is required")
    private String name;

    private String description;
}
