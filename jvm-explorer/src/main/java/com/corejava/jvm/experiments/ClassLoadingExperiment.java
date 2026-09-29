// The experiments package: throwaway programs that exist to make one lesson's point
// visible, kept separate from the project's real capabilities.
package com.corejava.jvm.experiments;

/**
 * EXPERIMENT (for learning only): shows that a class is not loaded when the program
 * starts, nor when it is imported, nor when a constant is read from it - but at the
 * exact instant the program first needs something real from it.
 *
 * <p>Run this with the JVM's class-loading log switched on:</p>
 *
 * <pre>{@code
 * mvn compile
 * java -verbose:class -cp target/classes com.corejava.jvm.experiments.ClassLoadingExperiment
 * }</pre>
 *
 * <p>The JVM prints one line per class it loads, mixed into this program's own output.
 * Between MARKER 3 and MARKER 4 you should see the nested class appear - and nowhere
 * earlier, even though the constant was read at MARKER 2.</p>
 *
 * <p>This is an observation, not an assertion: exactly when the JVM chooses to load a
 * class is its business, and the specification only promises it will not initialize one
 * too early. Your machine is the authority.</p>
 */
public final class ClassLoadingExperiment {

    // Static-only program: no instances.
    private ClassLoadingExperiment() {
    }

    /**
     * A class that arrives late. It is a nested class, which means javac compiles it
     * into its own separate file - ClassLoadingExperiment$LateComer.class - so the JVM
     * loads it separately too, and the log can show exactly when.
     *
     * <p>Package-private rather than private so the test class next door can reach it;
     * "static" so it needs no enclosing instance.</p>
     */
    static final class LateComer {

        // A compile-time constant. Reading this from another class does NOT load this
        // one: javac copies the text straight into the reader's own class file.
        static final String PINNED_NOTE = "read without loading LateComer at all";

        // The moment this line prints, the class has been loaded, verified, prepared,
        // and is now initializing. Printing from a static initializer is a terrible
        // habit in real code; here it is the entire instrument.
        static {
            System.out.println("      >>> LateComer's static initializer is running NOW");
        }

        // Not a compile-time constant, so it can only be filled in by initialization.
        static final String STAMP = buildStamp();

        private LateComer() {
        }

        /**
         * @return proof that this class initialized - calling any static method forces it
         */
        static String stamp() {
            return STAMP;
        }

        private static String buildStamp() {
            return "LateComer is now fully initialized";
        }
    }

    /**
     * Prints numbered markers around the moments that might load the nested class, so
     * the JVM's own {@code -verbose:class} lines can be lined up against them.
     */
    public static void main(String[] args) {
        // A banner, so this output is findable inside the flood of -verbose:class lines.
        System.out.println("=== class loading experiment: when does a class actually load? ===");

        System.out.println("MARKER 1 : the program is running and has touched nothing yet.");

        System.out.println("MARKER 2 : about to read a COMPILE-TIME CONSTANT from LateComer.");
        // Predicted: no [class,load] line for LateComer appears here, because this read
        // does not mention LateComer in the bytecode at all - the text below was copied
        // into THIS class's constant pool when javac compiled it.
        System.out.println("           value = " + LateComer.PINNED_NOTE);
        System.out.println("           ...and LateComer has still not been loaded.");

        System.out.println("MARKER 3 : about to CALL A STATIC METHOD on LateComer.");
        // Predicted: this is where the JVM must find, load, verify, prepare and finally
        // initialize LateComer - so its [class,load] line and its initializer's own
        // printout both appear between MARKER 3 and MARKER 4.
        System.out.println("           value = " + LateComer.stamp());

        System.out.println("MARKER 4 : done. Everything above happened in that order.");

        // An honest footer, matching the warm-up experiment's: what varies, and what does not.
        System.out.println("Run again with -verbose:class to see the JVM's own loading lines interleaved.");
    }
}
