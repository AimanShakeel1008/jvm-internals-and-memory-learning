package com.corejava.jvm.memory;

/**
 * Measures how deep this JVM's thread stack goes, by falling off the end of it on
 * purpose and counting the frames on the way down.
 *
 * <p>Every thread gets its own stack: a pile of <strong>frames</strong>, one per method
 * call that has started and not yet finished. A frame holds that call's parameters and
 * local variables, its working space, and where to return to. The pile has a fixed size,
 * chosen when the thread is created ({@code -Xss} sets it), so a chain of calls that
 * never returns must eventually run out of room. When it does, the JVM throws
 * {@link StackOverflowError}.</p>
 *
 * <p>There is no portable API that answers "how many frames fit". The only honest way to
 * find out is to try - which is what this class does, deliberately, in a controlled way
 * so the error is caught rather than killing the program.</p>
 *
 * <p><strong>Catching an {@code Error} is normally wrong</strong> and this class is the
 * exception that proves the rule. An {@code Error} signals a condition the program is not
 * expected to recover from, so code that swallows one usually just hides a disaster. Here
 * the overflow is the measurement itself, it is provoked on purpose, and by the time the
 * {@code catch} block runs the entire recursion has unwound - so the stack is empty again
 * and there is plenty of room to do the counting.</p>
 *
 * <p>This class is NOT thread-safe: it counts in a static field, so two threads probing
 * at the same time would overwrite each other's count. That is acceptable for a
 * measurement tool and unacceptable for anything else, so it is stated rather than
 * quietly hoped for.</p>
 */
public final class StackDepthProbe {

    // The deepest frame number reached so far. It has to live OUTSIDE the recursion,
    // because every local variable inside the recursion dies with its frame as the
    // stack unwinds - by the time we catch the error, they are all gone.
    private static int deepestReached;

    // The class name of whatever error the last probe actually caught. Recorded so a
    // test can assert we got a StackOverflowError specifically, rather than assuming it.
    private static String lastErrorType = "";

    // Static-only tool: no instances.
    private StackDepthProbe() {
    }

    /**
     * Recurses until the stack runs out, then reports how far it got.
     *
     * @return the number of nested calls that fitted on this thread's stack
     */
    public static int measureMaxDepth() {
        // Reset first, so each run reports its own result rather than the previous one.
        deepestReached = 0;
        lastErrorType = "";

        try {
            // One call in. This never returns normally - it can only end by overflowing.
            descend(1);
        } catch (StackOverflowError expected) {
            // Expected, provoked, and the whole point. Record WHAT we caught so the
            // claim "this throws StackOverflowError" is checked rather than asserted
            // by the lesson text alone.
            lastErrorType = expected.getClass().getName();
        }

        return deepestReached;
    }

    /**
     * Recurses with several extra local variables per call, so each frame is larger.
     *
     * <p>A frame's size comes from how many locals and how much working space the method
     * needs. Bigger frames mean fewer of them fit in the same fixed stack. Whether this
     * measurably differs from {@link #measureMaxDepth()} on YOUR machine is an
     * observation to make, not a promise: the JIT compiler is free to lay out frames
     * differently once it compiles the method.</p>
     *
     * @return the number of nested fat-framed calls that fitted on this thread's stack
     */
    public static int measureMaxDepthWithLargerFrames() {
        deepestReached = 0;
        lastErrorType = "";

        try {
            descendCarryingLocals(1, 1L, 2L, 3L, 4L);
        } catch (StackOverflowError expected) {
            lastErrorType = expected.getClass().getName();
        }

        return deepestReached;
    }

    /**
     * @return the class name of the error the last probe caught, or "" if none has run
     */
    public static String lastErrorType() {
        return lastErrorType;
    }

    /**
     * The recursion itself: the smallest method that can call itself forever.
     *
     * @param depth which call number this is
     */
    private static void descend(int depth) {
        // Record BEFORE recursing. The write must happen on the way down, because on
        // the way back up there is no way back up - the frame is being destroyed by
        // an error, not by a return.
        deepestReached = depth;

        // The call that never comes back. Note there is no work after it: if Java had
        // tail-call elimination this would loop forever instead of overflowing, and
        // the JVM specification deliberately does not require that optimisation.
        descend(depth + 1);
    }

    /**
     * The same recursion, but each frame also carries four long values.
     *
     * <p>The values are passed along and combined so the compiler cannot simply decide
     * they are unused and delete them, which would defeat the point of the measurement.</p>
     *
     * @param depth which call number this is
     * @param a     ballast that must survive the call
     * @param b     ballast that must survive the call
     * @param c     ballast that must survive the call
     * @param d     ballast that must survive the call
     */
    private static void descendCarryingLocals(int depth, long a, long b, long c, long d) {
        deepestReached = depth;

        // Each value is changed using the previous ones, so all four are genuinely
        // live across the recursive call and must occupy slots in this frame.
        descendCarryingLocals(depth + 1, b + 1, c + 1, d + 1, a + 1);
    }
}
