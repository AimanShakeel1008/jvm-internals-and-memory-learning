package com.corejava.jvm.jit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
// TreeMap keeps tiers in order 0, 1, 2, 3, 4 when printed.
import java.util.TreeMap;

/**
 * The headline numbers from a {@code -XX:+PrintCompilation} log.
 *
 * <p>One distinction matters more than any other here. A "made not entrant" line RETIRES
 * a compiled version, and that happens for two very different reasons:</p>
 *
 * <ul>
 *   <li><strong>Replacement</strong> - a tier 3 (C1) version is retired because the better
 *       tier 4 (C2) version is ready. Normal and healthy.</li>
 *   <li><strong>Deoptimization</strong> - a tier 4 (C2) version is retired because an
 *       assumption it was compiled under turned out to be wrong. Worth knowing about.</li>
 * </ul>
 *
 * <p>So the report counts retirements of tier 4 code separately.</p>
 *
 * @param compilations     compilation lines (not counting retirements)
 * @param compilationsByTier how many compilations happened at each tier
 * @param osrCompilations  on-stack-replacement compilations
 * @param retired          "made not entrant" lines of any tier
 * @param retiredFromTier4 "made not entrant" lines for tier 4 code - the deoptimizations
 * @param firstMillis      timestamp of the first event, or 0 if there were none
 * @param lastMillis       timestamp of the last event, or 0 if there were none
 */
public record CompilationReport(int compilations, Map<Integer, Integer> compilationsByTier, int osrCompilations,
                                int retired, int retiredFromTier4, long firstMillis, long lastMillis) {

    /**
     * Compact constructor: the tier map is copied and sorted, so the record can never be
     * altered through the caller's map and always prints tiers in order.
     */
    public CompilationReport {
        compilationsByTier = compilationsByTier == null
                ? Map.of()
                : Collections.unmodifiableMap(new TreeMap<>(compilationsByTier));
    }

    /**
     * @param events compilation events, in log order
     * @return their summary
     */
    public static CompilationReport of(List<CompilationEvent> events) {
        if (events == null) {
            throw new IllegalArgumentException("Cannot summarise a null list of events.");
        }
        int compilations = 0;
        Map<Integer, Integer> byTier = new TreeMap<>();
        int osr = 0;
        int retired = 0;
        int retiredFromTier4 = 0;
        long first = 0;
        long last = 0;

        for (int i = 0; i < events.size(); i++) {
            CompilationEvent e = events.get(i);
            if (i == 0) {
                first = e.timeMillis();
            }
            last = Math.max(last, e.timeMillis());

            if (e.madeNotEntrant()) {
                retired++;
                if (e.tier() == 4) {
                    retiredFromTier4++;
                }
            } else {
                compilations++;
                // merge adds 1 to the tier's count, creating the entry at 1 if absent.
                byTier.merge(e.tier(), 1, Integer::sum);
                if (e.osr()) {
                    osr++;
                }
            }
        }
        return new CompilationReport(compilations, byTier, osr, retired, retiredFromTier4, first, last);
    }

    /**
     * @param events         compilation events, in log order
     * @param methodFragment text that must appear in the method name, e.g. "JitSubjects"
     * @return only the events for matching methods, still in log order
     */
    public static List<CompilationEvent> historyOf(List<CompilationEvent> events, String methodFragment) {
        if (events == null || methodFragment == null) {
            throw new IllegalArgumentException("Events and method fragment must not be null.");
        }
        List<CompilationEvent> matching = new ArrayList<>();
        for (CompilationEvent e : events) {
            if (e.methodName().contains(methodFragment)) {
                matching.add(e);
            }
        }
        return List.copyOf(matching);
    }

    /**
     * @return a short multi-line report
     */
    public String describe() {
        StringBuilder byTier = new StringBuilder();
        for (Map.Entry<Integer, Integer> entry : compilationsByTier.entrySet()) {
            if (byTier.length() > 0) {
                byTier.append(", ");
            }
            byTier.append("tier ").append(entry.getKey()).append(": ").append(entry.getValue());
        }
        return "compilations       : " + compilations + "  (" + (byTier.length() == 0 ? "none" : byTier) + ")"
                + System.lineSeparator()
                + "OSR compilations   : " + osrCompilations
                + System.lineSeparator()
                + "made not entrant   : " + retired + "  (of which tier 4, i.e. deoptimizations: " + retiredFromTier4 + ")"
                + System.lineSeparator()
                + "log covers         : " + firstMillis + " ms to " + lastMillis + " ms after JVM start";
    }
}
