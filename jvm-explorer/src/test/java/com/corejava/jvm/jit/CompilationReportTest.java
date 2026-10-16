package com.corejava.jvm.jit;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the report's arithmetic against the numbers worked out by hand in {@link JitLogSamples}.
 */
class CompilationReportTest {

    // Counts by tier, OSR, and the first and last timestamps.
    @Test
    void countsCompilationsByTier() {
        CompilationReport r = CompilationReport.of(CompilationLogParser.parse(JitLogSamples.LOG));

        assertEquals(6, r.compilations(), "Six compilation lines, not counting the retirement.");
        assertEquals(Map.of(1, 1, 3, 4, 4, 1), r.compilationsByTier(), "Tier 1: 1, tier 3: 4, tier 4: 1.");
        assertEquals(1, r.osrCompilations(), "One OSR compilation.");
        assertEquals(45, r.firstMillis());
        assertEquals(131, r.lastMillis());
    }

    // The distinction that matters: retiring a tier 3 version is replacement, not deoptimization.
    @Test
    void separatesReplacementFromDeoptimization() {
        CompilationReport r = CompilationReport.of(CompilationLogParser.parse(JitLogSamples.LOG));
        assertEquals(1, r.retired(), "One version was made not entrant.");
        assertEquals(0, r.retiredFromTier4(), "It was a tier 3 version - replaced, not deoptimized.");

        // A tier 4 retirement IS a deoptimization: C2 code thrown away.
        CompilationReport deopt = CompilationReport.of(List.of(
                new CompilationEvent(500, 90, 4, false, "a.B::c", -1, 30, false),
                new CompilationEvent(900, 90, 4, false, "a.B::c", -1, 30, true)));
        assertEquals(1, deopt.retiredFromTier4(), "Retiring C2 code is a deoptimization.");
    }

    // One method's story, still in log order.
    @Test
    void tellsTheHistoryOfOneMethod() {
        List<CompilationEvent> history = CompilationReport.historyOf(
                CompilationLogParser.parse(JitLogSamples.LOG), "JitSubjects::sumOfSquares");

        assertEquals(3, history.size(), "Tier 3, tier 4, then the tier 3 version retired.");
        assertEquals(List.of(3, 4, 3), history.stream().map(CompilationEvent::tier).toList(),
                "Climbs from C1 to C2; then the C1 version is retired.");
        assertTrue(history.get(2).madeNotEntrant());
    }

    // An empty log is legitimate: zeros and "none", never a crash.
    @Test
    void anEmptyLogReportsNothingSafely() {
        CompilationReport r = CompilationReport.of(List.of());

        assertEquals(0, r.compilations());
        assertTrue(r.describe().contains("none"), "No tiers to list: " + r.describe());
        assertThrows(IllegalArgumentException.class, () -> CompilationReport.of(null));
        assertThrows(IllegalArgumentException.class,
                () -> new CompilationEvent(0, 0, 5, false, "a.B::c", -1, 1, false), "There is no tier 5.");
    }
}
