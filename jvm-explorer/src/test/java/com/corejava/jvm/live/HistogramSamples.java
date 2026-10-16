package com.corejava.jvm.live;

import java.util.List;

/**
 * Two class histograms of the same JVM taken a minute apart, in HotSpot's Java 21 format.
 *
 * <p>Worked out by hand, AFTER minus BEFORE:</p>
 * <ul>
 *   <li>{@code [B} (byte[]): 1,200 more instances, 39,300,000 more bytes - big, but every program has byte[]s;</li>
 *   <li>{@code LeakyRegistry$LeakedSession}: 600 more instances (20 -&gt; 620), 9,600 more bytes;</li>
 *   <li>{@code java.util.HashMap$Node}: 600 more instances, 19,200 more bytes;</li>
 *   <li>{@code java.lang.String}: 600 more, 14,400 more bytes;</li>
 *   <li>{@code java.lang.Class}: unchanged, so it is not listed as growing.</li>
 * </ul>
 * <p>Sorted by byte growth: byte[] first, then HashMap$Node, then String, then LeakedSession.</p>
 */
final class HistogramSamples {

    static final List<String> BEFORE = List.of(
            " num     #instances         #bytes  class name (module)",
            "-------------------------------------------------------",
            "   1:          3100        1700000  [B (java.base@21.0.4)",
            "   2:          4000          96000  java.lang.String (java.base@21.0.4)",
            "   3:           900          86400  java.lang.Class (java.base@21.0.4)",
            "   4:           500          16000  java.util.HashMap$Node (java.base@21.0.4)",
            "   5:            20            320  com.corejava.jvm.live.LeakyRegistry$LeakedSession",
            "Total          8520        1898720");

    static final List<String> AFTER = List.of(
            " num     #instances         #bytes  class name (module)",
            "-------------------------------------------------------",
            "   1:          4300       41000000  [B (java.base@21.0.4)",
            "   2:          4600         110400  java.lang.String (java.base@21.0.4)",
            "   3:           900          86400  java.lang.Class (java.base@21.0.4)",
            "   4:          1100          35200  java.util.HashMap$Node (java.base@21.0.4)",
            "   5:           620           9920  com.corejava.jvm.live.LeakyRegistry$LeakedSession",
            "Total         11520       41241920");

    // Not instantiable: shared constants.
    private HistogramSamples() {
    }
}
