// Same package as everything else in the project.
package com.corejava.jvm;

/**
 * The first eight bytes of a .class file, decoded into numbers a human can read.
 *
 * <p>This is a "record": a short way of declaring a small class whose only job is
 * to carry a fixed set of values. Writing {@code record ClassFileHeader(String a,
 * int b)} asks the compiler to generate the private fields, the constructor, the
 * reader methods, plus sensible {@code equals}, {@code hashCode} and
 * {@code toString} - all the boilerplate we would otherwise type by hand. The
 * values are final: once a header is created it can never be changed, which is
 * exactly right for a description of bytes already on disk.</p>
 */
public record ClassFileHeader(

        // The fully-qualified name of the class these bytes belong to, kept so a
        // header can be printed or asserted on without also passing the Class around.
        String className,

        // Bytes 0-3 of the file: the "magic number" that identifies the file type.
        // For every legal Java class file this is 0xCAFEBABE. Stored as a long, not
        // an int, because 0xCAFEBABE is larger than the biggest positive int - as an
        // int it would appear as a negative number and confuse every comparison.
        long magic,

        // Bytes 4-5: the minor version of the class file format. Nearly always 0.
        // The one value worth recognising is 65535, which marks a class compiled
        // with preview language features turned on.
        int minorVersion,

        // Bytes 6-7: the major version - the real version number of the file format,
        // written by the compiler and checked by the JVM before it will run the file.
        // 65 means "Java 21", which is what this course pins.
        int majorVersion) {

    // The magic number every Java class file must start with. Named as a constant
    // so the meaning of the value appears once, here, instead of as a bare hex
    // literal scattered through the code and the tests.
    public static final long JAVA_MAGIC = 0xCAFEBABEL;

    // The minor version the compiler writes when preview features were enabled.
    // 65535 is the largest number that fits in the two bytes the format allows,
    // deliberately chosen as an unmistakable flag value.
    public static final int PREVIEW_MINOR_VERSION = 65535;

    // Java's class file numbering starts at 45 for Java 1.1, then adds one per
    // release. From Java 5 (major 49) onwards the arithmetic is simply
    // "Java version = major - 44". Naming that offset makes the two methods below
    // readable instead of full of unexplained 44s.
    private static final int MAJOR_VERSION_OFFSET = 44;

    // True when these bytes really are a Java class file. Any other value means we
    // were handed something else entirely (a text file, an image, a truncated
    // download) - the check the JVM itself performs before reading one more byte.
    public boolean hasJavaMagic() {
        // Simple equality: the format allows exactly one legal value here.
        return magic == JAVA_MAGIC;
    }

    // The magic number formatted the way humans discuss it: 0xCAFEBABE.
    // %08X means "hexadecimal, uppercase, padded with zeros to 8 digits", so the
    // output has a fixed width and is instantly comparable by eye.
    public String magicAsHex() {
        return String.format("0x%08X", magic);
    }

    // Turns the raw major version into the Java release name a person recognises.
    // Printing "65" helps nobody; printing "Java 21" is the fact you actually want.
    public String javaVersionName() {
        // Java 5 and later (major 49+) use plain whole numbers: 49 -> 5, 65 -> 21.
        if (majorVersion >= 49) {
            return "Java " + (majorVersion - MAJOR_VERSION_OFFSET);
        }
        // Before that, releases were called 1.1 through 1.4 (majors 45 through 48),
        // so the same subtraction produces the digit after the dot.
        return "Java 1." + (majorVersion - MAJOR_VERSION_OFFSET);
    }

    // True when this class file was produced with preview language features enabled
    // (javac --enable-preview). Such a file may only run on a JVM of exactly the
    // same version, also started with --enable-preview - a restriction the JVM
    // enforces by reading this very field.
    public boolean usesPreviewFeatures() {
        return minorVersion == PREVIEW_MINOR_VERSION;
    }

    // One human-readable line summarising the header, used by JvmExplorer's output.
    // Kept separate from the record's auto-generated toString() so that toString()
    // stays the raw, debugging-friendly dump of the fields.
    public String describe() {
        // Deliberately shows both the raw number and its meaning: the raw number is
        // what appears in JVM error messages, the name is what a human understands.
        return className
                + "  magic=" + magicAsHex()
                + "  major=" + majorVersion + " (" + javaVersionName() + ")"
                + "  minor=" + minorVersion
                + (usesPreviewFeatures() ? "  [preview features enabled]" : "");
    }
}
