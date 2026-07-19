# Core Java Course 01 — JVM Internals & Memory Management

Course 01 of 19 in the Core Java In-Depth series. Each course is a standalone repository teaching one deep Java topic through one dedicated project. This one covers what actually happens when Java runs: how source becomes bytecode, how classes load, where objects live, how garbage collection works, and how the JIT compiler makes code fast — in plain Java, deliberately without frameworks.

## The lessons

The course text is a self-contained HTML learning system — open **[`lessons/index.html`](lessons/index.html)** in any browser (works fully offline, by double-click) to reach the course hub, a "you are here" roadmap, and every lesson. Each lesson is a standalone page sharing one stylesheet, with inline SVG diagrams, occasional interactive simulations, hover-to-define glossary terms, syntax-highlighted code, and a print stylesheet for clean PDF notes. Best viewed in either light or dark mode — the pages adapt.

## The project: jvm-explorer

`jvm-explorer/` is a Maven project built to provoke and observe the JVM itself. Lesson by lesson it gains modules that fill heap regions on purpose, trigger and parse GC logs, watch classes load, overflow the stack on demand, and expose JIT behavior.

**Current capabilities (after Lesson 00):**

- Self-inspection: prints the running JVM's Java version, JVM name (HotSpot), vendor, maximum heap size, and CPU core count.
- Environment tests: the build fails loudly if the running JVM is not the pinned Java 21.

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
```

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
