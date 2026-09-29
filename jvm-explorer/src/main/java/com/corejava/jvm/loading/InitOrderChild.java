package com.corejava.jvm.loading;

/**
 * DEMONSTRATION SUBJECT (subclass): see {@link InitOrderParent} for the two rules this
 * pair proves.
 *
 * <p>Reading {@code InitOrderChild.PARENT_FIELD} looks like it touches this class - the
 * class name is right there in the source - but the field is declared in the parent, so
 * only the parent initializes. Reading {@link #CHILD_FIELD}, declared here, finally
 * initializes this class, and the parent's entry is already in the log ahead of it
 * because a superclass always goes first.</p>
 */
public final class InitOrderChild extends InitOrderParent {

    // The witness for this class specifically. Its POSITION in the log is the evidence:
    // it can never appear before InitOrderParent's entry.
    static {
        InitializationLog.record("InitOrderChild");
    }

    // Declared HERE, so reading this one really does initialize this class. Again a
    // method call, so it is not a compile-time constant that javac could inline away.
    public static final String CHILD_FIELD = describeChild();

    // Nobody needs an instance; the demonstration is entirely about static state.
    private InitOrderChild() {
    }

    // Called from CHILD_FIELD above, during this class's initialization.
    private static String describeChild() {
        return "child field value";
    }
}
