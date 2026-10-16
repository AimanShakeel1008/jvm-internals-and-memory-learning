// The experiments package: throwaway programs that make one lesson's point visible.
package com.corejava.jvm.experiments;

import com.corejava.jvm.live.FixedRegistry;
import com.corejava.jvm.live.HeapDumper;
import com.corejava.jvm.live.LeakyRegistry;
import com.corejava.jvm.memory.HeapSnapshot;
import com.corejava.jvm.memory.MemoryRegions;

import java.io.IOException;
import java.nio.file.Path;

/**
 * EXPERIMENT (for learning only): a long-running "service" with a planted memory leak,
 * built to be examined from the outside with jps, jcmd, jstack and VisualVM.
 *
 * <p>It handles ten fake requests per second. With the default leaky registry every
 * request leaks 32 KB, so the heap climbs steadily; with {@code --fixed} the registry is
 * bounded and the heap stays flat. It keeps running until you press Enter (or a time
 * limit passes), so there is time to attach tools to it.</p>
 *
 * <pre>{@code
 * mvn compile
 * java -Xmx256m -cp target/classes com.corejava.jvm.experiments.LeakHunt
 * java -Xmx256m -cp target/classes com.corejava.jvm.experiments.LeakHunt --fixed
 * java -Xmx256m -cp target/classes com.corejava.jvm.experiments.LeakHunt --dump
 * }</pre>
 */
public final class LeakHunt {

    // Default running time if Enter is never pressed: long enough to explore, short
    // enough that a forgotten window does not run all day.
    public static final int DEFAULT_SECONDS = 600;

    // Ten requests per second.
    private static final long MILLIS_BETWEEN_REQUESTS = 100;

    // Static-only program: no instances.
    private LeakHunt() {
    }

    /**
     * The command-line choices, parsed once.
     *
     * @param fixed   use the bounded registry instead of the leaky one
     * @param dump    write a heap dump of this JVM before exiting
     * @param seconds stop after this many seconds if Enter is not pressed
     */
    public record Options(boolean fixed, boolean dump, int seconds) {
    }

    /**
     * @param args {@code --fixed}, {@code --dump}, {@code --seconds=N}, in any order
     * @return the parsed options
     */
    public static Options parse(String[] args) {
        boolean fixed = false;
        boolean dump = false;
        int seconds = DEFAULT_SECONDS;
        if (args != null) {
            for (String arg : args) {
                if ("--fixed".equals(arg)) {
                    fixed = true;
                } else if ("--dump".equals(arg)) {
                    dump = true;
                } else if (arg.startsWith("--seconds=")) {
                    seconds = Integer.parseInt(arg.substring("--seconds=".length()));
                    if (seconds < 1) {
                        throw new IllegalArgumentException("--seconds must be at least 1, but was: " + seconds);
                    }
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg
                            + " (expected --fixed, --dump or --seconds=N)");
                }
            }
        }
        return new Options(fixed, dump, seconds);
    }

    /**
     * Runs the fake service until Enter is pressed or the time limit passes.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        Options options = parse(args);
        long pid = ProcessHandle.current().pid();

        System.out.println("=== leak hunt: a long-running service with a planted leak ===");
        System.out.println("process id (PID)  : " + pid);
        System.out.println("registry          : " + (options.fixed() ? "FIXED (bounded at " + FixedRegistry.CAPACITY + ")" : "LEAKY (never removes)"));
        System.out.println("examine it from ANOTHER terminal, e.g.:");
        System.out.println("  jcmd " + pid + " GC.class_histogram > histo-before.log");
        System.out.println("  jcmd " + pid + " Thread.print");
        System.out.println("press Enter here to stop (or wait " + options.seconds() + " s)");
        System.out.println();

        long deadline = System.currentTimeMillis() + options.seconds() * 1000L;
        int request = 0;

        // System.in.available() > 0 means a line has been typed - checked without blocking,
        // so the "service" keeps working while it waits for you.
        while (System.in.available() == 0 && System.currentTimeMillis() < deadline) {
            request++;
            int held = options.fixed() ? FixedRegistry.handleRequest(request) : LeakyRegistry.handleRequest(request);

            // A status line every 2 seconds (20 requests): the heap's used figure should
            // climb under the leaky registry and stay flat under the fixed one.
            if (request % 20 == 0) {
                HeapSnapshot heap = MemoryRegions.heapSnapshot();
                System.out.printf("requests %6d | sessions held %6d | heap used %5d MB of %d MB%n",
                        request, held, HeapSnapshot.toMegabytes(heap.usedBytes()),
                        HeapSnapshot.toMegabytes(heap.maxBytes()));
            }
            Thread.sleep(MILLIS_BETWEEN_REQUESTS);
        }

        System.out.println();
        System.out.println("stopped after " + request + " requests.");

        if (options.dump()) {
            // Only reachable objects, so the dump shows what the leak is really holding.
            Path file = HeapDumper.dump(Path.of("leak-" + pid + HeapDumper.SUFFIX), true);
            System.out.println("heap dump written : " + file.toAbsolutePath());
            System.out.println("open it in VisualVM or Eclipse MAT and look for LeakedSession.");
        }
    }
}
