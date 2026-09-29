package com.corejava.jvm.loading;

// ArrayList holds the chain we build while walking from a class's own loader upward.
import java.util.ArrayList;
// List is what we hand back - the chain, in order, ending at the bootstrap loader.
import java.util.List;

/**
 * Reports WHICH class loader loaded a class, and the full parent chain above it.
 *
 * <p>Every class the JVM runs was found and defined by some class loader, and those
 * loaders are arranged in a chain: the application loader's parent is the platform
 * loader, whose parent is the bootstrap loader. A request always travels UP that chain
 * before anyone searches - the rule called parent delegation - so knowing a class's
 * loader tells you where the JVM actually found it.</p>
 *
 * <p>The bootstrap loader is written in C++ inside the JVM and has no Java object, so
 * {@code getClassLoader()} returns {@code null} for anything it loaded. That null is
 * not an error and not a missing value; it is the JVM's way of spelling "bootstrap",
 * and this class translates it into words instead of leaving a trap for the caller.</p>
 */
public final class ClassLoaderReporter {

    // What we print where the JVM hands us null. Named, not repeated as a literal,
    // because tests assert on it and a typo in one of two copies is invisible.
    public static final String BOOTSTRAP_DESCRIPTION = "bootstrap (built into the JVM, no Java object)";

    // Static-only helper class: no state, so no instances.
    private ClassLoaderReporter() {
    }

    /**
     * Turns one class loader (or null) into a readable description.
     *
     * @param loader any class loader, or null meaning the bootstrap loader
     * @return a human-readable description, never null
     */
    public static String describe(ClassLoader loader) {
        // null is the bootstrap loader, not a mistake. Handling it FIRST means every
        // line below can assume a real object exists.
        if (loader == null) {
            return BOOTSTRAP_DESCRIPTION;
        }

        // Since Java 9 the built-in loaders carry names: "app" and "platform". Loaders
        // created by other code may have no name at all, so getName() can return null
        // and we must have something to print when it does.
        String name = loader.getName();
        if (name == null) {
            name = "(unnamed)";
        }

        // The class name tells you WHICH implementation this is - for the built-in
        // loaders it is an inner class of jdk.internal.loader.ClassLoaders, and for a
        // framework's own loader it is the giveaway that something custom is in play.
        return name + " — " + loader.getClass().getName();
    }

    /**
     * Walks the delegation chain upward from the loader that defined the given class.
     *
     * @param type any loaded class
     * @return the chain, starting with the class's own loader and always ending with
     *         the bootstrap loader's description
     */
    public static List<String> chainFor(Class<?> type) {
        // Guard at the door with a message naming the mistake, the same habit as
        // ClassFileInspector - a null here would otherwise fail one line later with
        // nothing useful to say.
        if (type == null) {
            throw new IllegalArgumentException("Cannot report the class loader of a null class.");
        }

        List<String> chain = new ArrayList<>();

        // getClassLoader() answers "which loader DEFINED this class" - not "which one
        // was asked first". Under parent delegation those differ: the app loader is
        // asked for java.lang.String, but the bootstrap loader is what defines it.
        ClassLoader loader = type.getClassLoader();

        // Climb: each loader's parent is the one it delegates to. The loop stops at
        // null, which is the bootstrap loader and therefore the top of every chain.
        while (loader != null) {
            chain.add(describe(loader));
            loader = loader.getParent();
        }

        // The chain ALWAYS ends at bootstrap, whether we climbed to it or started
        // there (a bootstrap-loaded class skips the loop entirely). Adding it here,
        // outside the loop, is what makes that guarantee true in both cases.
        chain.add(BOOTSTRAP_DESCRIPTION);

        return List.copyOf(chain);
    }

    /**
     * One printable line: the class, then its loader chain, root last.
     *
     * @param type any loaded class
     * @return e.g. {@code java.lang.String  <-  bootstrap (built into the JVM, no Java object)}
     */
    public static String reportFor(Class<?> type) {
        // chainFor does the null check, so this method has exactly one job: formatting.
        List<String> chain = chainFor(type);

        // "<-" reads as "was reached through", so the line is read right to left the
        // same way delegation actually travels: the answer came DOWN from the root.
        return type.getName() + "  <-  " + String.join("  <-  ", chain);
    }

    /**
     * @param type any loaded class
     * @return true if the JVM's own built-in bootstrap loader defined this class
     */
    public static boolean isLoadedByBootstrap(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("Cannot report the class loader of a null class.");
        }
        // The whole test is "is the loader null" - but writing that check behind a
        // named method means calling code never has to remember what null means here.
        return type.getClassLoader() == null;
    }

    /**
     * The JVM's own three-step delegation chain, independent of any particular class.
     *
     * @return the application loader, its parent, and so on up to the bootstrap loader
     */
    public static List<String> builtInDelegationChain() {
        List<String> chain = new ArrayList<>();

        // getSystemClassLoader() is the loader that loads YOUR classpath - the one
        // everyone calls "the application class loader". The method is named "system"
        // for historical reasons, which is a permanent source of confusion.
        ClassLoader loader = ClassLoader.getSystemClassLoader();

        while (loader != null) {
            chain.add(describe(loader));
            loader = loader.getParent();
        }

        chain.add(BOOTSTRAP_DESCRIPTION);
        return List.copyOf(chain);
    }
}
