package com.corejava.jvm.memory;

// ArrayList is the list we hold the allocated chunks in. Holding them is the whole
// trick: an object the program can still reach is an object the collector may not take.
import java.util.ArrayList;
import java.util.List;

/**
 * Fills the heap on purpose, either up to a budget (safe) or until it runs out (fatal).
 *
 * <p>Allocating memory is not enough to exhaust a heap - the garbage collector reclaims
 * anything the program can no longer reach, so a loop that creates a million arrays and
 * forgets each one immediately will happily run forever in a tiny heap. To actually fill
 * a heap you must <strong>keep</strong> what you allocate, which is what the list below
 * is for. That is also exactly what a real memory leak is: not "allocating too much",
 * but holding on to things you no longer need.</p>
 *
 * <p>Each chunk is a {@code byte[]}, chosen because a byte array's size in memory is
 * almost exactly the number you asked for: one byte per element, plus a small fixed
 * header. An array of objects would instead hold references to objects stored elsewhere,
 * making the arithmetic much harder to reason about.</p>
 */
public final class HeapFiller {

    // A default chunk size of 1 MB: big enough that filling a heap takes a sensible
    // number of steps, small enough that the last successful allocation before the
    // ceiling does not overshoot by much.
    public static final int DEFAULT_CHUNK_BYTES = 1024 * 1024;

    // Static-only tool: no instances.
    private HeapFiller() {
    }

    /**
     * Allocates and retains chunks until the requested number of bytes is held, then
     * releases all of them.
     *
     * <p>This is the SAFE method: it never approaches the heap ceiling, so it can be
     * called from a test. The memory is released when the method returns, because the
     * list holding the chunks is a local variable and dies with the frame.</p>
     *
     * @param budgetBytes how many bytes to hold at once; 0 allocates nothing
     * @param chunkBytes  the size of each individual allocation, must be positive
     * @return how many chunks were allocated
     */
    public static int fillUpTo(long budgetBytes, int chunkBytes) {
        // Guard at the door, naming the mistake. A zero or negative chunk size would
        // loop forever without this check, which is a far worse failure than an
        // exception: the program simply stops responding with no message at all.
        if (chunkBytes <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive, but was: " + chunkBytes);
        }
        if (budgetBytes < 0) {
            throw new IllegalArgumentException("Budget cannot be negative, but was: " + budgetBytes);
        }

        // The list is what keeps every chunk REACHABLE, and therefore uncollectable.
        // Remove this list and the whole method allocates nothing that survives.
        List<byte[]> retained = new ArrayList<>();

        // Count bytes held so far as a long: a few thousand megabytes overflows an int.
        long held = 0;

        // Allocate whole chunks until one more would exceed the budget. Using
        // "held + chunkBytes <= budgetBytes" rather than "held < budgetBytes" means we
        // never overshoot, so the count below is exactly predictable in a test.
        while (held + chunkBytes <= budgetBytes) {
            // A fresh array of that many bytes, every element zero. The JVM must find
            // room for it on the heap right now, or grow the heap to make room.
            retained.add(new byte[chunkBytes]);
            held += chunkBytes;
        }

        int count = retained.size();

        // Dropping the last reference is what makes every chunk collectable again.
        // Written explicitly, rather than relying on the method returning, so the
        // release is visible as a step - that release is the mechanism Lesson 04 studies.
        retained.clear();

        return count;
    }

    /**
     * Allocates and retains chunks until the JVM cannot give out any more, then reports
     * what happened.
     *
     * <p><strong>This method deliberately exhausts the heap.</strong> It is never called
     * from a test, because a test JVM that has just run out of memory is in no state to
     * run the tests after it. Run it from
     * {@code com.corejava.jvm.experiments.MemoryLimitsExperiment} with a small
     * {@code -Xmx}, where exhausting the heap is the entire point.</p>
     *
     * @param chunkBytes the size of each individual allocation, must be positive
     * @return what was held at the moment the JVM gave up, and the error it threw
     */
    public static ExhaustionReport fillUntilExhausted(int chunkBytes) {
        if (chunkBytes <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive, but was: " + chunkBytes);
        }

        List<byte[]> retained = new ArrayList<>();
        long held = 0;
        int count = 0;

        try {
            // No exit condition: the loop ends only when an allocation fails. Each
            // successful chunk is recorded BEFORE the next attempt, so the counters are
            // accurate at the instant the failure happens.
            while (true) {
                retained.add(new byte[chunkBytes]);
                held += chunkBytes;
                count++;
            }
        } catch (OutOfMemoryError expected) {
            // The first thing we do - before formatting anything, before building a
            // result - is let go of every chunk. Reporting an out-of-memory condition
            // needs memory, and if we still held the heap hostage, the report itself
            // would fail with a second OutOfMemoryError.
            retained.clear();
            retained = null;

            // getMessage() carries the part everyone reads in production: "Java heap
            // space", "Metaspace", "GC overhead limit exceeded" - each meaning a
            // different thing, as Section 3 of the lesson explains.
            String message = expected.getMessage();

            return new ExhaustionReport(held, count, expected.getClass().getName(),
                    // The message can be null, and "null" printed in a report is worse
                    // than a word that says what happened.
                    message == null ? "(no message)" : message);
        }
    }

    /**
     * What the JVM was holding at the moment it ran out of heap.
     *
     * @param retainedBytes how many bytes had been successfully allocated and kept
     * @param chunkCount    how many chunks that was
     * @param errorType     the exact class of the error thrown, e.g. {@code java.lang.OutOfMemoryError}
     * @param errorMessage  the error's message, e.g. {@code Java heap space}
     */
    public record ExhaustionReport(long retainedBytes, int chunkCount, String errorType, String errorMessage) {

        /**
         * @return one human-readable line summarising the exhaustion
         */
        public String describe() {
            return "held " + HeapSnapshot.toMegabytes(retainedBytes) + " MB in "
                    + chunkCount + " chunks before " + errorType + ": " + errorMessage;
        }
    }
}
