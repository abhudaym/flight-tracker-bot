package com.example.flighttracker.tracking;

import com.example.flighttracker.FlightTrackerApplication;
import com.example.flighttracker.flight.FlightState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = FlightTrackerApplication.class)
@ActiveProfiles("test")
class TrackingServiceIntegrationTest {

    @Autowired
    private TrackingService trackingService;

    @Autowired
    private TrackingRepository trackingRepository;

    private static final Long CHAT_ID = 123456789L;

    @Test
    void testFullTrackingLifecycleWithMockProvider() {
        LocalDate today = LocalDate.now();

        // 1. Track flight AI171
        TrackedFlight tracked = trackingService.trackFlight(CHAT_ID, "AI171", today);
        assertNotNull(tracked.getId());
        assertTrue(tracked.isActive());

        // 2. Duplicate track call returns existing active flight record
        TrackedFlight duplicate = trackingService.trackFlight(CHAT_ID, "AI171", today);
        assertEquals(tracked.getId(), duplicate.getId());

        // 3. Verify flight appears in active flight list
        List<TrackedFlight> activeList = trackingService.getActiveTrackedFlights(CHAT_ID);
        assertFalse(activeList.isEmpty());
        assertTrue(activeList.stream().anyMatch(f -> f.getFlightNumber().equals("AI171")));

        // 4. Cancel tracking
        boolean cancelled = trackingService.cancelTracking(CHAT_ID, "AI171");
        assertTrue(cancelled);

        List<TrackedFlight> activeAfterCancel = trackingService.getActiveTrackedFlights(CHAT_ID);
        assertTrue(activeAfterCancel.stream().noneMatch(f -> f.getFlightNumber().equals("AI171")));
    }
}
