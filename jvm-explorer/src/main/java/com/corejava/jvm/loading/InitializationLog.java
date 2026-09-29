// A sub-package for everything this lesson adds about class loading, kept apart from
// the top-level com.corejava.jvm classes the same way experiments/ is kept apart.
// The folder path on disk MUST match this line: src/main/java/com/corejava/jvm/loading/.
package com.corejava.jvm.loading;

// ArrayList is the ordinary growable list; we need order, because "what initialized
// first" is exactly the question this class exists to answer.
import java.util.ArrayList;
// Collections.synchronizedList wraps a list so that two threads adding at the same
// moment cannot corrupt it. Class initialization can be triggered from any thread,
// so the witness has to be safe for that even though our tests are single-threaded.
import java.util.Collections;
// List is the interface we hand back to callers, so nobody can rely on our choice
// of ArrayList and we stay free to change it.
import java.util.List;

/**
 * A witness that records which classes have run their static initializer, in order.
 *
 * <p>Class initialization is invisible: it happens once, at a moment the JVM chooses,
 * and leaves no trace. To study it we need a trace, so every demonstration class in
 * this package calls {@link #record(String)} from its static initializer. Reading this
 * log afterwards answers the only question that matters: <em>did that class initialize
 * yet, and what initialized before it?</em></p>
 *
 * <p>This class is deliberately boring - it has no static initializer of its own beyond
 * creating the list, so simply asking it a question never disturbs the experiment.</p>
 */
public final class InitializationLog {

    // The recorded events, in the order they happened. "static" because there is one
    // log for the whole JVM - which is the right shape, since a class also initializes
    // once per JVM (strictly: once per class loader, which Section 3 explains).
    // "final" so the reference can never be swapped out from under a running test.
    private static final List<String> EVENTS = Collections.synchronizedList(new ArrayList<>());

    // A witness with no state of its own to carry: nobody should ever create one.
    private InitializationLog() {
    }

    /**
     * Called from a class's static initializer to say "I ran".
     *
     * @param simpleClassName the short name of the class that is initializing
     */
    public static void record(String simpleClassName) {
        // No validation, no formatting, no I/O. A static initializer is the worst
        // possible place for something that might throw or block, and this method is
        // called from six of them.
        EVENTS.add(simpleClassName);
    }

    /**
     * @return every recorded initialization, oldest first, as an unmodifiable snapshot
     */
    public static List<String> events() {
        // synchronizedList makes each individual add safe, but COPYING the list means
        // iterating it, and iteration must be guarded by hand - that is the documented
        // rule for synchronized wrappers, and forgetting it is a classic bug.
        synchronized (EVENTS) {
            // List.copyOf hands back an immutable snapshot, so a caller cannot alter
            // the history of the experiment by accident.
            return List.copyOf(EVENTS);
        }
    }

    /**
     * @param simpleClassName the short name passed to {@link #record(String)}
     * @return true if that class has already run its static initializer
     */
    public static boolean wasInitialized(String simpleClassName) {
        // The whole point of the lesson in one method: this answers "has it happened
        // YET", and the answer changes as the program touches things.
        return events().contains(simpleClassName);
    }

    /**
     * @param simpleClassName the short name passed to {@link #record(String)}
     * @return its position in the initialization order, or -1 if it never initialized
     */
    public static int positionOf(String simpleClassName) {
        // Order matters for one rule in particular: a superclass ALWAYS initializes
        // before its subclass, and a test can only prove that by comparing positions.
        return events().indexOf(simpleClassName);
    }
}
