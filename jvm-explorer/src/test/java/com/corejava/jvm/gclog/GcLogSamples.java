package com.corejava.jvm.gclog;

import java.util.List;

/**
 * Sample GC log lines shared by the gclog tests, in HotSpot's Java 21 format.
 *
 * <p>Kept in one place so every test reasons about the same log, and so the expected
 * numbers can be worked out by hand once, here, in the comments.</p>
 */
final class GcLogSamples {

    /**
     * A short G1 log. Worked out by hand:
     * <ul>
     *   <li>pauses: GC(0) young, GC(1) young, GC(2) Remark, GC(2) Cleanup, GC(3) full = 5;</li>
     *   <li>total pause: 2.512 + 3.100 + 1.200 + 0.100 + 15.300 = 22.212 ms; longest 15.3 ms;</li>
     *   <li>reclaimed: (25-3) + (40-38) + 0 + 0 + (30-2) = 52 MB;</li>
     *   <li>span: the last pause is at 1.000 s, so 22.212 ms of 1000 ms = 2.2212% paused;</li>
     *   <li>live-data estimate: 2 MB, the heap after the only full GC.</li>
     * </ul>
     */
    static final List<String> G1_LOG = List.of(
            "[0.004s][info][gc] Using G1",
            "[0.215s][info][gc] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 25M->3M(256M) 2.512ms",
            "[0.640s][info][gc] GC(1) Pause Young (Concurrent Start) (G1 Humongous Allocation) 40M->38M(64M) 3.100ms",
            "[0.641s][info][gc] GC(2) Concurrent Mark Cycle",
            "[0.660s][info][gc] GC(2) Pause Remark 41M->41M(64M) 1.200ms",
            "[0.680s][info][gc] GC(2) Pause Cleanup 41M->41M(64M) 0.100ms",
            "[0.690s][info][gc] GC(2) Concurrent Mark Cycle 49.012ms",
            "[1.000s][info][gc] GC(3) Pause Full (System.gc()) 30M->2M(64M) 15.300ms");

    // Not instantiable: a holder for shared constants.
    private GcLogSamples() {
    }
}
