package io.oneapi.admin.repository;

import io.oneapi.admin.entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for user preferences.
 */
@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {

    /**
     * Find preferences by user ID.
     */
    Optional<UserPreferences> findByUserId(String userId);

    /**
     * Check if preferences exist for user.
     */
    boolean existsByUserId(String userId);

    /**
     * Delete preferences by user ID.
     */
    void deleteByUserId(String userId);
}
