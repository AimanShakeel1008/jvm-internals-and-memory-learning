package com.corejava.jvm.gclog;

import java.io.IOException;
import java.io.UncheckedIOException;
// The log is plain text; naming the encoding means the same bytes read the same
// everywhere, regardless of the machine's default.
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The project's GC-log reader: give it a log file written with {@code -Xlog:gc:file=...}
 * and it prints a summary a human can read in ten seconds.
 *
 * <pre>{@code
 * java -cp target/classes com.corejava.jvm.gclog.GcLogSummaryTool gc.log
 * }</pre>
 */
public final class GcLogSummaryTool {

    // The file name the lesson's commands write to, used when no argument is given.
    public static final String DEFAULT_LOG_FILE = "gc.log";

    // Static-only tool: no instances.
    private GcLogSummaryTool() {
    }

    /**
     * Reads, parses and summarises one GC log file.
     *
     * @param logFile the log to read
     * @return the summary, or a plain message if the file does not exist yet
     */
    public static String summarise(Path logFile) {
        if (logFile == null) {
            throw new IllegalArgumentException("Cannot summarise a null path.");
        }

        // A missing file is the EXPECTED first-run situation, not a failure: answer with
        // the command that creates one rather than a stack trace.
        if (!Files.exists(logFile)) {
            return "No GC log at " + logFile.toAbsolutePath() + " yet. Produce one with:" + System.lineSeparator()
                    + "  java -Xmx64m -Xlog:gc:file=gc.log -cp target/classes "
                    + "com.corejava.jvm.experiments.GcWorkload";
        }

        try {
            // readAllLines is fine here: GC logs from these experiments are a few hundred
            // lines. For a log of many gigabytes you would stream it line by line instead.
            ParsedGcLog parsed = GcLogParser.parse(Files.readAllLines(logFile, StandardCharsets.UTF_8));
            return "summary of " + logFile + " (" + parsed.linesRead() + " lines read)"
                    + System.lineSeparator() + GcSummary.of(parsed).describe();
        } catch (IOException e) {
            // The file exists but cannot be read - a genuinely unexpected problem, so it
            // is rethrown with the original kept as the cause.
            throw new UncheckedIOException("Could not read GC log " + logFile, e);
        }
    }

    /**
     * @param args optional: the log file to read; defaults to {@value #DEFAULT_LOG_FILE}
     */
    public static void main(String[] args) {
        String fileName = (args != null && args.length > 0) ? args[0] : DEFAULT_LOG_FILE;
        System.out.println(summarise(Path.of(fileName)));
    }
}
