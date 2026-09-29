package com.corejava.jvm.loading;

/**
 * DEMONSTRATION SUBJECT (superclass): proves two rules about inheritance and
 * initialization that surprise almost everyone.
 *
 * <p><strong>Rule 1.</strong> Initializing a subclass always initializes its superclass
 * FIRST. It must: the subclass may use inherited state, and half-built state is worse
 * than none.</p>
 *
 * <p><strong>Rule 2, the surprising one.</strong> The reverse does not hold, and neither
 * does the obvious reading of the source. Reading a static field through the SUBCLASS's
 * name, when the field is actually declared HERE, initializes only this class. The
 * subclass is loaded (the JVM has to look at it to discover the field is inherited) but
 * its static initializer does not run. This is not a HotSpot quirk - the Java Language
 * Specification states it explicitly.</p>
 */
public class InitOrderParent {

    // The witness runs first, so the log records the parent before anything else this
    // initializer does.
    static {
        InitializationLog.record("InitOrderParent");
    }

    // Declared HERE, in the superclass. Not a compile-time constant (method call), so
    // reading it really does force an initialization - which is exactly what makes the
    // "only the parent wakes up" result observable.
    public static final String PARENT_FIELD = describeParent();

    // Not private and not final: this class exists to be extended by InitOrderChild.
    // A protected constructor says "subclasses only" without allowing outside code to
    // create loose instances.
    protected InitOrderParent() {
    }

    // Called from PARENT_FIELD above, during this class's initialization.
    private static String describeParent() {
        return "parent field value";
    }
}
