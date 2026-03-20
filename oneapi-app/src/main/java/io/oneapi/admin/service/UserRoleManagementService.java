package io.oneapi.admin.service;

import io.oneapi.admin.dto.AssignRoleRequest;
import io.oneapi.admin.dto.UserRoleDTO;
import io.oneapi.admin.entity.Role;
import io.oneapi.admin.entity.UserRole;
import io.oneapi.admin.repository.RoleRepository;
import io.oneapi.admin.repository.UserRoleRepository;
import io.oneapi.admin.security.domain.User;
import io.oneapi.admin.security.repository.UserRepository;
import io.oneapi.admin.security.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRoleManagementService {

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public UserRoleDTO assignRole(AssignRoleRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getUserId()));

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.getRoleId()));

        // Check if already assigned
        if (userRoleRepository.existsByUserIdAndRoleId(request.getUserId(), request.getRoleId())) {
            throw new IllegalArgumentException("Role already assigned to user");
        }

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setAssignedBy(SecurityUtils.getCurrentUserLogin().orElse("system"));
        userRole.setAssignedAt(LocalDateTime.now());

        userRole = userRoleRepository.save(userRole);
        log.info("Assigned role {} to user {}", role.getName(), user.getLogin());

        return toDTO(userRole);
    }

    @Transactional(readOnly = true)
    public List<UserRoleDTO> getAllUserRoles() {
        return userRoleRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserRoleDTO> getUserRolesByUserId(Long userId) {
        return userRoleRepository.findByUserId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeUserRole(Long id) {
        if (!userRoleRepository.existsById(id)) {
            throw new IllegalArgumentException("UserRole not found: " + id);
        }
        userRoleRepository.deleteById(id);
        log.info("Revoked user-role assignment: {}", id);
    }

    private UserRoleDTO toDTO(UserRole userRole) {
        UserRoleDTO dto = new UserRoleDTO();
        dto.setId(userRole.getId());
        dto.setUserId(userRole.getUser().getId());
        dto.setUserLogin(userRole.getUser().getLogin());
        dto.setRoleId(userRole.getRole().getId());
        dto.setRoleName(userRole.getRole().getName());
        dto.setAssignedBy(userRole.getAssignedBy());
        dto.setAssignedAt(userRole.getAssignedAt());
        return dto;
    }
}
