# Lesson 00 — Full Setup

**Position in course:** Lesson 00 of 07 (the setup lesson) — Core Java Course 01: JVM Internals & Memory Management
**Project files created:** `jvm-explorer/pom.xml`, `jvm-explorer/src/main/java/com/corejava/jvm/JvmExplorer.java`, `jvm-explorer/src/test/java/com/corejava/jvm/JvmExplorerTest.java`
**Course files created:** `glossary.md`, `almanac.md`, `README.md`, `troubleshooting.md`
**Prerequisites:** None. This is the first lesson of the first course in the series.

---

## SECTION 1 — THE PAIN

Picture two people working on the same small Java program. On Priya's laptop it compiles cleanly, all tests pass, everything is green. She sends the exact same code to Sam. On Sam's laptop the compiler rejects a line she wrote, one test fails with a bizarre message, and the build tool downloads a different version of a library without telling anyone. Nothing about the *code* changed. What changed is the *machines*: Sam has an older Java, a different build-tool version, and no record of which library versions the project expects. This failure is so common it has a sarcastic industry name: **"works on my machine."** It burns hours, and the hours are pure waste — nobody is fixing a real bug, they are fixing a disagreement between two computers.

Now shrink that story to one person: you, three weeks from now. You come back to this course after a break. In the meantime you installed some other tool that quietly put a *different* Java at the front of your system's search path. Suddenly lesson code that worked before behaves differently — and this course is about *observing the JVM's exact behavior*, so "slightly different Java" can mean "every observation is now slightly wrong." A GC log line looks different. A default changed. You cannot tell whether you are learning a fact about Java or a fact about your accidental upgrade.

The cure is boring and absolute: **decide every version once, write the decision into files that live with the code, and make the machine prove its identity before anything else runs.** That is all this lesson is. It looks like installation chores; it is actually the first real JVM lesson, because the very first thing we build is a program that interrogates the JVM about itself — version, vendor, memory ceiling — and a test that fails loudly if the machine drifts from the agreement.

## SECTION 2 — REAL WORLD ANALOGY

Setting up a professional kitchen before cooking a 7-course meal.

| Kitchen | This lesson |
| --- | --- |
| The stove, oven, and knives — the equipment that does the actual cooking | The **JDK** — the toolkit that compiles and runs Java |
| The head chef's rule "we use gas stove model X, nothing else" | **Pinning Java 21** — one agreed version, written down |
| The kitchen porter who fetches ingredients from suppliers so cooks never leave their station | **Maven** — fetches libraries and runs the build for you |
| The recipe card taped above the counter listing exact ingredient brands and amounts | **`pom.xml`** — the one file naming every ingredient and version |
| Tasting each dish before it leaves the kitchen | **JUnit tests** — automatic checks that run on demand |
| The kitchen diary: who changed which recipe, when, and why | **Git** — the history of every change to the project |
| The off-site recipe safe, in case the kitchen burns down | **GitHub** — the copy of that history on another computer |
| The "do not put in the diary" list (scratch notes, personal reminders) | **`.gitignore`** — files Git is told to pretend not to see |

One mapping deserves emphasis: the recipe card (`pom.xml`) is taped *inside the kitchen*, not kept in someone's head. Any cook who walks in can reproduce the dish exactly. That is the whole philosophy of this lesson: **the machine's configuration lives in committed files, never in memory or luck.**

## SECTION 3 — THE CONCEPT EXPLAINED

We will meet each tool in dependency order: first the thing that runs Java at all, then the thing that builds projects, then testing, then history-keeping. Every term gets introduced fully the first time. Take it slowly; everything later in the course stands on these.

### 3.1 What actually runs your Java code

Here is a puzzle worth sitting with. A program compiled on Windows normally *cannot* run on Linux, because a compiled program is a list of instructions for a specific kind of machine, and Windows-machine instructions and Linux-machine conventions differ. Yet compiled Java runs on both, unchanged. How?

The trick: Java code is not compiled for any real machine. It is compiled for an *imaginary, idealized* machine — a machine that does not exist as hardware. Then, on every real computer, a program is installed whose whole job is to *pretend to be* that imaginary machine: it reads the imaginary machine's instructions and carries them out using the real machine underneath. Your compiled program only ever speaks to the pretender, so it never notices which real computer it is on. A program that acts out the role of a machine like this is called a **virtual machine**, and the one that acts out Java's imaginary machine is the **Java Virtual Machine — the JVM**. The JVM is the main character of this entire course: it is where your objects live, where memory is managed, where garbage is collected, where code gets fast. (How your source code becomes instructions for that imaginary machine is exactly Lesson 01.)

Three package names get confused constantly, so let's pin them down:

- The JVM alone can run *already-compiled* code but cannot compile anything. The JVM bundled together with the standard libraries (the huge collection of ready-made classes like `String` and `ArrayList`) forms the minimum needed to *run* Java programs. This run-only bundle is called the **JRE — Java Runtime Environment**. (Modern Java no longer ships it as a separate download, but the name survives in error messages and old articles, so you must recognize it.)
- Everything in the JRE *plus* the developer tools — most importantly `javac` (the compiler, which turns your source code into instructions the JVM understands — Lesson 01 dissects this) and diagnostic tools we will use in Lesson 07 — is called the **JDK — Java Development Kit**. As a developer you always install a JDK.

```text
+------------------------------- JDK -------------------------------+
|  developer tools: javac, javap, jcmd, jmap, jstack, ...           |
|  +----------------------------- JRE ---------------------------+  |
|  |  standard libraries: String, List, Files, ...               |  |
|  |  +--------------------------- JVM -------------------------+|  |
|  |  |  the engine: executes code, owns memory, runs GC & JIT  ||  |
|  |  +----------------------------------------------------------+|  |
|  +--------------------------------------------------------------+  |
+--------------------------------------------------------------------+
```

Rule of thumb: **JDK = build + run. JRE = run only. JVM = the engine inside both.**

One more wrinkle: "the JDK" is not one product from one company. The source code is open (the OpenJDK project), and several organizations compile that shared source into installable packages — Eclipse Adoptium (whose builds are named **Temurin**), Oracle, Amazon, Microsoft, and others. Same engine, different wrapper, like the same novel issued by different publishers. This course uses **Eclipse Temurin** because it is free for all use and the de-facto community default. The engine inside all of these is named **HotSpot** — remember that name; when this course says "the JVM does X," it means HotSpot specifically, and our first program will print that name so you can see it is really there.

### 3.2 Why version 21, and why "pinned"

Java releases a new version every six months, but most versions get updates for only six months — fine for experiments, bad for a 19-course journey. Every couple of years, one release is designated for *years* of continued fixes and security patches. A release with that long support promise is called an **LTS — Long-Term Support** release. Java 21 is an LTS release, is what a large share of the industry runs, and has every feature this series needs. So the whole series pins **Java 21**.

**Pinning** means: the version is written into committed files (`pom.xml` says `21`; this lesson says Temurin 21; the almanac records it), the build *checks* it, and nobody changes it casually. The payoff is that when code behaves oddly, "wrong version somewhere" is eliminated as a suspect in seconds — you run one test and know. In a course about observing precise JVM behavior, that certainty is oxygen: GC defaults, log formats, and JIT behavior all shift between versions, and we need to know which Java produced what we observe.

### 3.3 Maven — the build tool

Suppose there were no build tool. To build even our small project you would personally: download the JUnit library from the internet — plus the *other* libraries JUnit itself needs, finding each by hand; invoke `javac` with a correctly-spelled list of every source file and every library location; keep compiled test code out of the shipped program; run the tests; package everything into a distributable archive. Every step retyped, in the right order, forever, and your teammate must replicate your exact downloads to get the same result. People really worked this way; it was misery.

A program that does all of that from a single written description is called a **build tool**, and the standard one in the Java world (alongside a popular alternative called Gradle) is **Maven**. You write *what the project is* — its name, its Java version, which libraries it uses — into one file, and Maven derives *how to build it*. That file is `pom.xml` (**POM** = Project Object Model — the project described as structured data), and it always sits at the project root. You have one committed at `jvm-explorer/pom.xml`, where every line carries a comment explaining why it exists.

Concepts inside the POM you will meet constantly:

- A **dependency** is a library your code depends on — someone else's compiled code your project uses instead of rewriting. You declare it by name; Maven fetches it, *and everything it in turn depends on*, automatically. Our only dependency is JUnit.
- Every library (and your own project) is identified by three **coordinates**, like a postal address: `groupId` (who made it — reversed-domain style, e.g. `org.junit.jupiter`), `artifactId` (which product, e.g. `junit-jupiter`), `version` (which edition, e.g. `5.13.4`). Three parts because names collide — dozens of libraries are called "core"; coordinates make each unambiguous.
- Where does Maven fetch from? A public warehouse site holding millions of published libraries, called **Maven Central**. Downloads are cached in a folder on your machine (`~/.m2/repository`, i.e. `C:\Users\<you>\.m2\repository` on Windows) so each library downloads once, not per-build. First builds are slow and network-hungry; later builds are quiet. Expect that.
- Maven itself is a thin coordinator; the real work is done by **plugins** — bolt-on components, one per job (a compiler plugin that calls `javac`, a test-running plugin, a run-my-main-class plugin). Plugins are versioned like everything else, and our POM pins each one, because an unpinned plugin version is chosen *for* you, differently on different machines — the exact disease we are curing.
- Our own version, `1.0-SNAPSHOT`, uses the Maven convention where the suffix **SNAPSHOT** marks a version as "still moving, not a frozen release." Right for a project that changes every lesson.

Maven also ends the "where do files go?" debate by *convention*: main code lives in `src/main/java`, test code in `src/test/java`, and everything Maven generates goes into `target/` — a disposable folder you never edit and never commit (our `.gitignore` already excludes it; deleting it costs nothing, which is literally what `mvn clean` does).

```text
jvm-explorer/
    pom.xml                                   <- the recipe card (committed)
    src/main/java/com/corejava/jvm/           <- real program code (committed)
    src/test/java/com/corejava/jvm/           <- test code (committed)
    target/                                   <- Maven's scratch output (NEVER committed)
```

You drive Maven from the terminal, from the folder containing `pom.xml`. The commands this course uses constantly:

| Command | What it does |
| --- | --- |
| `mvn compile` | Compile the main source code into `target/` |
| `mvn test` | Compile everything, then run all tests and report pass/fail |
| `mvn clean` | Delete `target/` for a guaranteed-fresh next build |
| `mvn exec:java` | Run the project's main class (our POM tells it which one) |

### 3.4 JUnit — tests that check themselves

Without tests, "does the project still work?" means running the program and eyeballing the output — and as the project grows, you re-eyeball *everything after every change*, or (realistically) you stop checking and break things silently. A **unit test** is a small piece of code whose job is to check a piece of real code automatically: it calls the real code, states what *must* be true about the result, and a framework runs every such check and prints a verdict. The standard framework for this in Java is **JUnit** (we pin version 5.13.4, the current release of the JUnit 5 line). Inside a test, each "this must be true" statement is called an **assertion** — like `assertEquals(21, actualVersion)`, which passes silently if the value is 21 and fails the test loudly if not.

The habit this course builds: every capability jvm-explorer gains comes with a JUnit test asserting its expected behavior, so **`mvn test` is a one-command answer to "did I break anything?"** — run it any time, especially before committing. Our first three tests (section 6) guard the machine itself.

### 3.5 Git and GitHub — the project's memory

Without version control, project history is a folder of copies named `project-final-v2-REALLY-FINAL/`, and one bad edit with no surviving copy loses work forever. A **version control system** is a program that records snapshots of your files over time, so you can see what changed, when, and why, and return to any earlier state. The standard one is **Git**. Its vocabulary, each piece first in plain words:

- A **repository** (repo) is a folder whose history Git is tracking — the visible files plus a hidden `.git` subfolder holding every recorded snapshot. This course folder is a repo; that is why it contains `.git/`.
- A **commit** is one saved snapshot with a message saying what changed. Made in two steps: `git add <files>` marks which changes to include (the shortcut `git add .` means "everything changed under this folder"), then `git commit -m "message"` seals them into history.
- Git history alone still lives on one disk. **GitHub** is a website that stores copies of Git repositories online — backup plus sharing. The online copy linked to your local repo is called the **remote**; `git push` uploads your new commits to it. Your repo's remote is already connected (the initial commit is already pushed).
- A **`.gitignore`** file lists names Git must pretend not to see, so `git add .` can never accidentally stage them. Two categories belong in ours: *rebuildable machine output* (`target/`, `*.class`, IDE settings folders) because committing generated files bloats history and causes fake conflicts; and *this course's three local-only control files* — `CLAUDE.md`, `curriculum.md`, `progress-tracker.md` — which are private course machinery, not course content. Critical ordering rule: **`.gitignore` must exist before the first `git add .`**, because ignoring a file *after* it is committed does not remove it from history. (Your repo did this correctly.)

The rhythm for the rest of the course: **one lesson → one commit → one push**, message format `lesson: 01/lesson-XX - <title>`. Each lesson ends by printing the exact commands.

### 3.6 The IDE

You could write Java in Notepad, but you would be typing blind: no red underline under a typo (you would discover it at compile time), no auto-completion of names, no one-keystroke rename-everywhere, no clickable test results. A program that bundles editor, error-checking, navigation, and test running into one workbench is called an **IDE — Integrated Development Environment**. Recommended here: **IntelliJ IDEA Community Edition** — free, and the strongest Java experience (VS Code with the "Extension Pack for Java" is a fine lighter alternative). One honest warning, because this course cares about truth-on-the-machine: IntelliJ ships its *own* JDK list and its *own* test runner, so the IDE can disagree with the pinned setup. The command line (`mvn test`) is this course's referee; the IDE is a comfortable place to read and write code. Open the `jvm-explorer` folder (the one holding `pom.xml`) and IntelliJ recognizes it as a Maven project automatically.

### 3.7 The installation checklist

Do these in order; each has a verification step, and troubleshooting for every step lives in `troubleshooting.md`. Two terms first. Programs on your machine find each other through the **PATH** — an operating-system list of folders searched, in order, whenever you type a bare command name like `java`; installers offering to "add to PATH" are offering to put their folder on that list, and *which* Java wins when several are installed is decided purely by list order (our version test exists to catch exactly that). Separately, many Java tools read **JAVA_HOME** — a conventionally-named variable you set to your JDK's install folder; Maven uses it to decide which Java to run *with*, so if it is unset or points at an old JDK, Maven misbehaves even though `java -version` looks right.

1. **JDK 21 (Temurin).** From [adoptium.net](https://adoptium.net/) download Temurin **21 (LTS)** for Windows x64 (the `.msi`). In the installer, enable both "Add to PATH" and "Set JAVA_HOME". Verify in a *fresh* terminal:
   ```text
   java -version    →  must say 21.x, e.g. openjdk version "21.0.x" ... Temurin ...
   javac -version   →  must say javac 21.x  (proves the full JDK, not just a runtime)
   ```
2. **Maven.** From [maven.apache.org/download.cgi](https://maven.apache.org/download.cgi) download the **binary zip** of the current 3.9.x release (3.9.16 as of this writing — any 3.9.x is fine). Unzip somewhere permanent (e.g. `C:\tools\apache-maven-3.9.16`), add its `bin` subfolder to PATH, open a fresh terminal, and verify: `mvn -version` must print the Maven version *and* `Java version: 21` (Maven reporting the wrong Java is the JAVA_HOME problem above).
3. **Git.** From [git-scm.com](https://git-scm.com/) install with default options. Verify: `git --version`. Introduce yourself to Git once (recorded into every commit): `git config --global user.name "Your Name"` and `git config --global user.email "you@example.com"`.
4. **GitHub.** Account at [github.com](https://github.com/); create the empty repository (no auto-generated README — the first push must not collide with anything). **Already done for this repo** — remote connected, initial commit pushed.
5. **IDE.** IntelliJ IDEA **Community Edition** from [jetbrains.com/idea](https://www.jetbrains.com/idea/download/). Open the `jvm-explorer` folder. In *File → Project Structure → SDK*, confirm the SDK is the Temurin 21 you installed.

## SECTION 4 — THE WRONG WAY

**WRONG APPROACH — no pinning, no ignore file, no environment check.** No code compiles wrongly here; the wrongness is in the *files that are missing or vague*. Three real mistakes:

**Mistake 1: a POM that names no versions.**

```xml
<!-- WRONG: compiles today, drifts tomorrow -->
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>          <!-- POM format version: fine -->
    <groupId>com.corejava</groupId>             <!-- coordinates: fine -->
    <artifactId>jvm-explorer</artifactId>       <!-- coordinates: fine -->
    <version>1.0-SNAPSHOT</version>             <!-- coordinates: fine -->
    <!-- WRONG: no <maven.compiler.release> - so which Java level compiles this
         project is whatever each machine's Maven decides. Priya's machine says 21,
         Sam's says 17, and code using a 21-only feature builds for her, not him. -->
    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>RELEASE</version>          <!-- WRONG: "RELEASE" means "whatever
                 is newest at the moment you happen to build" - so two builds of the
                 SAME code can use DIFFERENT JUnit versions and disagree. -->
        </dependency>
    </dependencies>
    <!-- WRONG: no pinned plugin versions - each Maven install picks its own,
         so the compiler and test runner themselves differ across machines. -->
</project>
```

**Mistake 2: committing before writing `.gitignore`.** Run `git add .` first and `target/` (thousands of generated files) plus private course-control files enter history permanently; adding `.gitignore` afterwards does *not* pull them back out.

**Mistake 3: never verifying which Java runs.** Install JDK 21, but an old JDK 8 sits earlier on the PATH. Every command "works" — and every observation this course makes is made on the wrong engine.

```text
PREDICTED SYMPTOMS (of the unpinned setup, appearing over following weeks):
- "invalid target release" / "release version not supported" on machine A only
- a JUnit upgrade nobody asked for changes test behavior between two builds
- git history bloated with target/ noise; private files exposed on GitHub
- and the quietest one: JVM behavior in later lessons that does not match the
  lesson text, because the engine underneath is not the engine we agreed on
```

## SECTION 5 — THE RIGHT WAY

**CORRECT APPROACH:** one POM that pins everything (saved at `jvm-explorer/pom.xml`, every line why-commented — read it now; it is Section 3.3 made real), a `.gitignore` written before the first commit (done in your repo), and a first program plus tests that make the machine *prove* its identity. The program (`JvmExplorer.java`, shown fully in Section 6) asks the running JVM for its version, its engine name, its vendor, its heap ceiling, and its CPU count. The tests (`JvmExplorerTest.java`) then *assert* the pinned agreement — most importantly `runsOnPinnedJavaVersion`, which fails the whole build with a fix-it message if the JVM executing the build is not Java 21.

What happens, step by step, when you run `mvn test` the first time:

1. Maven starts (on the Java that JAVA_HOME names), reads `pom.xml`, and learns coordinates, the Java 21 pin, the JUnit 5.13.4 dependency, the three pinned plugins.
2. It checks the local cache at `C:\Users\<you>\.m2\repository` for each needed item; anything missing is downloaded from Maven Central. First run: a minute or more of `Downloading...` lines — normal, once.
3. The compiler plugin calls `javac` on `JvmExplorer.java` with release 21; the class file lands in `target/classes/`.
4. The compiler plugin compiles the test source (JUnit now on the class path) into `target/test-classes/`.
5. The surefire plugin scans for test classes, finds `JvmExplorerTest`, and runs each `@Test` method on a fresh instance.
6. `runsOnPinnedJavaVersion` asks the running JVM its major version and asserts 21; the two sanity tests assert a positive heap ceiling and ≥1 CPU core.
7. Surefire prints the tally; Maven prints `BUILD SUCCESS` — or `BUILD FAILURE` naming exactly which assertion broke and why.

```text
PREDICTED OUTPUT of `mvn test` (trimmed; first run also shows many Downloading... lines):

[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.corejava.jvm.JvmExplorerTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.0xx s
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

Run `mvn test` from inside `jvm-explorer/` and check the result against the prediction above. If they differ, the machine is right — `troubleshooting.md` covers the likely failures, and the mismatch itself is the lesson.

## SECTION 6 — APPLIED TO JVM EXPLORER

**Files this lesson adds** (all saved to disk):

- `jvm-explorer/pom.xml` — the pinned build recipe (walked through in 3.3 and 5).
- `jvm-explorer/src/main/java/com/corejava/jvm/JvmExplorer.java` — the entry point.
- `jvm-explorer/src/test/java/com/corejava/jvm/JvmExplorerTest.java` — the environment tests.

The entry point, every line carrying its why:

```java
// Every Java file starts by declaring which package it belongs to. A package is a
// named folder for classes; ours mirrors the Maven groupId plus the project purpose,
// and Maven REQUIRES the folder path on disk to match this line exactly.
package com.corejava.jvm;

// The front door of jvm-explorer. Lesson by lesson this program will grow modules
// that provoke the JVM (fill memory, trigger garbage collection, overflow the stack).
// For Lesson 00 its only job is to prove the toolchain works end to end by asking
// the running JVM to describe itself.
// "final" because this class is a standalone entry point - nothing should ever
// extend it, and saying so makes that intent impossible to violate by accident.
public final class JvmExplorer {

    // The method the JVM looks for, by this exact signature, when told to run this
    // class. No object is created first, which is why it must be static; it must be
    // public so the JVM (which lives outside our package) is allowed to call it.
    public static void main(String[] args) {

        // A banner line so that in later lessons, when Maven's own output surrounds
        // ours, the learner can instantly spot where OUR program's output begins.
        System.out.println("=== jvm-explorer: hello from inside the JVM ===");

        // The JVM keeps a table of "system properties" - facts about itself and the
        // machine, stored as name/value text pairs. "java.version" is the exact
        // version of the Java runtime executing RIGHT NOW - which can differ from
        // what is installed elsewhere on the machine, so we ask the JVM, not the OS.
        System.out.println("Java version : " + System.getProperty("java.version"));

        // "java.vm.name" names the JVM implementation (expected: HotSpot, the JVM
        // inside every standard JDK). Printed because this whole course is about
        // HotSpot's internals - we should know we are actually standing on it.
        System.out.println("JVM name     : " + System.getProperty("java.vm.name"));

        // "java.vm.vendor" names who built this JVM (Eclipse Adoptium, Oracle, ...).
        // Different vendors ship the same core HotSpot; seeing the vendor teaches
        // that "a JDK" is one of many interchangeable builds of shared source.
        System.out.println("JVM vendor   : " + System.getProperty("java.vm.vendor"));

        // Runtime is the JVM's self-description object; getRuntime() hands us the
        // single instance representing THIS running JVM. maxMemory() answers: "how
        // big is the heap - the memory pool for objects - allowed to grow?" in bytes.
        // We store it in a long because byte counts overflow an int past ~2 GB.
        long maxHeapBytes = Runtime.getRuntime().maxMemory();

        // Divide bytes down to megabytes (1024 bytes = 1 KB, 1024 KB = 1 MB) purely
        // for human readability; a number like 4096 MB means more at a glance than
        // 4294967296. Lesson 03 explores the heap this number describes.
        System.out.println("Max heap     : " + (maxHeapBytes / (1024 * 1024)) + " MB");

        // How many CPU cores the JVM believes it may use. Printed now because the
        // JVM sizes its garbage-collection threads from this number - a fact that
        // becomes important when we study GC behavior in Lessons 04 and 05.
        System.out.println("CPU cores    : " + Runtime.getRuntime().availableProcessors());
    }
}
```

One term in there needs its proper introduction, briefly: the JVM sets aside one big pool of memory where every object your program creates with `new` is stored; that pool is called the **heap**. The number we print is its allowed maximum size — by default the JVM picks it from your machine's RAM (commonly around one quarter of it — verify on your machine; that default varies by version and setup). The heap is the main stage of Lessons 03–05; today we only read its size.

**What this adds:** jvm-explorer now exists, builds, runs, and tests — and its first capability is self-inspection: it makes the invisible engine introduce itself (version, engine name, vendor, heap ceiling, CPU count), and its tests turn the course's version agreement into an enforced fact rather than a hope.

**Numbered trace of a run** (`mvn compile exec:java` from `jvm-explorer/`):

1. Maven reads `pom.xml`, compiles anything out of date into `target/classes/`.
2. The exec plugin reads its configured main class, `com.corejava.jvm.JvmExplorer`.
3. Inside the already-running Maven JVM, `JvmExplorer.main` is invoked.
4. The banner prints; then five questions are put to the running JVM — two to the system-properties table (version, engine name, vendor is a third) and two to the `Runtime` object (heap ceiling, CPU count).
5. Each answer prints as one labelled line; `main` returns; Maven reports success.

```text
PREDICTED OUTPUT of `mvn compile exec:java` (Maven's [INFO] lines trimmed;
your exact numbers WILL differ - version patch level, heap size, and core count
are facts about YOUR machine, which is precisely the point):

=== jvm-explorer: hello from inside the JVM ===
Java version : 21.0.7
JVM name     : OpenJDK 64-Bit Server VM
JVM vendor   : Eclipse Adoptium
Max heap     : 4096 MB
CPU cores    : 8
[INFO] BUILD SUCCESS
```

Run it and compare — the two lines most worth checking: *Java version* starts with `21`, and *JVM vendor* names the vendor you actually installed. **Project state: compiles, 3 tests green (predicted — confirm with `mvn test`).**

## SECTION 7 — IN THE WILD

The exact self-inspection APIs we used are what real infrastructure uses to adapt to the machine it lands on. The clearest sighting: `Runtime.getRuntime().availableProcessors()` — our last line — is how the JDK's own concurrency machinery sizes itself. `ForkJoinPool.commonPool()`, the thread pool that silently powers parallel streams (`list.parallelStream()...`), calls it to decide how many worker threads to create; that is why the same jar uses 4 workers on your laptop and 32 on a server, with no configuration anywhere. The JVM itself does the same when choosing how many GC threads to run — we will see those threads with our own eyes in the GC log in Lesson 05. And build tools report their world the same way: look at the top of your own `mvn -version` output — Java version, vendor, OS — that is Maven doing exactly what `JvmExplorer.main` does, for exactly the same reason: when something goes wrong, the first question is always *"what machine, what engine, what version?"*

## SECTION 8 — GOTCHAS AND COMMON MISTAKES

- **Verifying in a stale terminal.** Installers edit PATH/JAVA_HOME, but an already-open terminal keeps its old copies, so `java -version` seems broken (or seems fine when it is not). People assume the install failed and reinstall. Always open a **fresh** terminal after installing; in doubt, restart the IDE too.
- **`java -version` right, `mvn -version` wrong.** The two can disagree: `java` is found via PATH, but Maven picks its JVM via JAVA_HOME. If JAVA_HOME points at an old JDK, Maven builds with it regardless of PATH. Read the `Java version:` line inside `mvn -version` output — it must say 21.
- **Running `mvn` from the wrong folder.** Maven needs `pom.xml` in the *current* folder; from the repo root you get `there is no POM in this directory`. All `mvn` commands in this course run from inside `jvm-explorer/`. People hit this because the repo root *looks* like the project.
- **JDK 8 muscle memory / old tutorials.** Half the internet's Java setup advice predates modern versions — separate JRE downloads, `CLASSPATH` variables, ancient POM snippets that misbehave with JUnit 5 (the classic symptom: `Tests run: 0` with old surefire — silent skipping, not failure). Trust the pinned setup here; treat pre-Java-11 tutorials as history.
- **Trusting the IDE's green checkmark over Maven's.** IntelliJ may run tests with its own bundled JDK and its own runner, so IDE-green and `mvn test`-green can differ. The command line is the referee; if they disagree, believe `mvn test` and fix the IDE's SDK setting.

## SECTION 9 — TRADEOFFS AND WHEN NOT TO USE THIS

Honesty about the cost: this ceremony is real overhead. For a 30-line throwaway experiment, Maven + Git + tests is a sledgehammer for a thumbtack — `javac One.java && java One` (or since Java 11, just `java One.java`, which compiles in memory) is the right tool, and *inside this course* such throwaways go into a labelled `experiments/` package rather than getting projects of their own. Pinning has a cost too: it is a promise to *manage* versions, not a way to stop thinking about them — real teams schedule upgrades deliberately (new patch releases carry security fixes; staying pinned forever quietly becomes staying vulnerable), and we will bump our pins deliberately if the series ever needs to. Maven's fixed conventions trade flexibility for uniformity — for odd layouts and exotic builds, its rigidity fights you, which is roughly why Gradle exists. And every-line comments plus environment tests are teaching devices dialed to maximum for this course; production code comments the *why that is not obvious*, not every line. What does **not** scale down, ever, for any project another human (including future-you) will touch: `.gitignore` before first commit, and versions written down rather than remembered.

## SECTION 10 — KEY TAKEAWAYS

- The JVM is a program that acts out an imaginary standard machine, which is why compiled Java runs unchanged on any real machine that has a JVM; the JDK is that engine plus the developer tools, and it is what you install.
- This series pins Java 21 (LTS), Maven 3.9.x, and JUnit 5.13.4 in committed files, because unpinned versions drift silently between machines and over time, and drifting versions poison every observation a JVM course makes.
- Maven turns one committed description (`pom.xml`) into an identical build anywhere: it fetches declared dependencies by their three coordinates from Maven Central, pins plugins, and enforces one folder layout with all generated output confined to the never-committed `target/`.
- `mvn test` runs the project's JUnit tests and is the standing one-command answer to "did I break anything?" — and jvm-explorer's very first tests guard the environment itself, failing loudly if the running JVM is not the pinned Java 21.
- `.gitignore` must exist before the first `git add .`, because Git ignores files only until they are committed — after that they are in history for good; each lesson ends with one commit pushed to GitHub.

## SECTION 11 — CHALLENGE WITH HIDDEN ANSWER

Using only this lesson: your friend clones the repo, runs `mvn test` inside `jvm-explorer/`, and gets `BUILD FAILURE`. The failing test is `runsOnPinnedJavaVersion` with `Expected Java 21 ... but was: 17`. Meanwhile `java -version` in their terminal prints `openjdk version "21.0.7"`. (1) Explain precisely how *both* outputs can be true at once. (2) Name the one command from this lesson that reveals which Java Maven is really using. (3) Say what to fix. Bonus: why is it *good* that this failure happened on day one?

<details>
<summary>Click to reveal the answer</summary>

**(1) How both can be true:** two different lookup mechanisms choose the JVM in the two cases. The terminal finds `java` by walking the **PATH** folder list — and on this machine that walk reaches Java 21 first. Maven ignores that walk and launches the JVM named by **JAVA_HOME** — which on this machine points at an installed JDK 17. So the terminal's Java really is 21 *and* the Java running the tests really is 17; the two facts describe two different JVMs on the same box. The test asserted the *actual running* JVM's major version (`Runtime.version().feature()`), which is exactly why it caught what `java -version` could not.

**(2) The revealing command:** `mvn -version` — its output includes a `Java version:` line stating which JVM Maven itself is running on (and the `runtime:` path shows where it lives, i.e. what JAVA_HOME resolved to).

**(3) The fix:** point JAVA_HOME at the JDK 21 install folder (Windows: *Edit environment variables for your account* → set `JAVA_HOME` to e.g. `C:\Program Files\Eclipse Adoptium\jdk-21...`), open a **fresh** terminal (a stale one keeps the old value — Gotcha #1), confirm `mvn -version` now says `Java version: 21`, then re-run `mvn test`.

```text
PREDICTED OUTPUT after the fix (trimmed):
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Bonus:** because the mismatch surfaced as one loud, named, message-bearing test failure on day one — instead of surfacing weeks later as JVM behavior that quietly contradicts the lesson text with no error at all. That trade — invisible drift exchanged for an immediate labelled failure — is the entire reason `runsOnPinnedJavaVersion` exists, and the entire point of pinning.

</details>

## SECTION 12 — ALMANAC UPDATE

Added to `almanac.md` this lesson: the pinned-version table for the series; *JDK = build + run, JRE = run only, JVM = the engine*; `.gitignore` before first `git add .` (post-commit ignoring does not un-commit); PATH picks your terminal's `java`, JAVA_HOME picks Maven's (check `mvn -version`, not just `java -version`, and always in a fresh terminal); `mvn test` as the standing regression check; never commit `target/`; the machine's real output always beats my prediction.

## SECTION 13 — WHAT IS NEXT

The toolchain now proves its own identity — so we can finally ask the first real question of the course: what *is* the `.class` file that `javac` produced in `target/classes/`, and what do its instructions look like? Lesson 01 follows one tiny method from source code to bytecode, disassembles it with `javap -c`, and reads the JVM's own instruction language line by line.

---

*Lesson 00 of 07 — Core Java Course 01: JVM Internals & Memory Management*
