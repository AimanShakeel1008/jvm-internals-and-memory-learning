package com.corejava.jvm.gclog;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the summary arithmetic exactly, against the numbers worked out by hand in
 * {@link GcLogSamples}.
 */
class GcSummaryTest {

    private static final long MB = 1024L * 1024L;

    // The counts by kind.
    @Test
    void countsPausesByKind() {
        GcSummary s = GcSummary.of(GcLogParser.parse(GcLogSamples.G1_LOG));

        assertEquals("G1", s.collectorName());
        assertEquals(5, s.pauseCount());
        assertEquals(2, s.youngCount(), "GC(0) and GC(1) are young pauses.");
        assertEquals(1, s.fullCount(), "GC(3) is the one full pause.");
        assertEquals(2, s.otherCount(), "Remark and Cleanup are neither young nor full.");
    }

    // Total, longest and average pause.
    @Test
    void addsUpThePauses() {
        GcSummary s = GcSummary.of(GcLogParser.parse(GcLogSamples.G1_LOG));

        assertEquals(22.212, s.totalPauseMillis(), 1e-9, "2.512 + 3.1 + 1.2 + 0.1 + 15.3.");
        assertEquals(15.3, s.maxPauseMillis(), 1e-9, "The full GC is the longest pause.");
        assertEquals(22.212 / 5, s.averagePauseMillis(), 1e-9, "Average = total / count.");
    }

    // The average hides the worst pause - the reason the summary prints both.
    @Test
    void theAverageHidesTheWorstPause() {
        GcSummary s = GcSummary.of(GcLogParser.parse(GcLogSamples.G1_LOG));

        assertTrue(s.maxPauseMillis() > 3 * s.averagePauseMillis(),
                "One full pause is more than three times the average - exactly what an average-only dashboard misses.");
    }

    // Reclaimed bytes, share of time paused, and the live-data estimate.
    @Test
    void computesReclaimThroughputAndLiveData() {
        GcSummary s = GcSummary.of(GcLogParser.parse(GcLogSamples.G1_LOG));

        assertEquals(52 * MB, s.totalReclaimedBytes(), "22 + 2 + 0 + 0 + 28 MB.");
        assertEquals(1.0, s.spanSeconds(), 1e-9, "The last pause was at 1.000 s.");
        assertEquals(2.2212, s.pausePercent(), 1e-9, "22.212 ms out of 1000 ms.");
        assertEquals(100 - 2.2212, s.throughputPercent(), 1e-9, "Throughput is the rest.");
        assertEquals(2 * MB, s.heapAfterLastFullBytes().getAsLong(), "2M after the full GC.");
    }

    // An empty log is a legitimate input: no division by zero, no invented numbers.
    @Test
    void anEmptyLogSummarisesSafely() {
        GcSummary s = GcSummary.of(new ParsedGcLog(null, List.of(), 0));

        assertEquals(ParsedGcLog.UNKNOWN_COLLECTOR, s.collectorName());
        assertEquals(0, s.pauseCount());
        assertEquals(0.0, s.averagePauseMillis(), "No pauses means an average of 0, not a crash.");
        assertEquals(0.0, s.pausePercent(), "No span means 0% paused, not a division by zero.");
        assertFalse(s.heapAfterLastFullBytes().isPresent(), "No full GC means no live-data estimate.");
        assertTrue(s.describe().contains("n/a"), "The report says n/a rather than printing a fake 0 MB.");
        assertThrows(IllegalArgumentException.class, () -> GcSummary.of(null));
    }
}
