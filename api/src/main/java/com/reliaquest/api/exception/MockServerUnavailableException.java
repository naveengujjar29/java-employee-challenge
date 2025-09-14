package com.reliaquest.api.exception;

public class MockServerUnavailableException extends RuntimeException {
    public MockServerUnavailableException(String message) {
        super(message);
    }

    public MockServerUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

