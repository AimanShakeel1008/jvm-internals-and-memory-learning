package com.corejava.jvm.live;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the planted leak's behaviour and its fix. Both registries are static - that is
 * the whole point of the leak - so every test leaves them empty afterwards.
 */
class RegistryTest {

    // Runs after EACH test, so no test can see another's sessions.
    @AfterEach
    void emptyBothRegistries() {
        LeakyRegistry.clear();
        FixedRegistry.clear();
    }

    // The leak: every request adds a session, and nothing ever removes one.
    @Test
    void theLeakyRegistryKeepsEverySession() {
        for (int request = 1; request <= 30; request++) {
            assertEquals(request, LeakyRegistry.handleRequest(request), "Each request adds exactly one session.");
        }
        assertEquals(30, LeakyRegistry.size(), "Thirty requests, thirty sessions - none were ever removed.");
        assertEquals(30L * LeakyRegistry.PAYLOAD_BYTES, LeakyRegistry.retainedPayloadBytes(),
                "Thirty payloads of 32 KB are held.");
    }

    // The bluntest fix - cutting the path - empties it completely.
    @Test
    void clearingCutsThePath() {
        LeakyRegistry.handleRequest(1);
        LeakyRegistry.clear();
        assertEquals(0, LeakyRegistry.size(), "After clear() nothing is reachable through the registry.");
    }

    // The real fix: bounded. It never exceeds its capacity...
    @Test
    void theFixedRegistryNeverExceedsItsCapacity() {
        for (int request = 1; request <= FixedRegistry.CAPACITY * 3; request++) {
            assertTrue(FixedRegistry.handleRequest(request) <= FixedRegistry.CAPACITY,
                    "The bounded registry must never hold more than its capacity.");
        }
        assertEquals(FixedRegistry.CAPACITY, FixedRegistry.size(), "Full, but no fuller.");
    }

    // ...and it evicts the OLDEST, so the newest sessions are the ones kept.
    @Test
    void theFixedRegistryEvictsTheOldestFirst() {
        int total = FixedRegistry.CAPACITY + 5;
        for (int request = 1; request <= total; request++) {
            FixedRegistry.handleRequest(request);
        }
        assertFalse(FixedRegistry.holds(1), "The very first session was evicted - and is now collectable.");
        assertFalse(FixedRegistry.holds(5), "The first five sessions were evicted.");
        assertTrue(FixedRegistry.holds(6), "Session 6 is the oldest one still held.");
        assertTrue(FixedRegistry.holds(total), "The newest session is always held.");
    }
}
