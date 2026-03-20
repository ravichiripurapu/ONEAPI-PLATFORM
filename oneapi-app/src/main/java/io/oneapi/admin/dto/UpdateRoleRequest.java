package io.oneapi.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleRequest {

    @NotBlank(message = "Role name is required")
    private String name;

    private String description;
}
