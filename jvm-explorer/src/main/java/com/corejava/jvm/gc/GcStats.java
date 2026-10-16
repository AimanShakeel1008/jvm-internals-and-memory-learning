package com.corejava.jvm.gc;

// One bean per garbage collector the JVM is running, each counting its own collections.
import java.lang.management.GarbageCollectorMXBean;
// The front door to every "MX bean" the JVM keeps about itself (met in Lesson 03).
import java.lang.management.ManagementFactory;
// One named memory pool, such as "G1 Eden Space".
import java.lang.management.MemoryPoolMXBean;
// Tells heap pools apart from non-heap ones like Metaspace.
import java.lang.management.MemoryType;
// A used/committed/max reading for one pool.
import java.lang.management.MemoryUsage;

import java.util.ArrayList;
import java.util.List;
// "A number, or nothing" - honest when a pool does not exist on this JVM.
import java.util.OptionalLong;

/**
 * Reads the garbage collector's own counters and the size of each generation, so the
 * project can watch collections happen instead of taking them on faith.
 *
 * <p>Everything here is an observation of one JVM at one moment. Collector names and
 * pool names depend on which collector is running (the default since Java 9 is G1), and
 * every figure depends on the machine - which is why the tests assert relationships,
 * never sizes.</p>
 */
public final class GcStats {

    // One kilobyte, named so the conversion below explains itself.
    private static final long BYTES_PER_KILOBYTE = 1024L;

    // Static-only helper: nothing to construct.
    private GcStats() {
    }

    /**
     * @return one reading per collector this JVM is running, in the JVM's own order
     */
    public static List<CollectorReading> collectors() {
        List<CollectorReading> readings = new ArrayList<>();

        // Each running collector exposes one bean. G1, for example, typically reports a
        // young-generation collector and an old-generation one (names to verify on your
        // JDK); the Serial collector reports "Copy" and "MarkSweepCompact".
        for (GarbageCollectorMXBean collector : ManagementFactory.getGarbageCollectorMXBeans()) {
            // The specification allows -1 to mean "this JVM does not track that number".
            // Treating it as 0 keeps the arithmetic safe; it never happens on HotSpot.
            long count = Math.max(0L, collector.getCollectionCount());
            long time = Math.max(0L, collector.getCollectionTime());
            readings.add(new CollectorReading(collector.getName(), count, time));
        }

        // An unmodifiable copy: a caller cannot rewrite the observation after the fact.
        return List.copyOf(readings);
    }

    /**
     * @return collections run by ALL collectors since the JVM started
     */
    public static long totalCollectionCount() {
        // Summed across collectors, because "has ANY collection happened since I last
        // looked?" is the question the allocation patterns need answered.
        long total = 0;
        for (CollectorReading reading : collectors()) {
            total += reading.collectionCount();
        }
        return total;
    }

    /**
     * @param generation which area of the heap to measure
     * @return the bytes currently used in that area, or empty if this JVM exposes no
     *         heap pool for it (for example, a non-generational collector)
     */
    public static OptionalLong usedBytes(Generation generation) {
        // The same door-guard habit as the rest of the project.
        if (generation == null) {
            throw new IllegalArgumentException("Cannot measure a null generation.");
        }

        long total = 0;
        boolean found = false;

        // Search by name rather than by position: the pool list differs per collector
        // and per JDK version, exactly as Lesson 03's Metaspace lookup did.
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            // Only heap pools count. Without this check a non-heap pool whose name
            // happened to contain "Old" would be added to the old generation.
            if (pool.getType() == MemoryType.HEAP && generation.matches(pool.getName())) {
                // getUsage() may be null for a pool that is not currently valid.
                MemoryUsage usage = pool.getUsage();
                if (usage != null) {
                    total += usage.getUsed();
                    found = true;
                }
            }
        }

        // Empty says "no such pool here" - a different statement from "zero bytes used".
        return found ? OptionalLong.of(total) : OptionalLong.empty();
    }

    /**
     * @param bytes a byte count that may be absent
     * @return the count in whole kilobytes, or "n/a" when the pool did not exist
     */
    public static String formatKilobytes(OptionalLong bytes) {
        // Kilobytes rather than megabytes: a survivor space is often only a few MB, and
        // the movements this lesson watches would vanish in megabyte rounding.
        if (bytes == null || bytes.isEmpty()) {
            return "n/a";
        }
        return (bytes.getAsLong() / BYTES_PER_KILOBYTE) + " KB";
    }

    /**
     * @return a multi-line report: every collector's totals, then each generation's size
     */
    public static String describe() {
        StringBuilder report = new StringBuilder();

        // One line per collector, so the young and old collectors can be told apart by
        // the learner's own eyes rather than by a guess written into this code.
        for (CollectorReading reading : collectors()) {
            report.append("collector  ").append(reading.describe()).append(System.lineSeparator());
        }

        // Then the three areas a minor collection moves objects between.
        report.append("generations  eden ").append(formatKilobytes(usedBytes(Generation.EDEN)))
                .append(" | survivor ").append(formatKilobytes(usedBytes(Generation.SURVIVOR)))
                .append(" | old ").append(formatKilobytes(usedBytes(Generation.OLD)));

        return report.toString();
    }
}
