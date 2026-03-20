package io.oneapi.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleDTO {

    private Long id;
    private Long userId;
    private String userLogin;
    private Long roleId;
    private String roleName;
    private String assignedBy;
    private LocalDateTime assignedAt;
}
