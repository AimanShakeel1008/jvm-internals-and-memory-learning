package com.corejava.jvm.gc;

/**
 * One reading of one garbage collector's running totals, taken at one instant.
 *
 * <p>The JVM keeps, for each of its collectors, two numbers that only ever go up: how
 * many collections it has run since the JVM started, and how many milliseconds those
 * collections took in total. Comparing two readings taken at different moments tells
 * you how many collections happened in between - which is how the project "watches"
 * garbage collection without parsing any logs.</p>
 *
 * @param name                 the collector's name as the JVM reports it, e.g.
 *                             {@code "G1 Young Generation"} - a HotSpot name, to verify
 *                             on your JDK
 * @param collectionCount      collections run since the JVM started
 * @param collectionTimeMillis total milliseconds spent in those collections
 */
public record CollectorReading(String name, long collectionCount, long collectionTimeMillis) {

    /**
     * Compact constructor: states what a valid reading looks like, once, so no other
     * method has to re-check.
     */
    public CollectorReading {
        // A reading with no name could not be matched against a later reading.
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A collector reading needs a name.");
        }
        // Totals that only ever grow can never be negative. (The JVM uses -1 to mean
        // "not available"; GcStats converts that to 0 before building a reading.)
        if (collectionCount < 0 || collectionTimeMillis < 0) {
            throw new IllegalArgumentException("Collector totals cannot be negative: count="
                    + collectionCount + ", time=" + collectionTimeMillis);
        }
    }

    /**
     * @param earlier a reading of the SAME collector taken before this one
     * @return how many collections this collector ran between the two readings
     */
    public long collectionsSince(CollectorReading earlier) {
        // Comparing two different collectors would produce a meaningless number that
        // looks perfectly plausible - the worst kind of bug - so refuse it outright.
        if (earlier == null || !earlier.name().equals(name)) {
            throw new IllegalArgumentException("Can only compare two readings of the same collector, got: "
                    + (earlier == null ? "null" : earlier.name()) + " and " + name);
        }
        // Totals only grow, so the difference is the number that happened in between.
        return collectionCount - earlier.collectionCount();
    }

    /**
     * @return one human-readable line, e.g. {@code "G1 Young Generation: 3 collections, 12 ms total"}
     */
    public String describe() {
        return name + ": " + collectionCount + " collections, " + collectionTimeMillis + " ms total";
    }
}
