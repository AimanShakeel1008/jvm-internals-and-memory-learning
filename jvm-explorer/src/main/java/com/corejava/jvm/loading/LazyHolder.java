package com.corejava.jvm.loading;

/**
 * DEMONSTRATION SUBJECT: the three things people expect to initialize a class, that
 * do not - and the one that does.
 *
 * <p>Mentioning a class is not using it. Writing {@code LazyHolder.class} asks the JVM
 * for the class object, which requires the class to be LOADED but not INITIALIZED.
 * Writing {@code new LazyHolder[3]} creates an array whose slots are all null, so no
 * LazyHolder is ever built and the class stays cold. Even naming it in an
 * {@code import} does nothing at all - imports vanish at compile time.</p>
 *
 * <p>Calling a static method, on the other hand, means running code that belongs to
 * this class, and the JVM will not run a class's code before that class's own
 * initializer has finished. So {@link #stamp()} is the line that finally wakes it up.</p>
 *
 * <p>Nothing else in this project may touch this class, or the demonstration is spoilt.</p>
 */
public final class LazyHolder {

    // The witness runs FIRST, before the field below, because static initializers and
    // static field assignments run in the order they are written in the source file.
    // Recording first means the log is correct even if later work in the initializer
    // were to throw.
    static {
        InitializationLog.record("LazyHolder");
    }

    // NOT a compile-time constant: the right-hand side is a method call, so this value
    // can only be produced by running the initializer. Reading it therefore forces
    // initialization - the exact opposite of ConstantHolder's inlined literals.
    public static final String STAMP = buildStamp();

    // Static-only demonstration class: no instances.
    private LazyHolder() {
    }

    /**
     * @return proof that this class's initializer has run
     */
    public static String stamp() {
        // Calling ANY static method of a class forces that class to initialize first,
        // which is why this method is enough to change the log - the caller never
        // touches a field at all.
        return STAMP;
    }

    // Called from the STAMP field above, during initialization.
    private static String buildStamp() {
        return "LazyHolder initialized";
    }
}
