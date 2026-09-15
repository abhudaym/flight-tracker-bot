package com.example.flighttracker.flight;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MockFlightDataProvider implements FlightDataProvider {

    private static final Logger log = LoggerFactory.getLogger(MockFlightDataProvider.class);

    private final Map<String, FlightState> stateOverrides = new ConcurrentHashMap<>();
    private final Map<String, Instant> actualArrivalOverrides = new ConcurrentHashMap<>();
    private final ZoneId applicationZone;

    public MockFlightDataProvider(ZoneId applicationZone) {
        this.applicationZone = applicationZone;
    }

    public void setFlightState(String flightNumber, LocalDate flightDate, FlightState state) {
        String key = key(flightNumber, flightDate);
        stateOverrides.put(key, state);
        if (state == FlightState.LANDED && !actualArrivalOverrides.containsKey(key)) {
            actualArrivalOverrides.put(key, Instant.now());
        }
    }

    public void setActualArrival(String flightNumber, LocalDate flightDate, Instant actualArrival) {
        String key = key(flightNumber, flightDate);
        actualArrivalOverrides.put(key, actualArrival);
    }

    @Override
    public Optional<FlightStatus> getFlightStatus(String flightNumber, LocalDate flightDate) {
        String normalizedNumber = FlightNumberNormalizer.normalize(flightNumber);
        String key = key(normalizedNumber, flightDate);

        if (normalizedNumber.startsWith("INVALID")) {
            return Optional.empty();
        }

        FlightState state = stateOverrides.getOrDefault(key, FlightState.AIRBORNE);

        Instant scheduledDeparture = flightDate.atTime(16, 0).atZone(applicationZone).toInstant();
        Instant scheduledArrival = flightDate.atTime(18, 40).atZone(applicationZone).toInstant();
        Instant estimatedDeparture = scheduledDeparture;
        Instant estimatedArrival = scheduledArrival;

        Instant actualDeparture = (state == FlightState.AIRBORNE || state == FlightState.LANDED) ? scheduledDeparture.plusSeconds(300) : null;
        Instant actualArrival = (state == FlightState.LANDED)
                ? actualArrivalOverrides.getOrDefault(key, scheduledArrival.plusSeconds(420))
                : actualArrivalOverrides.get(key);

        String departureAirport = "DEL";
        String arrivalAirport = "BOM";
        String terminal = "T3";
        String gate = "44B";
        String divertedToAirport = (state == FlightState.DIVERTED) ? "AMD" : null;
        boolean diverted = (state == FlightState.DIVERTED);

        FlightStatus status = new FlightStatus(
                normalizedNumber,
                flightDate,
                departureAirport,
                arrivalAirport,
                state,
                scheduledDeparture,
                scheduledArrival,
                estimatedDeparture,
                estimatedArrival,
                actualDeparture,
                actualArrival,
                terminal,
                gate,
                divertedToAirport,
                diverted
        );

        log.info("MockProvider: Returning status for flight {} date {} -> state={}", normalizedNumber, flightDate, state);
        return Optional.of(status);
    }

    private String key(String flightNumber, LocalDate flightDate) {
        return flightNumber + ":" + flightDate.toString();
    }
}
