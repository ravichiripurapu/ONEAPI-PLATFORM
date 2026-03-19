package io.oneapi.admin.service.security;

import io.oneapi.admin.security.domain.User;
import io.oneapi.admin.entity.metadata.DomainInfo;
import io.oneapi.admin.entity.metadata.EntityInfo;
import io.oneapi.admin.entity.metadata.FieldInfo;
import io.oneapi.admin.entity.SourceInfo;

import java.util.List;

/**
 * Security service for hierarchical RBAC permission checking.
 * Implements permission resolution from most specific (field) to least specific (super admin).
 */
public interface SecurityService {

    /**
     * Check if current authenticated user has access to a source
     */
    boolean hasSourceAccess(Long sourceId);

    /**
     * Check if specific user has access to a source
     */
    boolean hasSourceAccess(User user, Long sourceId);

    /**
     * Check if current authenticated user has access to a domain
     */
    boolean hasDomainAccess(Long sourceId, Long domainId);

    /**
     * Check if specific user has access to a domain
     */
    boolean hasDomainAccess(User user, Long sourceId, Long domainId);

    /**
     * Check if current authenticated user has access to an entity
     */
    boolean hasEntityAccess(Long sourceId, Long domainId, Long entityId);

    /**
     * Check if specific user has access to an entity
     */
    boolean hasEntityAccess(User user, Long sourceId, Long domainId, Long entityId);

    /**
     * Check if current authenticated user has access to a field
     */
    boolean hasFieldAccess(Long sourceId, Long domainId, Long entityId, Long fieldId);

    /**
     * Check if specific user has access to a field
     */
    boolean hasFieldAccess(User user, Long sourceId, Long domainId, Long entityId, Long fieldId);

    /**
     * Filter sources based on current user's permissions
     */
    List<SourceInfo> filterSources(List<SourceInfo> sources);

    /**
     * Filter domains based on current user's permissions
     */
    List<DomainInfo> filterDomains(List<DomainInfo> domains);

    /**
     * Filter entities based on current user's permissions
     */
    List<EntityInfo> filterEntities(List<EntityInfo> entities);

    /**
     * Filter fields based on current user's permissions (CRITICAL for field-level security)
     */
    List<FieldInfo> filterFields(List<FieldInfo> fields);

    /**
     * Get all accessible source IDs for current user
     */
    List<Long> getAccessibleSourceIds();

    /**
     * Get all accessible domain IDs for a source
     */
    List<Long> getAccessibleDomainIds(Long sourceId);

    /**
     * Get all accessible entity IDs for a domain
     */
    List<Long> getAccessibleEntityIds(Long sourceId, Long domainId);

    /**
     * Get all accessible field IDs for an entity
     */
    List<Long> getAccessibleFieldIds(Long sourceId, Long domainId, Long entityId);

    /**
     * Check if current user is super admin (has Level 0 permission)
     */
    boolean isSuperAdmin();

    /**
     * Check if specific user is super admin
     */
    boolean isSuperAdmin(User user);
}
