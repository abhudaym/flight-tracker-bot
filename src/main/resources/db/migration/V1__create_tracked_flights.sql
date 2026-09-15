CREATE TABLE tracked_flights (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    telegram_chat_id BIGINT NOT NULL,

    flight_number VARCHAR(16) NOT NULL,
    flight_date DATE NOT NULL,

    departure_airport VARCHAR(3),
    arrival_airport VARCHAR(3),

    status VARCHAR(32) NOT NULL,

    scheduled_departure TIMESTAMPTZ,
    scheduled_arrival TIMESTAMPTZ,
    estimated_departure TIMESTAMPTZ,
    estimated_arrival TIMESTAMPTZ,

    actual_departure TIMESTAMPTZ,
    actual_arrival TIMESTAMPTZ,

    diverted_to_airport VARCHAR(3),

    provider_subscription_id VARCHAR(128),

    active BOOLEAN NOT NULL DEFAULT TRUE,
    notification_sent BOOLEAN NOT NULL DEFAULT FALSE,

    last_provider_check_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_tracked_flight
        UNIQUE (telegram_chat_id, flight_number, flight_date)
);

CREATE INDEX idx_tracked_flights_active ON tracked_flights(active);
CREATE INDEX idx_tracked_flights_chat_flight ON tracked_flights(telegram_chat_id, flight_number);
