// Same package as the rest of the project, so this class sits beside JvmExplorer
// both in the source tree and inside target/classes after compilation.
package com.corejava.jvm;

// This class exists to be DISASSEMBLED, not to be useful. Every method here was
// chosen because its compiled form (its bytecode) teaches one specific idea when
// you run `javap -c` on it. Keeping the methods absurdly small is the point: the
// smaller the source, the easier it is to match every bytecode instruction back
// to the exact piece of Java that produced it.
// "final" because nothing should ever extend a class of teaching samples -
// a subclass could override these methods and silently change what you disassemble.
public final class BytecodeSubject {

    // A private constructor with an empty body. Nobody should ever create a
    // BytecodeSubject object: every method below is static, so there is nothing
    // an instance could add. Making the constructor private states that intent in
    // a way the compiler enforces. It is ALSO a teaching device - `javap -c -p`
    // will show this constructor's bytecode, which is the shortest possible proof
    // that a constructor is just another method under the hood.
    private BytecodeSubject() {
        // Deliberately empty. Even so, the compiler emits bytecode here: every
        // constructor must first call its superclass constructor (Object's), and
        // javap will show that call as `invokespecial java/lang/Object."<init>"`.
    }

    // SAMPLE 1 - the simplest possible arithmetic method.
    // Teaches: how arguments arrive (as numbered local variable slots), and that
    // the JVM computes on a stack: push a, push b, add (which pops both and pushes
    // the sum), return the top of the stack.
    // Static, so slot 0 is the FIRST PARAMETER. (In an instance method slot 0 is
    // always `this`, which is why the numbering seems to shift by one there.)
    public static int add(int a, int b) {
        // Compiles to exactly four instructions: iload_0, iload_1, iadd, ireturn.
        // "i" in each name means "int" - the JVM has separate instructions per
        // primitive type instead of one generic "load".
        return a + b;
    }

    // SAMPLE 2 - a counted loop, the smallest thing that produces a JUMP.
    // Teaches: how `for` disappears at the bytecode level. There is no "for"
    // instruction; a loop is a comparison plus two jumps (one forward to escape,
    // one backward to repeat), which is exactly how a CPU does it too.
    public static int sumTo(int n) {
        // Local variable slot 1 (slot 0 holds the parameter n). Starts at zero
        // because we are accumulating a sum and zero is the identity for addition.
        int total = 0;
        // The loop counter lands in slot 2. Counting from 1 (not 0) because we
        // want 1+2+...+n, and using <= so that n itself is included.
        for (int i = 1; i <= n; i++) {
            // Read total, read i, add, write back to total. In bytecode this is
            // four instructions (iload, iload, iadd, istore) - `+=` is not an
            // instruction, it is a shorthand the compiler expands.
            total += i;
        }
        // Push the accumulated value and hand it back to the caller.
        return total;
    }

    // SAMPLE 3 - string joining, chosen because its bytecode looks NOTHING like
    // its source. One "+" in Java 9 and later compiles to a single `invokedynamic`
    // instruction, which means "the first time this runs, ask a helper in the JDK
    // to build a fast recipe for this exact concatenation, then reuse that recipe
    // forever." It is the clearest proof that source lines and work done are not
    // the same thing.
    public static String greet(String name) {
        // The literal "Hello, " is baked into the concatenation recipe, so only
        // `name` has to be pushed at run time - watch for that in javap output.
        return "Hello, " + name;
    }

    // SAMPLE 4 - creating an object, in three instructions.
    // Teaches the shape of every `new` in Java: `new` allocates blank memory on
    // the heap and pushes a reference to it; `dup` copies that reference so there
    // are two on the stack; `invokespecial` consumes one copy to run the
    // constructor; the remaining copy is what the expression evaluates to.
    // Returning Object (not a project type) keeps the disassembly free of noise.
    public static Object newObject() {
        // Object is the simplest class in Java and its constructor does nothing,
        // so this is object creation with every distraction removed.
        return new Object();
    }
}
