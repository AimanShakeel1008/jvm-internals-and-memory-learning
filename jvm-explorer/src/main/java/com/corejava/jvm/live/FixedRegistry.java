package com.corejava.jvm.live;

// LinkedHashMap remembers insertion order, which is what lets it find the OLDEST entry.
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * THE FIX for {@link LeakyRegistry}: the same registry, but bounded. When it is full,
 * adding a new session evicts the oldest one, so the oldest becomes unreachable and the
 * collector can reclaim it.
 *
 * <p>The cure for every reachability leak is to cut the path (Lesson 04). Here the path
 * is cut automatically, by a size limit. Real code uses the same idea through a caching
 * library with a size or time limit.</p>
 */
public final class FixedRegistry {

    // The most sessions ever held at once.
    public static final int CAPACITY = 20;

    // Same payload as the leaky version, so the two are directly comparable.
    public static final int PAYLOAD_BYTES = LeakyRegistry.PAYLOAD_BYTES;

    // A LinkedHashMap that removes its eldest entry once it grows past CAPACITY. The
    // braces after "new LinkedHashMap<>()" create an anonymous subclass that overrides
    // one method: removeEldestEntry, which the map calls after every put.
    private static final Map<String, byte[]> SESSIONS = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
            // Returning true tells the map to drop the eldest entry now. That entry's
            // byte[] loses its only reference and becomes garbage.
            return size() > CAPACITY;
        }
    };

    // Static-only: no instances.
    private FixedRegistry() {
    }

    /**
     * @param requestNumber which request this is
     * @return how many sessions the registry now holds - never more than {@link #CAPACITY}
     */
    public static synchronized int handleRequest(int requestNumber) {
        SESSIONS.put("session-" + requestNumber, new byte[PAYLOAD_BYTES]);
        return SESSIONS.size();
    }

    /**
     * @return how many sessions are currently held
     */
    public static synchronized int size() {
        return SESSIONS.size();
    }

    /**
     * @param requestNumber a request number
     * @return true if that request's session is still held
     */
    public static synchronized boolean holds(int requestNumber) {
        return SESSIONS.containsKey("session-" + requestNumber);
    }

    /**
     * Empties the registry, so tests start from nothing.
     */
    public static synchronized void clear() {
        SESSIONS.clear();
    }
}
