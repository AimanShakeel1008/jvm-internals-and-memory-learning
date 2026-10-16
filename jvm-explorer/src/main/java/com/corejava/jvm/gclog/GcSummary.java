package com.corejava.jvm.gclog;

// "A number, or nothing": a log with no full collection has no live-data estimate.
import java.util.OptionalLong;

/**
 * The handful of numbers worth knowing about a GC log, computed from its pauses.
 *
 * <p>These are the numbers an engineer reads first: how many pauses of each kind, how
 * long they were in total and at worst, how much they reclaimed, what share of the run
 * was spent paused - and how full the heap was after the last full collection, which is
 * the best estimate of how much live data the program really holds (Lesson 04).</p>
 *
 * @param collectorName         the collector the log named
 * @param pauseCount            every stop-the-world pause found
 * @param youngCount            minor collections (Pause Young)
 * @param fullCount             full collections (Pause Full)
 * @param otherCount            other pauses, such as G1's Remark and Cleanup
 * @param totalPauseMillis      the sum of every pause
 * @param maxPauseMillis        the single longest pause
 * @param totalReclaimedBytes   heap freed by all pauses together
 * @param spanSeconds           JVM uptime at the last pause - the stretch of run the log covers
 * @param heapAfterLastFullBytes heap in use after the last full collection, if there was one
 */
public record GcSummary(String collectorName, int pauseCount, int youngCount, int fullCount, int otherCount,
                        double totalPauseMillis, double maxPauseMillis, long totalReclaimedBytes,
                        double spanSeconds, OptionalLong heapAfterLastFullBytes) {

    // Bytes per megabyte, for the printed report.
    private static final double BYTES_PER_MEGABYTE = 1024.0 * 1024.0;

    /**
     * Computes the summary of a parsed log.
     *
     * @param log a parsed GC log, possibly with no pauses at all
     * @return its summary
     */
    public static GcSummary of(ParsedGcLog log) {
        if (log == null) {
            throw new IllegalArgumentException("Cannot summarise a null log.");
        }

        int young = 0;
        int full = 0;
        int other = 0;
        double total = 0;
        double max = 0;
        long reclaimed = 0;
        double span = 0;
        OptionalLong afterLastFull = OptionalLong.empty();

        // One pass over the pauses, in log order, updating every running figure.
        for (GcEvent event : log.pauses()) {
            if (event.isYoung()) {
                young++;
            } else if (event.isFull()) {
                full++;
                // Overwritten each time, so it ends up holding the LAST full collection:
                // the most recent measurement of how much is still reachable.
                afterLastFull = OptionalLong.of(event.heapAfterBytes());
            } else {
                other++;
            }
            total += event.pauseMillis();
            max = Math.max(max, event.pauseMillis());
            reclaimed += event.reclaimedBytes();
            // Uptimes only grow down a log, so the last one is the largest.
            span = Math.max(span, event.uptimeSeconds());
        }

        return new GcSummary(log.collectorName(), log.pauses().size(), young, full, other,
                total, max, reclaimed, span, afterLastFull);
    }

    /**
     * @return the average pause in milliseconds, or 0 if there were no pauses
     */
    public double averagePauseMillis() {
        // Guarding the division: an empty log is a legitimate input, not a crash.
        return pauseCount == 0 ? 0.0 : totalPauseMillis / pauseCount;
    }

    /**
     * @return the share of the logged run spent in stop-the-world pauses, as a percentage
     */
    public double pausePercent() {
        if (spanSeconds <= 0) {
            return 0.0;
        }
        // Pauses are in milliseconds and the span in seconds; convert before dividing.
        return (totalPauseMillis / (spanSeconds * 1000.0)) * 100.0;
    }

    /**
     * @return throughput as a percentage: the share of the run NOT spent paused
     */
    public double throughputPercent() {
        // Approximate by design: the span ends at the last pause, not at the JVM's exit,
        // and concurrent collector work (which steals CPU without pausing) is invisible
        // in pause lines. The report says so.
        return 100.0 - pausePercent();
    }

    /**
     * @return a multi-line, human-readable report
     */
    public String describe() {
        StringBuilder report = new StringBuilder();
        report.append(String.format("collector            : %s%n", collectorName));
        report.append(String.format("pauses               : %d  (young %d, full %d, other %d)%n",
                pauseCount, youngCount, fullCount, otherCount));
        report.append(String.format("total / avg / max    : %.1f ms / %.2f ms / %.1f ms%n",
                totalPauseMillis, averagePauseMillis(), maxPauseMillis));
        report.append(String.format("heap reclaimed       : %.1f MB%n", totalReclaimedBytes / BYTES_PER_MEGABYTE));
        report.append(String.format("log covers           : %.2f s of uptime, %.2f%% of it paused "
                + "(throughput about %.2f%%)%n", spanSeconds, pausePercent(), throughputPercent()));
        // The live-data estimate only exists if a full collection happened.
        if (heapAfterLastFullBytes.isPresent()) {
            report.append(String.format("live data estimate   : %.1f MB (heap after the last full GC)",
                    heapAfterLastFullBytes.getAsLong() / BYTES_PER_MEGABYTE));
        } else {
            report.append("live data estimate   : n/a (no full GC in this log)");
        }
        return report.toString();
    }
}
