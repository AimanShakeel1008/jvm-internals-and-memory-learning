package com.corejava.jvm.loading;

/**
 * DEMONSTRATION SUBJECT: a class that holds only compile-time constants, and therefore
 * NEVER initializes when you read them.
 *
 * <p>A field that is {@code static final} AND assigned a constant expression - a
 * literal, or arithmetic on literals - is a special thing called a <em>compile-time
 * constant</em>. Its value is copied into the class file of everyone who reads it, at
 * compile time. So a program that reads {@code ConstantHolder.PINNED_MAJOR_VERSION}
 * contains the number 65 directly in its own bytecode and never mentions this class at
 * run time at all. No loading. No initialization. The static block below stays cold
 * forever, and the log proves it.</p>
 *
 * <p>Nothing else in this project may touch this class, or the demonstration is spoilt:
 * once a class initializes, it can never go back.</p>
 */
public final class ConstantHolder {

    // A compile-time constant: static + final + a literal on the right-hand side.
    // javac stores the value in a ConstantValue attribute and INLINES it into every
    // reader's class file. That inlining is the whole point of this class.
    public static final int PINNED_MAJOR_VERSION = 65;

    // String literals count too, and this one demonstrates the real-world sting:
    // change this text, rebuild only this project, and any separately-compiled caller
    // keeps printing the OLD text until it is recompiled as well.
    public static final String COURSE_NAME = "JVM Internals & Memory";

    // Deliberately NOT a compile-time constant, even though it looks like one: the
    // right-hand side is a method call, so its value can only be known by running
    // code, which means running this class's initializer. It is here so the class has
    // one field that DOES force initialization, for contrast in the lesson.
    public static final String DESCRIPTION = buildDescription();

    // The witness. If this line ever runs, the class initialized - and the log says so.
    static {
        InitializationLog.record("ConstantHolder");
    }

    // Constants only: no instances.
    private ConstantHolder() {
    }

    // A plain method, called from the DESCRIPTION field above. Its existence is what
    // stops DESCRIPTION being a compile-time constant: javac cannot evaluate a method
    // call while compiling, so the assignment has to happen at initialization time.
    private static String buildDescription() {
        return "Course 01 pins Java " + (PINNED_MAJOR_VERSION - 44);
    }
}
