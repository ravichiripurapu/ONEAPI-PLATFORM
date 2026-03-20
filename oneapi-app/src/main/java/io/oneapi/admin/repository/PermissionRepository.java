package io.oneapi.admin.repository;

import io.oneapi.admin.entity.Permission;
import io.oneapi.admin.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Permission entity
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * Find all permissions for a role
     */
    List<Permission> findByRole(Role role);

    /**
     * Find all permissions for a role ID
     */
    List<Permission> findByRoleId(Long roleId);

    /**
     * Find all permissions for multiple role IDs
     */
    List<Permission> findByRoleIdIn(List<Long> roleIds);

    /**
     * Check if user has access to a specific source
     * Checks hierarchically: Level 0 (all NULLs) or Level 1 (source_id matches)
     */
    @Query("""
        SELECT COUNT(p) > 0
        FROM Permission p
        WHERE p.role.id IN :roleIds
        AND p.permissionType = 'READ'
        AND (
            (p.source IS NULL AND p.domain IS NULL AND p.entity IS NULL AND p.field IS NULL)
            OR p.source.id = :sourceId
        )
    """)
    boolean hasSourceAccess(@Param("roleIds") List<Long> roleIds, @Param("sourceId") Long sourceId);

    /**
     * Check if user has access to a specific domain
     * Checks hierarchically: Level 0, 1, or 2
     */
    @Query("""
        SELECT COUNT(p) > 0
        FROM Permission p
        WHERE p.role.id IN :roleIds
        AND p.permissionType = 'READ'
        AND (
            (p.source IS NULL AND p.domain IS NULL AND p.entity IS NULL AND p.field IS NULL)
            OR (p.source.id = :sourceId AND p.domain IS NULL)
            OR p.domain.id = :domainId
        )
    """)
    boolean hasDomainAccess(@Param("roleIds") List<Long> roleIds,
                           @Param("sourceId") Long sourceId,
                           @Param("domainId") Long domainId);

    /**
     * Check if user has access to a specific entity
     * Checks hierarchically: Level 0, 1, 2, or 3
     */
    @Query("""
        SELECT COUNT(p) > 0
        FROM Permission p
        WHERE p.role.id IN :roleIds
        AND p.permissionType = 'READ'
        AND (
            (p.source IS NULL AND p.domain IS NULL AND p.entity IS NULL AND p.field IS NULL)
            OR (p.source.id = :sourceId AND p.domain IS NULL)
            OR (p.domain.id = :domainId AND p.entity IS NULL)
            OR p.entity.id = :entityId
        )
    """)
    boolean hasEntityAccess(@Param("roleIds") List<Long> roleIds,
                           @Param("sourceId") Long sourceId,
                           @Param("domainId") Long domainId,
                           @Param("entityId") Long entityId);

    /**
     * Check if user has access to a specific field
     * Checks hierarchically: Level 0, 1, 2, 3, or 4
     * IMPORTANT: Also checks for explicit DENY at field level
     */
    @Query("""
        SELECT CASE
            WHEN COUNT(CASE WHEN p.field.id = :fieldId AND p.permissionType = 'DENY' THEN 1 END) > 0 THEN false
            WHEN COUNT(CASE
                WHEN (p.source IS NULL AND p.domain IS NULL AND p.entity IS NULL AND p.field IS NULL)
                    OR (p.source.id = :sourceId AND p.domain IS NULL)
                    OR (p.domain.id = :domainId AND p.entity IS NULL)
                    OR (p.entity.id = :entityId AND p.field IS NULL)
                    OR p.field.id = :fieldId
                THEN 1 END) > 0 THEN true
            ELSE false
        END
        FROM Permission p
        WHERE p.role.id IN :roleIds
    """)
    boolean hasFieldAccess(@Param("roleIds") List<Long> roleIds,
                          @Param("sourceId") Long sourceId,
                          @Param("domainId") Long domainId,
                          @Param("entityId") Long entityId,
                          @Param("fieldId") Long fieldId);

    /**
     * Get all accessible source IDs for user's roles
     */
    @Query("""
        SELECT DISTINCT COALESCE(p.source.id, -1)
        FROM Permission p
        WHERE p.role.id IN :roleIds
        AND p.permissionType = 'READ'
        AND (p.source IS NULL OR p.source.id IS NOT NULL)
    """)
    List<Long> getAccessibleSourceIds(@Param("roleIds") List<Long> roleIds);

    /**
     * Get all accessible domain IDs for a source
     */
    @Query("""
        SELECT DISTINCT COALESCE(p.domain.id, -1)
        FROM Permission p
        WHERE p.role.id IN :roleIds
        AND p.permissionType = 'READ'
        AND (
            (p.source IS NULL AND p.domain IS NULL)
            OR (p.source.id = :sourceId AND p.domain IS NULL)
            OR p.domain.id IS NOT NULL
        )
    """)
    List<Long> getAccessibleDomainIds(@Param("roleIds") List<Long> roleIds, @Param("sourceId") Long sourceId);

    /**
     * Get all accessible entity IDs for a domain
     */
    @Query("""
        SELECT DISTINCT COALESCE(p.entity.id, -1)
        FROM Permission p
        WHERE p.role.id IN :roleIds
        AND p.permissionType = 'READ'
        AND (
            (p.source IS NULL AND p.domain IS NULL AND p.entity IS NULL)
            OR (p.source.id = :sourceId AND p.domain IS NULL)
            OR (p.domain.id = :domainId AND p.entity IS NULL)
            OR p.entity.id IS NOT NULL
        )
    """)
    List<Long> getAccessibleEntityIds(@Param("roleIds") List<Long> roleIds,
                                     @Param("sourceId") Long sourceId,
                                     @Param("domainId") Long domainId);

    /**
     * Get all accessible field IDs for an entity
     */
    @Query("""
        SELECT DISTINCT p.field.id
        FROM Permission p
        WHERE p.role.id IN :roleIds
        AND p.permissionType = 'READ'
        AND p.field.id IS NOT NULL
        AND p.entity.id = :entityId
    """)
    List<Long> getAccessibleFieldIds(@Param("roleIds") List<Long> roleIds, @Param("entityId") Long entityId);

    /**
     * Delete all permissions for a role and source
     */
    void deleteByRoleIdAndSourceId(Long roleId, Long sourceId);
}
