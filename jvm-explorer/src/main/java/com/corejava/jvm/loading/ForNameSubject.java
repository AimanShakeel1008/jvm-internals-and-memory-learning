package com.corejava.jvm.loading;

/**
 * DEMONSTRATION SUBJECT: loading a class BY NAME, with and without initializing it.
 *
 * <p>Sometimes a program does not know at compile time which class it needs - a plugin
 * named in a configuration file, a database driver named in a connection string. For
 * that, Java can look a class up by its name as text, with {@code Class.forName}. That
 * method comes in two shapes and the difference is this entire class's reason to exist:</p>
 *
 * <ul>
 *   <li>{@code Class.forName("com.example.Thing")} loads the class AND initializes it.</li>
 *   <li>{@code Class.forName("com.example.Thing", false, loader)} loads it and stops -
 *       the {@code false} means "do not initialize".</li>
 * </ul>
 *
 * <p>The one-argument form initializing is not an accident of the API; it is the whole
 * feature. It is why the classic JDBC line {@code Class.forName("com.mysql.jdbc.Driver")}
 * works: the driver registers itself with the JDBC machinery from inside its own static
 * initializer, so "just load it" would achieve nothing.</p>
 *
 * <p>Nothing else in this project may touch this class, or the demonstration is spoilt.</p>
 */
public final class ForNameSubject {

    // The witness. Under Class.forName(name, false, loader) this line does NOT run,
    // even though the class is fully loaded, verified and prepared by then.
    static {
        InitializationLog.record("ForNameSubject");
    }

    // Deliberately NOT a compile-time constant - the right-hand side is a method call.
    // That matters for the lesson's step-by-step story: after PREPARATION this field
    // holds null (the default value for a reference), and it only takes the text below
    // during INITIALIZATION. A field assigned a plain literal would instead be filled
    // in during preparation, from the ConstantValue attribute, before any code runs.
    public static final String LOADED_BY_NAME = describeItself();

    private ForNameSubject() {
    }

    // Called from the field above while this class initializes.
    private static String describeItself() {
        return "found by name, then initialized";
    }
}
