package com.corejava.jvm.memory;

/**
 * Demonstrates the single most important consequence of stack-versus-heap: a variable
 * never holds an object. It holds a <strong>reference</strong> - the address of an
 * object that lives on the heap.
 *
 * <p>A local variable lives in its method's frame, on the stack. If its type is a
 * primitive ({@code int}, {@code double}, {@code boolean}) the slot holds the value
 * itself. If its type is anything else, the slot holds an arrow pointing at a heap
 * object, and several slots can point at the same object.</p>
 *
 * <p>From that one fact everything below follows, including the behaviour that trips up
 * almost everyone at least once: Java passes arguments <strong>by value</strong>, always
 * - but for object types, the value being copied is the arrow, not the thing it points
 * at. So a method can change the object it was handed, and the caller sees it; and a
 * method can point its own copy of the arrow somewhere else, and the caller does not.</p>
 */
public final class ReferenceDemo {

    // Static-only demonstration: no instances.
    private ReferenceDemo() {
    }

    /**
     * Changes the object the caller passed in.
     *
     * @param text the caller's StringBuilder - the same heap object, not a copy
     */
    public static void mutate(StringBuilder text) {
        // append() reaches through the arrow and modifies the object on the heap. There
        // is exactly one such object, and the caller's variable points at it too, so
        // the caller sees this change the moment it looks.
        text.append(" (changed inside mutate)");
    }

    /**
     * Points this method's own copy of the arrow at a brand-new object, and changes that
     * one instead. The caller is entirely unaffected.
     *
     * @param text initially the caller's StringBuilder; re-pointed on the first line
     */
    public static void reassign(StringBuilder text) {
        // This line does NOT change any object. It overwrites a slot in THIS frame -
        // the parameter variable - with the address of a new heap object. The caller's
        // slot, in the caller's frame, still holds the old address and cannot be
        // touched from here.
        text = new StringBuilder("a different object entirely");

        // Proof that the re-pointing worked locally: this change lands on the new
        // object, which nobody else can reach, and is thrown away when this frame dies.
        text.append(" (changed inside reassign)");
    }

    /**
     * The primitive version of the same story, where there is no arrow at all.
     *
     * @param number a COPY of the caller's number, living in this frame's own slot
     */
    public static void reassignPrimitive(int number) {
        // The slot in this frame holds the number itself, so writing to it can only
        // ever affect this frame. The caller's variable is a different slot in a
        // different frame and never hears about this.
        number = 999;
    }

    /**
     * @param first  any reference
     * @param second any reference
     * @return true only if both arrows point at the very same heap object
     */
    public static boolean sameObject(Object first, Object second) {
        // == on references compares the ARROWS, not the contents. This is why comparing
        // two equal-looking Strings with == can be false: equal text, two objects.
        return first == second;
    }

    /**
     * Makes a genuinely separate object with the same contents.
     *
     * @param original the object to copy; must not be null
     * @return a new StringBuilder holding equal text, at a different heap address
     */
    public static StringBuilder copyOf(StringBuilder original) {
        // The same door-guard habit as the rest of the project: reject null with a
        // message that names the mistake, instead of a bare NullPointerException from
        // somewhere inside the JDK.
        if (original == null) {
            throw new IllegalArgumentException("Cannot copy a null StringBuilder.");
        }
        // new means "make a second object on the heap". Its contents start equal and
        // the two objects immediately go their separate ways.
        return new StringBuilder(original.toString());
    }

    /**
     * @return a short report showing both behaviours, for the program's console output
     */
    public static String describe() {
        // One object on the heap; one arrow to it in this frame.
        StringBuilder caller = new StringBuilder("original");

        // A second arrow to the SAME object - no new object was created by this line.
        StringBuilder alias = caller;

        // Handing the arrow to a method that modifies what it points at.
        mutate(caller);

        // Handing the arrow to a method that re-points its own copy. No effect here.
        reassign(caller);

        // A primitive, for contrast: the callee's change cannot escape its frame.
        int count = 1;
        reassignPrimitive(count);

        return "after mutate + reassign, the caller sees: \"" + caller + "\""
                + System.lineSeparator()
                + "the second variable points at the same object: " + sameObject(caller, alias)
                + System.lineSeparator()
                + "a copy has equal text but is a different object: "
                + sameObject(caller, copyOf(caller))
                + System.lineSeparator()
                + "the primitive the method reassigned is still: " + count;
    }
}
