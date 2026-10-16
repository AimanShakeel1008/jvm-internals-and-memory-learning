package com.corejava.jvm.gc;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the allocation patterns' arithmetic exactly, and their effect on the collector
 * only where the outcome is guaranteed.
 */
class AllocationPatternsTest {

    // Small objects, so these tests never put real pressure on the test JVM's heap.
    private static final int SMALL = 4 * 1024;

    // Whole objects only, never overshooting - the same promise as Lesson 03's filler.
    @Test
    void churnAllocatesWholeObjectsUpToTheBudget() {
        assertEquals(10, AllocationPatterns.churn(10L * SMALL, SMALL),
                "A budget of exactly ten objects must allocate exactly ten.");
        assertEquals(3, AllocationPatterns.churn(3L * SMALL + SMALL / 2, SMALL),
                "Three and a half objects of budget buys three, never four.");
        assertEquals(0, AllocationPatterns.churn(0, SMALL), "A zero budget allocates nothing.");
    }

    // The kept objects really are kept, really are the requested size, and really are
    // separate objects rather than one array referenced several times.
    @Test
    void retainHandsBackDistinctObjectsOfTheRightSize() {
        List<byte[]> kept = AllocationPatterns.retain(5, SMALL);

        assertEquals(5, kept.size(), "Asking for five objects must return five.");
        for (byte[] object : kept) {
            assertEquals(SMALL, object.length, "Every kept object must be the requested size.");
        }
        // Identity, not equality: two fresh arrays of zeros are equal in content but must
        // be two different objects on the heap.
        assertNotSame(kept.get(0), kept.get(1), "Each kept object must be a separate allocation.");
    }

    // This one IS guaranteed, which is why it can be a test: garbage beyond the heap's
    // ceiling has nowhere to go, so the JVM must collect before the cap is reached.
    @Test
    void enoughGarbageAlwaysForcesACollection() {
        long before = GcStats.totalCollectionCount();

        long garbage = AllocationPatterns.churnUntilCollection(
                AllocationPatterns.DEFAULT_OBJECT_BYTES, AllocationPatterns.safeCapBytes());

        assertTrue(garbage > 0,
                "Garbage beyond the heap ceiling must force a collection before the cap; got " + garbage);
        assertTrue(GcStats.totalCollectionCount() > before,
                "The collectors' total must have grown after a collection was observed.");
    }

    // Nonsense inputs fail at the door instead of looping forever or overflowing.
    @Test
    void impossibleArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> AllocationPatterns.churn(1024, 0),
                "An object size of zero would loop forever.");
        assertThrows(IllegalArgumentException.class, () -> AllocationPatterns.churn(-1, SMALL),
                "A negative budget is meaningless.");
        assertThrows(IllegalArgumentException.class, () -> AllocationPatterns.retain(-1, SMALL),
                "A negative count is meaningless.");
        assertThrows(IllegalArgumentException.class, () -> AllocationPatterns.churnUntilCollection(SMALL, 0),
                "A zero cap could never observe anything.");
        assertTrue(AllocationPatterns.safeCapBytes() > 0,
                "The safe cap must never overflow into a negative number.");
    }
}
