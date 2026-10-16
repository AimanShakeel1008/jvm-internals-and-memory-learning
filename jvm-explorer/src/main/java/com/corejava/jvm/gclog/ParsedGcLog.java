package com.corejava.jvm.gclog;

import java.util.List;

/**
 * Everything the parser understood in one GC log file.
 *
 * @param collectorName the collector the log names in its {@code Using ...} line, or
 *                      {@code "unknown"} if the log did not say
 * @param pauses        every stop-the-world pause, in the order they appear in the log
 * @param linesRead     how many lines were read in total, recognised or not
 */
public record ParsedGcLog(String collectorName, List<GcEvent> pauses, int linesRead) {

    // What we print when the log never said which collector it was.
    public static final String UNKNOWN_COLLECTOR = "unknown";

    /**
     * Compact constructor: never leave a null where a caller expects a value.
     */
    public ParsedGcLog {
        collectorName = (collectorName == null || collectorName.isBlank()) ? UNKNOWN_COLLECTOR : collectorName;
        // Copied, so the parsed result cannot be altered afterwards through the
        // parser's own working list.
        pauses = pauses == null ? List.of() : List.copyOf(pauses);
        if (linesRead < 0) {
            throw new IllegalArgumentException("Lines read cannot be negative: " + linesRead);
        }
    }
}
