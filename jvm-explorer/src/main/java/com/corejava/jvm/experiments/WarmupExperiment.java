// A SEPARATE package for throwaway experiments, so the project's real capabilities
// (the classes directly in com.corejava.jvm) never get confused with code that only
// exists to make one lesson's point visible. The folder path on disk must match:
// src/main/java/com/corejava/jvm/experiments/.
package com.corejava.jvm.experiments;

// We time a method from the main project rather than inventing a new one, so what
// gets measured is exactly the method whose bytecode Lesson 01 disassembles.
import com.corejava.jvm.BytecodeSubject;

/**
 * EXPERIMENT (for learning only): shows the same Java method getting faster while
 * the program runs.
 *
 * <p>Nothing about the code changes between batches. What changes is HOW the JVM is
 * executing it: at first it walks the bytecode instruction by instruction (slow but
 * instant to start), and once the method has run enough times to look worth the
 * effort, the JVM compiles it to real machine code in the background and quietly
 * starts using that instead. The compiled version is dramatically faster, so later
 * batches finish sooner. This settling-down period is called warm-up.</p>
 *
 * <p><strong>This is deliberately NOT how you benchmark Java.</strong> Real
 * measurement uses a harness (JMH) that handles the traps this file ignores. It is
 * here because a crude, readable stopwatch makes the effect visible in twenty lines,
 * and Lesson 06 does the real thing properly.</p>
 */
public final class WarmupExperiment {

    // The results are added into this field so the JVM cannot decide the whole loop
    // is pointless and delete it. A compiler is allowed to remove work whose result
    // nobody uses; writing to a field that outlives the method is a side effect it
    // must keep. (This guard is crude - a determined optimiser can still outsmart it,
    // which is precisely why serious benchmarks use a real harness.)
    public static long sink;

    // Static-only class: no instances, so the constructor is private and empty.
    private WarmupExperiment() {
    }

    /**
     * Runs the same work several times and reports how long each batch took.
     *
     * @param batches       how many times to repeat the batch of work
     * @param callsPerBatch how many method calls make up one batch
     * @param n             the argument passed to the method being timed
     * @return one elapsed time in nanoseconds per batch, in order
     */
    public static long[] timeBatches(int batches, int callsPerBatch, int n) {
        // Reject nonsense up front with a message naming the offending value, rather
        // than returning a confusing empty array or looping zero times in silence.
        if (batches < 1 || callsPerBatch < 1 || n < 0) {
            throw new IllegalArgumentException(
                    "batches and callsPerBatch must be at least 1 and n must not be negative; got "
                            + batches + ", " + callsPerBatch + ", " + n);
        }

        // One slot per batch, filled in order, so the caller can compare batch 1
        // against batch 10 without us deciding for them what "faster" means.
        long[] elapsedNanos = new long[batches];

        // The outer loop is the experiment; the inner loop is the work.
        for (int batch = 0; batch < batches; batch++) {

            // nanoTime is a stopwatch, not a clock: its absolute value is meaningless,
            // but the DIFFERENCE between two readings is a reliable elapsed time. It
            // is immune to the system clock being adjusted mid-measurement, which is
            // exactly why currentTimeMillis is the wrong tool here.
            long start = System.nanoTime();

            // A local accumulator, so the inner loop touches a cheap stack slot rather
            // than the static field on every single iteration.
            long batchTotal = 0;

            for (int call = 0; call < callsPerBatch; call++) {
                // The method under test. sumTo itself contains a loop, so each call is
                // real work rather than something the JVM can fold into a constant.
                batchTotal += BytecodeSubject.sumTo(n);
            }

            // Publish the result so the work above cannot be optimised away entirely.
            sink += batchTotal;

            // Stop the clock and store the difference for this batch.
            elapsedNanos[batch] = System.nanoTime() - start;
        }

        return elapsedNanos;
    }

    /**
     * Prints a small warm-up table. Run with:
     * {@code mvn compile exec:java -Dexec.mainClass=com.corejava.jvm.experiments.WarmupExperiment}
     */
    public static void main(String[] args) {
        // A banner so this output is distinguishable from Maven's own chatter.
        System.out.println("=== warm-up experiment: the same method, timed ten times ===");

        // Ten batches is enough to see the settling-down; 200,000 calls per batch is
        // enough work per batch that the numbers are not swamped by timer noise;
        // sumTo(1000) is small enough to stay instant and big enough to be real work.
        long[] elapsedNanos = timeBatches(10, 200_000, 1000);

        // Print one line per batch, converting nanoseconds to milliseconds for reading
        // comfort (1 millisecond = 1,000,000 nanoseconds).
        for (int batch = 0; batch < elapsedNanos.length; batch++) {
            // %2d right-aligns the batch number, %8.2f gives two decimal places, so the
            // columns line up and the downward trend is visible at a glance.
            System.out.printf("batch %2d : %8.2f ms%n", batch + 1, elapsedNanos[batch] / 1_000_000.0);
        }

        // Printing the sink guarantees the accumulated value is genuinely used, and
        // doubles as a correctness check: sumTo(1000) is 500500, times 200,000 calls,
        // times 10 batches.
        System.out.println("sink (proves the work really happened): " + sink);

        // An honest footer: absolute timings differ on every machine, JDK build and
        // even between runs. The SHAPE (early batches slower) is the observation.
        System.out.println("Timings vary by machine and run; the shape - early batches slower - is the point.");
    }
}
