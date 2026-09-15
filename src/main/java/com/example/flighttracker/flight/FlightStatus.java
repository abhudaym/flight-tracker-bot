package com.example.flighttracker.flight;

import java.time.Instant;
import java.time.LocalDate;

public record FlightStatus(
    String flightNumber,
    LocalDate flightDate,
    String departureAirport,
    String arrivalAirport,
    FlightState state,
    Instant scheduledDeparture,
    Instant scheduledArrival,
    Instant estimatedDeparture,
    Instant estimatedArrival,
    Instant actualDeparture,
    Instant actualArrival,
    String terminal,
    String gate,
    String divertedToAirport,
    boolean diverted
) {
    public boolean hasLanded() {
        return state == FlightState.LANDED || actualArrival != null;
    }
}
