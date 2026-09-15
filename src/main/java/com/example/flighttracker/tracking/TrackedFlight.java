package com.example.flighttracker.tracking;

import com.example.flighttracker.flight.FlightState;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "tracked_flights",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_tracked_flight", columnNames = {"telegram_chat_id", "flight_number", "flight_date"})
    }
)
public class TrackedFlight {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "telegram_chat_id", nullable = false)
    private Long telegramChatId;

    @Column(name = "flight_number", nullable = false, length = 16)
    private String flightNumber;

    @Column(name = "flight_date", nullable = false)
    private LocalDate flightDate;

    @Column(name = "departure_airport", length = 3)
    private String departureAirport;

    @Column(name = "arrival_airport", length = 3)
    private String arrivalAirport;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private FlightState status;

    @Column(name = "scheduled_departure")
    private Instant scheduledDeparture;

    @Column(name = "scheduled_arrival")
    private Instant scheduledArrival;

    @Column(name = "estimated_departure")
    private Instant estimatedDeparture;

    @Column(name = "estimated_arrival")
    private Instant estimatedArrival;

    @Column(name = "actual_departure")
    private Instant actualDeparture;

    @Column(name = "actual_arrival")
    private Instant actualArrival;

    @Column(name = "diverted_to_airport", length = 3)
    private String divertedToAirport;

    @Column(name = "provider_subscription_id", length = 128)
    private String providerSubscriptionId;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "notification_sent", nullable = false)
    private boolean notificationSent = false;

    @Column(name = "last_provider_check_at")
    private Instant lastProviderCheckAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public TrackedFlight() {
    }

    public TrackedFlight(Long telegramChatId, String flightNumber, LocalDate flightDate, FlightState status) {
        this.telegramChatId = telegramChatId;
        this.flightNumber = flightNumber;
        this.flightDate = flightDate;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(Long telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public LocalDate getFlightDate() {
        return flightDate;
    }

    public void setFlightDate(LocalDate flightDate) {
        this.flightDate = flightDate;
    }

    public String getDepartureAirport() {
        return departureAirport;
    }

    public void setDepartureAirport(String departureAirport) {
        this.departureAirport = departureAirport;
    }

    public String getArrivalAirport() {
        return arrivalAirport;
    }

    public void setArrivalAirport(String arrivalAirport) {
        this.arrivalAirport = arrivalAirport;
    }

    public FlightState getStatus() {
        return status;
    }

    public void setStatus(FlightState status) {
        this.status = status;
    }

    public Instant getScheduledDeparture() {
        return scheduledDeparture;
    }

    public void setScheduledDeparture(Instant scheduledDeparture) {
        this.scheduledDeparture = scheduledDeparture;
    }

    public Instant getScheduledArrival() {
        return scheduledArrival;
    }

    public void setScheduledArrival(Instant scheduledArrival) {
        this.scheduledArrival = scheduledArrival;
    }

    public Instant getEstimatedDeparture() {
        return estimatedDeparture;
    }

    public void setEstimatedDeparture(Instant estimatedDeparture) {
        this.estimatedDeparture = estimatedDeparture;
    }

    public Instant getEstimatedArrival() {
        return estimatedArrival;
    }

    public void setEstimatedArrival(Instant estimatedArrival) {
        this.estimatedArrival = estimatedArrival;
    }

    public Instant getActualDeparture() {
        return actualDeparture;
    }

    public void setActualDeparture(Instant actualDeparture) {
        this.actualDeparture = actualDeparture;
    }

    public Instant getActualArrival() {
        return actualArrival;
    }

    public void setActualArrival(Instant actualArrival) {
        this.actualArrival = actualArrival;
    }

    public String getDivertedToAirport() {
        return divertedToAirport;
    }

    public void setDivertedToAirport(String divertedToAirport) {
        this.divertedToAirport = divertedToAirport;
    }

    public String getProviderSubscriptionId() {
        return providerSubscriptionId;
    }

    public void setProviderSubscriptionId(String providerSubscriptionId) {
        this.providerSubscriptionId = providerSubscriptionId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(boolean notificationSent) {
        this.notificationSent = notificationSent;
    }

    public Instant getLastProviderCheckAt() {
        return lastProviderCheckAt;
    }

    public void setLastProviderCheckAt(Instant lastProviderCheckAt) {
        this.lastProviderCheckAt = lastProviderCheckAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrackedFlight that = (TrackedFlight) o;
        return Objects.equals(telegramChatId, that.telegramChatId) &&
               Objects.equals(flightNumber, that.flightNumber) &&
               Objects.equals(flightDate, that.flightDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(telegramChatId, flightNumber, flightDate);
    }
}
