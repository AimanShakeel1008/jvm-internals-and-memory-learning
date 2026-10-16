package com.corejava.jvm.gclog;

import org.junit.jupiter.api.Test;
// JUnit creates a fresh empty folder for the test and deletes it afterwards, so the test
// never touches the project folder or leaves files behind.
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the tool end to end: a real file on disk in, a readable summary out.
 */
class GcLogSummaryToolTest {

    // A log file on disk is read, parsed and summarised.
    @Test
    void summarisesALogFileFromDisk(@TempDir Path folder) throws IOException {
        Path log = folder.resolve("gc.log");
        Files.write(log, GcLogSamples.G1_LOG, StandardCharsets.UTF_8);

        String summary = GcLogSummaryTool.summarise(log);

        assertTrue(summary.contains("collector            : G1"), "The collector is named: " + summary);
        assertTrue(summary.contains("young 2, full 1, other 2"), "The counts are shown: " + summary);
        assertTrue(summary.contains("15.3 ms"), "The longest pause is shown: " + summary);
        assertTrue(summary.contains("2.0 MB"), "The live-data estimate is shown: " + summary);
    }

    // The expected first-run situation: no log yet. The answer is the command to make one.
    @Test
    void aMissingLogExplainsHowToProduceOne(@TempDir Path folder) {
        String message = GcLogSummaryTool.summarise(folder.resolve("does-not-exist.log"));

        assertTrue(message.startsWith("No GC log"), "A missing file is explained, not thrown: " + message);
        assertTrue(message.contains("-Xlog:gc:file=gc.log"), "The message gives the command to create one: " + message);
    }
}
