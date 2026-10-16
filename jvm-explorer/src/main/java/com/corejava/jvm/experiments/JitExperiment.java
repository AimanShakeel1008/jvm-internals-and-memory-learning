// The experiments package: throwaway programs that make one lesson's point visible.
package com.corejava.jvm.experiments;

// Lesson 06's modules: the methods built to be compiled, and the allocation meter.
import com.corejava.jvm.jit.AllocationMeter;
import com.corejava.jvm.jit.JitSubjects;
import com.corejava.jvm.jit.JitSubjects.Circle;
import com.corejava.jvm.jit.JitSubjects.Shape;
import com.corejava.jvm.jit.JitSubjects.Square;

/**
 * EXPERIMENT (for learning only): five phases, each designed to make one JIT behaviour
 * appear in the JVM's own compilation log, between this program's phase markers.
 *
 * <pre>{@code
 * mvn compile
 * java -XX:+PrintCompilation -cp target/classes com.corejava.jvm.experiments.JitExperiment > jit.log
 * java -cp target/classes com.corejava.jvm.jit.CompilationLogTool jit.log JitSubjects
 * }</pre>
 *
 * <p>Thresholds, timings and even which phase a compilation lands in depend on the JDK
 * build and the machine. The ORDER of events is the observation.</p>
 */
public final class JitExperiment {

    // Every result is added here, so the JIT cannot decide the work is unused and delete
    // it - which would make the experiment show "nothing compiled" for the wrong reason.
    private static double sink;

    // Static-only program: no instances.
    private JitExperiment() {
    }

    /**
     * Runs the five phases, printing a marker line before each.
     */
    public static void main(String[] args) {
        // PHASE 1 - few calls, tiny loops. Predicted: sumOfSquares stays in the
        // interpreter, because neither its call count nor its loop count has crossed the
        // first compilation threshold yet (a few hundred calls, to verify for your JDK).
        System.out.println("=== PHASE 1: cold - sumOfSquares(10) called 100 times ===");
        for (int i = 0; i < 100; i++) {
            sink += JitSubjects.sumOfSquares(10);
        }

        // PHASE 2 - many calls. Predicted: a tier 3 (C1) compilation after a few hundred
        // calls, then a tier 4 (C2) compilation after a few thousand, then the tier 3
        // version "made not entrant" because the better one replaced it.
        System.out.println("=== PHASE 2: warm - sumOfSquares(10) called 20,000 times ===");
        for (int i = 0; i < 20_000; i++) {
            sink += JitSubjects.sumOfSquares(10);
        }

        // PHASE 3 - ONE call, a huge loop. Counting calls would never compile this; the
        // loop counter does, and the running loop is switched to compiled code mid-flight.
        // Predicted: a line with "%" and "@ <position>" - an OSR compilation.
        System.out.println("=== PHASE 3: OSR - longRunningLoop called ONCE, looping 50,000,000 times ===");
        sink += JitSubjects.longRunningLoop(50_000_000);

        // PHASE 4 - escape analysis, measured. Each distanceSquared call creates an object
        // that never leaves the method. Predicted: early batches allocate about 24 bytes per
        // call (interpreted, or compiled by C1, which does no escape analysis); once C2 has
        // compiled the loop, close to 0 - the object is never created at all.
        System.out.println("=== PHASE 4: escape analysis - bytes allocated per distanceSquared call ===");
        for (int batch = 1; batch <= 8; batch++) {
            System.out.println("  batch " + batch + ": "
                    + AllocationMeter.format(AllocationMeter.bytesPerCall(200_000, JitSubjects::distanceLoop)));
        }

        // PHASE 5 - deoptimization. While Square is the only Shape ever loaded, C2 can
        // compile totalArea assuming every Shape is a Square, with Square.area() pasted
        // inline and no interface call at all.
        System.out.println("=== PHASE 5: deoptimization - totalArea sees only Squares ===");
        Shape[] squares = {new Square(1), new Square(2), new Square(3)};
        for (int i = 0; i < 50_000; i++) {
            sink += JitSubjects.totalArea(squares);
        }

        // Creating the first Circle loads the Circle class, which breaks that assumption.
        // Predicted: the tier 4 version of totalArea is "made not entrant", then
        // totalArea is compiled again, this time allowing for both shapes.
        System.out.println("=== PHASE 5b: a Circle arrives ===");
        Shape[] mixed = {new Square(1), new Circle(1)};
        for (int i = 0; i < 50_000; i++) {
            sink += JitSubjects.totalArea(mixed);
        }

        // Printing the sink proves the work was really done, and uses it one last time.
        System.out.println("=== DONE (sink = " + sink + ") ===");
    }
}
