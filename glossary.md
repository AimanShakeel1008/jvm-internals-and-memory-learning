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
| CPU | The chip that actually carries out instructions. It understands exactly one numeric command language and nothing else. | Lesson 01 |
| Instruction set | The specific set of numbered commands one family of chips understands. Intel/AMD (x86-64) and ARM have different, incompatible ones. | Lesson 01 |
| Machine code | The numeric command language a real CPU executes directly. Not portable: code for one instruction set is meaningless to another. | Lesson 01 |
| Native code | Machine code for the actual chip you are running on. "Native" always means *not translated, not simulated — the real thing*. | Lesson 01 |
| Bytecode | The instructions `javac` produces: commands for an imaginary machine, not for any real chip. Called *byte*code because each operation code fits in one byte. | Lesson 01 |
| Opcode | The one-byte number naming which operation an instruction performs. One byte allows at most 256 operations; the JVM defines a little over 200. | Lesson 01 |
| Operand (bytecode) | Extra bytes following an opcode that supply details — which local slot to read, where to jump to. | Lesson 01 |
| Mnemonic | The short human-readable name of an opcode (`iadd`, `iload_0`, `goto`). The file stores the number; tools print the mnemonic. | Lesson 01 |
| WORA ("write once, run anywhere") | The claim that a *compiled* Java file runs unchanged on any machine with a JVM. It is about the compiled artifact, not the source, and never about Java versions. | Lesson 01 |
| Stack (data structure) | A pile you may only add to and take from at one end. Adding is *push*, removing is *pop*. | Lesson 01 |
| Operand stack | The scratch pile the JVM computes on: instructions push values onto it, and operations like `iadd` pop their inputs and push the result. | Lesson 01 |
| Stack machine | A machine that computes using a stack rather than named registers. Its instructions are tiny because their inputs are implicit. | Lesson 01 |
| Local variable table / slot | The small numbered array of variables a method call gets. Your variable names became slot numbers at compile time. Slot 0 is the first parameter in a static method and `this` in an instance method. | Lesson 01 |
| Class file (`.class`) | The binary file `javac` produces, one per class, holding a class's bytecode and everything it refers to. Its layout is published in the JVM Specification. | Lesson 01 |
| Binary file | A file storing raw numbers rather than readable text. Opening one in a text editor shows nonsense — the bytes are fine, the interpretation is wrong. | Lesson 01 |
| Magic number (file) | A fixed value at the very start of a file announcing its type. Java's is `0xCAFEBABE`; the JVM refuses any file that lacks it. | Lesson 01 |
| Hexadecimal | Base 16, using digits 0–9 then A–F. Convenient for bytes because each byte is exactly two hex digits. | Lesson 01 |
| Class file major version | Bytes 6–7 of a class file: which edition of the format it follows. **Java release = major − 44**, so Java 21 writes 65, Java 17 writes 61, Java 8 writes 52. | Lesson 01 |
| Class file minor version | Bytes 4–5, almost always 0. The value 65535 marks a file compiled with preview features enabled. | Lesson 01 |
| Preview features | Experimental language features shipped for feedback, enabled with `--enable-preview`. Such class files run only on exactly the same Java release, also started with the flag. | Lesson 01 |
| `UnsupportedClassVersionError` | The JVM's refusal to run a class file newer than itself. "Compiled by a more recent version" describes the *file*; "this runtime only recognizes" describes the *JVM*. | Lesson 01 |
| Constant pool | A numbered table inside a class file holding every name and literal the class refers to. Bytecode cites entries by number, which is why disassembly is full of `#` numbers. | Lesson 01 |
| Descriptor | The compact code a class file uses for types: `I` = int, `J` = long, `Z` = boolean, `Ljava/lang/String;` = String, `[I` = int[]. So `add(int,int)` returning int is `(II)I`. | Lesson 01 |
| `javap` | The JDK's class file disassembler. `-c` shows bytecode, `-p` includes private members, `-v` shows the version header and constant pool, `-l` shows line numbers. | Lesson 01 |
| Disassembler | A program that reads compiled instructions and prints them back as readable mnemonics. It recovers instructions, never your source. | Lesson 01 |
| Byte offset | The number on the left of each disassembled instruction: its position in bytes from the start of the method. Not a line number, which is why the numbers jump unevenly. | Lesson 01 |
| `invokestatic` / `invokevirtual` / `invokespecial` / `invokeinterface` | The four ordinary call instructions: a static method; an ordinary method (implementation chosen by the object's real class); constructors, `super.` and private calls; and a call through an interface type. | Lesson 01 |
| `invokedynamic` | An instruction meaning "work out what to call the first time this runs, then reuse that answer." Used for string concatenation (`makeConcatWithConstants`) and lambdas (`LambdaMetafactory`). | Lesson 01 |
| `<init>` | The reserved name under which a constructor is stored in a class file. A constructor is an ordinary method returning `void` (`V`). | Lesson 01 |
| Sign extension / `& 0xFF` | A Java `byte` is signed (−128..127), so `0xCA` arrives as −54 and widening fills the high bits with ones. Masking with `& 0xFF` re-reads the same eight bits as 0..255. | Lesson 01 |
| Big-endian | Storing a multi-byte number most significant byte first — the order the class file format always uses. | Lesson 01 |
| Interpreter | The part of the JVM that carries out bytecode one instruction at a time without translating it first. Starts instantly; runs roughly 10–100× slower than native code. | Lesson 01 |
| JIT (just-in-time) compilation | Translating bytecode into native machine code *while the program runs*, on a background thread, once the code proves worth the effort. | Lesson 01 |
| Hot method | A method called (or looped) often enough that the JVM decides compiling it will pay for itself. The origin of the name *HotSpot*. | Lesson 01 |
| C1 / C2 | HotSpot's two JIT compilers: C1 compiles fast and optimises lightly; C2 compiles slowly and optimises aggressively. | Lesson 01 |
| Tiered compilation | HotSpot's arrangement of using the interpreter first, then C1, then C2 for the hottest code. | Lesson 01 |
| Warm-up | The settling-out period at the start of a run while interpretation gives way to compiled code. Any Java timing taken without warm-up is meaningless. | Lesson 01 |
| Record | A short way to declare a small immutable data-holder class: `record Point(int x, int y) { }` generates the fields, constructor, readers, `equals`, `hashCode` and `toString`. | Lesson 01 |
| try-with-resources | `try (Stream s = ...) { }` — a `try` that closes what it opened automatically, whether the block ends normally or by throwing. | Lesson 01 |
| `--release` | The compiler flag that targets an older Java release *and* swaps in that release's API definitions. Safer than `-source`/`-target`, which only change the language level and class file version. | Lesson 01 |
| JMH | The Java Microbenchmark Harness, OpenJDK's standard tool for measuring Java performance honestly. The right answer whenever a stopwatch is tempting. | Lesson 01 |
| Flaky test | A test that sometimes passes and sometimes fails for reasons unrelated to the code — typically timing. One flaky test teaches a team to ignore red builds. | Lesson 01 |
| Bytecode manipulation | Generating or rewriting class files programmatically. Behind Mockito's mocks (Byte Buddy), Hibernate and Spring proxies, and JaCoCo's coverage counters. | Lesson 01 |
