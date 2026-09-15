package com.example.flighttracker.flight;

import com.example.flighttracker.flight.exception.ProviderRateLimitException;
import com.example.flighttracker.flight.exception.ProviderUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class AeroDataBoxFlightDataProvider implements FlightDataProvider {

    private static final Logger log = LoggerFactory.getLogger(AeroDataBoxFlightDataProvider.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String rapidApiHost;
    private final ZoneId applicationZone;

    public AeroDataBoxFlightDataProvider(String baseUrl, String apiKey, String rapidApiHost, ZoneId applicationZone) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.rapidApiHost = rapidApiHost;
        this.applicationZone = applicationZone;
    }

    @Override
    public Optional<FlightStatus> getFlightStatus(String flightNumber, LocalDate flightDate) {
        String normalizedNumber = FlightNumberNormalizer.normalize(flightNumber);
        String formattedDate = flightDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

        log.debug("Querying AeroDataBox for flight={} date={}", normalizedNumber, formattedDate);

        try {
            AeroDataBoxResponse[] response = restClient.get()
                    .uri("/flights/number/{flightNumber}/{date}?dateType=Local", normalizedNumber, formattedDate)
                    .header("x-rapidapi-key", apiKey)
                    .header("x-rapidapi-host", rapidApiHost)
                    .retrieve()
                    .onStatus(status -> status.value() == 429, (req, resp) -> {
                        throw new ProviderRateLimitException("AeroDataBox API rate limit exceeded");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        throw new ProviderUnavailableException("AeroDataBox service error: " + resp.getStatusCode());
                    })
                    .body(AeroDataBoxResponse[].class);

            if (response == null || response.length == 0) {
                log.info("AeroDataBox returned empty response for flight={} date={}", normalizedNumber, formattedDate);
                return Optional.empty();
            }

            AeroDataBoxResponse item = response[0];
            return Optional.of(mapToFlightStatus(normalizedNumber, flightDate, item));

        } catch (HttpClientErrorException.NotFound e) {
            log.info("Flight not found on AeroDataBox for flight={} date={}", normalizedNumber, formattedDate);
            return Optional.empty();
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new ProviderRateLimitException("AeroDataBox API rate limit exceeded");
        } catch (ProviderRateLimitException | ProviderUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch flight status from AeroDataBox: {}", e.getMessage(), e);
            throw new ProviderUnavailableException("Error calling AeroDataBox API", e);
        }
    }

    private FlightStatus mapToFlightStatus(String flightNumber, LocalDate flightDate, AeroDataBoxResponse dto) {
        FlightState state = mapState(dto.status);

        String depAirport = (dto.departure != null && dto.departure.airport != null) ? dto.departure.airport.iata : null;
        String arrAirport = (dto.arrival != null && dto.arrival.airport != null) ? dto.arrival.airport.iata : null;

        Instant schedDep = parseTime(dto.departure != null ? dto.departure.scheduledTimeUtc : null);
        Instant schedArr = parseTime(dto.arrival != null ? dto.arrival.scheduledTimeUtc : null);
        Instant estDep = parseTime(dto.departure != null ? dto.departure.revisedTimeUtc : null);
        Instant estArr = parseTime(dto.arrival != null ? dto.arrival.revisedTimeUtc : null);
        Instant actDep = parseTime(dto.departure != null ? dto.departure.actualTimeUtc : null);
        Instant actArr = parseTime(dto.arrival != null ? dto.arrival.actualTimeUtc : null);

        String terminal = dto.arrival != null ? dto.arrival.terminal : null;
        String gate = dto.arrival != null ? dto.arrival.gate : null;
        String divertedTo = null;
        boolean diverted = "Diverted".equalsIgnoreCase(dto.status);

        return new FlightStatus(
                flightNumber,
                flightDate,
                depAirport,
                arrAirport,
                state,
                schedDep,
                schedArr,
                estDep != null ? estDep : schedDep,
                estArr != null ? estArr : schedArr,
                actDep,
                actArr,
                terminal,
                gate,
                divertedTo,
                diverted
        );
    }

    private FlightState mapState(String providerStatus) {
        if (providerStatus == null) return FlightState.UNKNOWN;
        return switch (providerStatus.toLowerCase()) {
            case "scheduled" -> FlightState.SCHEDULED;
            case "delayed" -> FlightState.DELAYED;
            case "boarding" -> FlightState.BOARDING;
            case "departed", "active", "enroute", "en-route", "airborne" -> FlightState.AIRBORNE;
            case "landed", "arrived" -> FlightState.LANDED;
            case "canceled", "cancelled" -> FlightState.CANCELLED;
            case "diverted" -> FlightState.DIVERTED;
            default -> FlightState.UNKNOWN;
        };
    }

    private Instant parseTime(String utcIsoString) {
        if (utcIsoString == null || utcIsoString.isBlank()) return null;
        try {
            return Instant.parse(utcIsoString);
        } catch (Exception e) {
            log.warn("Failed to parse ISO timestamp: {}", utcIsoString);
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AeroDataBoxResponse {
        @JsonProperty("status")
        public String status;

        @JsonProperty("departure")
        public FlightPoint departure;

        @JsonProperty("arrival")
        public FlightPoint arrival;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class FlightPoint {
        @JsonProperty("airport")
        public Airport airport;

        @JsonProperty("scheduledTimeUtc")
        public String scheduledTimeUtc;

        @JsonProperty("revisedTimeUtc")
        public String revisedTimeUtc;

        @JsonProperty("actualTimeUtc")
        public String actualTimeUtc;

        @JsonProperty("terminal")
        public String terminal;

        @JsonProperty("gate")
        public String gate;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Airport {
        @JsonProperty("iata")
        public String iata;
    }
}
