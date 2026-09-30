// Lesson 03's package: the JVM's runtime memory areas, made measurable.
package com.corejava.jvm.memory;

/**
 * One reading of the heap, taken at one instant.
 *
 * <p>The heap is the single shared pool of memory where every object created with
 * {@code new} lives. Three numbers describe it, and they are easy to confuse:</p>
 *
 * <ul>
 *   <li><strong>max</strong> - the ceiling the heap is allowed to grow to. Set by
 *       {@code -Xmx}; if you never set it, the JVM picks a default from the machine's
 *       RAM. Reaching this ceiling with nothing collectable left is what produces
 *       {@code OutOfMemoryError: Java heap space}.</li>
 *   <li><strong>committed</strong> - how much the JVM has actually taken from the
 *       operating system so far. It starts small and grows toward max on demand, which
 *       is why a fresh JVM does not reserve gigabytes it may never need.</li>
 *   <li><strong>used</strong> - how much of the committed memory currently holds live
 *       objects and garbage that has not been collected yet.</li>
 * </ul>
 *
 * <p>So the relationship is always {@code used <= committed <= max}. A record is the
 * right shape here because a snapshot is pure data: three numbers taken together, never
 * modified afterwards. The record gives us the fields, the constructor, {@code equals},
 * {@code hashCode} and {@code toString} for free.</p>
 *
 * @param maxBytes       the ceiling the heap may grow to, in bytes
 * @param committedBytes how much memory the JVM currently holds from the OS, in bytes
 * @param usedBytes      how much of that currently holds objects, in bytes
 */
public record HeapSnapshot(long maxBytes, long committedBytes, long usedBytes) {

    // How many bytes are in one megabyte. Named, because the raw expression
    // 1024 * 1024 appearing in several places is three chances to typo it.
    public static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

    /**
     * A compact constructor: it runs before the fields are assigned, so it is where a
     * record states what a VALID snapshot looks like. Rejecting nonsense here means no
     * other method in this class ever has to re-check.
     */
    public HeapSnapshot {
        // Byte counts cannot be negative. If one ever is, the bug is in whoever built
        // this snapshot, and we want to hear about it at that exact line.
        if (maxBytes < 0 || committedBytes < 0 || usedBytes < 0) {
            throw new IllegalArgumentException(
                    "Heap byte counts cannot be negative: max=" + maxBytes
                            + ", committed=" + committedBytes + ", used=" + usedBytes);
        }
        // The ordering rule that makes the three numbers mean anything. Used memory
        // living outside committed memory would be a contradiction, not a measurement.
        if (usedBytes > committedBytes) {
            throw new IllegalArgumentException(
                    "Used heap (" + usedBytes + ") cannot exceed committed heap (" + committedBytes + ").");
        }
    }

    /**
     * @return committed memory that is not currently holding anything, in bytes
     */
    public long freeBytes() {
        // The compact constructor already guaranteed used <= committed, so this
        // subtraction can never go negative - the check above is what makes it safe.
        return committedBytes - usedBytes;
    }

    /**
     * @return how full the heap is against its ceiling, as a percentage from 0 to 100
     */
    public double usedPercentOfMax() {
        // Guard against dividing by zero. A max of 0 should be impossible on a real
        // JVM, but "impossible" values are exactly what crashes reporting code.
        if (maxBytes == 0) {
            return 0.0;
        }
        // Multiply BEFORE dividing, and in floating point, so the answer keeps its
        // fractional part instead of collapsing to 0 through integer division.
        return (usedBytes * 100.0) / maxBytes;
    }

    /**
     * Converts a byte count to whole megabytes, for humans.
     *
     * @param bytes a non-negative byte count
     * @return the same amount in megabytes, rounded down
     */
    public static long toMegabytes(long bytes) {
        // The same door-guard habit the rest of the project uses: name the mistake at
        // the line that made it, rather than returning a quietly wrong number.
        if (bytes < 0) {
            throw new IllegalArgumentException("Cannot convert a negative byte count: " + bytes);
        }
        return bytes / BYTES_PER_MEGABYTE;
    }

    /**
     * @return one human-readable line describing this snapshot in megabytes
     */
    public String describe() {
        // String.format keeps the numbers lined up and rounds the percentage to one
        // decimal place; %d is a whole number, %.1f a number with one decimal.
        return String.format("heap: used %d MB of %d MB committed, ceiling %d MB (%.1f%% of ceiling)",
                toMegabytes(usedBytes), toMegabytes(committedBytes), toMegabytes(maxBytes),
                usedPercentOfMax());
    }
}
