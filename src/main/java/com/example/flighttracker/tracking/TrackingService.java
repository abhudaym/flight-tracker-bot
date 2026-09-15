package com.example.flighttracker.tracking;

import com.example.flighttracker.flight.FlightDataProvider;
import com.example.flighttracker.flight.FlightNumberNormalizer;
import com.example.flighttracker.flight.FlightState;
import com.example.flighttracker.flight.FlightStatus;
import com.example.flighttracker.flight.exception.FlightNotFoundException;
import com.example.flighttracker.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class TrackingService {

    private static final Logger log = LoggerFactory.getLogger(TrackingService.class);

    private final TrackingRepository trackingRepository;
    private final FlightDataProvider flightDataProvider;
    private final NotificationService notificationService;
    private final ZoneId applicationZone;

    public TrackingService(
            TrackingRepository trackingRepository,
            FlightDataProvider flightDataProvider,
            NotificationService notificationService,
            @Value("${flight.timezone:Asia/Kolkata}") String timezone
    ) {
        this.trackingRepository = trackingRepository;
        this.flightDataProvider = flightDataProvider;
        this.notificationService = notificationService;
        this.applicationZone = ZoneId.of(timezone);
    }

    @Transactional
    public TrackedFlight trackFlight(Long chatId, String rawFlightNumber, LocalDate requestedDate) {
        String normalizedNumber = FlightNumberNormalizer.normalize(rawFlightNumber);
        LocalDate flightDate = (requestedDate != null) ? requestedDate : LocalDate.now(applicationZone);

        Optional<TrackedFlight> existing = trackingRepository.findByTelegramChatIdAndFlightNumberAndFlightDate(
                chatId, normalizedNumber, flightDate
        );

        if (existing.isPresent() && existing.get().isActive()) {
            log.info("Flight {} date {} is already being tracked for chat {}", normalizedNumber, flightDate, chatId);
            return existing.get();
        }

        Optional<FlightStatus> statusOpt = flightDataProvider.getFlightStatus(normalizedNumber, flightDate);
        if (statusOpt.isEmpty()) {
            throw new FlightNotFoundException("Flight " + normalizedNumber + " not found for date " + flightDate);
        }

        FlightStatus status = statusOpt.get();

        TrackedFlight tracking = existing.orElseGet(TrackedFlight::new);
        tracking.setTelegramChatId(chatId);
        tracking.setFlightNumber(normalizedNumber);
        tracking.setFlightDate(flightDate);
        tracking.setActive(true);
        tracking.setNotificationSent(false);

        updateEntityFromStatus(tracking, status);

        TrackedFlight saved = trackingRepository.save(tracking);

        if (status.hasLanded()) {
            handleLanding(saved);
        } else if (status.state() == FlightState.CANCELLED) {
            handleCancellation(saved);
        }

        return saved;
    }

    public List<TrackedFlight> getActiveTrackedFlights(Long chatId) {
        return trackingRepository.findByTelegramChatIdAndActiveTrueOrderByScheduledArrivalAsc(chatId);
    }

    public List<TrackedFlight> getAllActiveTrackedFlights() {
        return trackingRepository.findByActiveTrue();
    }

    @Transactional
    public Optional<FlightStatus> getAndUpdateFlightStatus(Long chatId, String rawFlightNumber) {
        String normalizedNumber = FlightNumberNormalizer.normalize(rawFlightNumber);
        Optional<TrackedFlight> trackingOpt = trackingRepository.findFirstByTelegramChatIdAndFlightNumberAndActiveTrue(
                chatId, normalizedNumber
        );

        LocalDate flightDate = trackingOpt.map(TrackedFlight::getFlightDate).orElseGet(() -> LocalDate.now(applicationZone));
        Optional<FlightStatus> statusOpt = flightDataProvider.getFlightStatus(normalizedNumber, flightDate);

        statusOpt.ifPresent(status -> {
            trackingOpt.ifPresent(tracking -> {
                processStatusUpdate(tracking, status);
            });
        });

        return statusOpt;
    }

    @Transactional
    public boolean cancelTracking(Long chatId, String rawFlightNumber) {
        String normalizedNumber = FlightNumberNormalizer.normalize(rawFlightNumber);
        Optional<TrackedFlight> trackingOpt = trackingRepository.findFirstByTelegramChatIdAndFlightNumberAndActiveTrue(
                chatId, normalizedNumber
        );

        if (trackingOpt.isPresent()) {
            TrackedFlight tracking = trackingOpt.get();
            tracking.setActive(false);
            trackingRepository.save(tracking);
            log.info("Cancelled tracking for flight {} chat {}", normalizedNumber, chatId);
            return true;
        }

        return false;
    }

    @Transactional
    public int cancelAllTracking(Long chatId) {
        List<TrackedFlight> activeFlights = trackingRepository.findByTelegramChatIdAndActiveTrue(chatId);
        int count = activeFlights.size();
        for (TrackedFlight flight : activeFlights) {
            flight.setActive(false);
        }
        trackingRepository.saveAll(activeFlights);
        log.info("Cancelled all {} active flights for chat {}", count, chatId);
        return count;
    }

    @Transactional
    public void processStatusUpdate(TrackedFlight tracking, FlightStatus newStatus) {
        tracking.setLastProviderCheckAt(Instant.now());
        updateEntityFromStatus(tracking, newStatus);

        if (newStatus.hasLanded()) {
            handleLanding(tracking);
        } else if (newStatus.state() == FlightState.CANCELLED) {
            handleCancellation(tracking);
        } else if (newStatus.state() == FlightState.DIVERTED) {
            handleDiversion(tracking);
        } else {
            trackingRepository.save(tracking);
        }
    }

    private void handleLanding(TrackedFlight tracking) {
        tracking.setStatus(FlightState.LANDED);
        if (!tracking.isNotificationSent()) {
            boolean sent = notificationService.sendLandingNotification(tracking);
            if (sent) {
                tracking.setNotificationSent(true);
            }
        }
        tracking.setActive(false);
        trackingRepository.save(tracking);
    }

    private void handleCancellation(TrackedFlight tracking) {
        tracking.setStatus(FlightState.CANCELLED);
        if (!tracking.isNotificationSent()) {
            boolean sent = notificationService.sendCancellationNotification(tracking);
            if (sent) {
                tracking.setNotificationSent(true);
            }
        }
        tracking.setActive(false);
        trackingRepository.save(tracking);
    }

    private void handleDiversion(TrackedFlight tracking) {
        tracking.setStatus(FlightState.DIVERTED);
        if (!tracking.isNotificationSent()) {
            boolean sent = notificationService.sendDiversionNotification(tracking);
            if (sent) {
                tracking.setNotificationSent(true);
            }
        }
        trackingRepository.save(tracking);
    }

    private void updateEntityFromStatus(TrackedFlight tracking, FlightStatus status) {
        tracking.setStatus(status.state());
        tracking.setDepartureAirport(status.departureAirport());
        tracking.setArrivalAirport(status.arrivalAirport());
        tracking.setScheduledDeparture(status.scheduledDeparture());
        tracking.setScheduledArrival(status.scheduledArrival());
        tracking.setEstimatedDeparture(status.estimatedDeparture());
        tracking.setEstimatedArrival(status.estimatedArrival());
        if (status.actualDeparture() != null) {
            tracking.setActualDeparture(status.actualDeparture());
        }
        if (status.actualArrival() != null) {
            tracking.setActualArrival(status.actualArrival());
        }
        if (status.divertedToAirport() != null) {
            tracking.setDivertedToAirport(status.divertedToAirport());
        }
    }
}
