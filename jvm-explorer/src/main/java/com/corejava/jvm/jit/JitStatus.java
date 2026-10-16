package com.corejava.jvm.jit;

// The JVM's monitoring view of its JIT compiler.
import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * Asks the running JVM about its JIT compiler: is there one, what is it called, and how
 * much time has it spent compiling so far.
 *
 * <p>On a normal HotSpot JVM the name is something like "HotSpot 64-Bit Tiered Compilers"
 * - "tiered" meaning C1 and C2 working together (exact name to verify on your JDK). Started
 * with {@code -Xint} (interpreter only), the JVM has no compiler and reports none.</p>
 */
public final class JitStatus {

    // Static-only helper: nothing to construct.
    private JitStatus() {
    }

    /**
     * @return the JIT compiler's name, or empty if this JVM is running without one
     */
    public static Optional<String> compilerName() {
        // getCompilationMXBean() returns null - not an empty object - when there is no
        // compiler at all, so null is checked before anything else.
        CompilationMXBean bean = ManagementFactory.getCompilationMXBean();
        return bean == null ? Optional.empty() : Optional.of(bean.getName());
    }

    /**
     * @return total milliseconds the JIT has spent compiling since the JVM started, or
     *         empty if there is no compiler or it does not track the time
     */
    public static OptionalLong totalCompilationMillis() {
        CompilationMXBean bean = ManagementFactory.getCompilationMXBean();
        if (bean == null || !bean.isCompilationTimeMonitoringSupported()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(bean.getTotalCompilationTime());
    }

    /**
     * @return one or two printable lines about the compiler
     */
    public static String describe() {
        Optional<String> name = compilerName();
        if (name.isEmpty()) {
            return "JIT compiler       : none (running interpreted only, e.g. with -Xint)";
        }
        OptionalLong millis = totalCompilationMillis();
        // This time is spent on BACKGROUND compiler threads, so it is CPU time taken from
        // the machine - not time your own code stood still.
        return "JIT compiler       : " + name.get() + System.lineSeparator()
                + "time spent compiling so far : "
                + (millis.isPresent() ? millis.getAsLong() + " ms (on background compiler threads)" : "n/a");
    }
}
