# Glossary — Core Java Course 01: JVM Internals & Memory Management

Every term this course introduces, in plain language, with the lesson where it first appeared. This is an index for quick lookup — the full explanation always lives inline in the lesson itself.

| Term | Plain-language meaning | First appears |
| --- | --- | --- |
| Virtual machine | A program that pretends to be a computer: it reads instructions meant for an imaginary machine and carries them out on the real machine underneath. | Lesson 00 |
| JVM (Java Virtual Machine) | The virtual machine that runs compiled Java. It executes your code, owns the memory your objects live in, collects garbage, and speeds up hot code. The main character of this course. | Lesson 00 |
| JRE (Java Runtime Environment) | The JVM plus the standard libraries — enough to *run* Java programs but not to compile them. No longer shipped separately, but the name survives in old docs and error messages. | Lesson 00 |
| JDK (Java Development Kit) | Everything in the JRE plus developer tools like the compiler (`javac`) and diagnostic tools. What a developer installs. JDK = build + run; JRE = run only; JVM = the engine inside both. | Lesson 00 |
| `javac` | The Java compiler: the JDK tool that turns `.java` source files into `.class` files the JVM can execute. Dissected in Lesson 01. | Lesson 00 |
| OpenJDK | The open-source project holding Java's shared source code, from which every vendor builds their JDK. | Lesson 00 |
| Temurin | Eclipse Adoptium's free build of OpenJDK — the JDK this course installs. | Lesson 00 |
| HotSpot | The name of the JVM implementation inside standard JDK builds. When this course says "the JVM does X," it means HotSpot. | Lesson 00 |
| LTS (Long-Term Support) | A release designated to receive fixes and security patches for years instead of months. Java 21 is LTS, which is why the series pins it. | Lesson 00 |
| Pinning | Deciding a version once, writing it into committed files, and having the build check it — so version drift can never silently change behavior. | Lesson 00 |
| Build tool | A program that turns a written project description into a finished build: fetching libraries, compiling in order, running tests, packaging. | Lesson 00 |
| Maven | The standard Java build tool. Reads `pom.xml` and derives the whole build from it. | Lesson 00 |
| POM / `pom.xml` | Project Object Model — the single file at a Maven project's root describing what the project *is* (name, Java version, libraries, plugins). | Lesson 00 |
| Dependency | A library your code uses instead of rewriting — declared by name in the POM; Maven fetches it and everything it needs. | Lesson 00 |
| Coordinates (groupId, artifactId, version) | The three-part address uniquely naming any library or project: who made it, which product, which edition. | Lesson 00 |
| Maven Central | The public online warehouse of published Java libraries that Maven downloads dependencies from (cached locally in `~/.m2/repository`). | Lesson 00 |
| Plugin (Maven) | A bolt-on component that does one job of the build (compiling, running tests, running a main class). Maven is just the coordinator; plugins do the work. | Lesson 00 |
| SNAPSHOT | Version suffix meaning "still in development, not a frozen release." | Lesson 00 |
| `target/` | The folder where Maven writes everything it generates. Disposable, never edited, never committed; `mvn clean` deletes it. | Lesson 00 |
| Unit test | A small piece of code that automatically checks a piece of real code: call it, assert what must be true, get a pass/fail verdict. | Lesson 00 |
| JUnit | The standard Java framework for writing and running unit tests. This course pins the JUnit 5 line, version 5.13.4. | Lesson 00 |
| Assertion | One "this must be true" statement inside a test, e.g. `assertEquals(21, version)` — silent when true, loud failure when false. | Lesson 00 |
| Version control system | A program that records snapshots of your files over time so you can see what changed, when, why — and return to any earlier state. | Lesson 00 |
| Git | The standard version control system. The hidden `.git/` folder holds every recorded snapshot. | Lesson 00 |
| Repository (repo) | A folder whose history Git is tracking. | Lesson 00 |
| Commit | One saved snapshot with a message. Two steps: `git add` chooses the changes, `git commit -m "..."` seals them into history. | Lesson 00 |
| GitHub | A website hosting online copies of Git repositories — backup plus sharing. | Lesson 00 |
| Remote | The online copy of your repo that `git push` uploads new commits to. | Lesson 00 |
| `.gitignore` | A file listing names Git must pretend not to see. Must exist *before* the first `git add .` — ignoring after committing does not remove things from history. | Lesson 00 |
| IDE (Integrated Development Environment) | One workbench bundling editor, live error-checking, navigation, and test running — e.g. IntelliJ IDEA Community Edition. | Lesson 00 |
| PATH | The operating system's ordered list of folders searched when you type a bare command like `java`. List order decides which of several installed Javas wins. | Lesson 00 |
| Environment variable | A named value the operating system keeps for programs to read, e.g. PATH or JAVA_HOME. | Lesson 00 |
| JAVA_HOME | The conventional environment variable naming your JDK's install folder. Maven uses it to pick the Java it runs with — which is why it can disagree with `java -version`. | Lesson 00 |
| System property | One entry in the name/value table the JVM keeps about itself and the machine — read with `System.getProperty(...)`, e.g. `java.version`. | Lesson 00 |
| Heap | The JVM's big memory pool where every object created with `new` lives. Introduced by name only; explored fully in Lesson 03. | Lesson 00 |
