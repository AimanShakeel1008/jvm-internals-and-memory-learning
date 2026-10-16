package com.corejava.jvm.experiments;

import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the SHAPE of the generations experiment's output, never its numbers.
 *
 * <p>When objects get promoted depends on the collector, the young-generation size, the
 * promotion age and what else is alive - so no test here asserts "promotion happens in
 * round 4". That is an observation for the learner's own terminal, exactly like the
 * warm-up timings in Lesson 01.</p>
 */
class GenerationsExperimentTest {

    // A row must carry the round, the count and both sizes in kilobytes.
    @Test
    void aRowShowsTheRoundTheCountAndBothSizes() {
        String row = GenerationsExperiment.formatRow("3", 7, OptionalLong.of(1024 * 1024), OptionalLong.of(2048));

        assertTrue(row.startsWith("3"), "The row must begin with its round label: " + row);
        assertTrue(row.contains("7"), "The row must show the collection count: " + row);
        assertTrue(row.contains("1024 KB"), "1 MB of survivor must print as 1024 KB: " + row);
        assertTrue(row.contains("2 KB"), "2048 bytes of old must print as 2 KB: " + row);
    }

    // On a collector without separate generations, the table must say so rather than
    // print zeros that look like real measurements.
    @Test
    void aMissingGenerationPrintsNotApplicableRatherThanZero() {
        String row = GenerationsExperiment.formatRow("start", 0, OptionalLong.empty(), OptionalLong.empty());

        assertTrue(row.contains("n/a"), "An absent pool must print n/a: " + row);
    }
}
