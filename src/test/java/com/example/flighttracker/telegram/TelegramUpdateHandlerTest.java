package com.example.flighttracker.telegram;

import com.example.flighttracker.flight.FlightState;
import com.example.flighttracker.flight.FlightStatus;
import com.example.flighttracker.tracking.TrackedFlight;
import com.example.flighttracker.tracking.TrackingService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TelegramUpdateHandlerTest {

    private static final Long ALLOWED_CHAT_ID = 123456789L;
    private TrackingService trackingService;
    private TelegramUpdateHandler handler;

    @BeforeEach
    void setUp() {
        trackingService = Mockito.mock(TrackingService.class);
        handler = new TelegramUpdateHandler(ALLOWED_CHAT_ID, "Asia/Kolkata", trackingService);
    }

    @Test
    void testUnauthorizedUser() {
        String response = handler.handleIncomingMessage(999999999L, "/start");
        assertEquals("Unauthorized user.", response);
        verifyNoInteractions(trackingService);
    }

    @Test
    void testStartCommand() {
        String response = handler.handleIncomingMessage(ALLOWED_CHAT_ID, "/start");
        assertTrue(response.contains("Flight Tracker"));
        assertTrue(response.contains("/track AI171"));
    }

    @Test
    void testTrackCommandSuccess() {
        TrackedFlight mockFlight = new TrackedFlight(ALLOWED_CHAT_ID, "AI171", LocalDate.now(), FlightState.AIRBORNE);
        mockFlight.setDepartureAirport("DEL");
        mockFlight.setArrivalAirport("BOM");
        mockFlight.setScheduledArrival(Instant.now());

        when(trackingService.trackFlight(eq(ALLOWED_CHAT_ID), eq("AI171"), any())).thenReturn(mockFlight);

        String response = handler.handleIncomingMessage(ALLOWED_CHAT_ID, "/track AI171");
        assertTrue(response.contains("Tracking AI171"));
        assertTrue(response.contains("DEL → BOM"));
    }

    @Test
    void testTrackedCommandEmpty() {
        when(trackingService.getActiveTrackedFlights(ALLOWED_CHAT_ID)).thenReturn(Collections.emptyList());

        String response = handler.handleIncomingMessage(ALLOWED_CHAT_ID, "/tracked");
        assertEquals("You're not tracking any flights right now.", response);
    }

    @Test
    void testTrackedCommandWithFlights() {
        TrackedFlight f1 = new TrackedFlight(ALLOWED_CHAT_ID, "AI171", LocalDate.now(), FlightState.AIRBORNE);
        f1.setDepartureAirport("DEL");
        f1.setArrivalAirport("BOM");

        when(trackingService.getActiveTrackedFlights(ALLOWED_CHAT_ID)).thenReturn(List.of(f1));

        String response = handler.handleIncomingMessage(ALLOWED_CHAT_ID, "/tracked");
        assertTrue(response.contains("Active flights"));
        assertTrue(response.contains("AI171"));
    }

    @Test
    void testCancelCommand() {
        when(trackingService.cancelTracking(eq(ALLOWED_CHAT_ID), eq("AI171"))).thenReturn(true);

        String response = handler.handleIncomingMessage(ALLOWED_CHAT_ID, "/cancel AI171");
        assertTrue(response.contains("Stopped tracking <b>AI171</b>"));
    }

    @Test
    void testCancelAllCommand() {
        when(trackingService.cancelAllTracking(ALLOWED_CHAT_ID)).thenReturn(3);

        String response = handler.handleIncomingMessage(ALLOWED_CHAT_ID, "/cancel_all");
        assertTrue(response.contains("Stopped tracking 3 flights"));
    }
}
