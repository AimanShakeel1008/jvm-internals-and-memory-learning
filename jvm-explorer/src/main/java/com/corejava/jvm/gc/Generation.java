// Lesson 04's package: the object lifecycle - when an object becomes garbage, and how
// the heap is divided into generations so garbage can be found cheaply.
package com.corejava.jvm.gc;

// List holds the name fragments; we hand out no arrays, so nobody can change them.
import java.util.List;

/**
 * The three heap areas a generational collector divides objects into.
 *
 * <p>The JVM exposes each area as a named "memory pool", but the names depend on which
 * garbage collector is running: the default G1 collector calls the first one
 * {@code "G1 Eden Space"}, the Serial collector calls it {@code "Eden Space"}, the
 * Parallel collector {@code "PS Eden Space"}. These are HotSpot names, not something the
 * Java specification promises, so instead of listing every exact name we match on the
 * word they all share. That keeps the project working whichever collector you pick.</p>
 */
public enum Generation {

    // Where every new object is born. Every HotSpot collector's young-allocation pool
    // contains the word "Eden".
    EDEN("Eden"),

    // The small holding area for objects that survived at least one minor collection.
    // Every HotSpot collector's survivor pool contains the word "Survivor".
    SURVIVOR("Survivor"),

    // Where objects go once they have survived long enough to be promoted. G1 and
    // Parallel say "Old Gen"; the Serial collector says "Tenured Gen" - "tenured" being
    // the older word for the same idea, so both fragments are listed.
    OLD("Old", "Tenured");

    // The words that identify this area inside a pool name. "final" because an enum's
    // meaning must never change while the program runs.
    private final List<String> nameFragments;

    // An enum constructor runs once per constant above, when the enum class initializes.
    // "String..." accepts any number of words, so OLD can list two.
    Generation(String... fragments) {
        // List.of makes an unmodifiable copy: the array passed in can never be used to
        // alter this enum's behaviour later.
        this.nameFragments = List.of(fragments);
    }

    /**
     * @param poolName a memory pool name reported by the JVM, e.g. {@code "G1 Old Gen"}
     * @return true if that pool belongs to this generation
     */
    public boolean matches(String poolName) {
        // A missing name matches nothing - returning false is more useful to a caller
        // scanning a list of pools than throwing would be.
        if (poolName == null) {
            return false;
        }
        // Any one of the fragments appearing in the name is enough.
        for (String fragment : nameFragments) {
            if (poolName.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
