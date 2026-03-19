package io.oneapi.admin.repository;

import io.oneapi.admin.entity.Role;
import io.oneapi.admin.security.domain.User;
import io.oneapi.admin.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for UserRole junction entity
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    /**
     * Find all roles for a user
     */
    List<UserRole> findByUser(User user);

    /**
     * Find all roles for a user by user ID
     */
    List<UserRole> findByUserId(Long userId);

    /**
     * Find all users with a specific role
     */
    List<UserRole> findByRole(Role role);

    /**
     * Check if user has a specific role
     */
    boolean existsByUserAndRole(User user, Role role);

    /**
     * Get all role IDs for a user
     */
    @Query("SELECT ur.role.id FROM UserRole ur WHERE ur.user.id = :userId")
    List<Long> findRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * Get all roles for a user
     */
    @Query("SELECT ur.role FROM UserRole ur WHERE ur.user.id = :userId")
    List<Role> findRolesByUserId(@Param("userId") Long userId);
}
