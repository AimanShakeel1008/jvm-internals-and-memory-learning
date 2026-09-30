package com.corejava.jvm.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that overflowing the stack on purpose behaves exactly as the lesson claims.
 *
 * <p>No test here asserts a specific depth. The number of frames that fit depends on the
 * stack size the JVM chose, the size of a frame on this processor, and whether the JIT
 * has compiled the method yet - so "at least a thousand" is the strongest honest claim,
 * and a test asserting "exactly 21742" would fail on the next machine for no good
 * reason.</p>
 */
class StackDepthProbeTest {

    // A floor low enough that any JVM on any normal machine clears it easily, and high
    // enough that a broken probe returning 0 or 1 is caught.
    private static final int PLAUSIBLE_MINIMUM_DEPTH = 1_000;

    // The central claim: recursion with no way back really does run out of room, the
    // probe survives it, and the number it reports is large.
    @Test
    void recursionRunsOutOfStackAndTheDepthIsCounted() {
        int depth = StackDepthProbe.measureMaxDepth();

        assertTrue(depth > PLAUSIBLE_MINIMUM_DEPTH,
                "Expected thousands of frames before the stack ran out, but reached only: " + depth);
    }

    // The identity of the failure matters as much as the fact of it: a stack that runs
    // out throws StackOverflowError, which is an Error, not an Exception.
    @Test
    void theErrorThrownIsExactlyStackOverflowError() {
        StackDepthProbe.measureMaxDepth();

        assertEquals("java.lang.StackOverflowError", StackDepthProbe.lastErrorType(),
                "Running out of stack must produce a StackOverflowError, nothing else.");
    }

    // A stack overflow is not fatal to the JVM. Once the recursion unwinds, the stack is
    // empty again and the thread carries on normally - which is why the probe can be run
    // twice in a row and give a comparable answer both times.
    @Test
    void theThreadRecoversSoTheProbeCanRunAgain() {
        int first = StackDepthProbe.measureMaxDepth();
        int second = StackDepthProbe.measureMaxDepth();

        assertTrue(first > PLAUSIBLE_MINIMUM_DEPTH, "First probe reached only: " + first);
        assertTrue(second > PLAUSIBLE_MINIMUM_DEPTH, "Second probe reached only: " + second);
    }

    // Fatter frames are still frames: the measurement works the same way and still ends
    // in an overflow. Whether the depth is SMALLER than the plain one is an observation
    // the learner makes by running the program, not something asserted here - the JIT is
    // free to lay out a compiled frame however it likes.
    @Test
    void framesCarryingMoreLocalsAlsoOverflowAndAreCounted() {
        int fatDepth = StackDepthProbe.measureMaxDepthWithLargerFrames();

        assertTrue(fatDepth > PLAUSIBLE_MINIMUM_DEPTH,
                "Expected thousands of larger frames too, but reached only: " + fatDepth);
        assertEquals("java.lang.StackOverflowError", StackDepthProbe.lastErrorType(),
                "The larger-frame probe must end the same way: StackOverflowError.");
    }
}
