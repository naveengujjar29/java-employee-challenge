package com.reliaquest.api.exception;

/**
 * @author Naveen Kumar
 */
public class RateLimitExceededException extends RuntimeException {

    private final int retryAfterSeconds;

    public RateLimitExceededException(String message) {
        super(message);
        this.retryAfterSeconds = 0;
    }

    public RateLimitExceededException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public RateLimitExceededException(String message, Throwable cause) {
        super(message, cause);
        this.retryAfterSeconds = 0;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
