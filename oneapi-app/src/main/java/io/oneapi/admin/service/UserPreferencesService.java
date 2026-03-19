package io.oneapi.admin.service;

import io.oneapi.admin.config.QuerySessionProperties;
import io.oneapi.admin.entity.UserPreferences;
import io.oneapi.admin.repository.UserPreferencesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing user query preferences.
 */
@Service
@Transactional
public class UserPreferencesService {

    private final UserPreferencesRepository preferencesRepository;
    private final QuerySessionProperties properties;

    public UserPreferencesService(UserPreferencesRepository preferencesRepository,
                                  QuerySessionProperties properties) {
        this.preferencesRepository = preferencesRepository;
        this.properties = properties;
    }

    /**
     * Get page size for user (custom or default).
     */
    public int getPageSize(String userId) {
        if (userId == null) {
            return properties.getDefaultPageSize();
        }

        return preferencesRepository.findByUserId(userId)
                .map(UserPreferences::getPageSize)
                .map(ps -> properties.validatePageSize(ps))
                .orElse(properties.getDefaultPageSize());
    }

    /**
     * Get TTL for user (custom or default).
     */
    public int getTtlMinutes(String userId) {
        if (userId == null) {
            return properties.getDefaultTtlMinutes();
        }

        return preferencesRepository.findByUserId(userId)
                .map(UserPreferences::getTtlMinutes)
                .map(ttl -> properties.validateTtl(ttl))
                .orElse(properties.getDefaultTtlMinutes());
    }

    /**
     * Get max concurrent sessions for user.
     */
    public int getMaxConcurrentSessions(String userId) {
        if (userId == null) {
            return properties.getMaxSessionsPerUser();
        }

        return preferencesRepository.findByUserId(userId)
                .map(UserPreferences::getMaxConcurrentSessions)
                .orElse(properties.getMaxSessionsPerUser());
    }

    /**
     * Update user preferences.
     */
    public UserPreferences updatePreferences(String userId, Integer pageSize,
                                                  Integer ttlMinutes, Integer maxSessions) {
        UserPreferences prefs = preferencesRepository.findByUserId(userId)
                .orElse(new UserPreferences());

        prefs.setUserId(userId);

        if (pageSize != null) {
            prefs.setPageSize(properties.validatePageSize(pageSize));
        }

        if (ttlMinutes != null) {
            prefs.setTtlMinutes(properties.validateTtl(ttlMinutes));
        }

        if (maxSessions != null) {
            prefs.setMaxConcurrentSessions(maxSessions);
        }

        return preferencesRepository.save(prefs);
    }

    /**
     * Get preferences for user.
     */
    @Transactional(readOnly = true)
    public UserPreferences getPreferences(String userId) {
        return preferencesRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // Return default preferences
                    UserPreferences defaults = new UserPreferences();
                    defaults.setUserId(userId);
                    defaults.setPageSize(properties.getDefaultPageSize());
                    defaults.setTtlMinutes(properties.getDefaultTtlMinutes());
                    defaults.setMaxConcurrentSessions(properties.getMaxSessionsPerUser());
                    return defaults;
                });
    }

    /**
     * Delete user preferences.
     */
    public void deletePreferences(String userId) {
        preferencesRepository.deleteByUserId(userId);
    }
}
