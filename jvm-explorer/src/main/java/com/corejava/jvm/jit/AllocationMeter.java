package com.corejava.jvm.jit;

// The standard JVM monitoring entry point (Lesson 03).
import java.lang.management.ManagementFactory;
// "A number, or nothing" - on a JVM that cannot count allocations, say so honestly.
import java.util.OptionalDouble;
import java.util.OptionalLong;
// A function from an int to a long: "run this many calls, give back a result".
import java.util.function.IntToLongFunction;

/**
 * Measures how many bytes the current thread allocates while some work runs.
 *
 * <p>HotSpot keeps a running total of the bytes each thread has allocated. The standard
 * {@code java.lang.management.ThreadMXBean} does not expose it, but HotSpot's extended
 * version, {@code com.sun.management.ThreadMXBean} (in the jdk.management module), does.
 * Reading the total before and after a piece of work gives the bytes that work allocated
 * - which is how the project can SEE escape analysis remove an allocation.</p>
 *
 * <p>This is a HotSpot extension, not part of the Java specification, so every method
 * here copes with it being unavailable.</p>
 */
public final class AllocationMeter {

    // The result of every measured loop is written here, so the JIT cannot decide the
    // loop's result is unused and delete the loop - which would make the measurement
    // say "0 bytes" for the wrong reason.
    private static volatile long sink;

    // Static-only tool: no instances.
    private AllocationMeter() {
    }

    /**
     * @return the total bytes this thread has allocated since it started, or empty if
     *         this JVM cannot report it
     */
    public static OptionalLong currentThreadAllocatedBytes() {
        // "instanceof ... com.sun.management.ThreadMXBean sunBean" both checks the type and
        // names the converted variable in one step (pattern matching, Java 16 and later).
        if (ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean sunBean
                && sunBean.isThreadAllocatedMemorySupported()) {
            // Counting can be switched off; switch it on if needed, once.
            if (!sunBean.isThreadAllocatedMemoryEnabled()) {
                sunBean.setThreadAllocatedMemoryEnabled(true);
            }
            // Returns a plain long and allocates nothing itself, so reading it does not
            // disturb the measurement it is part of.
            return OptionalLong.of(sunBean.getCurrentThreadAllocatedBytes());
        }
        return OptionalLong.empty();
    }

    /**
     * @param work the work to measure; must not be null
     * @return the bytes this thread allocated while running it, or empty if unsupported
     */
    public static OptionalLong allocatedBytesDuring(Runnable work) {
        if (work == null) {
            throw new IllegalArgumentException("Cannot measure null work.");
        }
        OptionalLong before = currentThreadAllocatedBytes();
        work.run();
        OptionalLong after = currentThreadAllocatedBytes();

        if (before.isEmpty() || after.isEmpty()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(after.getAsLong() - before.getAsLong());
    }

    /**
     * Runs a loop of the given size and reports the bytes allocated per call.
     *
     * @param calls how many calls the loop makes, at least 1
     * @param loop  the loop to run, e.g. {@code JitSubjects::distanceLoop}; must not be null
     * @return average bytes allocated per call, or empty if unsupported
     */
    public static OptionalDouble bytesPerCall(int calls, IntToLongFunction loop) {
        if (calls < 1) {
            throw new IllegalArgumentException("Need at least one call to average over, but was: " + calls);
        }
        if (loop == null) {
            throw new IllegalArgumentException("Cannot measure a null loop.");
        }

        // A method reference like JitSubjects::distanceLoop is created once and reused, so
        // passing it here does not itself allocate inside the measured region.
        OptionalLong bytes = allocatedBytesDuring(() -> sink = loop.applyAsLong(calls));

        if (bytes.isEmpty()) {
            return OptionalDouble.empty();
        }
        // Floating-point division keeps fractions like 0.3 bytes per call visible.
        return OptionalDouble.of((double) bytes.getAsLong() / calls);
    }

    /**
     * @param perCall a bytes-per-call figure that may be absent
     * @return it formatted for printing, or a plain explanation if absent
     */
    public static String format(OptionalDouble perCall) {
        if (perCall == null || perCall.isEmpty()) {
            return "n/a (this JVM cannot report per-thread allocation)";
        }
        return String.format("%.1f bytes per call", perCall.getAsDouble());
    }
}
