package com.corejava.jvm.experiments;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks the workload's shape with tiny inputs - never what the collector does with it.
 */
class GcWorkloadTest {

    // Two garbage steps give two chances to keep an object, so a cap of 5 keeps only 2;
    // a cap of 1 keeps exactly 1.
    @Test
    void keepsOneObjectPerGarbageStepUpToTheCap() {
        long twoSteps = 2 * GcWorkload.GARBAGE_STEP_BYTES;

        assertEquals(2, GcWorkload.run(twoSteps, 5), "Two steps can keep at most two objects.");
        assertEquals(1, GcWorkload.run(twoSteps, 1), "The cap of one is respected.");
        assertEquals(0, GcWorkload.run(0, 5), "No garbage steps means no chances to keep anything.");
    }

    // Negative inputs are rejected at the door.
    @Test
    void negativeInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> GcWorkload.run(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> GcWorkload.run(0, -1));
    }
}
