package com.corejava.jvm.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the SAFE half of the heap filler.
 *
 * <p>{@code fillUntilExhausted} is never called from here on purpose. A JVM that has
 * just exhausted its heap is not a reliable place to run the remaining tests, and a test
 * suite that sometimes dies halfway through teaches a team to ignore red builds. The
 * exhaustion path is exercised by hand, from the experiment, with a small
 * {@code -Xmx}.</p>
 */
class HeapFillerTest {

    // Small enough that these tests never stress the heap: 64 KB chunks, a few of them.
    private static final int SMALL_CHUNK = 64 * 1024;

    // The arithmetic the method promises: whole chunks only, never overshooting.
    @Test
    void aBudgetIsFilledWithWholeChunks() {
        assertEquals(4, HeapFiller.fillUpTo(4L * SMALL_CHUNK, SMALL_CHUNK),
                "A budget of exactly four chunks must allocate exactly four.");
    }

    // The overshoot rule, which is the part that is easy to get wrong by one: a budget
    // that does not divide evenly stops BELOW it rather than stepping over it.
    @Test
    void aBudgetThatDoesNotDivideEvenlyStopsShort() {
        // Three and a half chunks of budget buys three chunks, not four.
        long budget = (3L * SMALL_CHUNK) + (SMALL_CHUNK / 2);

        assertEquals(3, HeapFiller.fillUpTo(budget, SMALL_CHUNK),
                "A partial chunk must not be allocated - the filler never exceeds its budget.");
    }

    // The boundary case. Asking for nothing must be a no-op, not an error and not one
    // chunk "just to be safe".
    @Test
    void aZeroBudgetAllocatesNothing() {
        assertEquals(0, HeapFiller.fillUpTo(0, SMALL_CHUNK),
                "A budget of zero bytes must allocate no chunks at all.");
    }

    // The guard that matters most: a chunk size of zero would loop forever, holding the
    // build open with no message. Failing fast is far better than hanging.
    @Test
    void impossibleArgumentsAreRejectedRatherThanLoopingForever() {
        assertThrows(IllegalArgumentException.class, () -> HeapFiller.fillUpTo(1024, 0),
                "A chunk size of zero would never finish and must be rejected.");
        assertThrows(IllegalArgumentException.class, () -> HeapFiller.fillUpTo(1024, -1),
                "A negative chunk size is meaningless and must be rejected.");
        assertThrows(IllegalArgumentException.class, () -> HeapFiller.fillUpTo(-1, SMALL_CHUNK),
                "A negative budget is meaningless and must be rejected.");
        assertThrows(IllegalArgumentException.class, () -> HeapFiller.fillUntilExhausted(0),
                "The exhausting version must guard its chunk size too.");
    }

    // The report is what the learner reads after a deliberate out-of-memory, so its
    // formatting is checked here without any memory being exhausted to produce it.
    @Test
    void theExhaustionReportReadsAsASentence() {
        HeapFiller.ExhaustionReport report = new HeapFiller.ExhaustionReport(
                32L * HeapSnapshot.BYTES_PER_MEGABYTE, 32, "java.lang.OutOfMemoryError", "Java heap space");

        String described = report.describe();

        assertTrue(described.contains("32 MB"), "The report must state the megabytes held: " + described);
        assertTrue(described.contains("32 chunks"), "The report must state the chunk count: " + described);
        assertTrue(described.contains("Java heap space"),
                "The report must carry the JVM's own message, which is the part people search for: " + described);
    }
}
