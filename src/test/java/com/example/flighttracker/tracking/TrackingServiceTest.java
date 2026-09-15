package com.example.flighttracker.tracking;

import com.example.flighttracker.flight.FlightDataProvider;
import com.example.flighttracker.flight.FlightState;
import com.example.flighttracker.flight.FlightStatus;
import com.example.flighttracker.notification.NotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TrackingServiceTest {

    private TrackingRepository trackingRepository;
    private FlightDataProvider flightDataProvider;
    private NotificationService notificationService;
    private TrackingService trackingService;

    private static final Long CHAT_ID = 123456789L;

    @BeforeEach
    void setUp() {
        trackingRepository = mock(TrackingRepository.class);
        flightDataProvider = mock(FlightDataProvider.class);
        notificationService = mock(NotificationService.class);
        trackingService = new TrackingService(trackingRepository, flightDataProvider, notificationService, "Asia/Kolkata");
    }

    @Test
    void testTrackFlightCreatesNewRecord() {
        LocalDate today = LocalDate.now();
        FlightStatus status = new FlightStatus(
                "AI171", today, "DEL", "BOM", FlightState.AIRBORNE,
                Instant.now(), Instant.now().plusSeconds(3600), Instant.now(), Instant.now().plusSeconds(3600),
                Instant.now(), null, "T3", "44B", null, false
        );

        when(trackingRepository.findByTelegramChatIdAndFlightNumberAndFlightDate(CHAT_ID, "AI171", today))
                .thenReturn(Optional.empty());
        when(flightDataProvider.getFlightStatus("AI171", today)).thenReturn(Optional.of(status));
        when(trackingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TrackedFlight result = trackingService.trackFlight(CHAT_ID, "AI171", today);

        assertNotNull(result);
        assertEquals("AI171", result.getFlightNumber());
        assertEquals(CHAT_ID, result.getTelegramChatId());
        assertTrue(result.isActive());
        assertFalse(result.isNotificationSent());
        verify(notificationService, never()).sendLandingNotification(any());
    }

    @Test
    void testProcessStatusUpdateLandedTriggersNotificationOnlyOnce() {
        LocalDate today = LocalDate.now();
        TrackedFlight tracking = new TrackedFlight(CHAT_ID, "AI171", today, FlightState.AIRBORNE);
        tracking.setNotificationSent(false);
        tracking.setActive(true);

        FlightStatus landedStatus = new FlightStatus(
                "AI171", today, "DEL", "BOM", FlightState.LANDED,
                Instant.now().minusSeconds(7200), Instant.now().minusSeconds(300),
                Instant.now().minusSeconds(7200), Instant.now().minusSeconds(300),
                Instant.now().minusSeconds(7200), Instant.now(), "T3", "44B", null, false
        );

        when(notificationService.sendLandingNotification(any())).thenReturn(true);
        when(trackingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // First status update -> status = LANDED
        trackingService.processStatusUpdate(tracking, landedStatus);

        assertTrue(tracking.isNotificationSent());
        assertFalse(tracking.isActive());
        assertEquals(FlightState.LANDED, tracking.getStatus());
        verify(notificationService, times(1)).sendLandingNotification(tracking);

        // Subsequent status update -> must NOT send notification again
        trackingService.processStatusUpdate(tracking, landedStatus);
        verify(notificationService, times(1)).sendLandingNotification(tracking);
    }
}
