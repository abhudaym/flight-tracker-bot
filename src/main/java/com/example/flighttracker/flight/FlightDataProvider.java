package com.example.flighttracker.flight;

import java.time.LocalDate;
import java.util.Optional;

public interface FlightDataProvider {

    /**
     * Retrieve current flight status for a given flight number and date.
     *
     * @param flightNumber normalized flight number e.g. "AI171"
     * @param flightDate   flight operating date
     * @return Optional containing FlightStatus if found, empty if unknown
     */
    Optional<FlightStatus> getFlightStatus(String flightNumber, LocalDate flightDate);
}
