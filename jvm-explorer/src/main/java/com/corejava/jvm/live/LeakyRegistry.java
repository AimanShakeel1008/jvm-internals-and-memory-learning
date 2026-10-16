// Lesson 07's package: examining a running JVM from the outside - and the leak we
// plant on purpose so there is something real to find.
package com.corejava.jvm.live;

import java.util.HashMap;
import java.util.Map;

/**
 * THE PLANTED LEAK. A registry of "sessions" that adds one entry per request and never
 * removes any.
 *
 * <p>This is the most common shape of a real Java memory leak (Lesson 04): a static map
 * - a GC root - that only ever grows. Every session stays reachable through
 * <em>root -&gt; static field -&gt; HashMap -&gt; entry -&gt; LeakedSession -&gt; byte[]</em>,
 * so the collector is forbidden to reclaim any of them. Nothing is wrong with the
 * collector. Something is wrong with the program, and Lesson 07's tools find it.</p>
 *
 * <p>The class and record are named distinctly on purpose, so they stand out in a class
 * histogram and a heap dump.</p>
 */
public final class LeakyRegistry {

    // Each leaked session carries 32 KB, so the leak grows fast enough to see in minutes.
    public static final int PAYLOAD_BYTES = 32 * 1024;

    // A static field: a GC root for as long as this class is loaded - the whole run.
    // "final" protects the reference, not the contents; the map itself grows forever.
    private static final Map<String, LeakedSession> SESSIONS = new HashMap<>();

    // Static-only: no instances.
    private LeakyRegistry() {
    }

    /**
     * One "user session" - the object that leaks.
     *
     * @param id      the session's name
     * @param payload the data it holds, standing in for a user's cached state
     */
    public record LeakedSession(String id, byte[] payload) {
    }

    /**
     * Handles one request: creates a session and registers it. Never removes anything.
     *
     * @param requestNumber which request this is
     * @return how many sessions the registry now holds
     */
    public static synchronized int handleRequest(int requestNumber) {
        String id = "session-" + requestNumber;
        // THE BUG: every request adds an entry, and no code anywhere removes one. A real
        // version usually looks innocent - a cache keyed by something unique per request.
        SESSIONS.put(id, new LeakedSession(id, new byte[PAYLOAD_BYTES]));
        return SESSIONS.size();
    }

    /**
     * @return how many sessions are currently held
     */
    public static synchronized int size() {
        return SESSIONS.size();
    }

    /**
     * @return roughly how many payload bytes the leak is holding (object headers and the
     *         map's own entries come on top)
     */
    public static synchronized long retainedPayloadBytes() {
        return (long) SESSIONS.size() * PAYLOAD_BYTES;
    }

    /**
     * Empties the registry - the fix in its bluntest form, and how tests reset it.
     */
    public static synchronized void clear() {
        SESSIONS.clear();
    }
}
