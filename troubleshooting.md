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
**Fix:** confirm you unzipped Maven somewhere permanent and added its `bin` subfolder (e.g. `C:\tools\apache-maven-3.9.16\bin`) to PATH. Fresh terminal, then `mvn -version`.

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
**Fix:** you almost certainly have a JRE-only or partial installation on the PATH ahead of the real JDK. Check `javac -version` too — if that also fails, reinstall Temurin 21 with "Add to PATH" enabled. Otherwise call it by full path once to confirm it exists: `"%JAVA_HOME%\bin\javap" -version`.

### `javap` prints `Error: class not found` for a class you can see on disk

**What it means:** `javap` was given a class *name* but does not know where to look, or was given a path it cannot resolve.
**Fix:** either point at the file directly — `javap -c -p target/classes/com/corejava/jvm/BytecodeSubject.class` — or give it the class path and the full package name: `javap -c -p -cp target/classes com.corejava.jvm.BytecodeSubject`. Mixing the two (a file path *and* a package name) does not work.

### `javap` says the file does not exist, right after editing the source

**What it means:** `javap` reads compiled files, and `target/classes/` still holds the previous build (or nothing at all).
**Fix:** run `mvn compile` first, every time. If the file is still missing, check that the folders under `target/classes/` match the `package` line.

### `javap` output has no `Code:` blocks

**What it means:** the `-c` flag was omitted. Plain `javap` prints signatures only.
**Fix:** `javap -c`. And add `-p`, or private members (including the private constructor) will be missing entirely, which looks exactly like the compiler having deleted them.

### `type` / `cat` on a `.class` file prints garbage and messes up the terminal

**Not an error.** A class file is binary; a text viewer guesses each byte is a character and prints the guess, and some byte values are terminal control codes. Nothing is damaged. Use `javap` to read the file, and run `cls` to tidy the terminal.

### `UnsupportedClassVersionError: ... class file version 65.0 ... only recognizes 61.0`

**What it means:** the class file was compiled for a newer Java than the JVM trying to run it. Subtract 44 from each number: 65 = Java 21, 61 = Java 17.
**Fix:** run it on a JVM of that release or newer (usually correct), or rebuild the project with `<maven.compiler.release>` set to the older release — and then actually test on that release. Never use `-source`/`-target` alone to do this; see the almanac.

### `mvn test` fails: `classFilesAreCompiledToThePinnedJavaVersion` — expected 65

**What it means:** the build produced class files for a Java release other than 21.
**Fix:** check `<maven.compiler.release>21</maven.compiler.release>` is still in `jvm-explorer/pom.xml`, then `mvn clean test` so no stale class files from an earlier setting survive in `target/`.

### `exec:java` runs `JvmExplorer` when you wanted the warm-up experiment

**What it means:** the POM sets a default main class, so a bare `mvn exec:java` always runs that one.
**Fix:** name the class explicitly: `mvn compile exec:java -Dexec.mainClass=com.corejava.jvm.experiments.WarmupExperiment`. In PowerShell, quote it if the shell objects: `mvn compile exec:java "-Dexec.mainClass=com.corejava.jvm.experiments.WarmupExperiment"`.

### The warm-up experiment's timings do not go down, or one batch spikes

**Not an error.** Timings depend on the machine, the JDK build, and whatever else the operating system is doing. On a fast machine the method may be compiled by batch 2, so the drop is over before you see it; a background process can make any batch slow. Run it a few times — the *shape* is the observation, and no test asserts on these numbers precisely because they are not dependable.
