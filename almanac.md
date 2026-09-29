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
