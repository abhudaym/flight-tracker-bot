package com.example.flighttracker.flight;

import java.util.regex.Pattern;

public class FlightNumberNormalizer {

    private static final Pattern FLIGHT_NUMBER_PATTERN = Pattern.compile("^(?:[A-Z]{2}|[A-Z][0-9]|[0-9][A-Z]|[A-Z]{3})[0-9]{1,4}$");

    /**
     * Normalizes a flight number string by trimming, stripping internal spaces,
     * converting to uppercase, and validating against standard format regex.
     *
     * @param input Raw flight number string (e.g. " ai 171 ")
     * @return Normalized flight number (e.g. "AI171")
     * @throws IllegalArgumentException if the flight number format is invalid
     */
    public static String normalize(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Flight number cannot be empty");
        }

        String cleaned = input.trim().replaceAll("\\s+", "").toUpperCase();

        if (!FLIGHT_NUMBER_PATTERN.matcher(cleaned).matches()) {
            throw new IllegalArgumentException("Invalid flight number format: " + input);
        }

        return cleaned;
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
