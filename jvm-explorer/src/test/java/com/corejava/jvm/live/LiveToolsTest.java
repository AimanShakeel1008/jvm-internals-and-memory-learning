package com.corejava.jvm.live;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the two file-based tools end to end: the histogram comparison and the heap dumper.
 */
class LiveToolsTest {

    // Two histogram files in, the growing classes out - with the leak suspect named.
    @Test
    void theDiffToolNamesWhatGrew(@TempDir Path folder) throws IOException {
        Path before = folder.resolve("histo-before.log");
        Path after = folder.resolve("histo-after.log");
        Files.write(before, HistogramSamples.BEFORE, StandardCharsets.UTF_8);
        Files.write(after, HistogramSamples.AFTER, StandardCharsets.UTF_8);

        String report = HistogramDiffTool.compare(before, after, 10);

        assertTrue(report.contains("byte[]"), "Descriptors are shown readably: " + report);
        assertTrue(report.contains("LeakyRegistry$LeakedSession"), "The leak suspect is listed: " + report);
        assertTrue(report.contains("+600 instances"), "Its growth is shown: " + report);
    }

    // A missing file is explained with the command that creates one.
    @Test
    void aMissingHistogramExplainsHowToProduceOne(@TempDir Path folder) {
        String message = HistogramDiffTool.compare(folder.resolve("a.log"), folder.resolve("b.log"), 5);
        assertTrue(message.contains("GC.class_histogram"), message);
    }

    // A real heap dump of the test JVM: the file exists and is not empty. (It is written
    // to JUnit's temporary folder and deleted afterwards, so it never lands in the project.)
    @Test
    void writesARealHeapDump(@TempDir Path folder) throws IOException {
        Path dump = HeapDumper.dump(folder.resolve("test.hprof"), true);

        assertTrue(Files.exists(dump), "The heap dump file must exist.");
        assertTrue(Files.size(dump) > 0, "A heap dump of a running JVM cannot be empty.");
    }

    // The two rules HotSpot imposes, checked up front with clear messages.
    @Test
    void heapDumpNamesAreValidated(@TempDir Path folder) throws IOException {
        assertThrows(IllegalArgumentException.class, () -> HeapDumper.dump(folder.resolve("dump.bin"), true),
                "A heap dump must end in .hprof.");

        Path existing = Files.createFile(folder.resolve("taken.hprof"));
        assertThrows(IllegalArgumentException.class, () -> HeapDumper.dump(existing, true),
                "An existing file must never be overwritten.");
    }
}
