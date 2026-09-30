package com.corejava.jvm.memory;

// The JVM's own self-monitoring API, in the java.management module. ManagementFactory
// is the front door: it hands out "MX beans", which are plain objects the JVM keeps
// about itself, describing memory, threads, class loading and more.
import java.lang.management.ManagementFactory;
// One memory pool - a single named region such as "Metaspace" or "G1 Eden Space".
import java.lang.management.MemoryPoolMXBean;
// A used/committed/max reading for one area, which is what a pool hands back.
import java.lang.management.MemoryUsage;

// OptionalLong is "a long, or nothing" without using -1 as a secret code for absent.
import java.util.OptionalLong;

/**
 * Reports the JVM's runtime memory areas as numbers you can print and assert on.
 *
 * <p>A running JVM divides the memory it takes from the operating system into areas
 * with different jobs. Two of them matter for this lesson:</p>
 *
 * <ul>
 *   <li>the <strong>heap</strong>: one shared pool holding every object created with
 *       {@code new}, for every thread;</li>
 *   <li><strong>non-heap</strong> memory, which is everything else the JVM needs -
 *       most importantly <strong>Metaspace</strong>, where the description of each
 *       loaded class is stored, and the code cache, where JIT-compiled machine code
 *       goes (Lesson 06).</li>
 * </ul>
 *
 * <p>The per-thread stacks are deliberately NOT reported here: the JVM exposes no
 * portable API for "how big is my stack", which is exactly why {@link StackDepthProbe}
 * measures it by experiment instead of by asking.</p>
 */
public final class MemoryRegions {

    // The name HotSpot gives the memory pool holding class metadata. Other JVM
    // implementations may name it differently or not have it at all, so every use of
    // this constant is written to cope with the pool being absent.
    public static final String METASPACE_POOL_NAME = "Metaspace";

    // Static-only helper: nothing to construct.
    private MemoryRegions() {
    }

    /**
     * Takes one reading of the heap using {@link Runtime}, the oldest and most portable
     * way to ask - available in every Java version, with no extra modules needed.
     *
     * @return the heap's ceiling, committed size and used size at this instant
     */
    public static HeapSnapshot heapSnapshot() {
        // getRuntime() returns the single object representing THIS running JVM.
        Runtime runtime = Runtime.getRuntime();

        // The ceiling the heap may grow to: what -Xmx sets, or a default derived from
        // the machine's RAM when -Xmx is absent.
        long max = runtime.maxMemory();

        // What the JVM currently holds from the OS. Read BEFORE freeMemory() so the
        // two readings are as close together in time as we can make them.
        long committed = runtime.totalMemory();

        // What is currently unused inside that committed memory. Between these two
        // calls a garbage collection can happen on another thread, which is why the
        // subtraction below is clamped rather than trusted blindly.
        long free = runtime.freeMemory();

        // used = committed - free, but defended in both directions: a GC landing
        // between the two reads could otherwise produce a negative number, or one
        // larger than committed. Clamping turns a rare race into a harmless rounding
        // error instead of an exception thrown from a reporting method.
        long used = Math.min(committed, Math.max(0L, committed - free));

        return new HeapSnapshot(max, committed, used);
    }

    /**
     * @return how much non-heap memory the JVM is currently using, in bytes
     */
    public static long nonHeapUsedBytes() {
        // getMemoryMXBean() is the JVM's summary view of memory. getNonHeapMemoryUsage()
        // lumps together everything that is not the object heap - Metaspace, the JIT's
        // code cache, compressed class space - into one used/committed/max reading.
        MemoryUsage nonHeap = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();

        // getUsed() is documented to be non-negative, so this needs no clamping.
        return nonHeap.getUsed();
    }

    /**
     * Looks for the Metaspace pool by name and reports how much of it is in use.
     *
     * @return the bytes of class metadata currently stored, or empty if this JVM has no
     *         pool by that name (Metaspace is a HotSpot concept, not a Java requirement)
     */
    public static OptionalLong metaspaceUsedBytes() {
        // Every memory pool the JVM chooses to expose, as a list we can search. The
        // list's contents differ by JVM, by garbage collector, and by version - which
        // is precisely why we search by name instead of indexing into it.
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (METASPACE_POOL_NAME.equals(pool.getName())) {
                // getUsage() can legitimately return null for a pool that is not
                // currently valid, so we check before touching it.
                MemoryUsage usage = pool.getUsage();
                if (usage != null) {
                    return OptionalLong.of(usage.getUsed());
                }
            }
        }
        // Not found. Empty says "this JVM did not offer that pool", which is a
        // different statement from "zero bytes are in use".
        return OptionalLong.empty();
    }

    /**
     * @return how many classes this JVM has loaded since it started and not unloaded
     */
    public static long loadedClassCount() {
        // The class-loading MX bean counts exactly what Lesson 02 watched scroll past
        // with -verbose:class. Printing it here connects the two lessons: every one of
        // those classes has a description sitting in Metaspace right now.
        return ManagementFactory.getClassLoadingMXBean().getLoadedClassCount();
    }

    /**
     * @return a multi-line report of every region this class can see
     */
    public static String describe() {
        // A StringBuilder because we are gluing several lines together; using + in a
        // loop would build and throw away a new String each time (Lesson 01's
        // makeConcatWithConstants does the same job for simple one-line cases).
        StringBuilder report = new StringBuilder();

        // The heap first: it is the region every later lesson is about.
        report.append(heapSnapshot().describe()).append(System.lineSeparator());

        // Then everything that is not the heap, as one number.
        report.append("non-heap: ")
                .append(HeapSnapshot.toMegabytes(nonHeapUsedBytes()))
                .append(" MB used (class metadata, JIT code cache, and more)")
                .append(System.lineSeparator());

        // Then Metaspace specifically, if this JVM exposes it - written so the report
        // stays readable on a JVM that does not, rather than failing.
        OptionalLong metaspace = metaspaceUsedBytes();
        if (metaspace.isPresent()) {
            report.append("Metaspace: ")
                    .append(HeapSnapshot.toMegabytes(metaspace.getAsLong()))
                    .append(" MB used, holding ")
                    .append(loadedClassCount())
                    .append(" loaded classes");
        } else {
            report.append("Metaspace: not exposed as a pool by this JVM; ")
                    .append(loadedClassCount())
                    .append(" classes are loaded");
        }

        return report.toString();
    }
}
