package com.example.flighttracker.flight;

import java.util.regex.Pattern;

public class FlightNumberNormalizer {

    private static final Pattern FLIGHT_NUMBER_PATTERN = Pattern.compile("^(?:[A-Z]{2}|[A-Z][0-9]|[0-9][A-Z]|[A-Z]{3})[0-9]{1,4}$");
    private static final Pattern REGISTRATION_PATTERN = Pattern.compile("^(?=.*[A-Z])[A-Z0-9]{1,3}-?[A-Z0-9]{2,5}$");

    /**
     * Normalizes a flight number or aircraft tail registration string by trimming, stripping
     * internal spaces, converting to uppercase, and validating against standard formats.
     *
     * @param input Raw flight or tail string (e.g. " ai 171 ", " vt-exn ")
     * @return Normalized string (e.g. "AI171", "VT-EXN")
     * @throws IllegalArgumentException if the format is invalid
     */
    public static String normalize(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Flight or registration number cannot be empty");
        }

        String cleaned = input.trim().replaceAll("\\s+", "").toUpperCase();

        if (FLIGHT_NUMBER_PATTERN.matcher(cleaned).matches()) {
            return cleaned;
        }

        if (REGISTRATION_PATTERN.matcher(cleaned).matches()) {
            return cleaned;
        }

        throw new IllegalArgumentException("Invalid flight number or tail registration format: " + input);
    }

    public static boolean isRegistration(String input) {
        if (input == null || input.isBlank()) return false;
        String cleaned = input.trim().replaceAll("\\s+", "").toUpperCase();
        return !FLIGHT_NUMBER_PATTERN.matcher(cleaned).matches() && REGISTRATION_PATTERN.matcher(cleaned).matches();
    }

    public static boolean isValid(String input) {
        try {
            normalize(input);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
