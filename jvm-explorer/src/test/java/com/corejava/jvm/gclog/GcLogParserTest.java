package com.corejava.jvm.gclog;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the GC log line shapes this course was written against (HotSpot, Java 21).
 *
 * <p>Parsing is pure string work, so - unlike anything that depends on when the
 * collector runs - every assertion here can be exact. The sample lines are written in
 * the format HotSpot uses; if your own log looks different, that difference is worth
 * telling me about, and a new test line here is how the parser would learn it.</p>
 */
class GcLogParserTest {

    // One megabyte in bytes, so the expected sizes below read as "25 MB" not a raw number.
    private static final long MB = 1024L * 1024L;

    // The default collector's most common line: a G1 young pause with two causes.
    @Test
    void parsesAG1YoungPauseFieldByField() {
        GcEvent e = GcLogParser.parsePause(
                "[0.215s][info][gc] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 25M->3M(256M) 2.512ms")
                .orElseThrow();

        assertEquals(0, e.gcId(), "GC(0) is collection number 0.");
        assertEquals(0.215, e.uptimeSeconds(), 1e-9, "The uptime decoration is 0.215 seconds.");
        assertEquals("Young", e.pauseKind(), "The word after Pause is the kind.");
        assertEquals(List.of("Normal", "G1 Evacuation Pause"), e.causes(), "Both bracketed causes, in order.");
        assertEquals(25 * MB, e.heapBeforeBytes(), "25M before.");
        assertEquals(3 * MB, e.heapAfterBytes(), "3M after.");
        assertEquals(256 * MB, e.heapCommittedBytes(), "256M committed, in brackets.");
        assertEquals(2.512, e.pauseMillis(), 1e-9, "The pause lasted 2.512 ms.");
        assertEquals(22 * MB, e.reclaimedBytes(), "25 MB minus 3 MB is 22 MB reclaimed.");
        assertTrue(e.isYoung(), "A Young pause is a minor collection.");
    }

    // The Serial collector's young pause, as Lesson 04's experiment logged it.
    @Test
    void parsesASerialYoungPause() {
        GcEvent e = GcLogParser.parsePause(
                "[0.214s][info][gc] GC(0) Pause Young (Allocation Failure) 26M->1M(123M) 3.912ms").orElseThrow();

        assertEquals(List.of("Allocation Failure"), e.causes(), "Serial gives one cause: Eden was full.");
        assertEquals(3.912, e.pauseMillis(), 1e-9);
    }

    // The trap: "(System.gc())" has brackets inside brackets. A naive pattern stops at the
    // first ")" and fails to match the line at all.
    @Test
    void parsesAFullPauseWhoseCauseContainsBrackets() {
        GcEvent e = GcLogParser.parsePause(
                "[1.000s][info][gc] GC(3) Pause Full (System.gc()) 30M->2M(64M) 15.300ms").orElseThrow();

        assertEquals("Full", e.pauseKind());
        assertEquals(List.of("System.gc()"), e.causes(), "The cause keeps its inner brackets.");
        assertTrue(e.isFull(), "A Full pause is a full collection.");
        assertEquals(2 * MB, e.heapAfterBytes(), "The heap after a full GC is the live-data estimate.");
    }

    // G1's concurrent cycle includes short pauses with no cause at all.
    @Test
    void parsesAPauseWithNoCauseAndOtherUnits() {
        GcEvent remark = GcLogParser.parsePause(
                "[0.660s][info][gc] GC(2) Pause Remark 41M->41M(64M) 1.200ms").orElseThrow();
        assertEquals("Remark", remark.pauseKind());
        assertTrue(remark.causes().isEmpty(), "A Remark pause has no bracketed cause.");
        assertEquals(0, remark.reclaimedBytes(), "41M to 41M reclaimed nothing.");

        // Kilobytes and gigabytes appear on very small and very large heaps.
        GcEvent big = GcLogParser.parsePause(
                "[9.5s][info][gc] GC(40) Pause Young (Normal) (G1 Evacuation Pause) 3G->512M(8G) 12ms").orElse(null);
        // "9.5s" has a decimal point, so the uptime decoration matches; "12ms" has none, which is allowed.
        assertEquals(3L * 1024 * MB, big.heapBeforeBytes(), "3G is three gigabytes.");
        assertEquals(12.0, big.pauseMillis(), 1e-9, "A whole number of milliseconds is still a pause length.");
        assertEquals(512 * 1024L, GcLogParser.toBytes("512", "K"), "512K is 524288 bytes.");
    }

    // A real log is mostly lines that are not pauses. They must be skipped, not crash.
    @Test
    void skipsLinesThatAreNotPauses() {
        assertFalse(GcLogParser.parsePause("[0.004s][info][gc] Using G1").isPresent(), "A start-up line is not a pause.");
        assertFalse(GcLogParser.parsePause("[0.641s][info][gc] GC(2) Concurrent Mark Cycle").isPresent(),
                "A concurrent phase runs alongside the program - it is not a pause.");
        assertFalse(GcLogParser.parsePause("[0.690s][info][gc] GC(2) Concurrent Mark Cycle 49.012ms").isPresent(),
                "A concurrent phase with a duration is still not a pause.");
        assertFalse(GcLogParser.parsePause("[2.1s][info][gc] GC(0) Garbage Collection (Warmup) 28M(11%)->10M(4%)")
                .isPresent(), "ZGC's cycle line has a different shape and is not a pause line.");
        assertFalse(GcLogParser.parsePause("completely unrelated text").isPresent());
        assertFalse(GcLogParser.parsePause(null).isPresent(), "A missing line is not a pause.");
    }

    // The collector's name comes from the start-up line.
    @Test
    void readsTheCollectorNameFromTheUsingLine() {
        assertEquals(Optional.of("G1"), GcLogParser.parseCollectorName("[0.004s][info][gc] Using G1"));
        assertEquals(Optional.of("Serial"), GcLogParser.parseCollectorName("[0.003s][info][gc] Using Serial"));
        assertEquals(Optional.empty(), GcLogParser.parseCollectorName("[0.215s][info][gc] GC(0) Pause Young"));
    }

    // A whole log: the collector, every pause in order, and the line count.
    @Test
    void parsesAWholeLogInOrder() {
        ParsedGcLog log = GcLogParser.parse(GcLogSamples.G1_LOG);

        assertEquals("G1", log.collectorName());
        assertEquals(5, log.pauses().size(), "Two young, Remark, Cleanup and one full: five pauses.");
        assertEquals(List.of(0L, 1L, 2L, 2L, 3L), log.pauses().stream().map(GcEvent::gcId).toList(),
                "Pauses come back in log order.");
        assertEquals(GcLogSamples.G1_LOG.size(), log.linesRead(), "Every line is counted, recognised or not.");
        assertThrows(IllegalArgumentException.class, () -> GcLogParser.parse(null));
    }
}
