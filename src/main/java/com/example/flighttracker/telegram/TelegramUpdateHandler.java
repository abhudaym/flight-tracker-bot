package com.example.flighttracker.telegram;

import com.example.flighttracker.flight.FlightNumberNormalizer;
import com.example.flighttracker.flight.FlightStatus;
import com.example.flighttracker.flight.exception.FlightNotFoundException;
import com.example.flighttracker.tracking.TrackedFlight;
import com.example.flighttracker.tracking.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Component
public class TelegramUpdateHandler {

    private static final Logger log = LoggerFactory.getLogger(TelegramUpdateHandler.class);

    private final Long allowedChatId;
    private final TrackingService trackingService;
    private final TelegramMessageFormatter formatter;
    private final ZoneId applicationZone;

    public TelegramUpdateHandler(
            @Value("${telegram.allowed-chat-id:0}") Long allowedChatId,
            @Value("${flight.timezone:Asia/Kolkata}") String timezone,
            TrackingService trackingService
    ) {
        this.allowedChatId = allowedChatId;
        this.trackingService = trackingService;
        this.applicationZone = ZoneId.of(timezone);
        this.formatter = new TelegramMessageFormatter(this.applicationZone);
    }

    public String handleIncomingMessage(Long chatId, String text) {
        if (chatId == null || !chatId.equals(allowedChatId)) {
            log.warn("Unauthorized access attempt from chatId={}", chatId);
            return formatter.formatUnauthorized();
        }

        if (text == null || text.isBlank()) {
            return formatter.formatStartHelp();
        }

        String command = text.trim();
        String[] parts = command.split("\\s+");
        String baseCmd = parts[0].toLowerCase();

        // Handle bot username suffix in command (e.g. /track@MyFlightBot)
        if (baseCmd.contains("@")) {
            baseCmd = baseCmd.substring(0, baseCmd.indexOf("@"));
        }

        try {
            return switch (baseCmd) {
                case "/start", "/help" -> formatter.formatStartHelp();
                case "/track" -> handleTrackCommand(chatId, parts);
                case "/tracked" -> handleTrackedCommand(chatId);
                case "/status" -> handleStatusCommand(chatId, parts);
                case "/cancel" -> handleCancelCommand(chatId, parts);
                case "/cancel_all" -> handleCancelAllCommand(chatId);
                default -> "Unknown command. Send /help for available commands.";
            };
        } catch (Exception e) {
            log.error("Error processing telegram command: {}", command, e);
            return "⚠️ An error occurred while processing your request. Please try again.";
        }
    }

    private String handleTrackCommand(Long chatId, String[] parts) {
        if (parts.length < 2) {
            return "Usage: <code>/track &lt;flight_number&gt;</code>\nExample: <code>/track AI171</code>";
        }

        String rawFlightNumber = parts[1];
        if (!FlightNumberNormalizer.isValid(rawFlightNumber)) {
            return formatter.formatFlightNotFound(rawFlightNumber);
        }

        try {
            TrackedFlight flight = trackingService.trackFlight(chatId, rawFlightNumber, LocalDate.now(applicationZone));
            return formatter.formatTrackSuccess(flight);
        } catch (FlightNotFoundException e) {
            return formatter.formatFlightNotFound(rawFlightNumber);
        }
    }

    private String handleTrackedCommand(Long chatId) {
        List<TrackedFlight> active = trackingService.getActiveTrackedFlights(chatId);
        return formatter.formatTrackedFlights(active);
    }

    private String handleStatusCommand(Long chatId, String[] parts) {
        if (parts.length < 2) {
            return "Usage: <code>/status &lt;flight_number&gt;</code>\nExample: <code>/status AI171</code>";
        }

        String rawFlightNumber = parts[1];
        if (!FlightNumberNormalizer.isValid(rawFlightNumber)) {
            return formatter.formatFlightNotFound(rawFlightNumber);
        }

        Optional<FlightStatus> statusOpt = trackingService.getAndUpdateFlightStatus(chatId, rawFlightNumber);
        if (statusOpt.isEmpty()) {
            return formatter.formatFlightNotFound(rawFlightNumber);
        }

        return formatter.formatStatus(statusOpt.get());
    }

    private String handleCancelCommand(Long chatId, String[] parts) {
        if (parts.length < 2) {
            return "Usage: <code>/cancel &lt;flight_number&gt;</code>\nExample: <code>/cancel AI171</code>";
        }

        String rawFlightNumber = parts[1];
        if (!FlightNumberNormalizer.isValid(rawFlightNumber)) {
            return formatter.formatCancelNotFound(rawFlightNumber);
        }

        boolean cancelled = trackingService.cancelTracking(chatId, rawFlightNumber);
        if (cancelled) {
            return formatter.formatCancelSuccess(FlightNumberNormalizer.normalize(rawFlightNumber));
        } else {
            return formatter.formatCancelNotFound(rawFlightNumber);
        }
    }

    private String handleCancelAllCommand(Long chatId) {
        int count = trackingService.cancelAllTracking(chatId);
        return formatter.formatCancelAllSuccess(count);
    }

    public TelegramMessageFormatter getFormatter() {
        return formatter;
    }
}
