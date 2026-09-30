// Tests mirror the main folder's package exactly, so they sit beside what they test.
package com.corejava.jvm.memory;

import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the memory report against facts every JVM must obey.
 *
 * <p>These tests deliberately assert RELATIONSHIPS (used is not more than committed,
 * committed is not more than the ceiling) rather than sizes. Actual megabyte figures
 * depend on the machine, the JDK build, the garbage collector and what has run before,
 * so a test asserting "the heap is 4096 MB" would be testing the laptop, not the code.</p>
 */
class MemoryRegionsTest {

    // The one rule that makes the three heap numbers meaningful. If this ever fails,
    // either the JVM is lying or our snapshot arithmetic is wrong - both worth knowing.
    @Test
    void theHeapNumbersAreOrderedUsedThenCommittedThenMax() {
        HeapSnapshot snapshot = MemoryRegions.heapSnapshot();

        assertTrue(snapshot.usedBytes() >= 0,
                "Used heap cannot be negative, but was: " + snapshot.usedBytes());
        assertTrue(snapshot.usedBytes() <= snapshot.committedBytes(),
                "Used heap must fit inside committed heap: " + snapshot);
        assertTrue(snapshot.committedBytes() <= snapshot.maxBytes(),
                "Committed heap cannot exceed the ceiling: " + snapshot);

        // freeBytes is the gap between the two, so it must close exactly.
        assertEquals(snapshot.committedBytes() - snapshot.usedBytes(), snapshot.freeBytes(),
                "freeBytes must be exactly committed minus used.");
    }

    // The megabyte conversion is plain arithmetic, so unlike everything else in this
    // class it CAN be asserted exactly - and exact assertions catch typos in the
    // 1024 * 1024 that a "greater than zero" check would sail straight past.
    @Test
    void bytesConvertToWholeMegabytes() {
        assertEquals(1, HeapSnapshot.toMegabytes(1024L * 1024L),
                "1048576 bytes is exactly 1 MB.");
        assertEquals(0, HeapSnapshot.toMegabytes(1024L * 1024L - 1),
                "Just under 1 MB rounds DOWN to 0, because this is integer division.");
        assertEquals(1024, HeapSnapshot.toMegabytes(1024L * 1024L * 1024L),
                "1 GB is 1024 MB.");
    }

    // Non-heap memory holds the description of every loaded class, and thousands of
    // classes are loaded before a test can even run - so it cannot be zero.
    @Test
    void nonHeapMemoryIsInUseBecauseClassesAreLoaded() {
        assertTrue(MemoryRegions.nonHeapUsedBytes() > 0,
                "A JVM running tests has loaded classes, so non-heap memory must be in use.");

        // The class count from the same lesson's other angle. One is a byte count, the
        // other a count of things stored in those bytes; both must be positive.
        assertTrue(MemoryRegions.loadedClassCount() > 0,
                "The JVM must report at least one loaded class while running a test.");
    }

    // Metaspace is a HotSpot pool name, not something the Java specification promises,
    // so the test states BOTH acceptable outcomes and checks the right one holds.
    @Test
    void metaspaceIsReportedAsAPoolOrHonestlyReportedAsAbsent() {
        OptionalLong metaspace = MemoryRegions.metaspaceUsedBytes();

        if (metaspace.isPresent()) {
            // Present means a real reading, and class metadata is never zero bytes.
            assertTrue(metaspace.getAsLong() > 0,
                    "If the Metaspace pool exists, loaded classes must occupy some of it.");
        } else {
            // Absent is a legitimate answer on a JVM that names its pools differently.
            // Asserting nothing here is the honest thing to do, so we simply record why.
            assertTrue(true, "This JVM exposes no pool named \"" + MemoryRegions.METASPACE_POOL_NAME + "\".");
        }
    }

    // A report nobody can read is not a report. This checks the shape of the text,
    // which is what the lesson's predicted output shows the learner.
    @Test
    void theReportNamesEveryRegionItMeasures() {
        String report = MemoryRegions.describe();

        assertTrue(report.contains("heap:"), "The report must have a heap line: " + report);
        assertTrue(report.contains("non-heap:"), "The report must have a non-heap line: " + report);
        assertTrue(report.contains("Metaspace"), "The report must mention Metaspace: " + report);
    }

    // Invalid input fails loudly at the line that caused it, the same contract every
    // other module in this project follows.
    @Test
    void impossibleSnapshotsAndNegativeByteCountsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> HeapSnapshot.toMegabytes(-1),
                "A negative byte count is a bug in the caller and must be rejected.");

        assertThrows(IllegalArgumentException.class, () -> new HeapSnapshot(-1, 0, 0),
                "A negative ceiling is impossible and must be rejected.");

        assertThrows(IllegalArgumentException.class, () -> new HeapSnapshot(100, 10, 50),
                "Used heap larger than committed heap is a contradiction, not a measurement.");
    }
}
