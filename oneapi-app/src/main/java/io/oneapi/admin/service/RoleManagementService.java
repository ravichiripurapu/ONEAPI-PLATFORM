package io.oneapi.admin.service;

import io.oneapi.admin.dto.CreateRoleRequest;
import io.oneapi.admin.dto.RoleDTO;
import io.oneapi.admin.dto.UpdateRoleRequest;
import io.oneapi.admin.entity.Role;
import io.oneapi.admin.repository.RoleRepository;
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
public class RoleManagementService {

    private final RoleRepository roleRepository;

    @Transactional
    public RoleDTO createRole(CreateRoleRequest request) {
        // Check if role name already exists
        if (roleRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("Role already exists: " + request.getName());
        }

        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setCreatedBy(SecurityUtils.getCurrentUserLogin().orElse("system"));
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedBy(SecurityUtils.getCurrentUserLogin().orElse("system"));
        role.setUpdatedAt(LocalDateTime.now());

        role = roleRepository.save(role);
        log.info("Created role: {}", role.getName());

        return toDTO(role);
    }

    @Transactional(readOnly = true)
    public List<RoleDTO> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleDTO getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));
        return toDTO(role);
    }

    @Transactional
    public RoleDTO updateRole(Long id, UpdateRoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));

        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setUpdatedBy(SecurityUtils.getCurrentUserLogin().orElse("system"));
        role.setUpdatedAt(LocalDateTime.now());

        role = roleRepository.save(role);
        log.info("Updated role: {}", role.getName());

        return toDTO(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new IllegalArgumentException("Role not found: " + id);
        }
        roleRepository.deleteById(id);
        log.info("Deleted role: {}", id);
    }

    private RoleDTO toDTO(Role role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setCreatedBy(role.getCreatedBy());
        dto.setCreatedAt(role.getCreatedAt());
        dto.setUpdatedBy(role.getUpdatedBy());
        dto.setUpdatedAt(role.getUpdatedAt());
        return dto;
    }
}
