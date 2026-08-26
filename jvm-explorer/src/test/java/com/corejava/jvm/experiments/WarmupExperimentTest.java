// Tests mirror the package of the code they test, including the experiments package.
package com.corejava.jvm.experiments;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the SHAPE of the warm-up experiment, never its speed.
 *
 * <p>There is a deliberate absence here: no test claims that later batches are faster
 * than earlier ones. That is the effect the experiment exists to show, and it is real
 * - but it depends on the machine, the JDK build, what else the operating system is
 * doing, and plain luck. An assertion on timing would fail on a loaded laptop and pass
 * on an idle one, which makes it a coin toss wearing the costume of a test. A test that
 * sometimes fails for reasons unrelated to the code is called a FLAKY test, and one
 * flaky test poisons the whole suite: people start ignoring red builds. So the machine
 * checks the parts that are always true, and the learner's own eyes read the timings.</p>
 */
class WarmupExperimentTest {

    // The contract callers depend on: ask for N batches, get exactly N timings back,
    // in order. Without this, the printed table could silently lose or repeat rows.
    @Test
    void returnsOneTimingPerBatch() {
        long[] elapsed = WarmupExperiment.timeBatches(3, 1_000, 100);

        assertEquals(3, elapsed.length, "Asking for 3 batches must return exactly 3 timings.");
    }

    // Every batch really did run: elapsed time is a positive number of nanoseconds.
    // This is safe to assert (unlike "faster") because thousands of calls cannot
    // possibly complete in zero measurable time on any real machine.
    @Test
    void everyBatchTakesSomeMeasurableTime() {
        long[] elapsed = WarmupExperiment.timeBatches(3, 5_000, 100);

        for (int batch = 0; batch < elapsed.length; batch++) {
            assertTrue(elapsed[batch] > 0,
                    // Including the batch number and the value makes a failure diagnosable
                    // from the report alone, without re-running anything.
                    "Batch " + (batch + 1) + " reported " + elapsed[batch]
                            + " ns; a batch of real work cannot take zero time.");
        }
    }

    // The work is genuinely performed, not optimised away: the sink must move.
    @Test
    void theWorkActuallyHappensAndReachesTheSink() {
        // Read the running total before and after, rather than expecting a fixed value -
        // other tests in the same JVM may have already added to this shared field.
        long before = WarmupExperiment.sink;

        // 2 batches x 10 calls x sumTo(10). sumTo(10) is 55, so exactly 20 * 55 = 1100
        // must be added - a number small enough to state exactly and check exactly.
        WarmupExperiment.timeBatches(2, 10, 10);

        assertEquals(before + 1_100, WarmupExperiment.sink,
                "2 batches of 10 calls to sumTo(10) must add 2 * 10 * 55 = 1100 to the sink.");
    }

    // Nonsense inputs must be refused loudly at the door instead of producing an empty
    // array, an infinite loop, or a table of meaningless zeros.
    @Test
    void nonsenseArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> WarmupExperiment.timeBatches(0, 10, 10),
                "Zero batches is not a measurement.");
        assertThrows(IllegalArgumentException.class, () -> WarmupExperiment.timeBatches(2, 0, 10),
                "Zero calls per batch is not a measurement.");
        assertThrows(IllegalArgumentException.class, () -> WarmupExperiment.timeBatches(2, 10, -1),
                "A negative n is not a valid argument for sumTo.");
    }
}
