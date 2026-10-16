package com.corejava.jvm.jit;

import java.util.List;

/**
 * Sample {@code -XX:+PrintCompilation} output shared by the jit tests, in the HotSpot
 * Java 21 column layout, mixed with the experiment's own marker lines exactly as a
 * redirected file would contain them.
 */
final class JitLogSamples {

    /**
     * Worked out by hand:
     * <ul>
     *   <li>compilation lines (not retirements): ids 1, 31, 35, 36, 40, 41 = 6;</li>
     *   <li>by tier: tier 1 = 1 (id 41), tier 3 = 4 (ids 1, 31, 35, 40), tier 4 = 1 (id 36);</li>
     *   <li>OSR: 1 (id 35); made not entrant: 1, a tier 3 version (id 31), so 0 deoptimizations;</li>
     *   <li>the native wrapper (id 5) and the two marker lines are not compilation lines;</li>
     *   <li>first event at 45 ms, last at 131 ms.</li>
     * </ul>
     */
    static final List<String> LOG = List.of(
            "=== PHASE 1: cold ===",
            "     45    1       3       java.lang.String::hashCode (60 bytes)",
            "     60    5     n 0       java.lang.invoke.MethodHandle::linkToStatic(LLLLLLL)L (native)   (static)",
            "=== PHASE 2: warm ===",
            "    112   31       3       com.corejava.jvm.jit.JitSubjects::sumOfSquares (25 bytes)",
            "    118   35 %     3       com.corejava.jvm.jit.JitSubjects::longRunningLoop @ 4 (27 bytes)",
            "    120   36       4       com.corejava.jvm.jit.JitSubjects::sumOfSquares (25 bytes)",
            "    124   31       3       com.corejava.jvm.jit.JitSubjects::sumOfSquares (25 bytes)   made not entrant",
            "    130   40   !   3       java.util.concurrent.ConcurrentHashMap::putVal (432 bytes)",
            "    131   41  s    1       java.lang.StringBuffer::length (5 bytes)");

    // Not instantiable: a holder for shared constants.
    private JitLogSamples() {
    }
}
