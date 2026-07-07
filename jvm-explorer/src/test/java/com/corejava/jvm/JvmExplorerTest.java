// Tests live in the SAME package as the code they test, but under src/test/java.
// Same package = tests may touch package-private members; separate folder = Maven
// compiles tests separately and never ships them inside the final jar.
package com.corejava.jvm;

// Brings in the @Test marker that tells JUnit "this method is a test to run".
import org.junit.jupiter.api.Test;

// Static imports let us write assertEquals(...) instead of the longer
// Assertions.assertEquals(...) - the standard, readable JUnit style.
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Lesson 00's test is really an ENVIRONMENT test: it proves the machine is set up
// exactly as the course pinned it, so that every later lesson's observations about
// JVM behavior are made on the JVM version those lessons assume.
class JvmExplorerTest {

    // Fails loudly if the JVM running the build is not Java 21 - catching, on day
    // one, the classic setup trap of a second Java installation hijacking the PATH.
    @Test
    void runsOnPinnedJavaVersion() {
        // Runtime.version() returns the running JVM's version as an object;
        // feature() extracts the major number (21 for any 21.x.y build).
        // We compare majors, not exact builds, because security patch releases
        // (21.0.1, 21.0.2, ...) are all still "Java 21" for this course.
        assertEquals(21, Runtime.version().feature(),
                // The message shown on failure - it should say what to FIX, not
                // merely what broke, because failing tests are read under stress.
                "Expected Java 21 (the pinned course version). "
                        + "Run 'java -version' and check troubleshooting.md.");
    }

    // Sanity check that the JVM reports a real, positive heap limit. If this ever
    // failed, the Runtime API itself would be broken - the point is to give later
    // memory lessons a baseline fact they can safely build on.
    @Test
    void jvmReportsAPositiveMaxHeap() {
        // maxMemory() is the byte ceiling the heap may grow to; any working JVM
        // must report a value above zero.
        assertTrue(Runtime.getRuntime().maxMemory() > 0,
                "The JVM reported a max heap of 0 bytes, which should be impossible.");
    }

    // Sanity check that the JVM sees at least one CPU core - the number it will
    // later use to size its garbage-collection threads (Lessons 04-05).
    @Test
    void jvmSeesAtLeastOneCpuCore() {
        // availableProcessors() can change over a JVM's lifetime (containers,
        // power settings) but can never legally be below 1.
        assertTrue(Runtime.getRuntime().availableProcessors() >= 1,
                "The JVM reported zero CPU cores, which should be impossible.");
    }
}
