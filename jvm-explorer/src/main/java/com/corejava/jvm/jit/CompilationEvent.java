package com.corejava.jvm.jit;

/**
 * One line of {@code -XX:+PrintCompilation} output: one method compiled at one tier, or
 * one compiled version retired.
 *
 * <p>A typical line, with the columns this record holds:</p>
 *
 * <pre>
 *     120   36       4       com.corejava.jvm.jit.JitSubjects::sumOfSquares (25 bytes)
 *     ^     ^        ^       ^                                              ^
 *     ms    compile  tier    method                                         bytecode size
 *           id
 * </pre>
 *
 * @param timeMillis      milliseconds since the JVM started
 * @param compileId       the compilation's number; a retirement line repeats the id it retires
 * @param tier            0 interpreter, 1-3 C1 (with less or more profiling), 4 C2
 * @param osr             true for an on-stack-replacement compilation (the % column)
 * @param methodName      class and method, e.g. {@code java.lang.String::hashCode}
 * @param osrBytecodeIndex for OSR, the loop position compiled into (the {@code @ 4}), else -1
 * @param bytecodeBytes   the method's size in bytecode bytes (Lesson 01)
 * @param madeNotEntrant  true if this line RETIRES a compiled version instead of creating one
 */
public record CompilationEvent(long timeMillis, int compileId, int tier, boolean osr, String methodName,
                               int osrBytecodeIndex, int bytecodeBytes, boolean madeNotEntrant) {

    /**
     * Compact constructor: one place stating what a valid event looks like.
     */
    public CompilationEvent {
        if (methodName == null || methodName.isBlank()) {
            throw new IllegalArgumentException("A compilation event needs a method name.");
        }
        // Tiers run from 0 (interpreter) to 4 (C2); anything else is a parsing bug.
        if (tier < 0 || tier > 4) {
            throw new IllegalArgumentException("Tier must be 0 to 4, but was: " + tier);
        }
        if (timeMillis < 0 || compileId < 0 || bytecodeBytes < 0 || osrBytecodeIndex < -1) {
            throw new IllegalArgumentException("Negative values in a compilation event: time=" + timeMillis
                    + ", id=" + compileId + ", bytes=" + bytecodeBytes + ", bci=" + osrBytecodeIndex);
        }
    }

    /**
     * @param tier a tier number from 0 to 4
     * @return what that tier means, in words
     */
    public static String tierName(int tier) {
        return switch (tier) {
            case 0 -> "interpreter";
            case 1 -> "C1, no profiling";
            case 2 -> "C1, light profiling";
            case 3 -> "C1, full profiling";
            case 4 -> "C2";
            default -> throw new IllegalArgumentException("Tier must be 0 to 4, but was: " + tier);
        };
    }

    /**
     * @return the method name without its package, e.g. {@code JitSubjects::sumOfSquares}
     */
    public String shortMethodName() {
        // The class part ends at "::"; the package ends at the last dot before that.
        int separator = methodName.indexOf("::");
        int lastDot = separator < 0 ? methodName.lastIndexOf('.') : methodName.lastIndexOf('.', separator);
        return lastDot < 0 ? methodName : methodName.substring(lastDot + 1);
    }

    /**
     * @return one human-readable line describing this event
     */
    public String describe() {
        String what = madeNotEntrant
                ? "tier " + tier + " version made not entrant (retired)"
                : "compiled at tier " + tier + " (" + tierName(tier) + ")" + (osr ? ", OSR into the loop at @ " + osrBytecodeIndex : "");
        return String.format("%7d ms  %-28s %s  [#%d]", timeMillis, shortMethodName(), what, compileId);
    }
}
