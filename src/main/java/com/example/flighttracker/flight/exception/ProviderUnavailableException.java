package com.example.flighttracker.flight.exception;

public class ProviderUnavailableException extends RuntimeException {
    public ProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProviderUnavailableException(String message) {
        super(message);
    }
}
