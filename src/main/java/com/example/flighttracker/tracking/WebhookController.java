package com.example.flighttracker.tracking;

import com.example.flighttracker.flight.FlightState;
import com.example.flighttracker.flight.FlightStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final TrackingService trackingService;

    public WebhookController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @PostMapping("/flights")
    public ResponseEntity<Map<String, Object>> handleFlightWebhook(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String webhookSecret,
            @RequestBody WebhookPayload payload
    ) {
        log.info("Received flight webhook for flightNumber={} date={} status={}",
                payload.flightNumber(), payload.flightDate(), payload.status());

        if (payload.flightNumber() == null || payload.flightDate() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required flightNumber or flightDate"));
        }

        List<TrackedFlight> activeFlights = trackingService.getAllActiveTrackedFlights();
        TrackedFlight target = activeFlights.stream()
                .filter(f -> f.getFlightNumber().equalsIgnoreCase(payload.flightNumber()) && f.getFlightDate().equals(payload.flightDate()))
                .findFirst()
                .orElse(null);

        if (target == null) {
            log.info("Webhook received for flight not actively tracked: {} {}", payload.flightNumber(), payload.flightDate());
            return ResponseEntity.ok(Map.of("status", "ignored", "reason", "not_tracked"));
        }

        FlightStatus status = new FlightStatus(
                payload.flightNumber(),
                payload.flightDate(),
                payload.departureAirport(),
                payload.arrivalAirport(),
                payload.status() != null ? payload.status() : FlightState.UNKNOWN,
                payload.scheduledDeparture(),
                payload.scheduledArrival(),
                payload.estimatedDeparture(),
                payload.estimatedArrival(),
                payload.actualDeparture(),
                payload.actualArrival(),
                payload.terminal(),
                payload.gate(),
                payload.divertedToAirport(),
                payload.status() == FlightState.DIVERTED
        );

        trackingService.processStatusUpdate(target, status);

        return ResponseEntity.ok(Map.of("status", "processed", "flightNumber", payload.flightNumber()));
    }

    public record WebhookPayload(
            String flightNumber,
            LocalDate flightDate,
            String departureAirport,
            String arrivalAirport,
            FlightState status,
            Instant scheduledDeparture,
            Instant scheduledArrival,
            Instant estimatedDeparture,
            Instant estimatedArrival,
            Instant actualDeparture,
            Instant actualArrival,
            String terminal,
            String gate,
            String divertedToAirport
    ) {}
}
