package com.corejava.jvm.live;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses class histograms and compares two of them to find what is growing.
 *
 * <p>The format is HotSpot's, shared by {@code jcmd <pid> GC.class_histogram} and
 * {@code jmap -histo <pid>}; like the other logs in this course it is an implementation
 * detail, so the tests pin the Java 21 shape. Header, separator and "Total" lines are
 * skipped.</p>
 */
public final class ClassHistogram {

    // "   1:         10527        1520096  [B (java.base@21.0.2)"
    private static final Pattern ROW = Pattern.compile(
            // rank followed by a colon
            "^\\s*(?<rank>\\d+):"
            // instance count, then total bytes
            + "\\s+(?<instances>\\d+)\\s+(?<bytes>\\d+)"
            // the class name - no spaces in it, ever
            + "\\s+(?<cls>\\S+)"
            // optionally " (module@version)" for classes that belong to a named module
            + "(?:\\s+\\((?<module>[^)]*)\\))?\\s*$");

    // Static-only: nothing to construct.
    private ClassHistogram() {
    }

    /**
     * @param line one line of a histogram
     * @return the row it describes, or empty for headers, separators and the total
     */
    public static Optional<HistogramRow> parseRow(String line) {
        if (line == null) {
            return Optional.empty();
        }
        Matcher m = ROW.matcher(line);
        if (!m.find()) {
            return Optional.empty();
        }
        return Optional.of(new HistogramRow(
                Integer.parseInt(m.group("rank")),
                Long.parseLong(m.group("instances")),
                Long.parseLong(m.group("bytes")),
                m.group("cls"),
                m.group("module")));
    }

    /**
     * @param lines a whole histogram, in order
     * @return every row, in order
     */
    public static List<HistogramRow> parse(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("Cannot parse a null list of lines.");
        }
        List<HistogramRow> rows = new ArrayList<>();
        for (String line : lines) {
            parseRow(line).ifPresent(rows::add);
        }
        return List.copyOf(rows);
    }

    /**
     * Compares two histograms of the same JVM and lists the classes that grew.
     *
     * @param before the earlier histogram
     * @param after  the later histogram
     * @return every class whose total bytes grew, largest growth first
     */
    public static List<ClassGrowth> growth(List<HistogramRow> before, List<HistogramRow> after) {
        if (before == null || after == null) {
            throw new IllegalArgumentException("Both histograms are required.");
        }
        // Index the earlier histogram by class name, so each later row can find its partner.
        Map<String, HistogramRow> earlier = new HashMap<>();
        for (HistogramRow row : before) {
            earlier.put(row.className(), row);
        }

        List<ClassGrowth> grew = new ArrayList<>();
        for (HistogramRow row : after) {
            HistogramRow old = earlier.get(row.className());
            // A class absent from the first histogram had zero instances then.
            ClassGrowth g = new ClassGrowth(row.className(),
                    old == null ? 0 : old.instances(), row.instances(),
                    old == null ? 0 : old.bytes(), row.bytes());
            if (g.byteGrowth() > 0) {
                grew.add(g);
            }
        }

        // Largest byte growth first: the top of this list is where to look.
        grew.sort(Comparator.comparingLong(ClassGrowth::byteGrowth).reversed());
        return List.copyOf(grew);
    }

    /**
     * Turns the JVM's internal class name into the one a Java programmer writes.
     *
     * <p>Arrays are written as type descriptors (Lesson 01): one {@code [} per dimension,
     * then a letter for a primitive or {@code L...;} for a class. So {@code [B} is
     * {@code byte[]} and {@code [[Ljava.lang.String;} is {@code java.lang.String[][]}.</p>
     *
     * @param className a class name as a histogram prints it
     * @return the readable form
     */
    public static String readableName(String className) {
        if (className == null || !className.startsWith("[")) {
            return className;
        }
        // Count the dimensions: one "[" each.
        int dimensions = 0;
        while (dimensions < className.length() && className.charAt(dimensions) == '[') {
            dimensions++;
        }
        String element = className.substring(dimensions);
        String base = switch (element) {
            case "B" -> "byte";
            case "C" -> "char";
            case "D" -> "double";
            case "F" -> "float";
            case "I" -> "int";
            case "J" -> "long";
            case "S" -> "short";
            case "Z" -> "boolean";
            // "Ljava.lang.Object;" -> "java.lang.Object"; anything unexpected is kept as-is.
            default -> element.startsWith("L") && element.endsWith(";")
                    ? element.substring(1, element.length() - 1)
                    : element;
        };
        return base + "[]".repeat(dimensions);
    }
}
