package com.example.flighttracker.telegram;

import com.example.flighttracker.flight.FlightStatus;
import com.example.flighttracker.tracking.TrackedFlight;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TelegramMessageFormatter {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final ZoneId applicationZone;

    public TelegramMessageFormatter(ZoneId applicationZone) {
        this.applicationZone = applicationZone;
    }

    public String formatStartHelp() {
        return """
                ✈️ <b>Flight Tracker</b>

                I can track flights and notify you when they land.

                <b>Commands:</b>

                /track AI171
                /crew AI171 AI456 AI789
                /tracked
                /status AI171
                /cancel AI171
                /cancel_all
                /help
                """;
    }

    public String formatTrackSuccess(TrackedFlight flight) {
        StringBuilder sb = new StringBuilder();
        sb.append("✈️ <b>Tracking ").append(escape(flight.getFlightNumber())).append("</b>\n\n");

        if (flight.getDepartureAirport() != null && flight.getArrivalAirport() != null) {
            sb.append(escape(flight.getDepartureAirport())).append(" → ").append(escape(flight.getArrivalAirport())).append("\n");
        }

        sb.append("Date: ").append(flight.getFlightDate().format(DATE_FORMATTER)).append("\n");

        if (flight.getScheduledArrival() != null) {
            sb.append("Scheduled arrival: ").append(formatTime(flight.getScheduledArrival())).append(" ").append(applicationZone.getId()).append("\n");
        }

        sb.append("\nI'll notify you when it lands.");
        return sb.toString();
    }

    public String formatAlreadyTracked(String flightNumber) {
        return "<b>" + escape(flightNumber) + "</b> is already being tracked.\n\nI'll notify you when it lands.";
    }

    public String formatFlightNotFound(String flightNumber) {
        return "❌ I couldn't find <b>" + escape(flightNumber) + "</b> for today.\n\nCheck the flight number and try again.";
    }

    public String formatTrackedFlights(List<TrackedFlight> flights) {
        if (flights == null || flights.isEmpty()) {
            return "You're not tracking any flights right now.";
        }

        StringBuilder sb = new StringBuilder("🛰 <b>Active flights</b>\n\n");
        for (int i = 0; i < flights.size(); i++) {
            TrackedFlight f = flights.get(i);
            sb.append(i + 1).append(". <b>").append(escape(f.getFlightNumber())).append("</b>\n");
            if (f.getDepartureAirport() != null && f.getArrivalAirport() != null) {
                sb.append("   ").append(escape(f.getDepartureAirport())).append(" → ").append(escape(f.getArrivalAirport())).append("\n");
            }

            Instant arrivalTime = f.getEstimatedArrival() != null ? f.getEstimatedArrival() : f.getScheduledArrival();
            if (arrivalTime != null) {
                sb.append("   ETA: ").append(formatTime(arrivalTime)).append("\n");
            }
            sb.append("   Status: ").append(f.getStatus()).append("\n\n");
        }
        return sb.toString().trim();
    }

    public String formatStatus(FlightStatus status) {
        StringBuilder sb = new StringBuilder();
        sb.append("✈️ <b>").append(escape(status.flightNumber())).append("</b>\n\n");

        if (status.departureAirport() != null && status.arrivalAirport() != null) {
            sb.append(escape(status.departureAirport())).append(" → ").append(escape(status.arrivalAirport())).append("\n\n");
        }

        String statusEmoji = switch (status.state()) {
            case AIRBORNE, DEPARTED -> "🟢 ";
            case LANDED -> "🛬 ";
            case CANCELLED -> "❌ ";
            case DIVERTED -> "⚠️ ";
            default -> "⏳ ";
        };

        sb.append("Status: ").append(statusEmoji).append(status.state()).append("\n");

        if (status.scheduledArrival() != null) {
            sb.append("Scheduled arrival: ").append(formatTime(status.scheduledArrival())).append("\n");
        }
        if (status.estimatedArrival() != null) {
            sb.append("Estimated arrival: ").append(formatTime(status.estimatedArrival())).append("\n");
        }
        if (status.actualArrival() != null) {
            sb.append("Actual arrival: ").append(formatTime(status.actualArrival())).append("\n");
        }

        return sb.toString();
    }

    public String formatCancelSuccess(String flightNumber) {
        return "🛑 Stopped tracking <b>" + escape(flightNumber) + "</b>.";
    }

    public String formatCancelNotFound(String flightNumber) {
        return "<b>" + escape(flightNumber) + "</b> isn't currently being tracked.";
    }

    public String formatCancelAllSuccess(int count) {
        if (count == 0) {
            return "No active flights to cancel.";
        }
        return "🛑 Stopped tracking " + count + " flight" + (count > 1 ? "s" : "") + ".";
    }

    public String formatUnauthorized() {
        return "Unauthorized user.";
    }

    private String formatTime(Instant instant) {
        if (instant == null) return "N/A";
        return instant.atZone(applicationZone).format(TIME_FORMATTER);
    }

    private String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
