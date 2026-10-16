package com.corejava.jvm.jit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the tool end to end, including the Windows trap it exists to survive.
 */
class CompilationLogToolTest {

    // A redirected log is summarised, and one method's story keeps the phase markers.
    @Test
    void summarisesALogAndTellsOneMethodsStory(@TempDir Path folder) throws IOException {
        Path log = folder.resolve("jit.log");
        Files.write(log, JitLogSamples.LOG, StandardCharsets.UTF_8);

        String summary = CompilationLogTool.summarise(log, "JitSubjects::sumOfSquares");

        assertTrue(summary.contains("tier 3: 4"), "Counts by tier: " + summary);
        assertTrue(summary.contains("=== PHASE 2: warm ==="), "The program's markers stay in the story: " + summary);
        assertTrue(summary.contains("compiled at tier 4 (C2)"), "The C2 compilation is told in words: " + summary);
        assertTrue(summary.contains("made not entrant"), "The retirement is in the story: " + summary);
    }

    // Windows PowerShell 5.1's ">" writes UTF-16 with a byte order mark. Read naively as
    // UTF-8, every line would be garbage and nothing would parse.
    @Test
    void readsAUtf16FileAsWrittenByPowerShell(@TempDir Path folder) throws IOException {
        Path log = folder.resolve("jit.log");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(0xFF);                     // the two-byte marker for UTF-16, least significant byte first
        bytes.write(0xFE);
        bytes.write(String.join("\r\n", JitLogSamples.LOG).getBytes(StandardCharsets.UTF_16LE));
        Files.write(log, bytes.toByteArray());

        assertEquals(JitLogSamples.LOG, CompilationLogTool.readLines(log),
                "A UTF-16 file with Windows line endings must read back as exactly the same lines.");
    }

    // No log yet: the answer is the command that makes one.
    @Test
    void aMissingLogExplainsHowToProduceOne(@TempDir Path folder) {
        String message = CompilationLogTool.summarise(folder.resolve("missing.log"), null);

        assertTrue(message.startsWith("No compilation log"), message);
        assertTrue(message.contains("-XX:+PrintCompilation"), message);
    }
}
