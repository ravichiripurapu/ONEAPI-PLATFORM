package io.oneapi.admin.controller;

import io.oneapi.admin.service.ThrottleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for rate limiting and throttling.
 */
@RestController
@RequestMapping("/api/throttle")
@Tag(name = "Throttle", description = "Manage rate limiting and request throttling")
public class ThrottleController {

    private final ThrottleService throttleService;

    public ThrottleController(ThrottleService throttleService) {
        this.throttleService = throttleService;
    }

    @GetMapping("/check/{userId}")
    @Operation(summary = "Check if request is allowed")
    public ResponseEntity<Map<String, Object>> checkRateLimit(@PathVariable String userId) {
        boolean allowed = throttleService.allowRequest(userId);
        int remaining = throttleService.getRemainingRequests(userId);

        return ResponseEntity.ok(Map.of(
                "allowed", allowed,
                "remaining", remaining,
                "userId", userId
        ));
    }

    @GetMapping("/status/{userId}")
    @Operation(summary = "Get rate limit status")
    public ResponseEntity<Map<String, Integer>> getRateLimitStatus(@PathVariable String userId) {
        int current = throttleService.getRequestCount(userId);
        int remaining = throttleService.getRemainingRequests(userId);

        return ResponseEntity.ok(Map.of(
                "current", current,
                "remaining", remaining
        ));
    }

    @PostMapping("/limits/{userId}")
    @Operation(summary = "Set custom rate limit")
    public ResponseEntity<Void> setRateLimit(
            @PathVariable String userId,
            @RequestBody Map<String, Integer> request) {

        int limit = request.get("requestsPerMinute");
        throttleService.setRateLimit(userId, limit);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/limits/{userId}")
    @Operation(summary = "Reset rate limit")
    public ResponseEntity<Void> resetRateLimit(@PathVariable String userId) {
        throttleService.resetRateLimit(userId);
        return ResponseEntity.noContent().build();
    }
}
