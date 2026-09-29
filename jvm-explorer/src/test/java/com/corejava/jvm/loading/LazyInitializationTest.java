package com.corejava.jvm.loading;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves, by observation rather than by claim, exactly what does and does not trigger a
 * class's static initializer.
 *
 * <p><strong>Why each test owns its own subject class.</strong> A class initializes at
 * most once per class loader for the whole life of a JVM, and there is no way to undo
 * it. All of a project's tests normally share one JVM, and JUnit gives no promise about
 * the order test methods run in. So a test that says "this class has not initialized
 * yet" is only trustworthy if it is the ONLY code in the project that ever touches that
 * class. Each test below therefore has a private subject, does its whole before-and-after
 * story inside a single method, and no other file may touch those classes.</p>
 */
class LazyInitializationTest {

    // Reading a compile-time constant is not "using" the class it appears to come from.
    // javac copies the value into the reader's own class file, so at run time this test's
    // bytecode does not mention ConstantHolder at all - and a class nobody mentions never
    // initializes. Reading a field that is NOT a compile-time constant does the opposite.
    @Test
    void readingACompileTimeConstantNeverInitializesTheClass() {
        // Step 1: nothing has touched it yet, so the witness log is silent about it.
        assertFalse(InitializationLog.wasInitialized("ConstantHolder"),
                "Nothing in the project may touch ConstantHolder before this test - the log must be silent.");

        // Step 2: read two compile-time constants. Both values were baked into THIS
        // class's constant pool when javac compiled it.
        assertEquals(65, ConstantHolder.PINNED_MAJOR_VERSION,
                "The pinned major version constant must still be 65 (Java 21).");
        assertEquals("JVM Internals & Memory", ConstantHolder.COURSE_NAME,
                "The course-name constant must be readable without loading its class.");

        // Step 3: still cold. This is the assertion that surprises people, and it is the
        // mechanism behind "I changed the constant, rebuilt the library, and the caller
        // still prints the old value" - the caller has its own copy.
        assertFalse(InitializationLog.wasInitialized("ConstantHolder"),
                "Reading compile-time constants must NOT initialize the class that declares them.");

        // Step 4: read a field whose value can only be produced by running code. Now the
        // JVM has no choice: the initializer must run before the field can be read.
        assertEquals("Course 01 pins Java 21", ConstantHolder.DESCRIPTION,
                "DESCRIPTION is computed during initialization, so it must read back correctly.");
        assertTrue(InitializationLog.wasInitialized("ConstantHolder"),
                "Reading a NON-constant static field must initialize the declaring class.");
    }

    // Three ways of mentioning a class that all leave it cold, and one that does not.
    @Test
    void mentioningAClassIsNotUsingIt() {
        assertFalse(InitializationLog.wasInitialized("LazyHolder"),
                "Nothing in the project may touch LazyHolder before this test.");

        // (a) A class literal. This asks the JVM for the Class object, which requires the
        // class to be LOADED - but loading and initializing are different steps.
        Class<?> type = LazyHolder.class;
        assertNotNull(type, "The class object must exist.");
        assertEquals("com.corejava.jvm.loading.LazyHolder", type.getName(),
                "The class literal must name the class we expect.");

        // (b) Creating an array. Every slot is null, so not one LazyHolder is ever built,
        // and the element type stays cold. The array type itself is created by the JVM.
        LazyHolder[] array = new LazyHolder[3];
        assertEquals(3, array.length, "The array exists and has three empty slots.");

        // (c) Reflection that only asks questions about the shape of the class.
        assertTrue(type.getDeclaredFields().length > 0,
                "Asking about a class's members inspects loaded metadata, nothing more.");

        // After all three: still cold.
        assertFalse(InitializationLog.wasInitialized("LazyHolder"),
                "A class literal, an array of the type, and reflection must all leave the class uninitialized.");

        // (d) Calling a static method. The JVM will not run a class's code until that
        // class's own initializer has finished, so this is the line that wakes it up.
        assertEquals("LazyHolder initialized", LazyHolder.stamp(),
                "The static method must return the value built during initialization.");
        assertTrue(InitializationLog.wasInitialized("LazyHolder"),
                "Calling a static method must initialize the declaring class first.");
    }

    // Class.forName has two shapes, and the difference between them is the whole reason
    // the classic JDBC line Class.forName("...Driver") works at all.
    @Test
    void forNameCanLoadWithoutInitializingAndThenInitializeOnDemand() throws ClassNotFoundException {
        // The name as text, exactly as it would come from a configuration file. Written
        // as a literal rather than taken from the class itself, so that looking the name
        // up cannot be what triggers anything.
        String name = "com.corejava.jvm.loading.ForNameSubject";

        assertFalse(InitializationLog.wasInitialized("ForNameSubject"),
                "Nothing in the project may touch ForNameSubject before this test.");

        // The three-argument form. The "false" is the whole point: find it, load it,
        // verify it, prepare it - and stop there, before running any of its code.
        ClassLoader loader = LazyInitializationTest.class.getClassLoader();
        Class<?> loadedOnly = Class.forName(name, false, loader);

        assertEquals(name, loadedOnly.getName(), "The class must have been found by name.");
        assertFalse(InitializationLog.wasInitialized("ForNameSubject"),
                "Class.forName(name, false, loader) must load the class WITHOUT initializing it.");

        // The one-argument form initializes. Note it returns the very same Class object:
        // the class was already loaded, so this is not a second load, only a second step.
        Class<?> initialized = Class.forName(name);
        assertEquals(loadedOnly, initialized,
                "The same loader must hand back the same class object, not load it twice.");
        assertTrue(InitializationLog.wasInitialized("ForNameSubject"),
                "Class.forName(name) must initialize the class it loads.");

        // And a name nobody can resolve fails with the checked exception that means
        // exactly that: "I went looking by name and did not find it."
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.corejava.jvm.loading.NoSuchClassAnywhere"),
                "A name that resolves to nothing must produce ClassNotFoundException.");
    }

    // Two rules about inheritance, both proved in one method because the second depends
    // on the first not having happened yet.
    @Test
    void aSuperclassInitializesFirstAndOnlyTheDeclaringClassWakesUp() {
        assertFalse(InitializationLog.wasInitialized("InitOrderParent"),
                "Nothing in the project may touch InitOrderParent before this test.");
        assertFalse(InitializationLog.wasInitialized("InitOrderChild"),
                "Nothing in the project may touch InitOrderChild before this test.");

        // Read a static field through the CHILD's name - but the field is declared in the
        // parent. The Java Language Specification is explicit: only the class that
        // actually declares the field initializes, even when a subclass name is used.
        assertEquals("parent field value", InitOrderChild.PARENT_FIELD,
                "The inherited field must read back the parent's value.");

        assertTrue(InitializationLog.wasInitialized("InitOrderParent"),
                "Reading the inherited field must initialize the class that DECLARES it.");
        assertFalse(InitializationLog.wasInitialized("InitOrderChild"),
                "The subclass named in the source must NOT initialize - it declares nothing we read.");

        // Now read a field the child really does declare. The child initializes, and its
        // parent is guaranteed to be initialized first - which it already is.
        assertEquals("child field value", InitOrderChild.CHILD_FIELD,
                "The child's own field must read back the child's value.");
        assertTrue(InitializationLog.wasInitialized("InitOrderChild"),
                "Reading a field the child declares must initialize the child.");

        // The order is the guarantee, not the coincidence: a superclass can never
        // initialize after its subclass.
        assertTrue(InitializationLog.positionOf("InitOrderParent")
                        < InitializationLog.positionOf("InitOrderChild"),
                "A superclass must always appear in the initialization log before its subclass.");
    }

    // The witness itself must be trustworthy: a caller who accidentally edits the history
    // would silently invalidate every test above.
    @Test
    void theInitializationLogHandsBackAnUnmodifiableSnapshot() {
        List<String> snapshot = InitializationLog.events();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.add("Forged"),
                "events() must return an immutable copy so no caller can rewrite history.");

        // A class that never initialized has no position, reported as -1 rather than by
        // throwing - so a test can ask the question safely before the answer exists.
        assertEquals(-1, InitializationLog.positionOf("AClassThatDoesNotExist"),
                "positionOf must report -1 for a class that never initialized.");
    }
}
