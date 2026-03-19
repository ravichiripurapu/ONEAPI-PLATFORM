package io.oneapi.admin.controller;

import io.oneapi.admin.entity.UserPreferences;
import io.oneapi.admin.service.UserPreferencesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * REST controller for managing user query preferences.
 */
@RestController
@RequestMapping("/api/preferences")
public class UserPreferencesController {

    private final UserPreferencesService preferencesService;

    public UserPreferencesController(UserPreferencesService preferencesService) {
        this.preferencesService = preferencesService;
    }

    /**
     * Get current user's preferences.
     * GET /api/preferences
     */
    @GetMapping
    public ResponseEntity<UserPreferences> getPreferences(Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";
        UserPreferences prefs = preferencesService.getPreferences(userId);
        return ResponseEntity.ok(prefs);
    }

    /**
     * Update user's query preferences.
     * PUT /api/preferences
     */
    @PutMapping
    public ResponseEntity<UserPreferences> updatePreferences(
            @RequestBody UserPreferences request,
            Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";

        UserPreferences updated = preferencesService.updatePreferences(
                userId,
                request.getPageSize(),
                request.getTtlMinutes(),
                request.getMaxConcurrentSessions()
        );

        return ResponseEntity.ok(updated);
    }

    /**
     * Delete user's preferences (reset to defaults).
     * DELETE /api/preferences
     */
    @DeleteMapping
    public ResponseEntity<Void> deletePreferences(Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";
        preferencesService.deletePreferences(userId);
        return ResponseEntity.noContent().build();
    }
}
