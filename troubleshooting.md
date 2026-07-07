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
