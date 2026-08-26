// Same package as the class under test.
package com.corejava.jvm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Turns the lesson's claims about the .class file format into checks the machine
 * makes for us.
 *
 * <p>Each test states one rule of the format - it starts with 0xCAFEBABE, it carries
 * the compiler's version number, an ordinary class has minor version 0 - and then
 * proves it against a real file our own build just produced.</p>
 */
class ClassFileInspectorTest {

    // The class file version javac writes when told to target Java 21. Java's numbering
    // starts at 45 for Java 1.1 and adds one per release, so Java 21 is 44 + 21 = 65.
    // Written as a named constant with the arithmetic spelled out, because a bare 65
    // in three assertions would be a mystery number.
    private static final int JAVA_21_MAJOR_VERSION = 65;

    // The rule the JVM itself checks before reading one further byte: if the first
    // four bytes are not 0xCAFEBABE, this is not a class file.
    @Test
    void everyClassFileStartsWithCafeBabe() {
        ClassFileHeader header = ClassFileInspector.inspect(BytecodeSubject.class);

        // Compare against the named constant rather than a raw literal, so a failure
        // message mentions a value with a meaning attached.
        assertEquals(ClassFileHeader.JAVA_MAGIC, header.magic(),
                "A .class file must begin with the four bytes 0xCAFEBABE.");
        // The convenience method must agree with the raw comparison above; testing both
        // catches the silly-but-real bug of a helper that inverts its own condition.
        assertTrue(header.hasJavaMagic(), "hasJavaMagic() must agree with the raw magic value.");
        // And the human-readable rendering must be the spelling people actually use.
        assertEquals("0xCAFEBABE", header.magicAsHex(), "The magic number must print as 0xCAFEBABE.");
    }

    // Guards the pin from the other side. Lesson 00's test proves we RUN on Java 21;
    // this one proves we COMPILE for Java 21 - a different setting (maven.compiler.release)
    // that can drift on its own without the running version changing at all.
    @Test
    void classFilesAreCompiledToThePinnedJavaVersion() {
        ClassFileHeader header = ClassFileInspector.inspect(BytecodeSubject.class);

        assertEquals(JAVA_21_MAJOR_VERSION, header.majorVersion(),
                // The failure message names the exact POM setting to look at, because a
                // failing test is read by someone who wants the fix, not a puzzle.
                "Class files should be Java 21 (major 65). Check <maven.compiler.release> in pom.xml.");
        // The decoded name must match too: printing "65" helps nobody, "Java 21" does.
        assertEquals("Java 21", header.javaVersionName(),
                "Major version 65 must decode to the name 'Java 21'.");
    }

    // The compile-time version and the run-time version are set by two different things
    // and can disagree; when they do, the JVM refuses the file. This test asserts the
    // agreement directly, so the mismatch is caught by the build rather than at startup.
    @Test
    void compiledVersionMatchesTheRunningJvm() {
        ClassFileHeader header = ClassFileInspector.inspect(JvmExplorer.class);

        // Runtime.version().feature() is the major release number of the JVM executing
        // this test (21 for any 21.x.y build) - the same number a class file's major
        // version decodes to once the offset of 44 is removed.
        assertEquals("Java " + Runtime.version().feature(), header.javaVersionName(),
                "The class files we compile must name the same Java release as the JVM running them.");
    }

    // Minor version is 0 for every ordinary class file. The one other value that occurs
    // in practice is 65535, which marks preview language features - a flag the JVM reads
    // to refuse the file on any other release.
    @Test
    void ordinaryClassFilesUseMinorVersionZeroAndNoPreviewFeatures() {
        ClassFileHeader header = ClassFileInspector.inspect(BytecodeSubject.class);

        assertEquals(0, header.minorVersion(), "An ordinary class file has minor version 0.");
        assertFalse(header.usesPreviewFeatures(),
                "This course compiles without --enable-preview, so no file should be flagged as preview.");
    }

    // The inspector finds a class's bytes through the class path, not through a hard-coded
    // folder - so it can read the file of the class doing the reading. Inspecting itself
    // and a record proves the lookup works for every kind of class in the project.
    @Test
    void theInspectorCanInspectItselfAndOtherProjectClasses() {
        // A plain final class...
        assertTrue(ClassFileInspector.inspect(ClassFileInspector.class).hasJavaMagic(),
                "The inspector must be able to read its own class file.");
        // ...and a record, which is compiled to an ordinary class file like any other.
        assertTrue(ClassFileInspector.inspect(ClassFileHeader.class).hasJavaMagic(),
                "A record compiles to a normal class file and must be readable too.");
        // The name carried in the header must be the class we asked about, so results
        // cannot be silently attributed to the wrong file.
        assertEquals("com.corejava.jvm.BytecodeSubject",
                ClassFileInspector.inspect(BytecodeSubject.class).className(),
                "The header must report the class it was asked to inspect.");
    }

    // A bad argument must fail immediately with a message that names the mistake,
    // instead of producing a NullPointerException several lines deeper.
    @Test
    void inspectingNullIsRejected() {
        // assertThrows runs the code and passes only if THAT exact exception type comes out.
        assertThrows(IllegalArgumentException.class, () -> ClassFileInspector.inspect(null),
                "Passing null must be rejected with a clear IllegalArgumentException.");
    }
}
