package com.corejava.jvm.gclog;

import java.util.ArrayList;
import java.util.List;
// "A value, or nothing" - most log lines are not pause lines, and that is not an error.
import java.util.Optional;
// Pattern and Matcher are Java's regular expressions: a compact language for describing
// the SHAPE of a piece of text and pulling named pieces out of it.
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns lines of a HotSpot unified-logging GC log ({@code -Xlog:gc}) into {@link GcEvent}s.
 *
 * <p>The parser recognises the one line shape every stop-the-world pause is logged with,
 * for the Serial, Parallel and G1 collectors:</p>
 *
 * <pre>
 * [uptime s][level][tags] GC(id) Pause Kind (cause) (cause) BEFORE-&gt;AFTER(COMMITTED) N.NNNms
 * </pre>
 *
 * <p>Everything else - concurrent-phase lines, start-up lines, lines from other tags - is
 * skipped rather than treated as an error, because a real log is full of them. This
 * format is HotSpot's, not part of the Java specification, and its exact wording can
 * change between JDK versions; the tests pin the Java 21 shapes this course was written
 * against.</p>
 */
public final class GcLogParser {

    // The shape of a pause line, written as a regular expression. Each part is explained
    // on its own line; "(?<name>...)" captures that part under a name we can ask for.
    private static final Pattern PAUSE_LINE = Pattern.compile(
            // "[0.215s]" - the uptime decoration. Matched anywhere in the line, so extra
            // decorations such as a wall-clock time in front do not break the parser.
            "\\[(?<uptime>\\d+\\.\\d+)s\\]"
            // anything (the level and tag decorations), then "GC(0) "
            + ".*?GC\\((?<id>\\d+)\\) "
            // "Pause Young", "Pause Full", "Pause Remark", "Pause Init Mark": one or two
            // capitalised words after "Pause".
            + "Pause (?<kind>[A-Z][a-z]+(?: [A-Z][a-z]+)?)"
            // zero or more bracketed causes. "(?:[^()]|\\(\\))*" allows ordinary characters
            // and the pair "()" inside, which is exactly what "(System.gc())" needs - a
            // simpler "[^)]*" would stop at the first ")" and break on that cause.
            + "(?<causes>(?: \\((?:[^()]|\\(\\))*\\))*) "
            // "25M->3M(256M)" - heap before, after, and committed, each with its unit
            + "(?<before>\\d+)(?<beforeUnit>[BKMG])->"
            + "(?<after>\\d+)(?<afterUnit>[BKMG])"
            + "\\((?<committed>\\d+)(?<committedUnit>[BKMG])\\) "
            // "2.512ms" - the pause length
            + "(?<millis>\\d+(?:\\.\\d+)?)ms");

    // One bracketed cause inside the causes group, with the same "()" allowance.
    private static final Pattern ONE_CAUSE = Pattern.compile("\\(((?:[^()]|\\(\\))*)\\)");

    // The start-up line naming the collector: "[0.004s][info][gc] Using G1".
    private static final Pattern USING_LINE = Pattern.compile("\\] Using (?<name>.+?)\\s*$");

    // Static-only parser: no state, nothing to construct.
    private GcLogParser() {
    }

    /**
     * @param line one line of a GC log
     * @return the pause it describes, or empty if it is not a pause line
     */
    public static Optional<GcEvent> parsePause(String line) {
        // A missing line is simply not a pause line.
        if (line == null) {
            return Optional.empty();
        }

        // find() looks for the shape anywhere in the line, rather than demanding the
        // whole line match - tolerant of trailing spaces and extra decorations.
        Matcher m = PAUSE_LINE.matcher(line);
        if (!m.find()) {
            return Optional.empty();
        }

        // Pull each bracketed cause out of the causes group, without its brackets.
        List<String> causes = new ArrayList<>();
        Matcher causeMatcher = ONE_CAUSE.matcher(m.group("causes"));
        while (causeMatcher.find()) {
            causes.add(causeMatcher.group(1));
        }

        return Optional.of(new GcEvent(
                Long.parseLong(m.group("id")),
                Double.parseDouble(m.group("uptime")),
                m.group("kind"),
                causes,
                toBytes(m.group("before"), m.group("beforeUnit")),
                toBytes(m.group("after"), m.group("afterUnit")),
                toBytes(m.group("committed"), m.group("committedUnit")),
                Double.parseDouble(m.group("millis"))));
    }

    /**
     * @param line one line of a GC log
     * @return the collector's name if this is the "Using ..." start-up line
     */
    public static Optional<String> parseCollectorName(String line) {
        if (line == null) {
            return Optional.empty();
        }
        Matcher m = USING_LINE.matcher(line);
        return m.find() ? Optional.of(m.group("name")) : Optional.empty();
    }

    /**
     * @param lines every line of a GC log, in order
     * @return the collector's name and every pause, in log order
     */
    public static ParsedGcLog parse(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("Cannot parse a null list of lines.");
        }

        String collector = null;
        List<GcEvent> pauses = new ArrayList<>();

        for (String line : lines) {
            // The collector line appears once, near the top; keep the first one found.
            if (collector == null) {
                Optional<String> name = parseCollectorName(line);
                if (name.isPresent()) {
                    collector = name.get();
                    continue;
                }
            }
            // ifPresent(pauses::add) adds the event only when the line was a pause.
            parsePause(line).ifPresent(pauses::add);
        }

        return new ParsedGcLog(collector, pauses, lines.size());
    }

    /**
     * Converts a size as the log prints it ("25" with unit "M") into bytes.
     *
     * @param number the digits
     * @param unit   B, K, M or G - the units HotSpot uses in these lines
     * @return the size in bytes
     */
    static long toBytes(String number, String unit) {
        long value = Long.parseLong(number);
        // Binary units: 1 K = 1024 bytes, matching how the JVM itself counts.
        return switch (unit) {
            case "B" -> value;
            case "K" -> value * 1024L;
            case "M" -> value * 1024L * 1024L;
            case "G" -> value * 1024L * 1024L * 1024L;
            // The pattern only lets B, K, M or G through, so reaching this line means
            // the pattern and this switch have drifted apart - say so loudly.
            default -> throw new IllegalArgumentException("Unknown size unit: " + unit);
        };
    }
}
