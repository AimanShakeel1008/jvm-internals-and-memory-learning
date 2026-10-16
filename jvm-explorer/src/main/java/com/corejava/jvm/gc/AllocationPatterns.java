package com.corejava.jvm.gc;

// The kept objects are returned in a list: holding the list is what keeps them reachable.
import java.util.ArrayList;
import java.util.List;

/**
 * Allocates objects in the two patterns the generational hypothesis is about: objects
 * that die immediately, and objects that are kept.
 *
 * <p>Every object is a {@code byte[]}, chosen (as in Lesson 03's filler) because its
 * size in memory is almost exactly the number requested, so the arithmetic in the
 * experiment's output stays easy to follow.</p>
 */
public final class AllocationPatterns {

    // 64 KB per object: small enough to be an ordinary young-generation allocation, big
    // enough that filling Eden takes hundreds of allocations rather than millions.
    public static final int DEFAULT_OBJECT_BYTES = 64 * 1024;

    // Every short-lived object is written here, then overwritten by the next one.
    // Writing it to a field that outlives the method makes the object ESCAPE, so the JIT
    // may not optimise the allocation away (escape analysis, Lesson 06) - without this,
    // a loop of unused "new byte[...]" could legally allocate nothing at all.
    // "volatile" so every write really happens, rather than being merged together.
    private static volatile Object sink;

    // Static-only tool: no instances.
    private AllocationPatterns() {
    }

    /**
     * Allocates short-lived objects that become garbage immediately.
     *
     * @param totalBytes  how many bytes of garbage to produce in total
     * @param objectBytes the size of each object, must be positive
     * @return how many objects were allocated
     */
    public static long churn(long totalBytes, int objectBytes) {
        // Guard at the door: a zero size would loop forever, which is far worse than an
        // exception because nothing tells you anything is wrong.
        if (objectBytes <= 0) {
            throw new IllegalArgumentException("Object size must be positive, but was: " + objectBytes);
        }
        if (totalBytes < 0) {
            throw new IllegalArgumentException("Total bytes cannot be negative, but was: " + totalBytes);
        }

        long count = 0;
        long allocated = 0;

        // Whole objects only, never overshooting - so the count is exactly predictable.
        while (allocated + objectBytes <= totalBytes) {
            // The previous object loses its only reference on this very line: it is now
            // unreachable, and therefore garbage, the instant the new one replaces it.
            sink = new byte[objectBytes];
            allocated += objectBytes;
            count++;
        }

        // Drop the last one too, so this method leaves nothing reachable behind.
        sink = null;
        return count;
    }

    /**
     * Allocates objects and hands them all back, still reachable.
     *
     * <p>As long as the caller holds the returned list, every object in it is reachable
     * from a GC root (the caller's local variable), so no collection may reclaim them.
     * They survive minor collections, grow older, and are eventually promoted.</p>
     *
     * @param count       how many objects to keep, zero or more
     * @param objectBytes the size of each object, must be positive
     * @return the kept objects
     */
    public static List<byte[]> retain(int count, int objectBytes) {
        if (count < 0) {
            throw new IllegalArgumentException("Count cannot be negative, but was: " + count);
        }
        if (objectBytes <= 0) {
            throw new IllegalArgumentException("Object size must be positive, but was: " + objectBytes);
        }

        // Sized up front so the list does not reallocate its own backing array as it
        // grows - which would add garbage of our own making to the measurement.
        List<byte[]> kept = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            kept.add(new byte[objectBytes]);
        }
        return kept;
    }

    /**
     * Allocates garbage until the JVM runs at least one garbage collection.
     *
     * <p>This is how the project makes a collection happen without calling
     * {@code System.gc()}: keep allocating, and sooner or later Eden fills up and the
     * JVM has no choice. It is guaranteed to happen before the garbage produced exceeds
     * the heap's ceiling, because there is nowhere else for new objects to go.</p>
     *
     * @param objectBytes the size of each garbage object, must be positive
     * @param capBytes    give up after this much garbage, must be positive
     * @return the bytes of garbage allocated before a collection was observed, or -1 if
     *         the cap was reached first
     */
    public static long churnUntilCollection(int objectBytes, long capBytes) {
        if (objectBytes <= 0) {
            throw new IllegalArgumentException("Object size must be positive, but was: " + objectBytes);
        }
        if (capBytes <= 0) {
            throw new IllegalArgumentException("Cap must be positive, but was: " + capBytes);
        }

        // The collectors' combined total right now. A larger number later means at
        // least one collection ran in between.
        long before = GcStats.totalCollectionCount();
        long allocated = 0;

        while (allocated < capBytes) {
            // Each new object makes the previous one garbage, exactly as in churn().
            sink = new byte[objectBytes];
            allocated += objectBytes;

            // Checked after every allocation: the counters are updated when a collection
            // finishes, so the first allocation after it is where we notice.
            if (GcStats.totalCollectionCount() > before) {
                sink = null;
                return allocated;
            }
        }

        // No collection within the cap. On a normal JVM with a sensible cap this does
        // not happen; -1 says so plainly rather than returning a misleading number.
        sink = null;
        return -1;
    }

    /**
     * @return a garbage cap that is guaranteed to force a collection: four times the
     *         heap ceiling, without overflowing when the ceiling is reported as unlimited
     */
    public static long safeCapBytes() {
        long max = Runtime.getRuntime().maxMemory();
        // maxMemory() may return Long.MAX_VALUE to mean "no limit", and multiplying
        // that by four would overflow into a negative number. Saturate instead.
        if (max > Long.MAX_VALUE / 4) {
            return Long.MAX_VALUE;
        }
        return max * 4;
    }
}
