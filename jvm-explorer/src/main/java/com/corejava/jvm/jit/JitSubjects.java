// Lesson 06's package: watching the JIT compiler find hot code, compile it, optimise
// it, and occasionally throw the compiled version away again.
package com.corejava.jvm.jit;

/**
 * Small methods built to be COMPILED, the way Lesson 01's BytecodeSubject was built to be
 * disassembled. Each one exists to make one JIT behaviour visible.
 *
 * <ul>
 *   <li>{@link #sumOfSquares(int)} - a plain hot method: called often enough, it climbs
 *       from the interpreter to C1 and then to C2.</li>
 *   <li>{@link #longRunningLoop(int)} - called once, but loops millions of times: the
 *       case on-stack replacement exists for.</li>
 *   <li>{@link #distanceSquared(int, int, int, int)} - creates an object that never leaves
 *       the method: the case escape analysis exists for.</li>
 *   <li>{@link #totalArea(Shape[])} - a call through an interface: the case where the JIT
 *       guesses which class will arrive, and must undo its work when the guess breaks.</li>
 * </ul>
 */
public final class JitSubjects {

    // Static-only subjects: no instances.
    private JitSubjects() {
    }

    /**
     * @param n how many squares to add, zero or more
     * @return 1*1 + 2*2 + ... + n*n
     */
    public static long sumOfSquares(int n) {
        // The same door-guard habit as the rest of the project.
        if (n < 0) {
            throw new IllegalArgumentException("n cannot be negative, but was: " + n);
        }
        long total = 0;
        for (int i = 1; i <= n; i++) {
            // The cast to long BEFORE multiplying stops i * i overflowing an int for large i.
            total += (long) i * i;
        }
        return total;
    }

    /**
     * A method meant to be called exactly once, with a huge loop inside.
     *
     * <p>Counting calls would never make it hot - it is only called once. But the JVM also
     * counts loop iterations ("back-edges": each jump back to the top of a loop), and when
     * those cross a threshold it compiles the method and switches the RUNNING loop over to
     * the compiled version mid-flight. That switch is on-stack replacement.</p>
     *
     * @param iterations how many times to go round the loop, zero or more
     * @return a value that depends on every iteration, so the loop cannot be skipped
     */
    public static long longRunningLoop(int iterations) {
        if (iterations < 0) {
            throw new IllegalArgumentException("iterations cannot be negative, but was: " + iterations);
        }
        long accumulator = 0;
        for (int i = 0; i < iterations; i++) {
            // Mixing each step into the previous result means no simple formula replaces
            // the loop, so the compiled code really has to run it.
            accumulator += i ^ (accumulator >>> 3);
        }
        return accumulator;
    }

    /**
     * Something with an area - an interface, so calls go through it and the JIT has to
     * work out which implementation it is actually calling.
     */
    public interface Shape {
        /**
         * @return this shape's area
         */
        double area();
    }

    /**
     * A square. While this is the ONLY Shape the program has ever loaded, the JIT can
     * assume every Shape is a Square.
     *
     * @param side the length of one side
     */
    public record Square(double side) implements Shape {
        @Override
        public double area() {
            return side * side;
        }
    }

    /**
     * A circle. Loading this class breaks the JIT's "every Shape is a Square" assumption,
     * which is the whole reason it exists.
     *
     * @param radius the circle's radius
     */
    public record Circle(double radius) implements Shape {
        @Override
        public double area() {
            return Math.PI * radius * radius;
        }
    }

    /**
     * @param shapes the shapes to add up; must not be null
     * @return the sum of their areas
     */
    public static double totalArea(Shape[] shapes) {
        if (shapes == null) {
            throw new IllegalArgumentException("Cannot add up a null array of shapes.");
        }
        double sum = 0;
        for (Shape shape : shapes) {
            // An interface call. If only Square has ever been seen here, C2 replaces this
            // with Square's area() code inline - no call at all - and records the
            // assumption so it can undo it if another implementation ever turns up.
            sum += shape.area();
        }
        return sum;
    }

    /**
     * The difference between two points. Private and small: an object built only to be
     * used and thrown away inside {@link #distanceSquared}.
     *
     * @param dx the horizontal difference
     * @param dy the vertical difference
     */
    private record Delta(int dx, int dy) {
    }

    /**
     * @return the squared distance between (x1, y1) and (x2, y2)
     */
    public static int distanceSquared(int x1, int y1, int x2, int y2) {
        // A brand-new object on every call - in the interpreter, a real heap allocation of
        // roughly 24 bytes. But the object never leaves this method: it is not returned,
        // not stored in a field, not passed anywhere. Once C2 can prove that, it may skip
        // the allocation and keep dx and dy as plain local values (escape analysis).
        Delta d = new Delta(x2 - x1, y2 - y1);
        return d.dx() * d.dx() + d.dy() * d.dy();
    }

    /**
     * Calls {@link #distanceSquared} many times, so the allocation it makes can be measured.
     *
     * @param calls how many calls to make, zero or more
     * @return the sum of every result - always 25 per call, since dx = 3 and dy = 4
     */
    public static long distanceLoop(int calls) {
        if (calls < 0) {
            throw new IllegalArgumentException("calls cannot be negative, but was: " + calls);
        }
        long total = 0;
        for (int i = 0; i < calls; i++) {
            // The points differ by (3, 4) every time, so each call returns 3*3 + 4*4 = 25.
            // A known answer makes the method's correctness testable exactly.
            total += distanceSquared(i, i + 1, i + 3, i + 5);
        }
        return total;
    }
}
