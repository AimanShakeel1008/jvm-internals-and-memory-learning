package com.corejava.jvm.experiments;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the experiment's one piece of real logic: the decision about whether to destroy
 * the heap. The dangerous half is never run from a test - only the decision is.
 */
class MemoryLimitsExperimentTest {

    // The safe default. A program whose destructive mode is the default would be a trap
    // waiting for anyone who runs it without reading it first.
    @Test
    void noArgumentsMeansTheSafeRun() {
        assertFalse(MemoryLimitsExperiment.exhaustionRequested(new String[0]),
                "With no arguments the experiment must not exhaust the heap.");
        assertFalse(MemoryLimitsExperiment.exhaustionRequested(null),
                "A null argument array must also mean the safe run, not a crash.");
    }

    // The flag must be recognised wherever it appears, because a real command line often
    // carries other arguments alongside it.
    @Test
    void theFlagIsRecognisedInAnyPosition() {
        assertTrue(MemoryLimitsExperiment.exhaustionRequested(new String[]{MemoryLimitsExperiment.OOM_FLAG}),
                "The flag on its own must request exhaustion.");
        assertTrue(MemoryLimitsExperiment.exhaustionRequested(
                        new String[]{"something", MemoryLimitsExperiment.OOM_FLAG, "else"}),
                "The flag must be found even when it is not the first argument.");
    }

    // Near-misses must NOT trigger it. Exact matching is what keeps a destructive action
    // from being started by a typo.
    @Test
    void anythingOtherThanTheExactFlagIsIgnored() {
        assertFalse(MemoryLimitsExperiment.exhaustionRequested(new String[]{"-oom"}),
                "A single dash is not the flag.");
        assertFalse(MemoryLimitsExperiment.exhaustionRequested(new String[]{"--OOM"}),
                "The flag is matched exactly, so a different case is not a match.");
        assertFalse(MemoryLimitsExperiment.exhaustionRequested(new String[]{"--oom=true"}),
                "A near-miss must not start a destructive run.");
    }
}
