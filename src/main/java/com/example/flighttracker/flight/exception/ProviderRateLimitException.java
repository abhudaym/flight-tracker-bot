package com.example.flighttracker.flight.exception;

public class ProviderRateLimitException extends RuntimeException {
    public ProviderRateLimitException(String message) {
        super(message);
    }
}
