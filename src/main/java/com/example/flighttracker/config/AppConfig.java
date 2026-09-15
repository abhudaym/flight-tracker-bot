package com.example.flighttracker.config;

import com.example.flighttracker.flight.AeroDataBoxFlightDataProvider;
import com.example.flighttracker.flight.FlightDataProvider;
import com.example.flighttracker.flight.MockFlightDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.ZoneId;

@Configuration
@EnableScheduling
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    @Bean
    public FlightDataProvider flightDataProvider(
            @Value("${flight.provider:mock}") String providerType,
            @Value("${flight.timezone:Asia/Kolkata}") String timezone,
            @Value("${aerodatabox.base-url:https://aerodatabox.p.rapidapi.com}") String aerodataboxBaseUrl,
            @Value("${aerodatabox.api-key:}") String aerodataboxApiKey,
            @Value("${aerodatabox.rapidapi-host:aerodatabox.p.rapidapi.com}") String aerodataboxHost
    ) {
        ZoneId zoneId = ZoneId.of(timezone);

        if ("aerodatabox".equalsIgnoreCase(providerType)) {
            log.info("Configuring AeroDataBoxFlightDataProvider with baseUrl={}", aerodataboxBaseUrl);
            return new AeroDataBoxFlightDataProvider(aerodataboxBaseUrl, aerodataboxApiKey, aerodataboxHost, zoneId);
        } else {
            log.info("Configuring MockFlightDataProvider (default)");
            return new MockFlightDataProvider(zoneId);
        }
    }
}
