package io.oneapi.admin.exception;

/**
 * Exception thrown when a query session has expired.
 */
public class SessionExpiredException extends RuntimeException {

    public SessionExpiredException(String message) {
        super(message);
    }

    public SessionExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
