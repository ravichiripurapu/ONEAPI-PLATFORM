package io.oneapi.admin.exception;

/**
 * Exception thrown when a query session is not found.
 */
public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(String message) {
        super(message);
    }

    public SessionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
