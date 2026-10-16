package com.corejava.jvm.jit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the {@code -XX:+PrintCompilation} line shapes (HotSpot, Java 21) the parser was
 * written against. Pure string work, so every assertion is exact.
 */
class CompilationLogParserTest {

    // An ordinary C1 compilation, column by column.
    @Test
    void parsesATier3CompilationColumnByColumn() {
        CompilationEvent e = CompilationLogParser.parseLine(
                "    112   31       3       com.corejava.jvm.jit.JitSubjects::sumOfSquares (25 bytes)").orElseThrow();

        assertEquals(112, e.timeMillis(), "112 ms after the JVM started.");
        assertEquals(31, e.compileId(), "Compilation number 31.");
        assertEquals(3, e.tier(), "Tier 3: C1 with full profiling.");
        assertFalse(e.osr(), "No % column, so not an OSR compilation.");
        assertEquals("com.corejava.jvm.jit.JitSubjects::sumOfSquares", e.methodName());
        assertEquals(-1, e.osrBytecodeIndex(), "Only OSR compilations have a loop position.");
        assertEquals(25, e.bytecodeBytes(), "25 bytes of bytecode.");
        assertFalse(e.madeNotEntrant(), "A compilation, not a retirement.");
        assertEquals("JitSubjects::sumOfSquares", e.shortMethodName(), "The package is dropped for printing.");
    }

    // On-stack replacement: the % column and the "@ position" after the method.
    @Test
    void parsesAnOsrCompilation() {
        CompilationEvent e = CompilationLogParser.parseLine(
                "    118   35 %     3       com.corejava.jvm.jit.JitSubjects::longRunningLoop @ 4 (27 bytes)").orElseThrow();

        assertTrue(e.osr(), "The % column marks on-stack replacement.");
        assertEquals(4, e.osrBytecodeIndex(), "Compiled into the loop at bytecode position 4.");
        assertEquals(27, e.bytecodeBytes());
    }

    // A retirement line: the same id as the compilation it retires, plus "made not entrant".
    @Test
    void parsesAMadeNotEntrantLine() {
        CompilationEvent e = CompilationLogParser.parseLine(
                "    124   31       3       com.corejava.jvm.jit.JitSubjects::sumOfSquares (25 bytes)   made not entrant")
                .orElseThrow();

        assertTrue(e.madeNotEntrant(), "This line retires compiled code.");
        assertEquals(31, e.compileId(), "It names the compilation it retires.");
        assertEquals(3, e.tier(), "A tier 3 version is being retired - normally because tier 4 replaced it.");
    }

    // Other attribute columns (! = exception handlers, s = synchronized) do not confuse it.
    @Test
    void parsesLinesWithOtherAttributeColumns() {
        CompilationEvent handlers = CompilationLogParser.parseLine(
                "    130   40   !   3       java.util.concurrent.ConcurrentHashMap::putVal (432 bytes)").orElseThrow();
        assertEquals(3, handlers.tier());
        assertFalse(handlers.osr(), "! is not the OSR column.");

        CompilationEvent sync = CompilationLogParser.parseLine(
                "    131   41  s    1       java.lang.StringBuffer::length (5 bytes)").orElseThrow();
        assertEquals(1, sync.tier(), "Tier 1: a trivial method, compiled by C1 without profiling.");
    }

    // Native wrappers, the program's own output, and nonsense are skipped.
    @Test
    void skipsLinesThatAreNotCompilations() {
        assertFalse(CompilationLogParser.parseLine(
                "     60    5     n 0       java.lang.invoke.MethodHandle::linkToStatic(LLLLLLL)L (native)   (static)")
                .isPresent(), "A native wrapper has no bytecode size and is not a compilation of Java code.");
        assertFalse(CompilationLogParser.parseLine("=== PHASE 2: warm ===").isPresent(), "A marker is not a compilation.");
        assertFalse(CompilationLogParser.parseLine("  batch 1: 24.0 bytes per call").isPresent());
        assertFalse(CompilationLogParser.parseLine(null).isPresent());
    }

    // A whole redirected log: only the compilation lines come back, in order.
    @Test
    void parsesAWholeLogInOrder() {
        var events = CompilationLogParser.parse(JitLogSamples.LOG);

        assertEquals(7, events.size(), "Seven compilation lines: six compilations and one retirement.");
        assertEquals(1, events.get(0).compileId(), "Log order is preserved.");
        assertThrows(IllegalArgumentException.class, () -> CompilationLogParser.parse(null));
    }
}
