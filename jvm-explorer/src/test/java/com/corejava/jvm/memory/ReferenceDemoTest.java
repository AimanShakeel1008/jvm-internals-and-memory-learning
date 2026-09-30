package com.corejava.jvm.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Turns "a variable holds a reference, not an object" into checks the machine makes.
 *
 * <p>Unlike the memory-size tests, every assertion here is exact and will hold on every
 * machine and every Java version: these are language rules, not measurements.</p>
 */
class ReferenceDemoTest {

    // Passing an object to a method passes a COPY OF THE ARROW. The arrow still points
    // at the caller's object, so anything the method does to that object is permanent.
    @Test
    void aMethodCanChangeTheObjectItWasHanded() {
        StringBuilder text = new StringBuilder("original");

        ReferenceDemo.mutate(text);

        assertTrue(text.toString().contains("changed inside mutate"),
                "mutate() reached through the reference and modified the caller's object.");
    }

    // The other half of the same rule, and the one people expect to behave the opposite
    // way: re-pointing the parameter only rewrites a slot inside the callee's own frame.
    @Test
    void aMethodCannotRepointTheCallersVariable() {
        StringBuilder text = new StringBuilder("original");

        ReferenceDemo.reassign(text);

        assertEquals("original", text.toString(),
                "reassign() pointed its OWN copy of the reference elsewhere; the caller is untouched.");
    }

    // Primitives make the contrast visible: there is no arrow at all, so the callee's
    // slot holds its own private copy of the number.
    @Test
    void aMethodCannotChangeTheCallersPrimitive() {
        int count = 1;

        ReferenceDemo.reassignPrimitive(count);

        assertEquals(1, count,
                "The method wrote 999 into its own frame's slot, which the caller never sees.");
    }

    // Two variables, one object. This is what makes the first test's behaviour possible
    // at all, and what makes accidental sharing a real bug in real programs.
    @Test
    void twoVariablesCanPointAtOneObject() {
        StringBuilder first = new StringBuilder("shared");
        StringBuilder second = first;

        assertTrue(ReferenceDemo.sameObject(first, second),
                "Assignment copies the reference, not the object - both arrows point at one object.");

        // Changing it through one name changes what the other name sees, because there
        // was only ever one object to change.
        second.append(" and changed");
        assertEquals("shared and changed", first.toString(),
                "There is one object on the heap; the two names are two ways to reach it.");
    }

    // A copy is a genuinely separate object: equal contents, different address, and
    // changes on one do not follow to the other.
    @Test
    void aCopyIsADifferentObjectWithEqualContents() {
        StringBuilder original = new StringBuilder("text");
        StringBuilder copy = ReferenceDemo.copyOf(original);

        assertFalse(ReferenceDemo.sameObject(original, copy),
                "copyOf() used new, so a second object exists on the heap.");
        assertEquals(original.toString(), copy.toString(),
                "The two objects start with equal contents.");

        original.append("!");
        assertEquals("text", copy.toString(),
                "Changing one object cannot change the other - they are separate objects.");
    }

    // The whole demonstration in one string, which is what the program prints. Checking
    // it here means the lesson's predicted output is anchored to something tested.
    @Test
    void theReportStatesBothOutcomesAndRejectsNull() {
        String report = ReferenceDemo.describe();

        assertTrue(report.contains("changed inside mutate"),
                "The report must show that mutation reached the caller: " + report);
        assertFalse(report.contains("changed inside reassign"),
                "The report must NOT show the reassignment, which never escaped its frame: " + report);
        assertTrue(report.contains("same object: true"),
                "The report must show that the alias points at the same object: " + report);

        assertThrows(IllegalArgumentException.class, () -> ReferenceDemo.copyOf(null),
                "Copying null is a caller's mistake and must be named as one.");
    }
}
