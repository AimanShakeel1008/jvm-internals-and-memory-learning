package com.corejava.jvm.jit;

import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the allocation meter on the one thing that is guaranteed - an allocation that
 * escapes MUST be counted - and the JIT status for honesty. Nothing here asserts that
 * escape analysis removes an allocation: whether and when C2 compiles a method in the
 * test JVM is not guaranteed, so that is an observation for the experiment, not a test.
 */
class AllocationMeterTest {

    // Kept here, so the array escapes the lambda and no compiler may remove it.
    private static byte[] escaped;

    // One million bytes stored in a field cannot be optimised away, so at least that many
    // bytes must be counted (plus the array's small header).
    @Test
    void anEscapingAllocationIsCountedInFull() {
        OptionalLong bytes = AllocationMeter.allocatedBytesDuring(() -> escaped = new byte[1_000_000]);

        if (bytes.isPresent()) {
            assertTrue(bytes.getAsLong() >= 1_000_000,
                    "A 1,000,000-byte array that escapes must be counted in full, but got " + bytes.getAsLong());
        }
        // On a JVM without the HotSpot extension the meter honestly reports nothing,
        // which is also correct - so there is nothing further to assert in that case.
        assertThrows(IllegalArgumentException.class, () -> AllocationMeter.allocatedBytesDuring(null));
        assertThrows(IllegalArgumentException.class, () -> AllocationMeter.bytesPerCall(0, JitSubjects::distanceLoop));
    }

    // A JVM that reports a compiler must report a sane compile time; one without must say so.
    @Test
    void jitStatusIsSaneOrHonestlyAbsent() {
        if (JitStatus.compilerName().isPresent()) {
            OptionalLong millis = JitStatus.totalCompilationMillis();
            assertTrue(millis.isEmpty() || millis.getAsLong() >= 0, "Compilation time cannot be negative.");
            assertTrue(JitStatus.describe().contains(JitStatus.compilerName().get()),
                    "The description must name the compiler.");
        } else {
            assertTrue(JitStatus.describe().contains("none"), "No compiler must be stated plainly.");
        }
    }
}
