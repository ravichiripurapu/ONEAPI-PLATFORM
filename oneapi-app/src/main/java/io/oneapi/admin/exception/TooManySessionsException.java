package io.oneapi.admin.exception;

/**
 * Exception thrown when a user exceeds the maximum number of concurrent sessions.
 */
public class TooManySessionsException extends RuntimeException {

    public TooManySessionsException(String message) {
        super(message);
    }

    public TooManySessionsException(String message, Throwable cause) {
        super(message, cause);
    }
}
