// Lesson 05's package: reading the JVM's own GC log and turning it into a summary.
package com.corejava.jvm.gclog;

// The causes of one pause, kept as an unmodifiable list.
import java.util.List;

/**
 * One stop-the-world pause, as recorded by one line of the JVM's GC log.
 *
 * <p>A pause line from {@code -Xlog:gc} looks like this:</p>
 *
 * <pre>
 * [0.215s][info][gc] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 25M-&gt;3M(256M) 2.512ms
 * </pre>
 *
 * <p>and every field of this record is one piece of that line: when it happened, which
 * collection it belonged to, what kind of pause it was, why it happened, how full the
 * heap was before and after, how big the heap was, and how long the application stood
 * still.</p>
 *
 * @param gcId               the collection's number in the log, {@code GC(0)} -&gt; 0
 * @param uptimeSeconds      seconds since the JVM started, from the {@code [0.215s]} decoration
 * @param pauseKind          the word after "Pause": Young, Full, Remark, Cleanup, ...
 * @param causes             the bracketed reasons, e.g. Normal, G1 Evacuation Pause
 * @param heapBeforeBytes    heap in use when the pause began
 * @param heapAfterBytes     heap in use when the pause ended
 * @param heapCommittedBytes the heap's committed size, the number in brackets
 * @param pauseMillis        how long every application thread was stopped, in milliseconds
 */
public record GcEvent(long gcId, double uptimeSeconds, String pauseKind, List<String> causes,
                      long heapBeforeBytes, long heapAfterBytes, long heapCommittedBytes,
                      double pauseMillis) {

    /**
     * Compact constructor: one place that states what a valid pause looks like.
     */
    public GcEvent {
        // A pause with no kind could not be classified, so it is a parsing bug.
        if (pauseKind == null || pauseKind.isBlank()) {
            throw new IllegalArgumentException("A GC event needs a pause kind.");
        }
        // Times, sizes and durations are never negative in a real log.
        if (gcId < 0 || uptimeSeconds < 0 || heapBeforeBytes < 0 || heapAfterBytes < 0
                || heapCommittedBytes < 0 || pauseMillis < 0) {
            throw new IllegalArgumentException("GC event values cannot be negative: id=" + gcId
                    + ", uptime=" + uptimeSeconds + ", before=" + heapBeforeBytes + ", after=" + heapAfterBytes
                    + ", committed=" + heapCommittedBytes + ", pause=" + pauseMillis);
        }
        // A null list is replaced by an empty one, and any list is copied, so the
        // record can never be changed through a list the caller still holds.
        causes = causes == null ? List.of() : List.copyOf(causes);
    }

    /**
     * @return how much heap this pause freed, never negative
     */
    public long reclaimedBytes() {
        // "After" can be LARGER than "before": objects promoted during a pause, or a
        // remark pause that frees nothing while the program keeps allocating around it.
        // Reporting that as negative reclaim would be nonsense, so it counts as zero.
        return Math.max(0L, heapBeforeBytes - heapAfterBytes);
    }

    /**
     * @return true for a minor collection - the young generation only (Lesson 04)
     */
    public boolean isYoung() {
        return "Young".equals(pauseKind);
    }

    /**
     * @return true for a full collection of the whole heap - the long pause to watch for
     */
    public boolean isFull() {
        return "Full".equals(pauseKind);
    }

    /**
     * @return the causes joined for printing, e.g. "Normal / G1 Evacuation Pause"
     */
    public String causeText() {
        return causes.isEmpty() ? "(no cause given)" : String.join(" / ", causes);
    }
}
