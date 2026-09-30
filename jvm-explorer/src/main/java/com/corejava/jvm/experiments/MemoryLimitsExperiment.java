// The experiments package: throwaway programs that exist to make one lesson's point
// visible, kept separate from the project's real capabilities.
package com.corejava.jvm.experiments;

// Lesson 03's modules: the region report, the stack measurement, and the heap filler.
import com.corejava.jvm.memory.HeapFiller;
import com.corejava.jvm.memory.HeapSnapshot;
import com.corejava.jvm.memory.MemoryRegions;
import com.corejava.jvm.memory.StackDepthProbe;

/**
 * EXPERIMENT (for learning only): walks up to each of the JVM's two memory walls and
 * reads the sign on it.
 *
 * <p>By default it does the safe half: prints the memory regions and measures how deep
 * this thread's stack goes by overflowing it on purpose and catching the result. Both
 * of those leave the JVM perfectly healthy afterwards.</p>
 *
 * <p>Pass the argument {@code --oom} and it also does the fatal half: it fills the heap
 * until the JVM refuses, prints the exact error, and stops. Give the JVM a small heap so
 * this takes a second rather than a minute:</p>
 *
 * <pre>{@code
 * mvn compile
 * java -Xmx32m -cp target/classes com.corejava.jvm.experiments.MemoryLimitsExperiment --oom
 * }</pre>
 *
 * <p>To move the other wall instead, set a tiny thread stack and watch the depth number
 * collapse:</p>
 *
 * <pre>{@code
 * java -Xss256k -cp target/classes com.corejava.jvm.experiments.MemoryLimitsExperiment
 * }</pre>
 *
 * <p>Every number this prints depends on your machine, your JDK build, your garbage
 * collector and even how warm the JIT is. They are observations, not constants.</p>
 */
public final class MemoryLimitsExperiment {

    // The single argument this program understands. Named so the check below and the
    // test next door cannot drift apart through a typo in one of two copies.
    public static final String OOM_FLAG = "--oom";

    // Static-only program: no instances.
    private MemoryLimitsExperiment() {
    }

    /**
     * Decides whether the fatal half should run, from the command-line arguments.
     *
     * <p>Kept as its own method purely so it can be tested: a test can ask "would these
     * arguments blow up the heap?" without actually blowing up the heap.</p>
     *
     * @param args the command-line arguments, possibly empty, never null
     * @return true only if the caller explicitly asked for heap exhaustion
     */
    public static boolean exhaustionRequested(String[] args) {
        // Defensive: main() is always handed a real array by the JVM, but this method
        // is public and a caller could pass null.
        if (args == null) {
            return false;
        }
        for (String arg : args) {
            if (OOM_FLAG.equals(arg)) {
                return true;
            }
        }
        // Default to safe. A program that destroys its own JVM unless told not to would
        // be a trap, and defaults should never be the dangerous option.
        return false;
    }

    /**
     * Runs the safe half and, only if asked, the fatal half.
     *
     * @param args pass {@code --oom} to exhaust the heap at the end
     */
    public static void main(String[] args) {
        System.out.println("=== memory limits experiment: the two walls of a JVM ===");

        // ---- Where the memory is, before we touch anything -------------------
        System.out.println();
        System.out.println("-- regions, at startup --");
        System.out.println(MemoryRegions.describe());

        // ---- Wall 1: the stack, one frame at a time --------------------------
        // Predicted to be tens of thousands of frames with a default stack size, and a
        // much smaller number under -Xss256k. The exact value is your machine's to give.
        System.out.println();
        System.out.println("-- wall 1: the stack --");
        int depth = StackDepthProbe.measureMaxDepth();
        System.out.println("plain frames reached depth : " + depth);
        System.out.println("caught                     : " + StackDepthProbe.lastErrorType());

        // The same measurement with more locals per frame. Fewer, fatter frames fit in
        // the same fixed stack - which is the clearest possible evidence that a frame
        // is a real object with a size, not an abstract idea.
        int fatDepth = StackDepthProbe.measureMaxDepthWithLargerFrames();
        System.out.println("larger frames reached depth: " + fatDepth);

        // ---- A safe allocation, to see the heap number move ------------------
        // Eight megabytes held at once: far below any sane heap ceiling, and enough
        // that the "used" figure visibly changes while it is being held.
        System.out.println();
        System.out.println("-- a safe fill: 8 MB held at once --");
        int chunks = HeapFiller.fillUpTo(8L * HeapSnapshot.BYTES_PER_MEGABYTE, HeapFiller.DEFAULT_CHUNK_BYTES);
        System.out.println("allocated and released " + chunks + " chunks of 1 MB");
        System.out.println(MemoryRegions.heapSnapshot().describe());

        // ---- Wall 2: the heap, only on request -------------------------------
        if (!exhaustionRequested(args)) {
            System.out.println();
            System.out.println("-- wall 2: the heap --");
            System.out.println("Skipped. Re-run with " + OOM_FLAG + " (and a small -Xmx) to hit it:");
            System.out.println("  java -Xmx32m -cp target/classes "
                    + MemoryLimitsExperiment.class.getName() + " " + OOM_FLAG);
            return;
        }

        System.out.println();
        System.out.println("-- wall 2: the heap (this will end the program) --");

        // From here the JVM is being pushed to its ceiling on purpose. Everything that
        // needs to be printed afterwards is printed by describe(), which builds its
        // string only after the filler has released every chunk.
        HeapFiller.ExhaustionReport report = HeapFiller.fillUntilExhausted(HeapFiller.DEFAULT_CHUNK_BYTES);
        System.out.println(report.describe());
        System.out.println("The JVM survived because the filler let go of everything before reporting.");
    }
}
