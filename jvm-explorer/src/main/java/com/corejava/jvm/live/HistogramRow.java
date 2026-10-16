package com.corejava.jvm.live;

/**
 * One row of a class histogram, as printed by {@code jcmd <pid> GC.class_histogram} or
 * {@code jmap -histo <pid>}:
 *
 * <pre>
 *    3:           640       20992000  com.corejava.jvm.live.LeakyRegistry$LeakedSession
 *    ^            ^         ^         ^
 *    rank         instances bytes     class (with " (module@version)" after it for JDK classes)
 * </pre>
 *
 * @param rank      position in the histogram, largest by bytes first
 * @param instances how many objects of this class are on the heap
 * @param bytes     their total SHALLOW size - the objects themselves, not what they point at
 * @param className the class name as the JVM writes it, e.g. {@code [B} for byte[]
 * @param module    the module the class comes from, or "" for classes on the class path
 */
public record HistogramRow(int rank, long instances, long bytes, String className, String module) {

    /**
     * Compact constructor: a valid row, once.
     */
    public HistogramRow {
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("A histogram row needs a class name.");
        }
        if (rank < 0 || instances < 0 || bytes < 0) {
            throw new IllegalArgumentException("Histogram numbers cannot be negative: rank=" + rank
                    + ", instances=" + instances + ", bytes=" + bytes);
        }
        module = module == null ? "" : module;
    }

    /**
     * @return the class name as a Java programmer would write it - {@code [B} becomes
     *         {@code byte[]}, {@code [Ljava.lang.Object;} becomes {@code java.lang.Object[]}
     */
    public String readableClassName() {
        return ClassHistogram.readableName(className);
    }
}
