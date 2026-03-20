package io.oneapi.admin.service;

import io.oneapi.admin.entity.Permission;
import io.oneapi.admin.entity.Role;
import io.oneapi.admin.entity.SourceInfo;
import io.oneapi.admin.entity.metadata.DomainInfo;
import io.oneapi.admin.entity.metadata.EntityInfo;
import io.oneapi.admin.entity.metadata.FieldInfo;
import io.oneapi.admin.repository.PermissionRepository;
import io.oneapi.admin.repository.RoleRepository;
import io.oneapi.admin.repository.SourceInfoRepository;
import io.oneapi.admin.repository.metadata.DomainInfoRepository;
import io.oneapi.admin.repository.metadata.EntityInfoRepository;
import io.oneapi.admin.repository.metadata.FieldInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing permissions (grant/revoke)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionManagementService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final SourceInfoRepository sourceRepository;
    private final DomainInfoRepository domainRepository;
    private final EntityInfoRepository entityRepository;
    private final FieldInfoRepository fieldRepository;

    /**
     * Grant permission to a role at any hierarchical level
     */
    @Transactional
    @CacheEvict(value = {"sourceAccess", "domainAccess", "entityAccess", "fieldAccess"}, allEntries = true)
    public Permission grantPermission(Long roleId, Long sourceId, Long domainId, Long entityId, Long fieldId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        Permission permission = new Permission();
        permission.setRole(role);

        // Set entities based on hierarchical level
        if (sourceId != null) {
            SourceInfo source = sourceRepository.findById(sourceId)
                    .orElseThrow(() -> new IllegalArgumentException("Source not found: " + sourceId));
            permission.setSource(source);
        }

        if (domainId != null) {
            DomainInfo domain = domainRepository.findById(domainId)
                    .orElseThrow(() -> new IllegalArgumentException("Domain not found: " + domainId));
            permission.setDomain(domain);
        }

        if (entityId != null) {
            EntityInfo entity = entityRepository.findById(entityId)
                    .orElseThrow(() -> new IllegalArgumentException("Entity not found: " + entityId));
            permission.setEntity(entity);
        }

        if (fieldId != null) {
            FieldInfo field = fieldRepository.findById(fieldId)
                    .orElseThrow(() -> new IllegalArgumentException("Field not found: " + fieldId));
            permission.setField(field);
        }

        permission = permissionRepository.save(permission);
        log.info("Granted permission: roleId={}, sourceId={}, domainId={}, entityId={}, fieldId={}",
                roleId, sourceId, domainId, entityId, fieldId);

        return permission;
    }

    /**
     * Revoke a specific permission
     */
    @Transactional
    @CacheEvict(value = {"sourceAccess", "domainAccess", "entityAccess", "fieldAccess"}, allEntries = true)
    public void revokePermission(Long permissionId) {
        permissionRepository.deleteById(permissionId);
        log.info("Revoked permission: {}", permissionId);
    }

    /**
     * Revoke all permissions for a role at a specific source
     */
    @Transactional
    @CacheEvict(value = {"sourceAccess", "domainAccess", "entityAccess", "fieldAccess"}, allEntries = true)
    public void revokeAllPermissionsForRoleAndSource(Long roleId, Long sourceId) {
        permissionRepository.deleteByRoleIdAndSourceId(roleId, sourceId);
        log.info("Revoked all permissions for roleId={}, sourceId={}", roleId, sourceId);
    }
}
