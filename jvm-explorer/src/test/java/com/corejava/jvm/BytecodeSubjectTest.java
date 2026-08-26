// Tests live in the same package as the code they test, under src/test/java.
package com.corejava.jvm;

// The marker that tells JUnit "run this method as a test".
import org.junit.jupiter.api.Test;

// Static imports so assertions read like sentences instead of Assertions.assertX(...).
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Pins down what the four disassembly samples DO, so that the lesson's claims about
 * what their bytecode MEANS can be trusted.
 *
 * <p>Reading bytecode is only useful if you already know the right answer: if
 * {@code sumTo(5)} is supposed to be 15 and the instructions seem to say something
 * else, you have misread the instructions. These tests are that known-right answer,
 * checked by the machine instead of by memory.</p>
 */
class BytecodeSubjectTest {

    // The exact call the lesson's bytecode walk-through and simulation use, so the
    // numbers on the page and the numbers the machine produces are the same numbers.
    @Test
    void addReturnsTheSumOfItsTwoArguments() {
        // 7 + 5 = 12: three values worth remembering, because they are the ones that
        // appear on the operand stack in the disassembly.
        assertEquals(12, BytecodeSubject.add(7, 5),
                "add(7, 5) must be 12 - if not, the bytecode walk-through describes a different method.");
    }

    // Checks the loop sample at three telling points rather than one.
    @Test
    void sumToAddsEveryWholeNumberUpToN() {
        // 1+2+3+4+5 = 15: the small case traced by hand in the lesson.
        assertEquals(15, BytecodeSubject.sumTo(5), "1+2+3+4+5 must be 15.");
        // The edge case: with n = 0 the loop body never runs, because i starts at 1
        // and 1 <= 0 is false. The forward jump fires immediately and total stays 0.
        assertEquals(0, BytecodeSubject.sumTo(0), "With n = 0 the loop body must never run.");
        // The value the warm-up experiment relies on: 1000 * 1001 / 2 = 500500.
        assertEquals(500_500, BytecodeSubject.sumTo(1000), "1+2+...+1000 must be 500500.");
    }

    // Confirms the string sample really produces the obvious result, even though its
    // bytecode is a single invokedynamic instruction that mentions neither piece.
    @Test
    void greetJoinsTheGreetingAndTheName() {
        assertEquals("Hello, Sam", BytecodeSubject.greet("Sam"),
                "The compiled concatenation must still produce the same text the source describes.");
    }

    // Proves what the new/dup/invokespecial trio in the bytecode is actually doing:
    // allocating a BRAND NEW object each time, not handing back a shared one.
    @Test
    void newObjectReturnsAFreshObjectEveryTime() {
        Object first = BytecodeSubject.newObject();
        Object second = BytecodeSubject.newObject();

        // A null return would mean the allocation never happened.
        assertNotNull(first, "newObject() must return an object.");
        // assertNotSame compares IDENTITY ("are these the same object?"), not equality
        // ("do these look alike?"). That distinction is the whole point here: two calls
        // to `new` must produce two separate objects at two separate heap addresses.
        assertNotSame(first, second, "Each call to new must allocate a separate object.");
    }
}
