// Same package as the rest of the project.
package com.corejava.jvm;

// IOException is the checked exception every read from a stream may throw; we must
// either handle it or declare it, and this import lets us name it in a catch block.
import java.io.IOException;
// InputStream is Java's most basic "source of bytes" - the type we get back when we
// ask the JVM for the raw contents of a file on the class path.
import java.io.InputStream;
// UncheckedIOException wraps an IOException so callers are not forced to write
// try/catch around a method that, in practice, cannot fail: we are reading a file
// the JVM has already loaded successfully.
import java.io.UncheckedIOException;

/**
 * Reads the first eight bytes of a compiled class's own .class file and decodes
 * them into a {@link ClassFileHeader}.
 *
 * <p>This is the project's first module that looks at the JVM's raw material.
 * Everything a JVM ever executes arrives as a .class file, and every .class file
 * begins with the same fixed eight bytes: a four-byte identifier, then a two-byte
 * minor version, then a two-byte major version. Decoding them by hand shows that a
 * class file is not magic - it is a documented binary format anyone can read.</p>
 */
public final class ClassFileInspector {

    // How many bytes of header we need: 4 (magic) + 2 (minor) + 2 (major).
    // Named rather than written as a bare 8 so the arithmetic explains itself.
    private static final int HEADER_BYTES = 8;

    // A private constructor prevents anyone creating an instance of a class that
    // holds no state and offers only static methods.
    private ClassFileInspector() {
    }

    /**
     * Finds the .class file that defines the given class and decodes its header.
     *
     * @param type any loaded class - including this one
     * @return the decoded first eight bytes of that class's file
     */
    public static ClassFileHeader inspect(Class<?> type) {
        // Guard first: a null argument would otherwise blow up several lines later
        // with a message that says nothing about what the caller did wrong.
        if (type == null) {
            throw new IllegalArgumentException("Cannot inspect a null class.");
        }

        // Class files are stored on the class path in folders that mirror the
        // package name, so com.corejava.jvm.BytecodeSubject lives at
        // com/corejava/jvm/BytecodeSubject.class. Translating dots to slashes and
        // appending .class rebuilds that path. The leading "/" makes the lookup
        // start at the ROOT of the class path rather than relative to our package.
        String resourcePath = "/" + type.getName().replace('.', '/') + ".class";

        // getResourceAsStream asks the same machinery that loaded the class to hand
        // back its raw bytes. We deliberately do NOT open the file with a hard-coded
        // "target/classes/..." path: that would break the moment the code runs from
        // a jar, whereas this works identically from folders, jars, and tests.
        // try-with-resources: the stream is closed automatically, even if we throw.
        try (InputStream in = ClassFileInspector.class.getResourceAsStream(resourcePath)) {

            // A null stream means the class path has no such resource. That should be
            // impossible for a class that is currently loaded, so it signals a real
            // problem (a stripped jar, an exotic class loader) worth reporting clearly.
            if (in == null) {
                throw new IllegalStateException("No class file found on the class path at " + resourcePath);
            }

            // readNBytes asks for exactly this many bytes and returns however many it
            // actually got - safer than read(byte[]), which may legally return fewer
            // bytes than the array holds without telling you why.
            byte[] header = in.readNBytes(HEADER_BYTES);

            // A file shorter than eight bytes cannot be a class file at all. Checking
            // here means the decode helpers below never index past the end of the array.
            if (header.length < HEADER_BYTES) {
                throw new IllegalStateException(
                        "File at " + resourcePath + " is only " + header.length + " bytes long; "
                                + "a class file must start with at least " + HEADER_BYTES + ".");
            }

            // Bytes 0-3: the magic number. Read as four bytes, most significant first
            // ("big-endian"), which is the order the class file format always uses.
            long magic = readUnsignedInt(header, 0);
            // Bytes 4-5: minor version.
            int minorVersion = readUnsignedShort(header, 4);
            // Bytes 6-7: major version.
            int majorVersion = readUnsignedShort(header, 6);

            // Package the three numbers with the class name into one immutable value.
            return new ClassFileHeader(type.getName(), magic, minorVersion, majorVersion);

        } catch (IOException e) {
            // Reading bytes the JVM already read successfully should not fail. If it
            // somehow does, rethrow unchecked so ordinary callers stay clean, but keep
            // the original exception as the cause so nothing about the failure is lost.
            throw new UncheckedIOException("Failed to read the class file for " + type.getName(), e);
        }
    }

    /**
     * Reads four bytes as one unsigned number, most significant byte first.
     *
     * <p>The {@code & 0xFF} on every byte is the whole trick. A Java {@code byte} is
     * SIGNED: it holds -128 to 127, so the byte 0xCA arrives as -54. Combining raw
     * negative bytes produces a wrong (negative) result. {@code & 0xFF} keeps the
     * same eight bits but re-reads them as 0 to 255, which is what the file format
     * means. The result is a {@code long} because a four-byte unsigned number can
     * exceed the largest positive {@code int}.</p>
     */
    private static long readUnsignedInt(byte[] bytes, int offset) {
        // Each byte is masked to 0-255, then shifted into its place: the first byte
        // is worth 256x256x256 (shift left 24 bits), the next 256x256, then 256, then 1.
        // "|" glues the four eight-bit pieces together into one 32-bit value.
        return ((long) (bytes[offset] & 0xFF) << 24)
                | ((long) (bytes[offset + 1] & 0xFF) << 16)
                | ((long) (bytes[offset + 2] & 0xFF) << 8)
                | (long) (bytes[offset + 3] & 0xFF);
    }

    /**
     * Reads two bytes as one unsigned number, most significant byte first.
     * Range 0 to 65535, which fits comfortably in an {@code int}.
     */
    private static int readUnsignedShort(byte[] bytes, int offset) {
        // Same masking rule as above, with only two pieces to glue together.
        return ((bytes[offset] & 0xFF) << 8)
                | (bytes[offset + 1] & 0xFF);
    }
}
