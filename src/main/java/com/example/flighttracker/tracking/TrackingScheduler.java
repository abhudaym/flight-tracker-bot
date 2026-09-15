package com.example.flighttracker.tracking;

import com.example.flighttracker.flight.FlightDataProvider;
import com.example.flighttracker.flight.FlightStatus;
import com.example.flighttracker.flight.exception.ProviderRateLimitException;
import com.example.flighttracker.flight.exception.ProviderUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class TrackingScheduler {

    private static final Logger log = LoggerFactory.getLogger(TrackingScheduler.class);

    private final TrackingService trackingService;
    private final FlightDataProvider flightDataProvider;
    private final boolean pollingEnabled;

    public TrackingScheduler(
            TrackingService trackingService,
            FlightDataProvider flightDataProvider,
            @Value("${flight.polling.enabled:true}") boolean pollingEnabled
    ) {
        this.trackingService = trackingService;
        this.flightDataProvider = flightDataProvider;
        this.pollingEnabled = pollingEnabled;
    }

    @Scheduled(fixedDelayString = "${flight.scheduler.interval:60000}")
    public void processTrackedFlights() {
        if (!pollingEnabled) {
            return;
        }

        List<TrackedFlight> activeFlights = trackingService.getAllActiveTrackedFlights();
        if (activeFlights.isEmpty()) {
            return;
        }

        log.debug("Scheduler running for {} active flight(s)", activeFlights.size());
        Instant now = Instant.now();

        for (TrackedFlight flight : activeFlights) {
            if (shouldCheck(flight, now)) {
                try {
                    log.info("Polling flight status flightNumber={} date={} id={}",
                            flight.getFlightNumber(), flight.getFlightDate(), flight.getId());

                    Optional<FlightStatus> statusOpt = flightDataProvider.getFlightStatus(
                            flight.getFlightNumber(), flight.getFlightDate()
                    );

                    if (statusOpt.isPresent()) {
                        trackingService.processStatusUpdate(flight, statusOpt.get());
                    } else {
                        log.warn("No flight status returned for flightNumber={} date={}",
                                flight.getFlightNumber(), flight.getFlightDate());
                    }
                } catch (ProviderRateLimitException e) {
                    log.warn("Rate limit hit during scheduled poll: {}", e.getMessage());
                    break; // stop current batch on rate limit
                } catch (ProviderUnavailableException e) {
                    log.warn("Provider unavailable during poll for flight {}: {}", flight.getFlightNumber(), e.getMessage());
                } catch (Exception e) {
                    log.error("Error processing flight status poll for flight {}: {}", flight.getFlightNumber(), e.getMessage(), e);
                }
            }
        }
    }

    public boolean shouldCheck(TrackedFlight flight, Instant now) {
        if (flight.getLastProviderCheckAt() == null) {
            return true;
        }

        Instant referenceTime = flight.getScheduledDeparture() != null ? flight.getScheduledDeparture() : flight.getScheduledArrival();
        if (referenceTime == null) {
            return Duration.between(flight.getLastProviderCheckAt(), now).toMinutes() >= 10;
        }

        Duration timeUntilDeparture = Duration.between(now, referenceTime);
        Duration elapsedSinceLastCheck = Duration.between(flight.getLastProviderCheckAt(), now);

        if (timeUntilDeparture.toHours() > 24) {
            return elapsedSinceLastCheck.toHours() >= 6;
        } else if (timeUntilDeparture.toHours() >= 6) {
            return elapsedSinceLastCheck.toHours() >= 2;
        } else if (timeUntilDeparture.toHours() >= 2) {
            return elapsedSinceLastCheck.toMinutes() >= 30;
        } else {
            return elapsedSinceLastCheck.toMinutes() >= 10;
        }
    }
}
