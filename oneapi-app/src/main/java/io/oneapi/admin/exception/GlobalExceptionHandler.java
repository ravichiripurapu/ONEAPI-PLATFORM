package io.oneapi.admin.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler for REST API.
 * Maps custom exceptions to appropriate HTTP status codes and user-friendly messages.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle session not found exception.
     * Returns 404 NOT FOUND when session key doesn't exist in cache.
     */
    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleSessionNotFound(
            SessionNotFoundException ex, WebRequest request) {

        log.warn("Session not found: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Session Not Found");
        body.put("message", "The query session does not exist. It may have expired or already completed. Please start a new query.");
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle session expired exception.
     * Returns 410 GONE when session has expired or been completed.
     */
    @ExceptionHandler(SessionExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleSessionExpired(
            SessionExpiredException ex, WebRequest request) {

        log.warn("Session expired: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.GONE.value());
        body.put("error", "Session Expired");
        body.put("message", "The query session has expired or completed. Please start a new query to retrieve data.");
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.GONE);
    }

    /**
     * Handle too many sessions exception.
     * Returns 429 TOO MANY REQUESTS when user exceeds concurrent session limit.
     */
    @ExceptionHandler(TooManySessionsException.class)
    public ResponseEntity<Map<String, Object>> handleTooManySessions(
            TooManySessionsException ex, WebRequest request) {

        log.warn("Too many sessions: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("error", "Too Many Sessions");
        body.put("message", "Maximum concurrent query sessions exceeded. Please wait for existing queries to complete or expire.");
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.TOO_MANY_REQUESTS);
    }

    /**
     * Handle illegal argument exception.
     * Returns 400 BAD REQUEST for invalid input parameters.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {

        log.warn("Invalid argument: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle security exception.
     * Returns 403 FORBIDDEN for authorization failures.
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(
            SecurityException ex, WebRequest request) {

        log.warn("Security violation: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "Forbidden");
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle generic runtime exceptions.
     * Returns 500 INTERNAL SERVER ERROR for unexpected errors.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {

        log.error("Runtime error: {}", ex.getMessage(), ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "An unexpected error occurred. Please try again or contact support.");
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
