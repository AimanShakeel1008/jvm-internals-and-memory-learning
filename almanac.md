# Almanac — Core Java Course 01: JVM Internals & Memory Management

The living cheat-sheet of this course: rules of thumb, contracts, gotchas, and decision rules, appended lesson by lesson. Reread this before interviews and real work.

---

## Lesson 00 — Full Setup

**Pinned versions for the whole 19-course series** (decided once, checked by the build):

| Thing | Pinned | Where it is enforced |
| --- | --- | --- |
| Java | 21 (LTS), Temurin build | `pom.xml` (`maven.compiler.release`) + the `runsOnPinnedJavaVersion` test |
| Maven | 3.9.x (3.9.16 at setup) | installed on the machine; check `mvn -version` |
| JUnit | 5.13.4 (the JUnit 5 line) | `pom.xml` (`junit.version` property) |

**Rules of thumb:**

- **JDK = build + run. JRE = run only. JVM = the engine inside both.** You always install a JDK.
- **Pin versions in committed files; never rely on memory or "latest."** Unpinned versions drift silently between machines and over time — and drift poisons observations about JVM behavior.
- **`.gitignore` before the first `git add .`** — Git only ignores files that were never committed; ignoring after committing does not pull them back out of history.
- **PATH picks your terminal's `java`; JAVA_HOME picks Maven's.** They can disagree on the same machine. `mvn -version` shows Maven's truth; `java -version` shows the terminal's. Check both, always in a **fresh** terminal after any install.
- **`mvn test` is the standing regression check** — one command answering "did I break anything?" Run it before every commit.
- **Never commit `target/`** (or any generated output). If a tool made it, the build can remake it.
- **The command line is the referee; the IDE is furniture.** If IntelliJ and `mvn test` disagree, believe `mvn test` and fix the IDE's SDK setting.
- **The machine's real output beats every prediction.** When observed behavior differs from expected, the observation wins and the difference is the lesson.
- **One lesson → one commit → one push**, message format `lesson: 01/lesson-XX - <title>`.

---

## Lesson 01 — How Java Actually Runs

**The class file version table** (the number lives in bytes 6–7 of every `.class` file):

| Major | 45 | 52 | 53 | 55 | 61 | **65** |
| --- | --- | --- | --- | --- | --- | --- |
| Java | 1.1 | 8 | 9 | 11 | 17 | **21 (pinned)** |

- **Java release = major version − 44.** Do it in your head: 65 − 44 = 21. Going the other way, Java 21 writes 65.
- **Compatibility is one-directional: a newer JVM runs older class files; an older JVM always refuses newer ones.** It has to — the newer file may contain structures that did not exist when that JVM was built.
- **Decoding `UnsupportedClassVersionError`:** *"has been compiled by a more recent version (class file version X)"* = the **file**. *"this runtime only recognizes version Y"* = the **JVM you are running**. Subtract 44 from both. The usual fix is a newer runtime, not a downgraded build.
- **Use `--release`, never `-source`/`-target` alone.** The old flags set the language level and class file version but still compile against the *current* JDK's libraries — producing a file marked "Java 17" that calls a Java 21 method and dies at runtime with `NoSuchMethodError`. `--release` swaps the API definitions too. In Maven: `<maven.compiler.release>`.
- **Check a jar's Java version without running it:** `javap -v -cp thing.jar com.acme.Thing | findstr "major"` (`| grep major` elsewhere).

**The class file header** (identical in every `.class` file ever produced):

| Bytes | Field | Value |
| --- | --- | --- |
| 0–3 | magic | always `0xCAFEBABE` |
| 4–5 | minor version | 0, except **65535** = compiled with preview features |
| 6–7 | major version | 65 for Java 21 |

- **A Java `byte` is signed.** Reading binary formats by hand means masking every byte with `& 0xFF` the moment it leaves the array, and holding four-byte unsigned values in a `long` — `0xCAFEBABE` does not fit in a positive `int`.
- **Find a class's own bytes via `getResourceAsStream("/pkg/Name.class")`, never a hard-coded `target/classes/...` path.** The resource lookup keeps working from a jar and inside tests.

**Reading bytecode:**

- **`javap -c -p <file>.class` is the default muscle memory.** Without `-p` private members are simply absent, which reads exactly like "the compiler removed it". Add `-v` for the version header and constant pool, `-l` for line numbers and local variable names.
- **The number on the left of a disassembled instruction is a byte offset, not a line number.** Uneven gaps tell you an instruction carried operand bytes (3 apart = opcode + two operand bytes).
- **Instructions carry their type in their name:** `i` = int, `l` = long, `f` = float, `d` = double, `a` = reference. So `areturn` returns an object, `ireturn` returns an int.
- **Descriptors:** `I` int, `J` long, `Z` boolean, `V` void, `Ljava/lang/String;` a class, `[I` an array. `main` is `([Ljava/lang/String;)V`.

**Bytecode shapes worth recognising on sight:**

| Shape | What it was in source |
| --- | --- |
| conditional jump forward + `goto` backward | any loop (`for`, `while`, enhanced-for) |
| `new` → `dup` → `invokespecial` | one `new Something(...)` |
| `iinc slot, 1` | `i++` as a statement (one instruction, no stack traffic) |
| `invokedynamic … makeConcatWithConstants` | string `+` (Java 9 and later) |
| `invokedynamic … LambdaMetafactory` | a lambda expression |
| `"<init>"` | a constructor — stored as an ordinary method returning `V` |

- **Source lines and work done are different things.** `total += i` is four instructions; `i++` is one; a single `+` on strings is a call into a JDK helper. Never estimate cost from the shape of the source.

**Interpreter and JIT:**

- **The JVM does both at once:** it interprets immediately (instant start, ~10–100× slower than native) while counting how often each method and loop runs, then compiles the hot ones to native code on a background thread — C1 first (fast compile), C2 for the hottest (deep optimisation). The arrangement is called tiered compilation.
- **No Java timing means anything without warm-up.** First run slow, tenth run fast is the normal, expected behaviour — not noise, not a bug.
- **Bytecode is what you ship; native code is what runs.** After C2, the loop you are reading may not exist. Read bytecode to understand mechanisms and settle factual arguments; use a profiler to find slow code and JMH to measure it.
- **Never assert on timing in a test.** A test that depends on speed is flaky, and one flaky test teaches a team to ignore red builds. Assert the shape (right number of results, positive elapsed time, work actually happened); let human eyes read the timings.
