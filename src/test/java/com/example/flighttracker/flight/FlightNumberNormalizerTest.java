package com.example.flighttracker.flight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlightNumberNormalizerTest {

    @Test
    void testNormalizeValidFlightNumbers() {
        assertEquals("AI171", FlightNumberNormalizer.normalize("AI171"));
        assertEquals("AI171", FlightNumberNormalizer.normalize("ai171"));
        assertEquals("AI171", FlightNumberNormalizer.normalize(" AI 171 "));
        assertEquals("6E2034", FlightNumberNormalizer.normalize("6E 2034"));
        assertEquals("BA117", FlightNumberNormalizer.normalize("ba 117"));
        assertEquals("UAE201", FlightNumberNormalizer.normalize(" uae 201 "));
    }

    @Test
    void testNormalizeValidTailRegistrations() {
        assertEquals("VT-EXN", FlightNumberNormalizer.normalize("VT-EXN"));
        assertEquals("VT-EXN", FlightNumberNormalizer.normalize(" vt-exn "));
        assertEquals("VTEXN", FlightNumberNormalizer.normalize("vtexn"));
        assertEquals("N12345", FlightNumberNormalizer.normalize("n12345"));
        assertEquals("G-XWBA", FlightNumberNormalizer.normalize("g-xwba"));
        assertTrue(FlightNumberNormalizer.isRegistration("VT-EXN"));
        assertFalse(FlightNumberNormalizer.isRegistration("AI171"));
    }

    @Test
    void testInvalidFlightNumbers() {
        assertThrows(IllegalArgumentException.class, () -> FlightNumberNormalizer.normalize(""));
        assertThrows(IllegalArgumentException.class, () -> FlightNumberNormalizer.normalize("   "));
        assertThrows(IllegalArgumentException.class, () -> FlightNumberNormalizer.normalize(null));
        assertThrows(IllegalArgumentException.class, () -> FlightNumberNormalizer.normalize("12345"));
        assertThrows(IllegalArgumentException.class, () -> FlightNumberNormalizer.normalize("TOOLONG123456"));
    }
}
