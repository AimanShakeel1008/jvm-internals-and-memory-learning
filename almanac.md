# Almanac — Core Java Course 01: JVM Internals & Memory Management

The living cheat-sheet of this course: rules of thumb, contracts, gotchas, and decision rules, appended lesson by lesson. Reread this before interviews and real work.

---

## Lesson 00 — Full Setup

**Pinned versions for the whole 19-course series** (decided once, checked by the build):

| Thing | Pinned | Where it is enforced |
| --- | --- | --- |
| Java | 21 (LTS), Temurin build | `pom.xml` (`maven.compiler.release`) + the `runsOnPinnedJavaVersion` test |
| Maven | 3.9.x | installed on the machine; check `mvn -version` |
| JUnit | 5.13.4 (the JUnit 5 line) | `pom.xml` (`junit.version` property) |
| Maven plugins | compiler 3.13.0 · surefire 3.5.2 · exec 3.5.0 | `pom.xml` |

**Rules of thumb:**

- **JDK = build + run. JRE = run only. JVM = the engine inside both.** You always install a JDK.
- **Pin versions in committed files; never rely on memory or "latest."** Never write `RELEASE` as a version, never leave out `maven.compiler.release`, never leave plugin versions unset — each of those lets a different machine silently choose differently.
- **Pinning means managing versions, not freezing them.** Patch releases carry security fixes; upgrade deliberately and re-check. Pin the *major* version in tests (21), not the exact patch build.
- **`.gitignore` before the first `git add .`** — Git only ignores files that were never committed; ignoring after committing does not pull them back out of history.
- **PATH picks your terminal's `java`; JAVA_HOME picks Maven's.** They can disagree on the same machine. `mvn -version` shows Maven's truth; `java -version` shows the terminal's. Check both, always in a **fresh** terminal after any install.
- **`mvn test` is the standing regression check** — one command answering "did I break anything?" Run it before every commit. Treat `Tests run: 0` as a failure, not a pass.
- **Never commit `target/`** (or any generated output). If a tool made it, the build can remake it.
- **The command line is the referee; the IDE is furniture.** If IntelliJ and `mvn test` disagree, believe `mvn test` and fix the IDE's SDK setting.
- **A 30-line throwaway does not need a project:** `java One.java` compiles and runs one file (Java 11+). In this course, throwaways go in a labelled `experiments/` package.
- **The machine's real output beats every prediction.** When observed behavior differs from expected, the observation wins and the difference is the lesson.
- **One lesson → one commit → one push**, message format `lesson: 01/lesson-XX - <title>`.

---

## Lesson 01 — How Java Actually Runs

**The class file version table** (the number lives in bytes 6–7 of every `.class` file):

| Major | 45 | 52 | 55 | 61 | **65** |
| --- | --- | --- | --- | --- | --- |
| Java | 1.1 | 8 | 11 | 17 | **21 (pinned)** |

- **Java release = major version − 44.** 65 − 44 = 21. Going the other way, Java 21 writes 65.
- **Compatibility goes one way: a newer JVM runs older class files; an older JVM always refuses newer ones.**
- **Decoding `UnsupportedClassVersionError`:** *"compiled by a more recent version (class file version X)"* = the **file**. *"this runtime only recognizes Y"* = the **JVM you are running**. Subtract 44 from both. The usual fix is a newer runtime, not a downgraded build.
- **Use `--release`, never `-source`/`-target` alone.** The old flags compile against the *current* JDK's library, so a "Java 17" file can call a Java 21 method and die with `NoSuchMethodError`. In Maven: `<maven.compiler.release>`.
- **Check a jar's Java version without running it:** `javap -v -cp thing.jar com.acme.Thing | findstr "major"` (`| grep major` elsewhere).
- **"Write once, run anywhere" covers the compiled file, not versions** — and not code that hard-codes paths, OS libraries, or thread timing.

**The class file header:**

| Bytes | Field | Value |
| --- | --- | --- |
| 0–3 | magic | always `0xCAFEBABE` |
| 4–5 | minor version | 0, except **65535** = compiled with preview features |
| 6–7 | major version | 65 for Java 21 |

- **A Java `byte` is signed.** Mask every byte with `& 0xFF` as it leaves the array, and hold four-byte unsigned values in a `long` — `0xCAFEBABE` does not fit in a positive `int`.
- **Find a class's own bytes via `getResourceAsStream("/pkg/Name.class")`, never a hard-coded `target/classes/...` path** — the lookup keeps working from a jar and inside tests.

**Reading bytecode:**

- **`javap -c -p <file>.class` is the default habit.** Without `-p`, private members are simply missing. Add `-v` for the version header and constant pool, `-l` for line numbers and local names.
- **The number on the left of an instruction is a byte offset, not a line number.** A gap of 3 means opcode + two operand bytes.
- **Instructions carry their type in their name:** `i` int, `l` long, `f` float, `d` double, `a` reference.
- **Descriptors:** `I` int, `J` long, `Z` boolean, `V` void, `Ljava/lang/String;` a class, `[I` an array. `main` is `([Ljava/lang/String;)V`.

**Bytecode shapes worth recognising on sight:**

| Shape | What it was in source |
| --- | --- |
| conditional jump forward + `goto` backward | any loop |
| `new` → `dup` → `invokespecial` | one `new Something(...)` |
| `iinc slot, 1` | `i++` as a statement (one instruction, no stack traffic) |
| `invokedynamic … makeConcatWithConstants` | string `+` (Java 9 and later) |
| `invokedynamic … LambdaMetafactory` | a lambda expression |
| `"<init>"` | a constructor — stored as an ordinary method returning `V` |

- **Source lines and work done are different things.** Never estimate cost from how source looks.

**Interpreter and JIT:**

- **The JVM does both at once:** it interprets immediately while counting calls, then compiles hot methods on a background thread — C1 first, C2 for the hottest (tiered compilation).
- **No Java timing means anything without warm-up.** First run slow, tenth run fast is normal.
- **Bytecode is what you ship; native code is what runs.** Read bytecode to understand mechanisms and settle facts; use a profiler or JMH to find and measure slowness.
- **Never assert on timing in a test.** Assert the shape (count of results, positive elapsed time, work done); let human eyes read the timings.

---

## Lesson 02 — Class Loading

**The three built-in loaders** (every standard JVM starts with these, in this parent chain):

| Loader | `getName()` | Defines | Parent |
| --- | --- | --- | --- |
| Bootstrap | *no Java object — `getClassLoader()` returns `null`* | `java.base`: `Object`, `String`, `Integer`… | — (top) |
| Platform | `"platform"` | the rest of the JDK, e.g. `java.sql`, `java.xml` | bootstrap |
| Application | `"app"` | your class path — your code and libraries | platform |

- **`getClassLoader() == null` means bootstrap, not "missing".** Calling a method on it is a guaranteed `NullPointerException`. Translate null to the word "bootstrap" at the boundary.
- **`ClassLoader.getSystemClassLoader()` is the APPLICATION loader.** "System" is historical and has nothing to do with the operating system.
- **Parent delegation: ask upward first, search downward.** Payoffs: a forged `java.lang.String` on your class path is *unreachable*; every JDK class exists exactly once; "not found" means genuinely nowhere.
- **Delegation is a convention in `ClassLoader.loadClass`, not a law.** Servlet containers and plugin systems deliberately invert it, which is where the strangest class-loading bugs live.
- **A class's runtime identity is its name PLUS its defining loader.** Two loaders defining `com.acme.Config` give two incompatible types → `ClassCastException: com.acme.Config cannot be cast to com.acme.Config`. Read the loader names in brackets; usually one jar is present in two places.

**The five lifecycle steps** — *loading → linking (verify, prepare, resolve) → initialization*:

| Step | What actually happens |
| --- | --- |
| 1 Loading | find the bytes, parse them, make the `Class` object. Magic number + version checked here. |
| 2a Verification | prove the bytecode cannot misbehave *before* running it. Failure = `VerifyError`. |
| 2b Preparation | static fields get **default** values (`0` / `false` / `null`) — *not* yours. Compile-time constants are the exception: filled in here from `ConstantValue`. |
| 2c Resolution | `#7` symbols become real pointers. HotSpot does this **lazily** — a missing class can surface hours into a run. |
| 3 Initialization | `<clinit>` runs: your static assignments and `static { }` blocks, in **source order**, exactly once. |

**`<clinit>` — three permanent facts:**

- **Runs exactly once, per class, per loader.** There is no "re-initialize". Hot reload = a whole new class loader.
- **Thread-safe for free.** The JVM locks it — which is what makes the *initialization-on-demand holder* idiom work with no `synchronized`.
- **If it throws, the class is poisoned forever.** First failure = `ExceptionInInitializerError` (with the real cause). Every later touch = `NoClassDefFoundError: Could not initialize class X`.
- **Keep `<clinit>` boring.** No file reading, no network, nothing that can throw. Put anything that can fail in a method the caller invokes on purpose.

**What triggers initialization:**

| Wakes the class | Leaves it cold |
| --- | --- |
| `new Foo()` | `import com.acme.Foo;` |
| a `static` method **declared by** `Foo` | `Foo.class` |
| a **non-constant** `static` field declared by `Foo` | `new Foo[10]` |
| `Class.forName("Foo")` | `Class.forName("Foo", false, loader)` / `loader.loadClass("Foo")` |
| initializing a **subclass** (parent goes first) | reading a `static final` compile-time constant |
| being the class named on the `java` command line | a static field declared in the **superclass**, named through the subclass |
| | initializing a superclass (parents never wake children) |

- **Only the class that DECLARES a static field initializes.** `Child.PARENT_FIELD` initializes `Parent` only; `Child` is loaded but stays cold.
- **Compile-time constants are inlined into the reader's own class file.** Changing `public static final String API_VERSION = "1.0"` in a library does nothing for callers that were not recompiled. Expose changeable values through a method or a non-constant field; rebuild every consumer from clean when a constant changes.
- **A mention is not a use.** An import never registers a JDBC driver; that is why old code calls `Class.forName("...Driver")`.

**Reading the errors:**

| Message | Real meaning | Where to look |
| --- | --- | --- |
| `ClassNotFoundException` (checked) | someone asked **by name as text** and the whole chain came up empty | the name: typo, wrong package, jar absent |
| `NoClassDefFoundError` (Error) | the JVM was **resolving** a reference your compiled code contains | a dependency present at build time, missing at run time |
| `NoClassDefFoundError: Could not initialize class X` | X is **present** and permanently broken — its `<clinit>` threw earlier | scroll **back** for the first `ExceptionInInitializerError` |
| `ExceptionInInitializerError` | the first static-initializer failure | its `getCause()` |
| `ClassCastException: X cannot be cast to X` | two loaders, one name, two types | the loader names in brackets |

**Observing it:**

- `java -verbose:class -cp target/classes com.example.Main` — one line per class as it loads, with its source. `-Xlog:class+load=info` is the modern equivalent; `-Xlog:class+load=info:file=classes.txt` sends it to a file.
- **Use a plain `java` command, not `mvn exec:java`,** for this — `exec:java` runs inside Maven's own JVM.
- **Custom class loaders solve exactly two problems:** isolation and reloading. Everything else is a dependency-management problem. Every custom loader risks a **class loader leak**.


---

## Lesson 03 — Runtime Memory Areas

**The region map** (know which column a thing is in, and half of memory debugging is already done):

| Region | Per thread or shared? | Holds | Sized by |
| --- | --- | --- | --- |
| Stack | **per thread** | one frame per in-progress method call | `-Xss` |
| PC register | **per thread** | the address of the instruction running now | — |
| Native method stack | **per thread** | frames for calls into C/C++ inside the JVM | platform |
| **Heap** | shared | every object and array made with `new` | `-Xmx` / `-Xms` |
| **Metaspace** | shared | one description per loaded class | `-XX:MaxMetaspaceSize` (unbounded by default) |
| Code cache | shared | native machine code the JIT produced | `-XX:ReservedCodeCacheSize` (verify on your JDK) |

**The stack:**

- **A frame holds three things:** the local variable slots, the operand stack, and the return address. Pushed on call, popped on return.
- **Stack memory is reclaimed by arithmetic, not by a collector** — popping a frame is one subtraction. That is why locals are effectively free.
- **Local variables are thread-safe for free**, not by luck: a local lives in a frame, a frame lives on one thread's stack, and no other thread can reach it. "Keep state local" works for a mechanical reason.
- **The stack does not grow on demand.** Its size is fixed when the thread is created.

**The heap:**

- **Every object and array lives on the heap**, shared by every thread. (One honest footnote: escape analysis may keep a non-escaping object off it — Lesson 06. Never changes behaviour.)
- **`used <= committed <= max`, always.** `max` is the ceiling, `committed` is what the JVM has taken from the OS so far, `used` is what currently holds data — *including garbage not yet collected*, which is why a single `used` reading is a terrible basis for sizing a heap.
- **Running out of heap is not "allocating too much"** — it is *retaining* too much. A program can allocate a terabyte in a 32 MB heap as long as it stops using each piece.
- **`x = null` makes an object eligible for collection; it does not collect it** and does not return memory to the OS. `System.gc()` is a request, not a command.

**The one rule about variables:**

- **A variable of a class, interface or array type holds a reference — an arrow — not the object.** Assignment copies the arrow. Two arrows can point at one object (aliasing).
- **A primitive variable holds the value itself** in its slot. Eight types: `byte short int long float double char boolean`.
- **Java is pass-by-value, always — including references.** Retire the phrase *"Java passes objects by reference"*; it predicts real bugs incorrectly.

| Inside a method | Caller sees it? | Why |
| --- | --- | --- |
| `obj.setX(...)` — change the **object** | **yes** | one object on the shared heap, reached through the copied arrow |
| `obj = new Thing()` — change the **variable** | **no** | overwrites a slot in the callee's own frame, which is about to be destroyed |
| `n = 99` on an `int` parameter | **no** | the slot holds a copy of the number; there is no arrow at all |

- **Arrays are objects.** `int[] b = a;` shares one array. For a separate one: `a.clone()` or `Arrays.copyOf(a, a.length)`.
- **`==` compares arrows; `equals` compares contents.** Two objects with equal contents are `equals` and not `==`.

**Metaspace and the PermGen history:**

- **One class description per loaded class, however many instances exist.** A million `Customer` objects on the heap; one description of `Customer` in Metaspace.
- **Java 8 removed PermGen.** Metaspace replaced it, comes from **native** memory, and is **unbounded by default** — so a class-loader leak now eats the machine instead of hitting a small ceiling. Set `-XX:MaxMetaspaceSize` deliberately if you want it to announce itself.
- **`-XX:MaxPermSize` on Java 8+ is ignored with a warning**, which is how it survives in startup scripts for a decade.
- **Interned strings moved to the ordinary heap in Java 7** — one release *before* PermGen was removed. Two separate moves, routinely confused.
- **HotSpot detail (implementation, not specification):** a class's *static fields* live in its `java.lang.Class` object on the **heap**; Metaspace holds the structural description. Mark to verify against your JDK if it ever matters.

**Reading the two walls:**

- **`StackOverflowError` is a logic bug until proven otherwise.** The repeated line in the trace *is* the diagnosis: one method repeating = missing base case; a short cycle (`a → b → c → a`) = mutual recursion, often two `toString`s calling each other or a cyclic entity graph; a thousand *different* frames = legitimately deep recursion, so raise `-Xss` or rewrite with an explicit list.
- **`-Xss` is per thread.** Doubling it on a 500-thread server asks the OS for 500 × the increase.
- **Never raise `-Xmx` for a `StackOverflowError`.** Different region, zero effect.

**The `OutOfMemoryError` message table — the words after the colon decide the investigation:**

| Message | What filled | Look first at |
| --- | --- | --- |
| `Java heap space` | the object heap | what still *retains* objects: a growing cache, a static collection, a listener never removed. Heap dump (Lesson 07) |
| `Metaspace` | class metadata | far too many classes: repeated redeploys, a class-loader leak, runaway proxy generation |
| `GC overhead limit exceeded` | nothing yet | same causes as `Java heap space`; the collector is recovering almost nothing and the JVM gave up early |
| `Requested array size exceeds VM limit` | nothing | one array near `Integer.MAX_VALUE` elements — a bad length calculation, not a shortage |
| `Direct buffer memory` | off-heap native memory | direct `ByteBuffer`s not released, usually an I/O or networking pool |
| `unable to create native thread` | OS memory or an OS limit | a thread leak, or stacks too large. **Raising `-Xmx` makes this worse** |

- **In production, let an `OutOfMemoryError` kill the JVM and read the dump.** Catching it in application code almost never helps: you are still holding everything, and building the log line needs memory. Run with `-XX:+HeapDumpOnOutOfMemoryError`.
- **Catching an `Error` is right in exactly one situation:** a tool that provokes it on purpose and drops every reference it held *before* doing anything else.

**Observing it:**

- `Runtime.getRuntime()` gives `maxMemory()` / `totalMemory()` / `freeMemory()` — always available, no extra modules.
- `ManagementFactory` gives the finer view: `getMemoryMXBean().getNonHeapMemoryUsage()`, `getMemoryPoolMXBeans()` (search **by name**, the list varies), `getClassLoadingMXBean().getLoadedClassCount()`.
- **There is no portable API for "how big is my stack".** Measure it by overflowing on purpose and counting.
- **Every memory number is an observation about one machine at one moment.** Defaults come from the machine's RAM — and inside a container, from the *container's* limits, which is why a service that is fine on a laptop dies in Kubernetes.
