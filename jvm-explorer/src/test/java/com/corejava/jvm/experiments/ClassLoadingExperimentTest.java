package com.corejava.jvm.experiments;

// Lesson 01's module, reused here: it reads the first eight bytes of any loaded class's
// own file. Pointing it at a NESTED class proves that a nested class really does get a
// separate .class file of its own.
import com.corejava.jvm.ClassFileHeader;
import com.corejava.jvm.ClassFileInspector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the SHAPE of the class-loading experiment, never its timing.
 *
 * <p>There is deliberately no test here asserting WHEN the nested class was loaded.
 * That is observable only from the outside, by running the JVM with
 * {@code -verbose:class} and reading its log, and the specification leaves the exact
 * moment of loading up to the implementation - it only promises a class is not
 * INITIALIZED too early. A test that pinned the loading moment would be asserting an
 * implementation detail, and would be exactly the kind of flaky test Lesson 01 warned
 * about. The learner's own terminal is the authority for that observation.</p>
 */
class ClassLoadingExperimentTest {

    // A compile-time constant can be read by anyone who was compiled against it, and the
    // value they read is their own inlined copy. Pinning the text here means that if
    // somebody edits the constant without rebuilding everything, this test says so.
    @Test
    void theCompileTimeConstantIsPinned() {
        assertEquals("read without loading LateComer at all",
                ClassLoadingExperiment.LateComer.PINNED_NOTE,
                "The inlined constant must still hold the text the experiment narrates.");
    }

    // Calling a static method forces the declaring class to initialize first, so this
    // value can only be correct if the initializer really ran.
    @Test
    void callingAStaticMethodProducesTheInitializedValue() {
        assertEquals("LateComer is now fully initialized",
                ClassLoadingExperiment.LateComer.stamp(),
                "stamp() returns a value that only exists after initialization has run.");
    }

    // A nested class is not a decoration in the outer class's file: javac compiles it
    // into its own separate class file, named Outer$Inner.class. Lesson 01's inspector
    // finds that file through the class path and reads its header, which is only possible
    // if the file genuinely exists.
    @Test
    void theNestedClassHasAClassFileOfItsOwn() {
        // Using a class literal here, which loads the class but does not initialize it -
        // the very rule this experiment exists to demonstrate.
        ClassFileHeader header = ClassFileInspector.inspect(ClassLoadingExperiment.LateComer.class);

        assertTrue(header.hasJavaMagic(),
                "A nested class has a real .class file of its own, starting with 0xCAFEBABE.");
        assertEquals("com.corejava.jvm.experiments.ClassLoadingExperiment$LateComer",
                header.className(),
                "The nested class's binary name joins outer and inner with a dollar sign.");
    }
}
