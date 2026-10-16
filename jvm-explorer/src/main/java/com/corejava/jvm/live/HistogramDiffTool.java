package com.corejava.jvm.live;

// Lesson 06's encoding-aware reader: jcmd output redirected with ">" in Windows
// PowerShell 5.1 is UTF-16, exactly like the compilation logs were.
import com.corejava.jvm.jit.CompilationLogTool;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Compares two class histograms of the same JVM and prints the classes that grew most -
 * the first step of every leak hunt.
 *
 * <pre>{@code
 * jcmd <pid> GC.class_histogram > histo-before.log
 * ... wait a minute or two ...
 * jcmd <pid> GC.class_histogram > histo-after.log
 * java -cp target/classes com.corejava.jvm.live.HistogramDiffTool histo-before.log histo-after.log
 * }</pre>
 */
public final class HistogramDiffTool {

    // How many growing classes to show by default.
    public static final int DEFAULT_TOP = 10;

    // Static-only tool: no instances.
    private HistogramDiffTool() {
    }

    /**
     * @param before the earlier histogram file
     * @param after  the later histogram file
     * @param top    how many growing classes to list, at least 1
     * @return the report, or a plain message if a file is missing
     */
    public static String compare(Path before, Path after, int top) {
        if (before == null || after == null) {
            throw new IllegalArgumentException("Both histogram files are required.");
        }
        if (top < 1) {
            throw new IllegalArgumentException("Must show at least one class, but was: " + top);
        }
        for (Path file : List.of(before, after)) {
            if (!Files.exists(file)) {
                return "No histogram at " + file.toAbsolutePath() + ". Produce one with:" + System.lineSeparator()
                        + "  jcmd <pid> GC.class_histogram > " + file.getFileName();
            }
        }
        try {
            List<HistogramRow> first = ClassHistogram.parse(CompilationLogTool.readLines(before));
            List<HistogramRow> second = ClassHistogram.parse(CompilationLogTool.readLines(after));
            List<ClassGrowth> grew = ClassHistogram.growth(first, second);

            StringBuilder out = new StringBuilder();
            out.append("classes that grew between ").append(before.getFileName()).append(" and ")
                    .append(after.getFileName()).append(" (").append(first.size()).append(" and ")
                    .append(second.size()).append(" classes):");
            if (grew.isEmpty()) {
                out.append(System.lineSeparator()).append("  nothing grew - no leak suspect in this interval");
            }
            for (int i = 0; i < Math.min(top, grew.size()); i++) {
                out.append(System.lineSeparator()).append(grew.get(i).describe());
            }
            return out.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read a histogram file", e);
        }
    }

    /**
     * @param args the earlier file, the later file, and optionally how many classes to show
     */
    public static void main(String[] args) {
        if (args == null || args.length < 2) {
            System.out.println("usage: HistogramDiffTool <before-file> <after-file> [top]");
            return;
        }
        int top = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_TOP;
        System.out.println(compare(Path.of(args[0]), Path.of(args[1]), top));
    }
}
