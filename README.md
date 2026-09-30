# Core Java Course 01 — JVM Internals & Memory Management

Course 01 of 19 in the Core Java In-Depth series. Each course is a standalone repository teaching one deep Java topic through one dedicated project. This one covers what actually happens when Java runs: how source becomes bytecode, how classes load, where objects live, how garbage collection works, and how the JIT compiler makes code fast — in plain Java, deliberately without frameworks.

## The lessons

The course text is a self-contained HTML learning system — open **[`lessons/index.html`](lessons/index.html)** in any browser (works fully offline, by double-click) to reach the course hub, a "you are here" roadmap, and every lesson. Each lesson is a standalone page sharing one stylesheet, with inline SVG diagrams, occasional interactive simulations, hover-to-define glossary terms, syntax-highlighted code, and a print stylesheet for clean PDF notes. Best viewed in either light or dark mode — the pages adapt.

## The project: jvm-explorer

`jvm-explorer/` is a Maven project built to provoke and observe the JVM itself. Lesson by lesson it gains modules that fill heap regions on purpose, trigger and parse GC logs, watch classes load, overflow the stack on demand, and expose JIT behavior.

**Current capabilities (after Lesson 03):**

- Self-inspection: prints the running JVM's Java version, JVM name (HotSpot), vendor, maximum heap size, and CPU core count.
- Class file inspection: reads any loaded class's own `.class` bytes off the class path and decodes the eight-byte header — magic number, minor version, and major version turned into a Java release name.
- Disassembly subjects: four deliberately tiny methods (`BytecodeSubject`) built to be read with `javap -c -p`.
- Class loader reporting: for any class, names the loader that defined it and the full parent chain up to the bootstrap loader.
- Lazy-initialization demonstration: a witness log records which classes have run their static initializer, showing live that reading a compile-time constant wakes nothing while a static method call wakes its class.
- Memory region reporting: prints the heap's ceiling, committed size and used size, total non-heap usage, Metaspace usage, and how many classes are currently loaded.
- Reference semantics demonstration: shows live that a method can change the object it was handed but never the caller's variable, that two names can share one object, and that a copy is a separate object.
- Stack depth probe: overflows one thread's stack on purpose, catches the error, and reports how many frames fitted — with a fatter-framed variant for comparison.
- Heap filler: allocates and retains memory either safely up to a budget or, on explicit request, until the JVM throws `OutOfMemoryError` — then releases everything and reports the exact message.
- Experiments (labelled for-learning-only): a JIT warm-up stopwatch, a class-loading experiment whose markers line up against the JVM's own `-verbose:class` output, and a memory-limits experiment that walks up to both the stack wall and the heap wall.
- Tests: 55 JUnit tests. The build fails loudly if the running JVM is not the pinned Java 21, if class files are not compiled to Java 21, if the two disagree, if any class-initialization rule stops holding, or if the reference and memory-reporting rules stop holding.

## Requirements (pinned for the whole series)

| Tool | Version |
| --- | --- |
| Java | 21 (LTS) — Eclipse Temurin build recommended |
| Maven | 3.9.x |
| JUnit | 5.13.4 (managed by `pom.xml` — nothing to install) |

## How to run

All commands run from inside `jvm-explorer/`:

```bash
mvn test                 # compile everything and run all tests (the regression check)
mvn compile exec:java    # run the main program (com.corejava.jvm.JvmExplorer)
mvn clean                # delete generated output (target/) for a fresh build

# run the labelled warm-up experiment instead of the default main class
mvn compile exec:java -Dexec.mainClass=com.corejava.jvm.experiments.WarmupExperiment

# read the bytecode of the disassembly samples (after mvn compile)
javap -c -p target/classes/com/corejava/jvm/BytecodeSubject.class
javap -v target/classes/com/corejava/jvm/BytecodeSubject.class

# watch the JVM load classes, in a clean JVM (not Maven's) — after mvn compile
java -cp target/classes com.corejava.jvm.experiments.ClassLoadingExperiment
java -verbose:class -cp target/classes com.corejava.jvm.experiments.ClassLoadingExperiment

# walk up to the JVM's two memory walls — flags only apply in a clean JVM, so compile first
java -cp target/classes com.corejava.jvm.experiments.MemoryLimitsExperiment
java -Xss256k -cp target/classes com.corejava.jvm.experiments.MemoryLimitsExperiment
java -Xmx32m -cp target/classes com.corejava.jvm.experiments.MemoryLimitsExperiment --oom
```

The last command deliberately exhausts the heap and prints the resulting `OutOfMemoryError`. That is the intended outcome, not a bug — nothing else in the project does it, and no test ever does.

## Repository layout

```text
lessons/            the HTML learning system — start at lessons/index.html
  assets/lesson.css   the one shared stylesheet for every page
  index.html          course hub + "you are here" roadmap
  <phase>/<chapter>/  standalone lesson HTML pages
jvm-explorer/       the Maven project the course builds
glossary.md         every term the course introduces, in plain language
almanac.md          rules of thumb, contracts, and gotchas, lesson by lesson
troubleshooting.md  decodes the errors you are likely to hit, with fixes
```
