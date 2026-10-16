package com.corejava.jvm.experiments;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks the experiment's argument handling - never runs the long-lived service itself.
 */
class LeakHuntTest {

    // No arguments: the leaky registry, no dump, the default time limit.
    @Test
    void defaultsAreLeakyNoDumpAndTenMinutes() {
        assertEquals(new LeakHunt.Options(false, false, LeakHunt.DEFAULT_SECONDS), LeakHunt.parse(new String[0]));
        assertEquals(new LeakHunt.Options(false, false, LeakHunt.DEFAULT_SECONDS), LeakHunt.parse(null));
    }

    // Flags in any order; nonsense refused with a message rather than silently ignored.
    @Test
    void flagsAreParsedAndNonsenseIsRejected() {
        assertEquals(new LeakHunt.Options(true, true, 30),
                LeakHunt.parse(new String[] {"--dump", "--seconds=30", "--fixed"}));
        assertThrows(IllegalArgumentException.class, () -> LeakHunt.parse(new String[] {"--seconds=0"}));
        assertThrows(IllegalArgumentException.class, () -> LeakHunt.parse(new String[] {"--leak-faster"}));
    }
}
