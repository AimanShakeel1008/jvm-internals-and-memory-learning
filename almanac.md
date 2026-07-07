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
