# Troubleshooting — Core Java Course 01

Decodes the errors this course's setup and lessons are likely to produce, with the fix for each. Errors are grouped by the lesson that first makes them possible.

---

## Lesson 00 — setup errors

### `'java' is not recognized as an internal or external command`

**What it means:** the terminal searched every folder on its PATH and found no program named `java`.
**Fix:** either the JDK is not installed, or its folder is not on the PATH. Reinstall Temurin 21 with "Add to PATH" checked — then open a **fresh** terminal (an already-open terminal keeps the old PATH and will keep failing even after a correct install).

### `java -version` prints the wrong version (e.g. 1.8 or 17 instead of 21)

**What it means:** more than one Java is installed, and an older one sits earlier on the PATH, so it wins.
**Fix:** *Edit environment variables for your account* → open `Path` → move the Temurin 21 entry above any other Java entries (or remove old ones). Fresh terminal, verify again. Note: some machines have `C:\Program Files\Common Files\Oracle\Java\javapath` early on PATH — that entry belongs to an old Oracle install and may need removing.

### `'mvn' is not recognized as an internal or external command`

**What it means:** same as the `java` version — no folder on PATH contains `mvn`.
**Fix:** confirm you unzipped Maven somewhere permanent and added its `bin` subfolder (e.g. `C:\tools\apache-maven-3.9.x\bin`) to PATH. Fresh terminal, then `mvn -version`.

### `mvn -version` says `JAVA_HOME not found` / `JAVA_HOME is set to an invalid directory`

**What it means:** Maven picks its Java via the JAVA_HOME environment variable, and it is missing or pointing at a folder that is not a JDK.
**Fix:** set `JAVA_HOME` to the JDK install folder itself, e.g. `C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot` (the folder *containing* `bin`, not `bin` itself). Fresh terminal; `mvn -version` must then print `Java version: 21`.

### `mvn -version` works but shows `Java version: 17` (or anything not 21)

**What it means:** JAVA_HOME points at a different JDK than the one your PATH finds. `java -version` and Maven genuinely run two different JVMs.
**Fix:** repoint JAVA_HOME at the Temurin 21 folder. This exact situation is the Lesson 00 challenge — and the `runsOnPinnedJavaVersion` test exists to catch it.

### `mvn test` fails: `runsOnPinnedJavaVersion` — `Expected Java 21 ... but was: <other>`

**What it means:** the build is executing on a non-21 JVM. The test did its job.
**Fix:** see the two entries above — this is always a PATH or JAVA_HOME issue. `mvn -version` tells you which Java Maven is really using.

### `mvn test` from the repo root: `there is no POM in this directory`

**What it means:** Maven only works in a folder containing `pom.xml`; the repo root does not have one.
**Fix:** `cd jvm-explorer` first. Every `mvn` command in this course runs from inside `jvm-explorer/`.

### First `mvn test` takes minutes and prints endless `Downloading from central...`

**Not an error.** Maven is fetching JUnit, the pinned plugins, and everything they need from Maven Central into your local cache (`C:\Users\<you>\.m2\repository`). It happens once; later builds are fast and quiet.

### Build fails with `Could not resolve dependencies` / `Could not transfer artifact`

**What it means:** a download from Maven Central failed — usually no network, a proxy/VPN in the way, or a half-written file in the local cache after an interrupted download.
**Fix:** check the network, retry. If it keeps failing on the same artifact, delete that artifact's folder under `C:\Users\<you>\.m2\repository` and run `mvn test` again so Maven re-downloads it cleanly.

### `Tests run: 0` — build "succeeds" but no tests ran

**What it means:** the test runner silently found nothing — classically an old surefire plugin that cannot see JUnit 5 tests, or test classes in the wrong folder.
**Fix:** confirm `pom.xml` still pins `maven-surefire-plugin` 3.5.2 and that tests live under `src/test/java/...` with the package folders matching the `package` line exactly.

### `mvn test` reports more tests than expected after the course restart

**What it means:** compiled files from before the restart are still sitting in `target/`, or the older lesson source files were not deleted.
**Fix:** make sure the cleanup commands from the restarted Lesson 00 were run, then use `mvn clean test` (not plain `mvn test`) so `target/` is rebuilt from scratch.

### IntelliJ shows red errors but `mvn test` is green (or the reverse)

**What it means:** the IDE compiles with its own selected SDK and runs tests with its own runner, so it can disagree with Maven.
**Fix:** the command line is the referee — trust `mvn test`. Then fix IntelliJ: *File → Project Structure → Project → SDK* → select the Temurin 21 JDK; if things stay strange, right-click `pom.xml` → *Maven → Reload Project*.

### `git push` rejected or asks for a password that never works

**What it means:** GitHub no longer accepts account passwords from the command line; it needs a credential helper or token.
**Fix:** the Windows Git installer includes Git Credential Manager — the first `git push` should open a browser window to log in. If no window appears, run `git config --global credential.helper manager` and push again.

---

## Lesson 01 — bytecode and class file errors

### `'javap' is not recognized as an internal or external command`

**What it means:** the terminal cannot find `javap`, which ships inside the JDK next to `java` and `javac`.
**Fix:** a runtime-only or partial installation is probably ahead of the real JDK on PATH. Check `javac -version` too — if that also fails, reinstall Temurin 21 with "Add to PATH" enabled. To confirm it exists, call it by full path once: `"%JAVA_HOME%\bin\javap" -version`.

### `javap` prints `Error: class not found` for a class you can see on disk

**What it means:** `javap` was given a class *name* but not where to look, or a path it cannot resolve.
**Fix:** either point at the file — `javap -c -p target/classes/com/corejava/jvm/BytecodeSubject.class` — or give the class path plus the full name: `javap -c -p -cp target/classes com.corejava.jvm.BytecodeSubject`. Do not mix a file path with a package name.

### `javap` says the file does not exist, right after editing the source

**What it means:** `javap` reads compiled files, and `target/classes/` still holds the previous build (or nothing).
**Fix:** run `mvn compile` first, every time.

### `javap` output has no `Code:` blocks, or a method seems to be missing

**What it means:** `-c` was left out (plain `javap` prints signatures only), or `-p` was left out (private members are hidden).
**Fix:** use `javap -c -p`.

### `type` / `cat` on a `.class` file prints garbage and messes up the terminal

**Not an error.** A class file is binary; a text viewer guesses each byte is a character. Nothing is damaged. Use `javap`, and run `cls` to tidy the terminal.

### `UnsupportedClassVersionError: ... class file version 65.0 ... only recognizes 61.0`

**What it means:** the class file was compiled for a newer Java than the JVM trying to run it. Subtract 44 from each number: 65 = Java 21, 61 = Java 17.
**Fix:** run it on a JVM of that release or newer (usually right), or rebuild with `<maven.compiler.release>` set to the older release and then actually test on it. Never use `-source`/`-target` alone.

### `mvn test` fails: `classFilesAreCompiledToThePinnedJavaVersion` — expected 65

**What it means:** the build produced class files for a Java release other than 21.
**Fix:** check `<maven.compiler.release>21</maven.compiler.release>` is still in `jvm-explorer/pom.xml`, then `mvn clean test` so no stale class files survive.

### `exec:java` runs `JvmExplorer` when you wanted the warm-up experiment

**What it means:** the POM sets a default main class, so a bare `mvn exec:java` always runs that one.
**Fix:** name the class: `mvn compile exec:java -Dexec.mainClass=com.corejava.jvm.experiments.WarmupExperiment`. If PowerShell objects, quote it: `mvn compile exec:java "-Dexec.mainClass=com.corejava.jvm.experiments.WarmupExperiment"`.

### The warm-up experiment's timings do not go down, or one batch spikes

**Not an error.** Timings depend on the machine, the JDK build, and whatever else is running. On a fast machine the drop may be over by batch 2; a background process can slow any batch. Run it a few times — the *shape* is the observation, which is why no test asserts on these numbers.

---

## Lesson 02 — class loading errors

### `NullPointerException` on a line calling `getClassLoader()`

**What it means:** `SomeJdkClass.class.getClassLoader()` returns `null` for anything the bootstrap loader defined (`String`, `Object`, everything in `java.base`). That `null` means "bootstrap loader" — C++ code with no Java object.
**Fix:** check for null and treat it as "bootstrap", the way `ClassLoaderReporter.describe` does. For resources, prefer `YourClass.class.getResourceAsStream("/path")`.

### `ClassNotFoundException: com.example.Something`

**What it means:** something asked for that class **by name, as text** (`Class.forName`, `loadClass`, a framework reading a config file) and the whole delegation chain found nothing.
**Fix:** check the name character by character first, then confirm the jar is on the class path you are *running* with, not just the one you compiled with.

### `NoClassDefFoundError: com/example/Something`

**What it means:** your *compiled code* refers to that class, so it existed when you compiled; at run time it was gone. (The slashes are the internal form of the name.)
**Fix:** a packaging problem, not a name problem. Compare the compile class path with the runtime class path: a `provided` dependency, a jar not copied into the image.

### `NoClassDefFoundError: Could not initialize class com.example.Settings`

**What it means:** NOT that the class is missing. It is present, but its static initializer threw earlier, so the JVM marked it permanently unusable.
**Fix:** search **backwards** in the log for the first `ExceptionInInitializerError` naming that class — its `Caused by:` is the real problem.

### `ExceptionInInitializerError`

**What it means:** a `static` block or static field initializer threw. This is the *first* failure, and the useful one.
**Fix:** read the `Caused by:`. Longer term, move anything that can fail out of the static initializer into a method the caller invokes on purpose.

### `ClassCastException: com.acme.Config cannot be cast to com.acme.Config`

**What it means:** two different class loaders each defined a class with that name; a class's identity is its name *plus* its loader. The two loaders are named in brackets at the end of the message.
**Fix:** find why the class is available in two places (usually a shared API jar bundled inside a plugin *and* on the parent class path) and remove the duplicate.

### `-verbose:class` prints thousands of lines and drowns my program's output

**Not an error** — the JVM really loads that many classes. Filter it: `... | findstr YourClassName` on Windows, `... | grep YourClassName` elsewhere. Or write it to a file: `java -Xlog:class+load=info:file=classes.txt -cp target/classes com.corejava.jvm.JvmExplorer`.

### `-verbose:class` with `mvn exec:java` shows Maven's classes, not mine

**What it means:** `exec:java` runs your `main` inside Maven's own JVM.
**Fix:** `mvn compile`, then run a clean JVM directly: `java -verbose:class -cp target/classes com.corejava.jvm.experiments.ClassLoadingExperiment`.

### `Error: Could not find or load main class` from a plain `java` command

**What it means:** the class path you gave `java` does not contain the class, or you gave a file path where a class name belongs.
**Fix:** run from inside `jvm-explorer/` after `mvn compile`, with `-cp target/classes` and the **fully-qualified class name** (dots, no `.class`).

### `mvn test` fails a lazy-initialization test with "the log must be silent"

**What it means:** one of the demonstration subjects (`ConstantHolder`, `LazyHolder`, `ForNameSubject`, `InitOrderParent`/`InitOrderChild`) was initialized by something other than its own test. A class initializes once per JVM and cannot be reset.
**Fix:** find what else touched it. Each subject must be referenced by exactly one test method and nothing else — the rule is in `LazyInitializationTest`'s header comment.

### A stray `>>> LateComer's static initializer is running NOW` line in the test output

**Not an error.** That is the experiment's nested class printing from inside its own `<clinit>` during the test that calls its static method.
