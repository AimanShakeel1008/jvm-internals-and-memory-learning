// The experiments package: throwaway programs that make one lesson's point visible.
package com.corejava.jvm.experiments;

// Lesson 04's modules: the allocation patterns, the generation names and the counters.
import com.corejava.jvm.gc.AllocationPatterns;
import com.corejava.jvm.gc.CollectorReading;
import com.corejava.jvm.gc.GcStats;
import com.corejava.jvm.gc.Generation;

// Keeps the long-lived objects reachable until the very end of main, on purpose.
import java.lang.ref.Reference;
import java.util.List;
import java.util.OptionalLong;

/**
 * EXPERIMENT (for learning only): watch long-lived objects age through minor
 * collections and then get promoted into the old generation.
 *
 * <p>It allocates a small set of objects and keeps them, then produces garbage in rounds.
 * Each round ends at the moment the JVM runs a collection. After every round it prints
 * how full the survivor space and the old generation are. The kept objects should sit in
 * the survivor space for a few rounds and then move into the old generation in one jump.</p>
 *
 * <p>Run it with the Serial collector, whose heap layout matches the textbook picture
 * exactly (one Eden, two survivor spaces, one old generation), a fixed young-generation
 * size, and a low promotion age so the jump arrives quickly:</p>
 *
 * <pre>{@code
 * mvn compile
 * java -XX:+UseSerialGC -Xmx128m -Xmn32m -XX:MaxTenuringThreshold=3 -cp target/classes com.corejava.jvm.experiments.GenerationsExperiment
 * }</pre>
 *
 * <p>Every number it prints depends on the machine, the JDK build and the collector.
 * The SHAPE - survivor first, then a jump into old - is the observation.</p>
 */
public final class GenerationsExperiment {

    // 16 objects of 64 KB = 1 MB kept alive. Small enough to fit in a survivor space of
    // a 32 MB young generation (about 3.2 MB each, by HotSpot's default 8:1:1 split),
    // so the objects age normally instead of being pushed into old early.
    public static final int LONG_LIVED_COUNT = 16;

    // How many collections to watch. Enough to see the objects age past a threshold
    // of 3 and then stay put in the old generation for a few more rounds.
    public static final int ROUNDS = 6;

    // Static-only program: no instances.
    private GenerationsExperiment() {
    }

    /**
     * Formats one row of the table, kept separate so a test can check it without
     * running any collections.
     *
     * @param label       the round name, e.g. "start" or "3"
     * @param collections total collections so far
     * @param survivor    bytes used in the survivor space, or empty if this JVM has none
     * @param old         bytes used in the old generation, or empty if this JVM has none
     * @return one aligned line of the table
     */
    public static String formatRow(String label, long collections, OptionalLong survivor, OptionalLong old) {
        // %-6s pads the label to 6 characters, left-aligned; %12d right-aligns a number
        // in 12; %14s right-aligns text. Fixed widths make the jump easy to spot by eye.
        return String.format("%-6s %12d %14s %14s",
                label, collections, GcStats.formatKilobytes(survivor), GcStats.formatKilobytes(old));
    }

    /**
     * Runs the experiment and prints the table.
     */
    public static void main(String[] args) {
        System.out.println("=== generations experiment: watch objects age and promote ===");

        // Name the collectors first, so the learner can confirm which collector the
        // -XX flag actually selected - the table only means what the text says if it is
        // the Serial collector ("Copy" and "MarkSweepCompact").
        for (CollectorReading reading : GcStats.collectors()) {
            System.out.println("collector: " + reading.name());
        }

        // The long-lived objects. This local variable in main's frame is a GC root for
        // as long as main runs, so every object in the list stays reachable.
        List<byte[]> longLived = AllocationPatterns.retain(LONG_LIVED_COUNT, AllocationPatterns.DEFAULT_OBJECT_BYTES);
        System.out.println("keeping " + longLived.size() + " objects of "
                + (AllocationPatterns.DEFAULT_OBJECT_BYTES / 1024) + " KB reachable for the whole run");
        System.out.println();

        // The table header, then the starting point before any collection we caused.
        System.out.println(String.format("%-6s %12s %14s %14s", "round", "collections", "survivor", "old"));
        System.out.println(formatRow("start", GcStats.totalCollectionCount(),
                GcStats.usedBytes(Generation.SURVIVOR), GcStats.usedBytes(Generation.OLD)));

        for (int round = 1; round <= ROUNDS; round++) {
            // Make garbage until the JVM has to collect. Every collection copies the
            // reachable objects (ours) out of Eden or the survivor space and adds one to
            // their age; at the threshold, they are promoted instead.
            long garbage = AllocationPatterns.churnUntilCollection(
                    AllocationPatterns.DEFAULT_OBJECT_BYTES, AllocationPatterns.safeCapBytes());

            if (garbage < 0) {
                // Should not happen with a normal collector; say so plainly if it does.
                System.out.println("round " + round + ": no collection observed - is a no-op collector selected?");
                break;
            }

            System.out.println(formatRow(String.valueOf(round), GcStats.totalCollectionCount(),
                    GcStats.usedBytes(Generation.SURVIVOR), GcStats.usedBytes(Generation.OLD)));
        }

        System.out.println();
        System.out.println("Expected shape: about 1 MB sits in survivor for a few rounds, then moves to old in one jump.");

        // Keep the list reachable right up to this line. Without the fence the JIT could
        // decide longLived is dead after its last use above, and the objects might be
        // collected instead of promoted - which would quietly ruin the experiment.
        Reference.reachabilityFence(longLived);
    }
}
