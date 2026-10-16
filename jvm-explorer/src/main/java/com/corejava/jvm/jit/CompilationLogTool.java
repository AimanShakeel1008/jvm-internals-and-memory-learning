package com.corejava.jvm.jit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reads a file of {@code -XX:+PrintCompilation} output and prints a report - plus, for a
 * chosen method, its compilation story interleaved with the program's own phase markers.
 *
 * <pre>{@code
 * java -XX:+PrintCompilation -cp target/classes com.corejava.jvm.experiments.JitExperiment > jit.log
 * java -cp target/classes com.corejava.jvm.jit.CompilationLogTool jit.log JitSubjects
 * }</pre>
 */
public final class CompilationLogTool {

    // The file name the lesson's commands write to.
    public static final String DEFAULT_LOG_FILE = "jit.log";

    // Lines the experiment prints to mark its phases; kept in the story for context.
    private static final String PHASE_MARKER = "===";

    // Static-only tool: no instances.
    private CompilationLogTool() {
    }

    /**
     * Reads a text file whatever encoding a shell wrote it in.
     *
     * <p>This matters on Windows: in Windows PowerShell 5.1, {@code >} writes UTF-16 with a
     * "byte order mark" (BOM) - two marker bytes, FF FE, at the start of the file -
     * whereas cmd and most other shells write plain bytes. Reading a UTF-16 file as UTF-8
     * produces garbage, and every line would silently fail to parse. So the first bytes are
     * checked, and the right decoding chosen.</p>
     *
     * @param file the file to read
     * @return its lines
     * @throws IOException if the file cannot be read
     */
    public static List<String> readLines(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        Charset charset = StandardCharsets.UTF_8;
        int skip = 0;

        // "& 0xFF" re-reads a signed Java byte as 0..255 - Lesson 01's trap, met again.
        int b0 = bytes.length > 0 ? bytes[0] & 0xFF : -1;
        int b1 = bytes.length > 1 ? bytes[1] & 0xFF : -1;
        int b2 = bytes.length > 2 ? bytes[2] & 0xFF : -1;

        if (b0 == 0xFF && b1 == 0xFE) {
            // FF FE: UTF-16, least significant byte first (what PowerShell 5.1 writes).
            charset = StandardCharsets.UTF_16LE;
            skip = 2;
        } else if (b0 == 0xFE && b1 == 0xFF) {
            // FE FF: UTF-16, most significant byte first.
            charset = StandardCharsets.UTF_16BE;
            skip = 2;
        } else if (b0 == 0xEF && b1 == 0xBB && b2 == 0xBF) {
            // EF BB BF: UTF-8 with a BOM. Same decoding, just skip the marker.
            skip = 3;
        }

        // lines() splits on \n, \r\n and \r alike, so Windows line endings need no special care.
        return new String(bytes, skip, bytes.length - skip, charset).lines().toList();
    }

    /**
     * @param lines          the raw output, in order
     * @param methodFragment text that must appear in a method's name
     * @return the matching methods' compilation events, with phase markers kept in place
     */
    public static List<String> storyOf(List<String> lines, String methodFragment) {
        List<String> story = new ArrayList<>();
        for (String line : lines) {
            if (line.startsWith(PHASE_MARKER)) {
                // The program's own markers show WHEN, relative to its work, each event happened.
                story.add(line);
                continue;
            }
            Optional<CompilationEvent> event = CompilationLogParser.parseLine(line);
            if (event.isPresent() && event.get().methodName().contains(methodFragment)) {
                story.add(event.get().describe());
            }
        }
        return story;
    }

    /**
     * @param logFile        the file to read
     * @param methodFragment a method to tell the story of, or null for the report only
     * @return the report, or a plain message if the file does not exist yet
     */
    public static String summarise(Path logFile, String methodFragment) {
        if (logFile == null) {
            throw new IllegalArgumentException("Cannot summarise a null path.");
        }
        if (!Files.exists(logFile)) {
            return "No compilation log at " + logFile.toAbsolutePath() + " yet. Produce one with:" + System.lineSeparator()
                    + "  java -XX:+PrintCompilation -cp target/classes com.corejava.jvm.experiments.JitExperiment > jit.log";
        }
        try {
            List<String> lines = readLines(logFile);
            List<CompilationEvent> events = CompilationLogParser.parse(lines);

            StringBuilder out = new StringBuilder();
            out.append("summary of ").append(logFile).append(" (").append(lines.size()).append(" lines read)")
                    .append(System.lineSeparator())
                    .append(CompilationReport.of(events).describe());

            if (methodFragment != null && !methodFragment.isBlank()) {
                out.append(System.lineSeparator()).append(System.lineSeparator())
                        .append("story of methods matching \"").append(methodFragment).append("\":");
                for (String storyLine : storyOf(lines, methodFragment)) {
                    out.append(System.lineSeparator()).append(storyLine);
                }
            }
            return out.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read compilation log " + logFile, e);
        }
    }

    /**
     * @param args optional: the log file (default jit.log), then a method-name fragment
     */
    public static void main(String[] args) {
        String file = (args != null && args.length > 0) ? args[0] : DEFAULT_LOG_FILE;
        String method = (args != null && args.length > 1) ? args[1] : null;
        System.out.println(summarise(Path.of(file), method));
    }
}
