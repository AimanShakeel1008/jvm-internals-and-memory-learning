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
