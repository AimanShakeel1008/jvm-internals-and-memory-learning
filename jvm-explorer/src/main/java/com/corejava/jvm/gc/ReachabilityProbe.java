package com.corejava.jvm.gc;

// Reference.reachabilityFence: a promise to the JVM that an object stays reachable up
// to that exact line, even if the variable holding it is never used again.
import java.lang.ref.Reference;
// A reference that lets us WATCH an object without keeping it alive (explained below).
import java.lang.ref.WeakReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Proves, by observation, the rule that decides when an object becomes garbage.
 *
 * <p>The rule is reachability: an object stays alive exactly as long as some chain of
 * references leads to it from a GC root - a local variable in a live frame, a static
 * field, a running thread. To observe that rule from Java we need a way to point at an
 * object without that pointing itself keeping it alive. Every ordinary reference keeps
 * its target alive, so ordinary references cannot do this.</p>
 *
 * <p>A {@link WeakReference} can. It is a small wrapper object that refers to another
 * object but does NOT count toward reachability: if the only remaining path to an object
 * goes through weak references, the collector may reclaim it, and when it does,
 * {@code get()} starts returning {@code null}. That turns "was it collected?" into a
 * question a program can ask.</p>
 */
public final class ReachabilityProbe {

    // How many times to ask for a collection before concluding it is not coming. More
    // than one, because System.gc() is a REQUEST the JVM may satisfy at its own pace.
    public static final int DEFAULT_GC_ATTEMPTS = 10;

    // A static field is a GC root: everything reachable from here stays alive for as
    // long as the class is loaded. That is precisely how real Java memory leaks happen,
    // and why this list is cleared at the end of every method that uses it.
    private static final List<Object> STATIC_HOLDER = new ArrayList<>();

    // Static-only probe: no instances.
    private ReachabilityProbe() {
    }

    /**
     * A tiny object that can point at another of its own kind, used to build a cycle.
     */
    private static final class Node {
        // The arrow to the next node. Two nodes pointing at each other form a cycle.
        Node next;
    }

    /**
     * Asks for a garbage collection the given number of times.
     *
     * @param times how many requests to make, at least 1
     */
    public static void requestGc(int times) {
        if (times < 1) {
            throw new IllegalArgumentException("Must request at least one collection, but was: " + times);
        }
        for (int i = 0; i < times; i++) {
            // A REQUEST, not a command: the specification lets the JVM ignore it, and
            // the flag -XX:+DisableExplicitGC makes HotSpot ignore it on purpose. With
            // the default settings HotSpot honours it with a full collection.
            System.gc();
        }
    }

    /**
     * Asks for collections until the watched object has been reclaimed, or attempts run out.
     *
     * @param watcher  a weak reference to the object being watched
     * @param attempts how many collections to request at most, at least 1
     * @return true if the object was collected
     */
    public static boolean requestGcUntilCleared(WeakReference<?> watcher, int attempts) {
        if (watcher == null) {
            throw new IllegalArgumentException("Cannot watch through a null reference.");
        }
        if (attempts < 1) {
            throw new IllegalArgumentException("Must make at least one attempt, but was: " + attempts);
        }
        for (int i = 0; i < attempts; i++) {
            // Check first: an object can be collected by a collection we did not ask for.
            if (watcher.get() == null) {
                return true;
            }
            System.gc();
        }
        // One last look after the final request.
        return watcher.get() == null;
    }

    /**
     * @return true if an object that is still reachable survives requested collections -
     *         which the rules guarantee
     */
    public static boolean reachableObjectSurvives() {
        // A strong (ordinary) reference from a local variable in this live frame: a root.
        Object strong = new Object();

        // A weak reference to the same object, so we can check on it afterwards.
        WeakReference<Object> watcher = new WeakReference<>(strong);

        requestGc(2);
        boolean survived = watcher.get() != null;

        // Without this line the JIT is allowed to decide that "strong" is never used
        // again after the WeakReference was built, treat it as dead, and let the object
        // be collected while the variable is still technically in scope. Scope is a
        // compile-time idea; reachability is decided at run time. The fence keeps the
        // object reachable up to here, which is what makes "survived" trustworthy.
        Reference.reachabilityFence(strong);

        return survived;
    }

    /**
     * @return true if an object nobody can reach is reclaimed after requested collections
     */
    public static boolean unreachableObjectIsCollected() {
        // The new object is referenced ONLY by the weak reference - no root leads to it,
        // so it is garbage from the moment this line finishes.
        WeakReference<Object> watcher = new WeakReference<>(new Object());
        return requestGcUntilCleared(watcher, DEFAULT_GC_ATTEMPTS);
    }

    /**
     * @return true if two objects that point at each other, but that nothing else can
     *         reach, are both reclaimed - the case that defeats reference counting
     */
    public static boolean unreachableCycleIsCollected() {
        Node first = new Node();
        Node second = new Node();

        // The cycle: each node keeps the other "referenced". A collector that counted
        // incoming arrows would see 1 for each, forever, and never free either.
        first.next = second;
        second.next = first;

        WeakReference<Node> watchFirst = new WeakReference<>(first);
        WeakReference<Node> watchSecond = new WeakReference<>(second);

        // Cut the only paths from a root (this frame's two locals). The nodes still
        // point at each other, but no chain from any root reaches either of them.
        first = null;
        second = null;

        // A tracing collector starts from the roots, never reaches the pair, and so
        // reclaims both - the cycle makes no difference at all.
        return requestGcUntilCleared(watchFirst, DEFAULT_GC_ATTEMPTS)
                && requestGcUntilCleared(watchSecond, DEFAULT_GC_ATTEMPTS);
    }

    /**
     * @return true if an object held only by a static collection survives requested
     *         collections - the shape of every real Java memory leak
     */
    public static boolean objectHeldByStaticFieldSurvives() {
        Object held = new Object();
        STATIC_HOLDER.add(held);
        WeakReference<Object> watcher = new WeakReference<>(held);

        // Our local arrow is gone. The ONLY remaining path is static field -> list ->
        // object, and a static field is a root, so the object must stay.
        held = null;

        requestGc(2);
        boolean survived = watcher.get() != null;

        // Undo the "leak" so this probe leaves the JVM exactly as it found it.
        STATIC_HOLDER.clear();
        return survived;
    }

    /**
     * @return a short report of all four observations, for the program's console output
     */
    public static String describe() {
        return "reachable object survives a GC request : " + reachableObjectSurvives()
                + System.lineSeparator()
                + "unreachable object is collected         : " + unreachableObjectIsCollected()
                + System.lineSeparator()
                + "unreachable CYCLE is collected          : " + unreachableCycleIsCollected()
                + System.lineSeparator()
                + "object held by a static list survives   : " + objectHeldByStaticFieldSurvives();
    }
}
