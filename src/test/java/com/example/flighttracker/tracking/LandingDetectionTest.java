package com.example.flighttracker.tracking;

import com.example.flighttracker.flight.FlightState;
import com.example.flighttracker.flight.FlightStatus;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class LandingDetectionTest {

    @Test
    void testAirborneWithoutActualArrivalIsNotLanded() {
        FlightStatus status = new FlightStatus(
                "AI171",
                LocalDate.now(),
                "DEL",
                "BOM",
                FlightState.AIRBORNE,
                Instant.now(),
                Instant.now().plusSeconds(7200),
                Instant.now(),
                Instant.now().plusSeconds(7200),
                Instant.now(),
                null,
                "T3",
                "44B",
                null,
                false
        );

        assertFalse(status.hasLanded(), "Flight in AIRBORNE state without actualArrival should not be detected as landed");
    }

    @Test
    void testLandedStateIsDetectedAsLanded() {
        FlightStatus status = new FlightStatus(
                "AI171",
                LocalDate.now(),
                "DEL",
                "BOM",
                FlightState.LANDED,
                Instant.now().minusSeconds(7200),
                Instant.now().minusSeconds(300),
                Instant.now().minusSeconds(7200),
                Instant.now().minusSeconds(300),
                Instant.now().minusSeconds(7200),
                Instant.now(),
                "T3",
                "44B",
                null,
                false
        );

        assertTrue(status.hasLanded(), "Flight in LANDED state should be detected as landed");
    }

    @Test
    void testActualArrivalPopulatedIsDetectedAsLanded() {
        FlightStatus status = new FlightStatus(
                "AI171",
                LocalDate.now(),
                "DEL",
                "BOM",
                FlightState.AIRBORNE,
                Instant.now().minusSeconds(7200),
                Instant.now().minusSeconds(300),
                Instant.now().minusSeconds(7200),
                Instant.now().minusSeconds(300),
                Instant.now().minusSeconds(7200),
                Instant.now(),
                "T3",
                "44B",
                null,
                false
        );

        assertTrue(status.hasLanded(), "Flight with actualArrival timestamp populated should be detected as landed even if state is AIRBORNE");
    }
}
