package com.example.flighttracker.tracking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrackingRepository extends JpaRepository<TrackedFlight, UUID> {

    List<TrackedFlight> findByActiveTrue();

    List<TrackedFlight> findByTelegramChatIdAndActiveTrueOrderByScheduledArrivalAsc(Long telegramChatId);

    Optional<TrackedFlight> findByTelegramChatIdAndFlightNumberAndFlightDate(
            Long telegramChatId,
            String flightNumber,
            LocalDate flightDate
    );

    Optional<TrackedFlight> findFirstByTelegramChatIdAndFlightNumberAndActiveTrue(
            Long telegramChatId,
            String flightNumber
    );

    List<TrackedFlight> findByTelegramChatIdAndActiveTrue(Long telegramChatId);
}
