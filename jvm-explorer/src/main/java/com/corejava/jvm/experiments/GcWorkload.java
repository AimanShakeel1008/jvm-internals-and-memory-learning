// The experiments package: throwaway programs that make one lesson's point visible.
package com.corejava.jvm.experiments;

// Lesson 04's allocation patterns supply the garbage.
import com.corejava.jvm.gc.AllocationPatterns;

import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.List;

/**
 * EXPERIMENT (for learning only): a small, repeatable workload whose only purpose is to
 * give a garbage collector something to do, so it writes an interesting GC log.
 *
 * <p>It produces a lot of short-lived garbage (many minor collections), slowly grows a
 * set of long-lived objects (promotion into the old generation), and finally asks for
 * one full collection. Run it once per collector, each writing its own log, then compare
 * the summaries:</p>
 *
 * <pre>{@code
 * mvn compile
 * java -Xmx64m -Xlog:gc:file=gc.log -cp target/classes com.corejava.jvm.experiments.GcWorkload
 * java -cp target/classes com.corejava.jvm.gclog.GcLogSummaryTool gc.log
 * }</pre>
 */
public final class GcWorkload {

    // Garbage is produced in steps of 4 MB; between steps the long-lived set may grow.
    public static final long GARBAGE_STEP_BYTES = 4L * 1024 * 1024;

    // The default run: 512 MB of garbage in total, and 400 long-lived objects of 64 KB
    // (about 25 MB) - enough, in a 64 MB heap, to make the old generation work too.
    public static final long DEFAULT_GARBAGE_BYTES = 512L * 1024 * 1024;
    public static final int DEFAULT_KEPT_OBJECTS = 400;

    // Static-only program: no instances.
    private GcWorkload() {
    }

    /**
     * Produces garbage while growing a set of long-lived objects.
     *
     * @param garbageBytes total garbage to produce, zero or more
     * @param keptObjects  the most long-lived objects to accumulate, zero or more
     * @return how many long-lived objects were actually kept by the end
     */
    public static int run(long garbageBytes, int keptObjects) {
        if (garbageBytes < 0 || keptObjects < 0) {
            throw new IllegalArgumentException("Garbage and kept counts cannot be negative: "
                    + garbageBytes + ", " + keptObjects);
        }

        // A local variable in this live frame: a GC root for as long as run() runs, so
        // everything added to it survives collections, ages, and gets promoted.
        List<byte[]> kept = new ArrayList<>();
        long produced = 0;

        while (produced < garbageBytes) {
            // One step of pure garbage: each object dies the instant the next replaces it.
            AllocationPatterns.churn(GARBAGE_STEP_BYTES, AllocationPatterns.DEFAULT_OBJECT_BYTES);
            produced += GARBAGE_STEP_BYTES;

            // Grow the long-lived set slowly, so it has time to age through minor
            // collections before it reaches the old generation.
            if (kept.size() < keptObjects) {
                kept.add(new byte[AllocationPatterns.DEFAULT_OBJECT_BYTES]);
            }
        }

        int count = kept.size();
        // Keep the list reachable right up to here, so the JIT cannot release it early
        // and quietly change what the collector sees (Lesson 04).
        Reference.reachabilityFence(kept);
        return count;
    }

    /**
     * Runs the default workload, then requests one full collection so the log has one.
     */
    public static void main(String[] args) {
        System.out.println("=== GC workload: giving the collector something to log ===");
        int kept = run(DEFAULT_GARBAGE_BYTES, DEFAULT_KEPT_OBJECTS);
        System.out.println("produced " + (DEFAULT_GARBAGE_BYTES / (1024 * 1024)) + " MB of garbage, kept "
                + kept + " long-lived objects until the end of the run");

        // One explicit request at the end, purely so every log contains a full
        // collection to read. Never do this in application code (Lesson 04).
        System.gc();
        System.out.println("requested one full collection; now summarise the log:");
        System.out.println("  java -cp target/classes com.corejava.jvm.gclog.GcLogSummaryTool gc.log");
    }
}
