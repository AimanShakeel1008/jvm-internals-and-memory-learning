# Core Java Course 01 — JVM Internals & Memory Management

Course 01 of 19 in the Core Java In-Depth series. Each course is a standalone repository teaching one deep Java topic through one dedicated project. This one covers what actually happens when Java runs: how source becomes bytecode, how classes load, where objects live, how garbage collection works, how the JIT compiler makes code fast, and how to look inside a running JVM — in plain Java, deliberately without frameworks.

**Status: complete — all eight lessons (00–07).**

## The lessons

The course text is a self-contained HTML learning system — open **[`lessons/index.html`](lessons/index.html)** in any browser (works fully offline, by double-click) to reach the course hub, a roadmap, and every lesson. Each lesson is a standalone page sharing one stylesheet, with inline SVG diagrams, occasional interactive simulations, hover-to-define glossary terms, syntax-highlighted code, and a print stylesheet for clean PDF notes. Best viewed in either light or dark mode — the pages adapt.

| # | Lesson | In one line |
| --- | --- | --- |
| 00 | Full Setup | pin every version in committed files and make the machine prove its identity |
| 01 | How Java Actually Runs | bytecode, the class file's header, `javap`, interpreter versus JIT |
| 02 | Class Loading | the three loaders, parent delegation, and exactly when a class initializes |
| 03 | Runtime Memory Areas | stack frames, the shared heap, Metaspace — and both memory walls on purpose |
| 04 | Object Lifecycle & GC Fundamentals | reachability, how Java still leaks, generations, ageing and promotion |
| 05 | GC Algorithms & Reading GC Logs | Serial, Parallel, G1, ZGC as trade-offs; a GC log read field by field |
| 06 | The JIT Compiler | tiers, OSR, inlining, escape analysis measured, deoptimization |
| 07 | Seeing Inside a Live JVM | `jcmd`, thread dumps, class histograms, heap dumps — catching a planted leak |

`almanac.md` collects every rule of thumb, lesson by lesson, and ends with **the whole course on one page**: the life of one object from `new` to collection.

## The project: jvm-explorer

`jvm-explorer/` is a Maven project built to provoke and observe the JVM itself. `mvn compile exec:java` runs a report with one section per lesson; the `experiments` package holds labelled for-learning-only programs that push the JVM to each behaviour on purpose.

**Capabilities:**

- **Self-inspection** (L00): the running JVM's Java version, JVM name (HotSpot), vendor, maximum heap and CPU cores.
- **Class file inspection** (L01): decodes any loaded class's eight-byte header — magic number and major/minor version as a Java release name; tiny `BytecodeSubject` methods to read with `javap -c -p`.
- **Class loading** (L02): any class's defining loader and full parent chain; a witness log that shows live which lines of code wake a class up.
- **Memory regions** (L03): heap ceiling / committed / used, non-heap, Metaspace and loaded-class count; reference-semantics demonstration; a stack-depth probe; a heap filler that can (on request) exhaust the heap and report the exact `OutOfMemoryError`.
- **Garbage collection** (L04): a reachability probe proving the GC rule four ways with weak references; every collector's counters and each generation's size.
- **GC logs** (L05): a parser and summariser for `-Xlog:gc` logs — pause counts by kind, total / average / max pause, share paused, and a live-data estimate.
- **JIT** (L06): JIT status; an allocation meter that shows escape analysis removing an object (≈24 bytes per call cold → ≈0 hot); a `-XX:+PrintCompilation` reader that tells replacement from deoptimization and narrates one method's compilation story.
- **Live-JVM tools** (L07): a planted memory leak and its bounded fix; a class-histogram parser and comparison tool that names what grew; an in-process heap dumper.
- **Experiments:** JIT warm-up, class loading, memory limits, generations, a GC workload, a five-phase JIT experiment, and a long-running leak hunt to attach `jcmd` / `jstack` / VisualVM to.
- **Tests:** 120 JUnit tests. They fail loudly if the JVM or the compiled class files are not the pinned Java 21, or if any rule the course demonstrates — class initialization, references, reachability, log parsing, the leak and its fix — stops holding. No test asserts on timing or on when the JIT compiles something.

## Requirements (pinned for the whole series)

| Tool | Version |
| --- | --- |
| Java | 21 (LTS) — Eclipse Temurin build recommended |
| Maven | 3.9.x |
| JUnit | 5.13.4 (managed by `pom.xml` — nothing to install) |

## How to run

All commands run from inside `jvm-explorer/`. Commands with JVM flags use plain `java`, so the flags reach the program's own JVM rather than Maven's — run `mvn compile` first.

```bash
mvn test                 # compile everything and run all tests (the regression check)
mvn compile exec:java    # the main report: one section per lesson
mvn clean                # delete generated output (target/) for a fresh build

# L01 — warm-up, and reading bytecode
mvn compile exec:java -Dexec.mainClass=com.corejava.jvm.experiments.WarmupExperiment
javap -c -p target/classes/com/corejava/jvm/BytecodeSubject.class

# L02 — watch classes load
java -verbose:class -cp target/classes com.corejava.jvm.experiments.ClassLoadingExperiment

# L03 — the two memory walls (the --oom run exhausts the heap on purpose)
java -Xss256k -cp target/classes com.corejava.jvm.experiments.MemoryLimitsExperiment
java -Xmx32m -cp target/classes com.corejava.jvm.experiments.MemoryLimitsExperiment --oom

# L04 — objects ageing and being promoted (Serial collector = textbook layout)
java -XX:+UseSerialGC -Xmx128m -Xmn32m -XX:MaxTenuringThreshold=3 "-Xlog:gc,gc+age=trace" -cp target/classes com.corejava.jvm.experiments.GenerationsExperiment

# L05 — produce a GC log and summarise it (repeat with -XX:+UseSerialGC / UseParallelGC / UseZGC)
java -Xmx64m -Xlog:gc:file=gc.log -cp target/classes com.corejava.jvm.experiments.GcWorkload
java -cp target/classes com.corejava.jvm.gclog.GcLogSummaryTool gc.log

# L06 — watch the JIT, then compare with escape analysis switched off
java -XX:+PrintCompilation -cp target/classes com.corejava.jvm.experiments.JitExperiment > jit.log
java -cp target/classes com.corejava.jvm.jit.CompilationLogTool jit.log JitSubjects
java -XX:-DoEscapeAnalysis -cp target/classes com.corejava.jvm.experiments.JitExperiment

# L07 — hunt the planted leak (needs a second terminal for jcmd)
java -Xmx256m -cp target/classes com.corejava.jvm.experiments.LeakHunt
jcmd <pid> GC.class_histogram > histo-before.log      # second terminal; again a minute later as histo-after.log
java -cp target/classes com.corejava.jvm.live.HistogramDiffTool histo-before.log histo-after.log
java -Xmx256m -cp target/classes com.corejava.jvm.experiments.LeakHunt --seconds=60 --dump
```

Two runs above end in `OutOfMemoryError` when asked to (`--oom`, or `LeakHunt` with a small `-Xmx`). That is intended; no test ever does it. Logs (`*.log`) and heap dumps (`*.hprof`) are git-ignored — heap dumps contain every value in memory and must never be committed.

## Repository layout

```text
lessons/            the HTML learning system — start at lessons/index.html
  assets/lesson.css   the one shared stylesheet for every page
  index.html          course hub + roadmap
  <phase>/<chapter>/  standalone lesson HTML pages
jvm-explorer/       the Maven project the course builds
glossary.md         every term the course introduces, in plain language
almanac.md          rules of thumb, contracts and gotchas — ends with the whole course on one page
troubleshooting.md  decodes the errors you are likely to hit, with fixes
```
