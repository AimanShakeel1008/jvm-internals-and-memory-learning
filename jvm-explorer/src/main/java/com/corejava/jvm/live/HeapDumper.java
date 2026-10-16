package com.corejava.jvm.live;

// HotSpot's diagnostic MX bean - the same machinery "jcmd <pid> GC.heap_dump" uses -
// reachable from inside the program (jdk.management module; a HotSpot extension).
import com.sun.management.HotSpotDiagnosticMXBean;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes a heap dump of THIS JVM to a file - every object on the heap and every
 * reference between them, in the standard {@code .hprof} format that tools such as
 * VisualVM and Eclipse MAT open.
 *
 * <p>Three costs, every time: the JVM pauses while the dump is written; the file is
 * roughly as big as the live heap; and the file contains every value in memory -
 * including passwords, tokens and personal data. Treat a heap dump like a database
 * backup.</p>
 */
public final class HeapDumper {

    // HotSpot requires this suffix and refuses other names.
    public static final String SUFFIX = ".hprof";

    // Static-only tool: no instances.
    private HeapDumper() {
    }

    /**
     * @param file     where to write the dump; must end in ".hprof" and must not exist yet
     * @param liveOnly true to include only reachable objects (forces a full GC first,
     *                 and gives a smaller file); false to include garbage too
     * @return the file written
     * @throws IOException if the dump cannot be written
     */
    public static Path dump(Path file, boolean liveOnly) throws IOException {
        if (file == null || !file.getFileName().toString().endsWith(SUFFIX)) {
            throw new IllegalArgumentException("A heap dump file name must end in " + SUFFIX + ", got: " + file);
        }
        // HotSpot also refuses to overwrite; checking first gives a clearer message.
        if (Files.exists(file)) {
            throw new IllegalArgumentException("Refusing to overwrite an existing heap dump: " + file.toAbsolutePath());
        }

        HotSpotDiagnosticMXBean bean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        if (bean == null) {
            throw new UnsupportedOperationException("This JVM offers no HotSpotDiagnosticMXBean; heap dumps are a HotSpot feature.");
        }

        // An absolute path: a relative one would be resolved against the JVM's working
        // folder, which is not always the folder you are thinking of.
        bean.dumpHeap(file.toAbsolutePath().toString(), liveOnly);
        return file;
    }
}
