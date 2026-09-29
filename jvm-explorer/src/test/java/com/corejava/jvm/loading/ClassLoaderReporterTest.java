// Same package as the class under test, so package-private members would be reachable
// and the test folder mirrors the main folder exactly.
package com.corejava.jvm.loading;

// The class from the top-level package we use as "a class of our own", to contrast with
// classes that come out of the JDK.
import com.corejava.jvm.JvmExplorer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Turns the lesson's claims about class loaders into checks the machine makes for us.
 *
 * <p>These tests assert only facts the Java specification guarantees - that core JDK
 * classes come from the bootstrap loader, that every chain ends there, that the built-in
 * loaders are arranged app to platform to bootstrap. They deliberately do NOT assert
 * which loader loaded the test classes themselves: a test framework is entitled to use
 * a loader of its own, and a test that breaks when the runner changes is testing the
 * runner, not the JVM.</p>
 */
class ClassLoaderReporterTest {

    // The two names the JVM gives its built-in loaders since Java 9. Named constants so
    // a failure message mentions a value with a meaning attached, not a bare string.
    private static final String APPLICATION_LOADER_NAME = "app";
    private static final String PLATFORM_LOADER_NAME = "platform";

    // The rule that explains the most surprising line in this whole lesson: for a core
    // JDK class, getClassLoader() hands back null. That null is not a failure and not a
    // missing value - it is how the JVM spells "the bootstrap loader", which is C++ code
    // inside the JVM and has no Java object to return.
    @Test
    void coreJdkClassesAreLoadedByTheBootstrapLoader() {
        // The raw fact, straight from the JDK, with no help from our code.
        assertNull(String.class.getClassLoader(),
                "java.lang.String lives in java.base and must be defined by the bootstrap loader (null).");

        // Our helper must agree with that raw fact rather than inventing its own answer.
        assertTrue(ClassLoaderReporter.isLoadedByBootstrap(String.class),
                "isLoadedByBootstrap must report true exactly when getClassLoader() is null.");

        // A bootstrap-loaded class has nothing above it, so its chain is one entry long.
        assertEquals(List.of(ClassLoaderReporter.BOOTSTRAP_DESCRIPTION),
                ClassLoaderReporter.chainFor(String.class),
                "A bootstrap-loaded class has no parents, so its chain is just the bootstrap loader.");
    }

    // Our own code arrives from the class path, which no built-in JDK loader owns. So it
    // must come from a loader below the platform loader, and never from the bootstrap.
    @Test
    void ourOwnClassesAreLoadedByAClassPathLoaderNotTheBootstrap() {
        assertFalse(ClassLoaderReporter.isLoadedByBootstrap(JvmExplorer.class),
                "Project classes come from the class path, which the bootstrap loader never searches.");

        List<String> chain = ClassLoaderReporter.chainFor(JvmExplorer.class);

        // At minimum: the loader that defined it, the platform loader, and bootstrap.
        // "at least" rather than "exactly", because a test runner may legitimately add
        // a loader of its own below the application loader.
        assertTrue(chain.size() >= 3,
                "Expected at least loader -> platform -> bootstrap, but the chain was: " + chain);

        // The one thing that is true of every chain in every JVM.
        assertEquals(ClassLoaderReporter.BOOTSTRAP_DESCRIPTION, chain.get(chain.size() - 1),
                "Every delegation chain ends at the bootstrap loader.");
    }

    // The structure the whole lesson rests on, asserted directly from the JDK's own
    // accessors rather than through any class of ours.
    @Test
    void theBuiltInLoadersFormTheDocumentedThreeStepChain() {
        ClassLoader application = ClassLoader.getSystemClassLoader();
        ClassLoader platform = ClassLoader.getPlatformClassLoader();

        // Since Java 9 these loaders carry names, which is what makes a report readable.
        assertEquals(APPLICATION_LOADER_NAME, application.getName(),
                "The class-path loader is named \"app\" from Java 9 onward.");
        assertEquals(PLATFORM_LOADER_NAME, platform.getName(),
                "The loader for non-core JDK modules is named \"platform\" from Java 9 onward.");

        // The delegation links themselves: app's parent IS the platform loader object.
        // assertSame, not assertEquals, because identity is the claim - there is exactly
        // one platform loader in a JVM.
        assertSame(platform, application.getParent(),
                "The application loader must delegate to the platform loader.");

        // And the platform loader's parent is the bootstrap loader, which is null.
        assertNull(platform.getParent(),
                "The platform loader's parent is the bootstrap loader, spelled null.");
    }

    // The helper's whole job is to make the chain readable, so the descriptions must
    // actually say something useful.
    @Test
    void describeTranslatesNullIntoWordsAndNamesRealLoaders() {
        assertEquals(ClassLoaderReporter.BOOTSTRAP_DESCRIPTION, ClassLoaderReporter.describe(null),
                "null must be described as the bootstrap loader, never left as \"null\".");

        // A real loader's description must contain its name, or the report tells the
        // reader nothing they could not have guessed.
        assertTrue(ClassLoaderReporter.describe(ClassLoader.getPlatformClassLoader())
                        .contains(PLATFORM_LOADER_NAME),
                "The platform loader's description must contain its name.");
        assertTrue(ClassLoaderReporter.describe(ClassLoader.getSystemClassLoader())
                        .contains(APPLICATION_LOADER_NAME),
                "The application loader's description must contain its name.");
    }

    // The invariant that makes the chain a chain: it always terminates, and always at
    // the same place, no matter where you start.
    @Test
    void everyChainEndsAtTheBootstrapLoaderWhereverItStarts() {
        // One class from java.base, one of ours, and one from the loading module itself.
        List<Class<?>> samples = List.of(String.class, JvmExplorer.class, ClassLoaderReporter.class);

        for (Class<?> sample : samples) {
            List<String> chain = ClassLoaderReporter.chainFor(sample);
            assertEquals(ClassLoaderReporter.BOOTSTRAP_DESCRIPTION, chain.get(chain.size() - 1),
                    "The chain for " + sample.getName() + " must end at the bootstrap loader.");
        }

        // The JVM's own chain, asked for without naming any class at all, follows the
        // same rule and is at least app -> platform -> bootstrap.
        List<String> builtIn = ClassLoaderReporter.builtInDelegationChain();
        assertTrue(builtIn.size() >= 3, "Expected at least three built-in loaders, got: " + builtIn);
        assertEquals(ClassLoaderReporter.BOOTSTRAP_DESCRIPTION, builtIn.get(builtIn.size() - 1),
                "The built-in delegation chain must end at the bootstrap loader.");
    }

    // Bad input fails immediately, with a message naming the mistake - the same contract
    // ClassFileInspector follows, so the project behaves consistently.
    @Test
    void reportingANullClassIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ClassLoaderReporter.chainFor(null),
                "chainFor(null) must be rejected with a clear IllegalArgumentException.");
        assertThrows(IllegalArgumentException.class, () -> ClassLoaderReporter.isLoadedByBootstrap(null),
                "isLoadedByBootstrap(null) must be rejected with a clear IllegalArgumentException.");
    }
}
