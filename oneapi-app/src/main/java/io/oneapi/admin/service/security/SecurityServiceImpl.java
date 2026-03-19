package io.oneapi.admin.service.security;

import io.oneapi.admin.security.domain.User;
import io.oneapi.admin.entity.metadata.DomainInfo;
import io.oneapi.admin.entity.metadata.EntityInfo;
import io.oneapi.admin.entity.metadata.FieldInfo;
import io.oneapi.admin.entity.SourceInfo;
import io.oneapi.admin.repository.PermissionRepository;
import io.oneapi.admin.repository.UserRoleRepository;
import io.oneapi.admin.security.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of SecurityService with caching for performance
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SecurityServiceImpl implements SecurityService {

    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    public boolean hasSourceAccess(Long sourceId) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new SecurityException("User not authenticated"));
        return hasSourceAccess(currentUser, sourceId);
    }

    @Override
    @Cacheable(value = "sourceAccess", key = "#user.id + '-' + #sourceId")
    public boolean hasSourceAccess(User user, Long sourceId) {
        List<Long> roleIds = getUserRoleIds(user);
        if (roleIds.isEmpty()) {
            return false;
        }
        return permissionRepository.hasSourceAccess(roleIds, sourceId);
    }

    @Override
    public boolean hasDomainAccess(Long sourceId, Long domainId) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new SecurityException("User not authenticated"));
        return hasDomainAccess(currentUser, sourceId, domainId);
    }

    @Override
    @Cacheable(value = "domainAccess", key = "#user.id + '-' + #sourceId + '-' + #domainId")
    public boolean hasDomainAccess(User user, Long sourceId, Long domainId) {
        List<Long> roleIds = getUserRoleIds(user);
        if (roleIds.isEmpty()) {
            return false;
        }
        return permissionRepository.hasDomainAccess(roleIds, sourceId, domainId);
    }

    @Override
    public boolean hasEntityAccess(Long sourceId, Long domainId, Long entityId) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new SecurityException("User not authenticated"));
        return hasEntityAccess(currentUser, sourceId, domainId, entityId);
    }

    @Override
    @Cacheable(value = "entityAccess", key = "#user.id + '-' + #sourceId + '-' + #domainId + '-' + #entityId")
    public boolean hasEntityAccess(User user, Long sourceId, Long domainId, Long entityId) {
        List<Long> roleIds = getUserRoleIds(user);
        if (roleIds.isEmpty()) {
            return false;
        }
        return permissionRepository.hasEntityAccess(roleIds, sourceId, domainId, entityId);
    }

    @Override
    public boolean hasFieldAccess(Long sourceId, Long domainId, Long entityId, Long fieldId) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new SecurityException("User not authenticated"));
        return hasFieldAccess(currentUser, sourceId, domainId, entityId, fieldId);
    }

    @Override
    @Cacheable(value = "fieldAccess", key = "#user.id + '-' + #sourceId + '-' + #domainId + '-' + #entityId + '-' + #fieldId")
    public boolean hasFieldAccess(User user, Long sourceId, Long domainId, Long entityId, Long fieldId) {
        List<Long> roleIds = getUserRoleIds(user);
        if (roleIds.isEmpty()) {
            return false;
        }
        return permissionRepository.hasFieldAccess(roleIds, sourceId, domainId, entityId, fieldId);
    }

    @Override
    public List<SourceInfo> filterSources(List<SourceInfo> sources) {
        if (isSuperAdmin()) {
            return sources;
        }

        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new SecurityException("User not authenticated"));

        return sources.stream()
                .filter(source -> hasSourceAccess(currentUser, source.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<DomainInfo> filterDomains(List<DomainInfo> domains) {
        if (isSuperAdmin()) {
            return domains;
        }

        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new SecurityException("User not authenticated"));

        return domains.stream()
                .filter(domain -> hasDomainAccess(currentUser,
                        domain.getSource().getId(),
                        domain.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<EntityInfo> filterEntities(List<EntityInfo> entities) {
        if (isSuperAdmin()) {
            return entities;
        }

        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new SecurityException("User not authenticated"));

        return entities.stream()
                .filter(entity -> hasEntityAccess(currentUser,
                        entity.getDomain().getSource().getId(),
                        entity.getDomain().getId(),
                        entity.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<FieldInfo> filterFields(List<FieldInfo> fields) {
        if (isSuperAdmin()) {
            return fields;
        }

        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new SecurityException("User not authenticated"));

        return fields.stream()
                .filter(field -> hasFieldAccess(currentUser,
                        field.getDataEntity().getDomain().getSource().getId(),
                        field.getDataEntity().getDomain().getId(),
                        field.getDataEntity().getId(),
                        field.getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "accessibleSourceIds", key = "#root.target.getCurrentUserId()")
    public List<Long> getAccessibleSourceIds() {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new SecurityException("User not authenticated"));

        List<Long> roleIds = getUserRoleIds(currentUser);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> sourceIds = permissionRepository.getAccessibleSourceIds(roleIds);
        // -1 indicates wildcard (super admin)
        if (sourceIds.contains(-1L)) {
            return List.of(-1L); // Special marker for "all sources"
        }
        return sourceIds;
    }

    @Override
    @Cacheable(value = "accessibleDomainIds", key = "#root.target.getCurrentUserId() + '-' + #sourceId")
    public List<Long> getAccessibleDomainIds(Long sourceId) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new SecurityException("User not authenticated"));

        List<Long> roleIds = getUserRoleIds(currentUser);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> domainIds = permissionRepository.getAccessibleDomainIds(roleIds, sourceId);
        if (domainIds.contains(-1L)) {
            return List.of(-1L); // Special marker for "all domains"
        }
        return domainIds;
    }

    @Override
    @Cacheable(value = "accessibleEntityIds", key = "#root.target.getCurrentUserId() + '-' + #sourceId + '-' + #domainId")
    public List<Long> getAccessibleEntityIds(Long sourceId, Long domainId) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new SecurityException("User not authenticated"));

        List<Long> roleIds = getUserRoleIds(currentUser);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> entityIds = permissionRepository.getAccessibleEntityIds(roleIds, sourceId, domainId);
        if (entityIds.contains(-1L)) {
            return List.of(-1L); // Special marker for "all entities"
        }
        return entityIds;
    }

    @Override
    @Cacheable(value = "accessibleFieldIds", key = "#root.target.getCurrentUserId() + '-' + #sourceId + '-' + #domainId + '-' + #entityId")
    public List<Long> getAccessibleFieldIds(Long sourceId, Long domainId, Long entityId) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new SecurityException("User not authenticated"));

        List<Long> roleIds = getUserRoleIds(currentUser);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        return permissionRepository.getAccessibleFieldIds(roleIds, entityId);
    }

    @Override
    @Cacheable(value = "isSuperAdmin", key = "#root.target.getCurrentUserId()")
    public boolean isSuperAdmin() {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new SecurityException("User not authenticated"));
        return isSuperAdmin(currentUser);
    }

    @Override
    @Cacheable(value = "isSuperAdmin", key = "#user.id")
    public boolean isSuperAdmin(User user) {
        List<Long> roleIds = getUserRoleIds(user);
        if (roleIds.isEmpty()) {
            return false;
        }

        // Check if user has a permission with all NULLs (Level 0 - Super Admin)
        return permissionRepository.findByRoleIdIn(roleIds).stream()
                .anyMatch(p -> p.getSource() == null &&
                             p.getDomain() == null &&
                             p.getEntity() == null &&
                             p.getField() == null);
    }

    /**
     * Get role IDs for a user (cached)
     */
    @Cacheable(value = "userRoleIds", key = "#user.id")
    private List<Long> getUserRoleIds(User user) {
        return userRoleRepository.findRoleIdsByUserId(user.getId());
    }

    /**
     * Helper method for cache key generation
     */
    public Long getCurrentUserId() {
        return SecurityUtils.getCurrentUser()
                .map(User::getId)
                .orElse(null);
    }
}
