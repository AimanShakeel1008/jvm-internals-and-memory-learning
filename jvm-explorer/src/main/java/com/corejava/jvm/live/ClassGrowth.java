package com.corejava.jvm.live;

/**
 * How one class changed between two class histograms taken some time apart.
 *
 * <p>A single histogram mostly shows what every Java program has a lot of - byte arrays,
 * strings. A class whose count keeps GROWING between two histograms, with nothing in the
 * program's workload that should explain it, is the classic leak suspect.</p>
 *
 * @param className       the class, as the JVM writes it
 * @param instancesBefore instances in the first histogram (0 if absent)
 * @param instancesAfter  instances in the second histogram (0 if absent)
 * @param bytesBefore     shallow bytes in the first histogram
 * @param bytesAfter      shallow bytes in the second histogram
 */
public record ClassGrowth(String className, long instancesBefore, long instancesAfter,
                          long bytesBefore, long bytesAfter) {

    /**
     * @return instances gained (negative if the class shrank)
     */
    public long instanceGrowth() {
        return instancesAfter - instancesBefore;
    }

    /**
     * @return bytes gained (negative if the class shrank)
     */
    public long byteGrowth() {
        return bytesAfter - bytesBefore;
    }

    /**
     * @return one aligned line for the report
     */
    public String describe() {
        return String.format("%+10d instances  %+14d bytes  %s",
                instanceGrowth(), byteGrowth(), ClassHistogram.readableName(className));
    }
}
