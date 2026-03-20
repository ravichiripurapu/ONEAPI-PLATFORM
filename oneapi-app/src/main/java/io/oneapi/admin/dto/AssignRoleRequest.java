package io.oneapi.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignRoleRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Role ID is required")
    private Long roleId;
}
