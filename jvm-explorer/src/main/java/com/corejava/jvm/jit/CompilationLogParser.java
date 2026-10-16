package com.corejava.jvm.jit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns lines of {@code -XX:+PrintCompilation} output into {@link CompilationEvent}s.
 *
 * <p>{@code -XX:+PrintCompilation} is a HotSpot product flag that prints one line every
 * time a method is compiled or a compiled version is retired. Its column layout is a
 * HotSpot implementation detail - not specified anywhere official - so this parser is
 * deliberately tolerant, and the tests pin the Java 21 shapes this course was written
 * against. Lines it does not recognise (the program's own output, native-method wrappers)
 * are skipped, not treated as errors.</p>
 */
public final class CompilationLogParser {

    // The shape of a compilation line, piece by piece.
    private static final Pattern COMPILATION_LINE = Pattern.compile(
            // leading spaces, then the timestamp in milliseconds
            "^\\s*(?<time>\\d+)"
            // the compile id
            + "\\s+(?<id>\\d+)"
            // the attribute columns: % (OSR), s (synchronized), ! (has exception handlers),
            // b (blocking), n (native wrapper). Each is printed in its own position, with
            // spaces for the absent ones, so they may be separated by spaces.
            + "\\s+(?<flags>[%sbn!]*(?:\\s+[%sbn!])*)"
            // the tier, 0 to 4
            + "\\s*(?<tier>[0-4])"
            // the method, always written Class::method
            + "\\s+(?<method>\\S+::\\S+)"
            // for OSR compilations only: "@ 7", the loop position compiled into
            + "(?:\\s+@\\s+(?<bci>\\d+))?"
            // the bytecode size. Native wrappers say "(native)" instead, so they never match.
            + "\\s+\\((?<size>\\d+) bytes\\)"
            // anything after: "made not entrant" for a retirement line
            + "(?<rest>.*)$");

    // Static-only parser: nothing to construct.
    private CompilationLogParser() {
    }

    /**
     * @param line one line of output, possibly unrelated to compilation
     * @return the compilation event it describes, or empty
     */
    public static Optional<CompilationEvent> parseLine(String line) {
        if (line == null) {
            return Optional.empty();
        }
        Matcher m = COMPILATION_LINE.matcher(line);
        if (!m.find()) {
            return Optional.empty();
        }
        String bci = m.group("bci");
        return Optional.of(new CompilationEvent(
                Long.parseLong(m.group("time")),
                Integer.parseInt(m.group("id")),
                Integer.parseInt(m.group("tier")),
                // The % column marks an on-stack-replacement compilation.
                m.group("flags").contains("%"),
                m.group("method"),
                bci == null ? -1 : Integer.parseInt(bci),
                Integer.parseInt(m.group("size")),
                m.group("rest").contains("made not entrant")));
    }

    /**
     * @param lines every line of output, in order
     * @return every compilation event, in order
     */
    public static List<CompilationEvent> parse(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("Cannot parse a null list of lines.");
        }
        List<CompilationEvent> events = new ArrayList<>();
        for (String line : lines) {
            parseLine(line).ifPresent(events::add);
        }
        return List.copyOf(events);
    }
}
