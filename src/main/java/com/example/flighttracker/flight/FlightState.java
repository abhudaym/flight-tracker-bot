package com.example.flighttracker.flight;

public enum FlightState {
    SCHEDULED,
    DELAYED,
    BOARDING,
    DEPARTED,
    AIRBORNE,
    LANDED,
    CANCELLED,
    DIVERTED,
    UNKNOWN;

    public boolean isTerminal() {
        return this == LANDED || this == CANCELLED;
    }
}
