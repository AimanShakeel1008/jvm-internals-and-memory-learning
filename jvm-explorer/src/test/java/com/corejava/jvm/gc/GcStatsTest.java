// Tests mirror the main folder's package exactly.
package com.corejava.jvm.gc;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the collector counters and generation sizes against facts that hold on every
 * JVM, whatever the machine.
 *
 * <p>Like Lesson 03's memory tests, these assert RELATIONSHIPS and exact arithmetic,
 * never sizes or counts: how many collections have run before a test starts depends on
 * everything that ran before it in the same JVM.</p>
 */
class GcStatsTest {

    // A normal JVM always runs at least one collector. (The experimental no-op "Epsilon"
    // collector reports none, but nobody runs tests on it by accident.)
    @Test
    void theJvmReportsAtLeastOneCollectorWithSaneTotals() {
        List<CollectorReading> readings = GcStats.collectors();

        assertFalse(readings.isEmpty(), "A JVM with a garbage collector must report at least one.");
        for (CollectorReading reading : readings) {
            // The record's constructor already rejects negatives; this states it as a
            // fact about what the JVM handed us.
            assertTrue(reading.collectionCount() >= 0, "Counts never go negative: " + reading);
            assertTrue(reading.collectionTimeMillis() >= 0, "Times never go negative: " + reading);
        }
    }

    // The total is defined as the sum of the parts, so it can be checked exactly - with
    // one subtlety: a collection could land between the two calls, so we allow "at least".
    @Test
    void theTotalIsTheSumOfEveryCollector() {
        long summed = 0;
        for (CollectorReading reading : GcStats.collectors()) {
            summed += reading.collectionCount();
        }
        long total = GcStats.totalCollectionCount();

        // Counters only grow, so a total read AFTER the sum can only be equal or larger.
        assertTrue(total >= summed, "The total (" + total + ") cannot be less than the sum read just before it ("
                + summed + ").");
    }

    // Pure arithmetic, so asserted exactly: the gap between two readings of the same
    // collector is the number of collections that happened in between.
    @Test
    void collectionsBetweenTwoReadingsIsTheDifference() {
        CollectorReading earlier = new CollectorReading("Copy", 3, 10);
        CollectorReading later = new CollectorReading("Copy", 7, 25);

        assertEquals(4, later.collectionsSince(earlier), "7 collections minus 3 is 4 in between.");

        // Comparing two DIFFERENT collectors would give a plausible-looking wrong answer,
        // so it must be refused.
        assertThrows(IllegalArgumentException.class,
                () -> later.collectionsSince(new CollectorReading("MarkSweepCompact", 1, 5)),
                "Readings of different collectors must not be compared.");
        assertThrows(IllegalArgumentException.class, () -> new CollectorReading("Copy", -1, 0),
                "A negative count is impossible and must be rejected.");
    }

    // The name matching is pure string logic, so every HotSpot collector's pool names can
    // be checked exactly - including the ones this machine is not running.
    @Test
    void poolNamesFromEveryCollectorMapToTheRightGeneration() {
        // G1 (the default since Java 9)
        assertTrue(Generation.EDEN.matches("G1 Eden Space"));
        assertTrue(Generation.SURVIVOR.matches("G1 Survivor Space"));
        assertTrue(Generation.OLD.matches("G1 Old Gen"));
        // Serial - note "Tenured", the older word for the old generation
        assertTrue(Generation.EDEN.matches("Eden Space"));
        assertTrue(Generation.OLD.matches("Tenured Gen"));
        // Parallel
        assertTrue(Generation.SURVIVOR.matches("PS Survivor Space"));
        assertTrue(Generation.OLD.matches("PS Old Gen"));

        // Non-heap pools belong to no generation, and nothing matches a missing name.
        assertFalse(Generation.OLD.matches("Metaspace"), "Metaspace is not a heap generation.");
        assertFalse(Generation.EDEN.matches("G1 Old Gen"), "An old pool must not be mistaken for Eden.");
        assertFalse(Generation.EDEN.matches(null), "A missing name matches nothing.");
    }

    // A generation that exists must report a sane size; one that does not must say so
    // honestly rather than pretend to be zero. Both outcomes are legitimate.
    @Test
    void generationSizesArePresentAndSaneOrHonestlyAbsent() {
        for (Generation generation : Generation.values()) {
            OptionalLong used = GcStats.usedBytes(generation);
            if (used.isPresent()) {
                assertTrue(used.getAsLong() >= 0, generation + " cannot use a negative number of bytes.");
            }
        }
        // And the formatter's two cases, which can be checked exactly.
        assertEquals("2 KB", GcStats.formatKilobytes(OptionalLong.of(2048)), "2048 bytes is 2 KB.");
        assertEquals("n/a", GcStats.formatKilobytes(OptionalLong.empty()), "An absent pool prints n/a, not 0.");
        assertThrows(IllegalArgumentException.class, () -> GcStats.usedBytes(null),
                "A null generation must be rejected.");
    }
}
