# Glossary — Core Java Course 01: JVM Internals & Memory Management

Every term this course introduces, in plain language, with the lesson where it first appeared. This is an index for quick lookup — the full explanation always lives inline in the lesson itself.

| Term | Plain-language meaning | First appears |
| --- | --- | --- |
| Virtual machine | A program that pretends to be a computer: it reads instructions meant for an imaginary machine and carries them out on the real machine underneath. | Lesson 00 |
| JVM (Java Virtual Machine) | The virtual machine that runs compiled Java. It executes your code, owns the memory your objects live in, collects garbage, and speeds up hot code. The main character of this course. | Lesson 00 |
| Standard library | The huge collection of ready-made classes that ships with Java (`String`, `ArrayList`, `Files`…) and that every program can use. | Lesson 00 |
| JRE (Java Runtime Environment) | The JVM plus the standard library — enough to *run* Java programs but not to compile them. No longer shipped separately, but the name survives in old docs and error messages. | Lesson 00 |
| JDK (Java Development Kit) | Everything in the JRE plus developer tools like the compiler (`javac`) and diagnostic tools. What a developer installs. JDK = build + run; JRE = run only; JVM = the engine inside both. | Lesson 00 |
| `javac` | The Java compiler: the JDK tool that turns `.java` source files into `.class` files the JVM can execute. Dissected in Lesson 01. | Lesson 00 |
| OpenJDK | The open-source project holding Java's shared source code, from which every vendor builds their JDK. | Lesson 00 |
| Temurin | Eclipse Adoptium's free build of OpenJDK — the JDK this course installs. | Lesson 00 |
| HotSpot | The name of the JVM implementation inside standard JDK builds. When this course says "the JVM does X," it means HotSpot. | Lesson 00 |
| LTS (Long-Term Support) | A release designated to receive fixes and security patches for years instead of months. Java 21 is LTS, which is why the series pins it. | Lesson 00 |
| Pinning | Deciding a version once, writing it into committed files, and having the build check it — so version drift can never silently change behavior. A promise to manage versions deliberately, not to freeze them forever. | Lesson 00 |
| Build tool | A program that turns a written project description into a finished build: fetching libraries, compiling in order, running tests, packaging. | Lesson 00 |
| Maven | The standard Java build tool. Reads `pom.xml` and derives the whole build from it. | Lesson 00 |
| POM / `pom.xml` | Project Object Model — the single file at a Maven project's root describing what the project *is* (name, Java version, libraries, plugins). | Lesson 00 |
| Dependency | A library your code uses instead of rewriting — declared by name in the POM; Maven fetches it and everything it needs. | Lesson 00 |
| Coordinates (groupId, artifactId, version) | The three-part address uniquely naming any library or project: who made it, which product, which edition. | Lesson 00 |
| Maven Central | The public online warehouse of published Java libraries that Maven downloads dependencies from (cached locally in `~/.m2/repository`). | Lesson 00 |
| Plugin (Maven) | A bolt-on component that does one job of the build (compiling, running tests, running a main class). Maven is just the coordinator; plugins do the work. | Lesson 00 |
| Surefire | The Maven plugin that finds and runs your tests during `mvn test`. Old versions cannot see JUnit 5 tests and silently report `Tests run: 0`. | Lesson 00 |
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
| Sign extension / `& 0xFF` | A Java `byte` is signed (−128..127), so `0xCA` arrives as −54 and widening fills the high bits with ones. Masking with `& 0xFF` re-reads the same eight bits as 0..255. | Lesson 01 |
| Constant pool | A numbered table inside a class file holding every name and literal the class refers to. Bytecode cites entries by number, which is why disassembly is full of `#` numbers. | Lesson 01 |
| Descriptor | The compact code a class file uses for types: `I` = int, `J` = long, `Z` = boolean, `Ljava/lang/String;` = String, `[I` = int[]. So `add(int,int)` returning int is `(II)I`. | Lesson 01 |
| `javap` | The JDK's class file disassembler. `-c` shows bytecode, `-p` includes private members, `-v` shows the version header and constant pool, `-l` shows line numbers. | Lesson 01 |
| Disassembler | A program that reads compiled instructions and prints them back as readable mnemonics. It recovers instructions, never your source. | Lesson 01 |
| Byte offset | The number on the left of each disassembled instruction: its position in bytes from the start of the method. Not a line number, which is why the numbers jump unevenly. | Lesson 01 |
| `invokestatic` / `invokevirtual` / `invokespecial` / `invokeinterface` | The four ordinary call instructions: a static method; an ordinary method (implementation chosen by the object's real class); constructors, `super.` and private calls; and a call through an interface type. | Lesson 01 |
| `invokedynamic` | An instruction meaning "work out what to call the first time this runs, then reuse that answer." Used for string concatenation (`makeConcatWithConstants`) and lambdas (`LambdaMetafactory`). | Lesson 01 |
| `<init>` | The reserved name under which a constructor is stored in a class file. A constructor is an ordinary method returning `void` (`V`). | Lesson 01 |
| Interpreter | The part of the JVM that carries out bytecode one instruction at a time without translating it first. Starts instantly; runs roughly 10–100× slower than native code. | Lesson 01 |
| JIT (just-in-time) compilation | Translating bytecode into native machine code *while the program runs*, on a background thread, once the code proves worth the effort. | Lesson 01 |
| Hot method | A method called (or looped) often enough that the JVM decides compiling it will pay for itself. The origin of the name *HotSpot*. | Lesson 01 |
| C1 / C2 | HotSpot's two JIT compilers: C1 compiles fast and optimises lightly; C2 compiles slowly and optimises aggressively. | Lesson 01 |
| Tiered compilation | HotSpot's arrangement of using the interpreter first, then C1, then C2 for the hottest code. | Lesson 01 |
| Warm-up | The settling-in period at the start of a run while interpretation gives way to compiled code. Any Java timing taken without warm-up is meaningless. | Lesson 01 |
| Record | A short way to declare a small immutable data-holder class: `record Point(int x, int y) { }` generates the fields, constructor, readers, `equals`, `hashCode` and `toString`. | Lesson 01 |
| try-with-resources | `try (Stream s = ...) { }` — a `try` that closes what it opened automatically, whether the block ends normally or by throwing. | Lesson 01 |
| `--release` | The compiler flag that targets an older Java release *and* swaps in that release's library definitions. Safer than `-source`/`-target`, which only change the language level and class file version. | Lesson 01 |
| JMH | The Java Microbenchmark Harness, OpenJDK's standard tool for measuring Java performance honestly. The right answer whenever a stopwatch is tempting. | Lesson 01 |
| Flaky test | A test that sometimes passes and sometimes fails for reasons unrelated to the code — typically timing. One flaky test teaches a team to ignore red builds. | Lesson 01 |
| Bytecode manipulation | Generating or rewriting class files programmatically. Behind Mockito's mocks (Byte Buddy), Hibernate and Spring proxies, and JaCoCo's coverage counters. | Lesson 01 |
| Class loader | An ordinary Java object whose job is: given a class's name as text, produce a loaded class. Not a keyword and not a build phase — an object you can hold and question. | Lesson 02 |
| Bootstrap class loader | The loader built into the JVM in C++ that defines `java.base` (`Object`, `String`, `Integer`…). It has no Java object, so `getClassLoader()` returns `null` for anything it loaded — that null *means* bootstrap. | Lesson 02 |
| Platform class loader | The loader for JDK modules outside `java.base`, e.g. `java.sql`. Named `"platform"`. Its parent is the bootstrap loader. Replaced the pre-Java-9 "extension" loader. | Lesson 02 |
| Application (system) class loader | The loader that defines everything on your class path — your code and your libraries. Named `"app"`, returned by `ClassLoader.getSystemClassLoader()`. "System" here has nothing to do with the operating system. | Lesson 02 |
| Parent delegation | The rule that a class loader asks its parent before searching itself, so a class is always found as high in the chain as possible. Gives security by construction, uniqueness of JDK types, and a definite "not found". | Lesson 02 |
| Defining loader | The loader that actually turned the bytes into a class, as opposed to the loader that was first asked. `getClassLoader()` reports the defining loader. | Lesson 02 |
| Runtime identity of a class | A class is identified by its fully-qualified name **plus** its defining loader. Two loaders defining the same name produce two different, incompatible types. | Lesson 02 |
| `ClassCastException: X cannot be cast to X` | The signature of two loaders having each defined a class named X. The loader names in brackets in the message are the only differing part. | Lesson 02 |
| Loading (step 1) | Finding a class's bytes, parsing them, and creating the `Class` object. The magic number and version check happen here. | Lesson 02 |
| Linking (step 2) | The middle phase of the class lifecycle, made of verification, preparation and resolution. | Lesson 02 |
| Verification | The JVM proving to itself, before running anything, that a class's bytecode cannot misbehave: consistent stack, jumps land on instructions, no type confusion, access rules obeyed. Failure is `VerifyError`. | Lesson 02 |
| Preparation | Allocating static fields and setting them to *default* values (`0`, `false`, `null`) — not your values. The exception: `static final` compile-time constants are filled in here from the `ConstantValue` attribute. | Lesson 02 |
| Resolution | Turning a class file's `#`-numbered symbolic references into direct pointers, loading further classes as needed. HotSpot does it lazily, which is why a missing class can surface hours into a run. | Lesson 02 |
| Initialization | The final step: running the class's static field assignments and `static { }` blocks, exactly once per loader. The only step that runs code you wrote. | Lesson 02 |
| `<clinit>` | The synthetic method `javac` builds by welding every static field assignment and `static { }` block together in source order. You never write it, cannot call it, and the JVM runs it once under its own lock. | Lesson 02 |
| Compile-time constant (constant variable) | A `static final` field of primitive or `String` type assigned a constant expression. Its value is copied into every reader's class file at compile time — so reading it never loads or initializes the declaring class, and changing it does not reach un-recompiled callers. | Lesson 02 |
| `ConstantValue` attribute | The place in a class file where a compile-time constant's value is stored, so preparation can assign it without running any code. | Lesson 02 |
| Lazy loading | The JVM's policy of loading a class at first use rather than at startup. Buys fast start and small footprint; costs late-surfacing errors. | Lesson 02 |
| `ClassNotFoundException` | A *checked exception*: someone asked for a class **by name as text** (`Class.forName`, `loadClass`, a framework reading config) and the whole delegation chain came up empty. | Lesson 02 |
| `NoClassDefFoundError` | An *Error*: the JVM was resolving a reference your compiled code contains, so the class existed at compile time and is missing now. A packaging problem, not a name problem. | Lesson 02 |
| `ExceptionInInitializerError` | Thrown the first time a static initializer throws. Its `getCause()` holds the real exception. | Lesson 02 |
| `NoClassDefFoundError: Could not initialize class X` | Not "X is missing" — X is present and permanently broken because its `<clinit>` threw earlier. The real cause is in the earlier `ExceptionInInitializerError`. | Lesson 02 |
| `Class.forName(name)` | Looks a class up by name and **initializes** it. The three-argument form `Class.forName(name, false, loader)` loads without initializing. | Lesson 02 |
| Thread context class loader | A loader reference carried on each thread so that framework code high in the hierarchy can reach classes below it — the escape hatch delegation would otherwise forbid. Used by `ServiceLoader`. | Lesson 02 |
| `-verbose:class` / `-Xlog:class+load=info` | JVM flags that print one line per class as it loads, with the source it came from. The modern unified-logging form is the `-Xlog` one. | Lesson 02 |
| `jrt:` | The URL scheme naming the JDK's own module storage, e.g. `jrt:/java.sql` — the modern replacement for the old `rt.jar`. | Lesson 02 |
| Class-data sharing (CDS) | A pre-parsed archive of core JDK classes the JDK ships and maps into memory at startup, so common classes are not re-parsed every run. Shows up in the load log as `source: shared objects file`. | Lesson 02 |
| Class loader leak | A memory leak where one lingering reference (a `ThreadLocal`, a static registry, a running thread) keeps a whole class loader — and every class it defined — alive. The classic cost of repeated redeployment. | Lesson 02 |
| Initialization-on-demand holder idiom | A lazy singleton built from the initialization rules: a private nested `Holder` class holds the instance, and reading `Holder.INSTANCE` initializes it exactly once, thread-safely, with no locks written by hand. | Lesson 02 |
| Unnamed module | Where code loaded from the class path lives under the Java 9+ module system. This course runs entirely from the class path, so entirely in the unnamed module. | Lesson 02 |
| Thread | One independent line of execution — one worker carrying out instructions in order. A plain Java program starts with one thread (`main`) and may create more. Each gets its own stack. | Lesson 03 |
| Stack (JVM / thread stack) | The per-thread region holding one frame per method call that has started and not yet finished. Fixed in size, reclaimed by arithmetic, never shared with another thread. | Lesson 03 |
| Frame (stack frame) | One method call's private working space: its local variable slots, its operand stack, and the return address. Pushed on call, popped on return — which is why local variables cost nothing to clean up. | Lesson 03 |
| Return address | The part of a frame recording where to carry on in the calling method once this call finishes. | Lesson 03 |
| Reference | The value an object-typed variable actually holds: an arrow identifying where the object lives on the heap. Assignment copies the arrow, never the object. | Lesson 03 |
| Primitive type | One of Java's eight built-in value types (`byte`, `short`, `int`, `long`, `float`, `double`, `char`, `boolean`). A primitive variable holds the value itself in its slot — no arrow, no heap. | Lesson 03 |
| Pass-by-value | Java's one argument-passing rule: the callee always receives a *copy* of what the caller had. For object types the copied thing is the reference, which is why a method can change the object it was handed but never change which object the caller's variable points at. "Java passes objects by reference" is wrong. | Lesson 03 |
| Aliasing | Two or more variables holding the same reference, so they are two names for one heap object. A change made through one name is visible through all of them. | Lesson 03 |
| Max / committed / used memory | The three numbers that describe a region: the ceiling it may grow to, how much the JVM has actually taken from the operating system so far, and how much of that currently holds data. Always `used <= committed <= max`. | Lesson 03 |
| Metaspace | The region holding one description per loaded class — its bytecode, field and method tables and constant pool. Allocated from native memory, unbounded by default, and it replaced PermGen in Java 8. | Lesson 03 |
| PermGen (permanent generation) | The pre-Java-8 heap region that held class metadata, with a small fixed ceiling (`-XX:MaxPermSize`). Its exhaustion after repeated redeploys was the classic `OutOfMemoryError: PermGen space`. Removed in Java 8; `-XX:MaxPermSize` is now ignored with a warning. | Lesson 03 |
| Native memory | Memory the JVM requests directly from the operating system, outside the Java heap. Metaspace, thread stacks and direct buffers all come from here, which is why a process can use far more memory than its heap. | Lesson 03 |
| PC register (program counter) | A tiny per-thread area holding the address of the bytecode instruction that thread is executing right now. What makes "each thread has its own place in the program" true. | Lesson 03 |
| Native method stack | A second per-thread stack, used for calls that leave Java for C or C++ code inside the JVM (file access, networking, timing). | Lesson 03 |
| Code cache | The shared region holding the native machine code the JIT produced. If it fills, the JVM prints `CodeCache is full. Compiler has been disabled` and keeps running correctly but interpreted — and therefore slow. | Lesson 03 |
| Direct (off-heap) buffer | A `ByteBuffer` created with `allocateDirect`, whose bytes live in native memory with only a small heap object pointing at them. Heavily used by I/O and networking libraries; exhausting them gives `OutOfMemoryError: Direct buffer memory`. | Lesson 03 |
| String pool / interned string | The JVM's shared table of string literals, so two classes using the same literal share one `String` object. It moved out of PermGen onto the ordinary heap in Java 7. | Lesson 03 |
| `StackOverflowError` | The error thrown when one thread's stack has no room for another frame. Almost always runaway recursion — read the repeated line in the trace. Moved by `-Xss`, never by `-Xmx`. | Lesson 03 |
| `OutOfMemoryError` | The error thrown when the JVM cannot satisfy an allocation and cannot reclaim enough to try again. The words after the colon name the region and decide the whole investigation: `Java heap space`, `Metaspace`, `Direct buffer memory`, and others. | Lesson 03 |
| `GC overhead limit exceeded` | An `OutOfMemoryError` variant meaning the collector is spending nearly all its time recovering nearly nothing. The heap is not quite full yet; the JVM gave up early rather than crawl. Same causes as `Java heap space`. | Lesson 03 |
| `Error` vs `Exception` | Both are throwable, but an `Exception` signals a condition a program may reasonably handle, while an `Error` signals one it is not expected to recover from. `StackOverflowError` and `OutOfMemoryError` are Errors, which is why catching them is normally wrong. | Lesson 03 |
| `-Xmx` / `-Xms` | JVM flags setting the heap's maximum size and its starting size. `-Xmx` is the ceiling whose exhaustion produces `OutOfMemoryError: Java heap space`; it does nothing for Metaspace, stacks or native memory. | Lesson 03 |
| `-Xss` | The JVM flag setting the size of each thread's stack. Per thread, not global — doubling it on a 500-thread server asks the OS for 500 times the increase. | Lesson 03 |
| `-XX:MaxMetaspaceSize` | The flag that puts a deliberate ceiling back on Metaspace, so a class-loader leak announces itself as `OutOfMemoryError: Metaspace` instead of quietly consuming the machine's memory. | Lesson 03 |
| Escape analysis | A JIT optimisation: if the compiler can prove an object never escapes the method that created it, it may skip the heap allocation entirely. Invisible from Java, never changes behaviour. Covered in Lesson 06. | Lesson 03 |
| MX bean / `ManagementFactory` | The JVM's self-monitoring API: plain Java objects the JVM keeps about itself, describing memory, threads and class loading. `ManagementFactory` is the front door to them. | Lesson 03 |
| Memory pool | One named region the JVM exposes for monitoring, such as `Metaspace` or `G1 Eden Space`. Which pools exist depends on the JVM, the garbage collector and the version, so code should search them by name rather than by position. | Lesson 03 |
| `OptionalLong` | A JDK type meaning "a long, or nothing", used instead of returning `-1` as a secret code for a missing value. The empty case says "not available", which is a different statement from "zero". | Lesson 03 |
