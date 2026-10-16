package com.corejava.jvm.jit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pins what the compilation subjects compute. Whatever the JIT does to them - inlining,
 * removing allocations, compiling and recompiling - the answers must never change. That is
 * the JIT's one absolute promise, and these tests hold it to it.
 */
class JitSubjectsTest {

    // 1 + 4 + 9 = 14, plus the edge case and the guard.
    @Test
    void sumOfSquaresIsCorrect() {
        assertEquals(14, JitSubjects.sumOfSquares(3), "1 + 4 + 9 = 14.");
        assertEquals(0, JitSubjects.sumOfSquares(0), "No squares add up to 0.");
        assertEquals(385, JitSubjects.sumOfSquares(10), "The sum of squares up to 10 is 385.");
        assertThrows(IllegalArgumentException.class, () -> JitSubjects.sumOfSquares(-1));
    }

    // dx = 3 and dy = 4 every time, so each call is 25 - however the call is compiled.
    @Test
    void distanceResultsAreExactWithOrWithoutTheObject() {
        assertEquals(25, JitSubjects.distanceSquared(0, 0, 3, 4), "3*3 + 4*4 = 25.");
        assertEquals(25L * 1000, JitSubjects.distanceLoop(1000), "1000 calls of 25 each.");
        assertEquals(0, JitSubjects.distanceLoop(0));
    }

    // Both shapes compute their own areas, through the same interface call.
    @Test
    void totalAreaAddsEveryShape() {
        JitSubjects.Shape[] shapes = {new JitSubjects.Square(2), new JitSubjects.Circle(1)};

        assertEquals(4 + Math.PI, JitSubjects.totalArea(shapes), 1e-12, "A 2x2 square plus a unit circle.");
        assertThrows(IllegalArgumentException.class, () -> JitSubjects.totalArea(null));
        // longRunningLoop with zero iterations does nothing and returns 0.
        assertEquals(0, JitSubjects.longRunningLoop(0));
    }
}
