package com.corejava.jvm.live;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pins the class histogram format (HotSpot, Java 21) and the growth comparison against
 * the numbers worked out by hand in {@link HistogramSamples}.
 */
class ClassHistogramTest {

    // A JDK class: it carries its module in brackets.
    @Test
    void parsesARowWithAModule() {
        HistogramRow row = ClassHistogram.parseRow("   1:          3100        1700000  [B (java.base@21.0.4)").orElseThrow();

        assertEquals(1, row.rank());
        assertEquals(3100, row.instances());
        assertEquals(1_700_000, row.bytes());
        assertEquals("[B", row.className());
        assertEquals("java.base@21.0.4", row.module());
        assertEquals("byte[]", row.readableClassName(), "[B is the descriptor for byte[] (Lesson 01).");
    }

    // A class from the class path: no module, and a $ for the nested record.
    @Test
    void parsesARowWithoutAModule() {
        HistogramRow row = ClassHistogram.parseRow(
                "   5:            20            320  com.corejava.jvm.live.LeakyRegistry$LeakedSession").orElseThrow();

        assertEquals("com.corejava.jvm.live.LeakyRegistry$LeakedSession", row.className());
        assertEquals("", row.module(), "Classes on the class path have no module.");
    }

    // Header, separator and total lines are not rows.
    @Test
    void skipsHeaderSeparatorAndTotal() {
        assertFalse(ClassHistogram.parseRow(" num     #instances         #bytes  class name (module)").isPresent());
        assertFalse(ClassHistogram.parseRow("-------------------------------------------------------").isPresent());
        assertFalse(ClassHistogram.parseRow("Total          8520        1898720").isPresent());
        assertEquals(5, ClassHistogram.parse(HistogramSamples.BEFORE).size(), "Five real rows in the sample.");
    }

    // The leak hunt itself: what grew, largest first; unchanged classes are left out.
    @Test
    void comparingTwoHistogramsListsWhatGrew() {
        List<ClassGrowth> grew = ClassHistogram.growth(
                ClassHistogram.parse(HistogramSamples.BEFORE), ClassHistogram.parse(HistogramSamples.AFTER));

        assertEquals(4, grew.size(), "java.lang.Class did not change, so four classes grew.");
        assertEquals(List.of("[B", "java.util.HashMap$Node", "java.lang.String",
                        "com.corejava.jvm.live.LeakyRegistry$LeakedSession"),
                grew.stream().map(ClassGrowth::className).toList(), "Sorted by byte growth, largest first.");

        ClassGrowth session = grew.get(3);
        assertEquals(600, session.instanceGrowth(), "20 -> 620 sessions: 600 more in one minute.");
        assertEquals(9600, session.byteGrowth(), "320 -> 9920 bytes of shallow size.");
        assertThrows(IllegalArgumentException.class, () -> ClassHistogram.growth(null, List.of()));
    }

    // Array descriptors read as Java.
    @Test
    void arrayNamesReadAsJava() {
        assertEquals("int[][]", ClassHistogram.readableName("[[I"));
        assertEquals("java.lang.Object[]", ClassHistogram.readableName("[Ljava.lang.Object;"));
        assertEquals("long[]", ClassHistogram.readableName("[J"), "J is long - L was taken (Lesson 01).");
        assertEquals("java.lang.String", ClassHistogram.readableName("java.lang.String"), "Plain names are unchanged.");
    }
}
