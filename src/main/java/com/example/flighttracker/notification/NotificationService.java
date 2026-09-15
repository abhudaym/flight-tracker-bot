package com.example.flighttracker.notification;

import com.example.flighttracker.tracking.TrackedFlight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final TelegramMessageSender telegramMessageSender;
    private final ZoneId applicationZone;

    public NotificationService(
            @Lazy TelegramMessageSender telegramMessageSender,
            @Value("${flight.timezone:Asia/Kolkata}") String timezone
    ) {
        this.telegramMessageSender = telegramMessageSender;
        this.applicationZone = ZoneId.of(timezone);
    }

    public boolean sendLandingNotification(TrackedFlight flight) {
        String msg = buildLandingMessage(flight);
        log.info("Sending landing notification for flight={} chat={}", flight.getFlightNumber(), flight.getTelegramChatId());
        return telegramMessageSender.sendMessage(flight.getTelegramChatId(), msg);
    }

    public boolean sendDiversionNotification(TrackedFlight flight) {
        String msg = buildDiversionMessage(flight);
        log.info("Sending diversion notification for flight={} chat={}", flight.getFlightNumber(), flight.getTelegramChatId());
        return telegramMessageSender.sendMessage(flight.getTelegramChatId(), msg);
    }

    public boolean sendCancellationNotification(TrackedFlight flight) {
        String msg = buildCancellationMessage(flight);
        log.info("Sending cancellation notification for flight={} chat={}", flight.getFlightNumber(), flight.getTelegramChatId());
        return telegramMessageSender.sendMessage(flight.getTelegramChatId(), msg);
    }

    public String buildLandingMessage(TrackedFlight flight) {
        StringBuilder sb = new StringBuilder();
        sb.append("🛬 <b>").append(escapeHtml(flight.getFlightNumber())).append(" has landed</b>\n\n");

        if (flight.getDepartureAirport() != null && flight.getArrivalAirport() != null) {
            sb.append(escapeHtml(flight.getDepartureAirport())).append(" → ").append(escapeHtml(flight.getArrivalAirport())).append("\n\n");
        }

        if (flight.getScheduledArrival() != null) {
            sb.append("Scheduled arrival: ").append(formatTime(flight.getScheduledArrival())).append("\n");
        }
        if (flight.getActualArrival() != null) {
            sb.append("Actual arrival: ").append(formatTime(flight.getActualArrival())).append("\n");
        }

        if (flight.getScheduledArrival() != null && flight.getActualArrival() != null) {
            long diffMinutes = Duration.between(flight.getScheduledArrival(), flight.getActualArrival()).toMinutes();
            if (diffMinutes > 0) {
                sb.append("Delay: +").append(diffMinutes).append(" min\n");
            } else if (diffMinutes < 0) {
                sb.append("Early: ").append(diffMinutes).append(" min\n");
            } else {
                sb.append("On time\n");
            }
        }

        sb.append("\nFlight tracking completed.");
        return sb.toString();
    }

    public String buildDiversionMessage(TrackedFlight flight) {
        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ <b>").append(escapeHtml(flight.getFlightNumber())).append(" was diverted</b>\n\n");

        if (flight.getArrivalAirport() != null) {
            sb.append("Original destination: ").append(escapeHtml(flight.getArrivalAirport())).append("\n");
        }
        if (flight.getDivertedToAirport() != null) {
            sb.append("Actual airport: ").append(escapeHtml(flight.getDivertedToAirport())).append("\n");
        }

        sb.append("\nTracking completed.");
        return sb.toString();
    }

    public String buildCancellationMessage(TrackedFlight flight) {
        StringBuilder sb = new StringBuilder();
        sb.append("❌ <b>").append(escapeHtml(flight.getFlightNumber())).append(" has been cancelled.</b>\n\n");
        sb.append("No further tracking will occur.");
        return sb.toString();
    }

    private String formatTime(Instant instant) {
        if (instant == null) return "N/A";
        return instant.atZone(applicationZone).format(TIME_FORMATTER);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
