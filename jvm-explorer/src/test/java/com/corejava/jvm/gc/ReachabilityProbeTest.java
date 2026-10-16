package com.corejava.jvm.gc;

import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the reachability rule by watching objects through weak references.
 *
 * <p><strong>Two kinds of claim live in this file, and they are not equally strong.</strong>
 * "A reachable object is never collected" is a guarantee of the Java specification, so
 * those tests can never fail on a correct JVM. "An unreachable object IS collected" is
 * not guaranteed - the collector may run when it likes, and {@code System.gc()} is only
 * a request. Those tests rely on HotSpot honouring {@code System.gc()} with a full
 * collection, which it does by default. If you run with {@code -XX:+DisableExplicitGC},
 * they will fail, and troubleshooting.md says so.</p>
 */
class ReachabilityProbeTest {

    // GUARANTEED: an object reachable from a root (a live local variable) survives.
    @Test
    void aReachableObjectIsNeverCollected() {
        assertTrue(ReachabilityProbe.reachableObjectSurvives(),
                "An object still reachable from a live local variable must survive any collection.");
    }

    // GUARANTEED: a static field is a root, so whatever it reaches stays alive - the
    // mechanism behind every real Java memory leak.
    @Test
    void anObjectHeldByAStaticFieldIsNeverCollected() {
        assertTrue(ReachabilityProbe.objectHeldByStaticFieldSurvives(),
                "An object reachable from a static field must survive any collection.");
    }

    // RELIES ON System.gc() being honoured (HotSpot default): unreachable means collectable.
    @Test
    void anUnreachableObjectIsCollected() {
        assertTrue(ReachabilityProbe.unreachableObjectIsCollected(),
                "An object no root can reach should be reclaimed after requested collections. "
                        + "If this fails, check that -XX:+DisableExplicitGC is not set.");
    }

    // RELIES ON System.gc() being honoured: a cycle reachable from no root is garbage too.
    // This is the test that a reference-counting collector would fail.
    @Test
    void anUnreachableCycleIsCollected() {
        assertTrue(ReachabilityProbe.unreachableCycleIsCollected(),
                "Two objects pointing at each other, reachable from no root, should both be reclaimed. "
                        + "If this fails, check that -XX:+DisableExplicitGC is not set.");
    }

    // GUARANTEED in the other direction: watching an object we still hold must report
    // "not cleared", and bad arguments must be refused.
    @Test
    void watchingAHeldObjectReportsNotClearedAndBadArgumentsAreRejected() {
        Object held = new Object();
        WeakReference<Object> watcher = new WeakReference<>(held);

        assertFalse(ReachabilityProbe.requestGcUntilCleared(watcher, 2),
                "An object this test still holds cannot have been cleared.");

        // Keeps "held" reachable up to here, so the JIT cannot treat it as dead early.
        java.lang.ref.Reference.reachabilityFence(held);

        assertThrows(IllegalArgumentException.class, () -> ReachabilityProbe.requestGcUntilCleared(null, 1),
                "Watching through a null reference must be rejected.");
        assertThrows(IllegalArgumentException.class, () -> ReachabilityProbe.requestGcUntilCleared(watcher, 0),
                "Zero attempts could never observe anything.");
        assertThrows(IllegalArgumentException.class, () -> ReachabilityProbe.requestGc(0),
                "Requesting zero collections is meaningless.");
    }
}
