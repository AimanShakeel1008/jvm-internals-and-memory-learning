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
| Garbage collector (GC) | The part of the JVM that finds objects the program can no longer reach and reclaims their memory. Your code never frees an object itself. | Lesson 04 |
| Reference counting | Freeing an object when the number of references pointing at it drops to zero. Java does NOT use it, because two objects pointing at each other never reach zero — cycles leak. | Lesson 04 |
| GC roots | The starting points the collector is certain the program is using: local variables in every live frame, static fields of loaded classes, running threads, and a few JVM-internal references. | Lesson 04 |
| Reachability / reachable | An object is reachable if some chain of references leads to it from a GC root. Unreachable objects are garbage, however many other objects point at them. | Lesson 04 |
| Garbage | An object no chain of references from any GC root can reach. Eligible for collection — not yet collected. | Lesson 04 |
| Tracing collector | A collector that finds live objects by following references outward from the roots. Every HotSpot collector is one, which is why unreachable cycles are collected. | Lesson 04 |
| Memory leak (in Java) | An object the program no longer needs that is still reachable — usually through a static collection, an unbounded cache, a listener never removed, or a `ThreadLocal`. The collector is forbidden to touch it. | Lesson 04 |
| Eligible for collection | The state of an object once its last path from a root is cut. It still occupies heap and counts in `used` until a collection actually runs. | Lesson 04 |
| Marking | The collector's walk from the roots, flagging every object it reaches as alive. Its cost is proportional to the number of *live* objects; garbage is never visited. | Lesson 04 |
| Stop-the-world pause | A period during which every application thread is paused so the collector can work safely. The "freeze" users notice. Modern collectors keep these short. | Lesson 04 |
| Sweep / fragmentation | Sweeping adds dead objects' space to a list of free gaps. Fragmentation is free memory split into holes too small to use, so a large object can fail to fit despite plenty of free memory in total. | Lesson 04 |
| Compaction | Sliding live objects together so free space becomes one solid block. Removes fragmentation at the cost of moving objects and updating references to them. | Lesson 04 |
| Copying collection | Copying every live object into an empty area, then treating the whole old area as free in one step. Its cost depends only on how much is alive — cheap when almost everything is dead. | Lesson 04 |
| Generational hypothesis | The observation that most objects die very young, and the few that survive tend to live long. The bet behind splitting the heap into young and old generations. | Lesson 04 |
| Generation | An area of the heap holding objects of roughly the same age. HotSpot uses a young generation and an old generation. | Lesson 04 |
| Young generation | Where new objects live: Eden plus two survivor spaces. Collected often and cheaply. | Lesson 04 |
| Eden | The part of the young generation where (almost) every new object is created. | Lesson 04 |
| Bump-the-pointer allocation | Creating an object by handing out the next N free bytes and moving a pointer forward by N. Why `new` is cheap in Java. | Lesson 04 |
| TLAB (thread-local allocation buffer) | A private slice of Eden given to one thread, so it can bump-allocate without coordinating with other threads. | Lesson 04 |
| Survivor spaces (S0 / S1) | Two small, equal areas beside Eden. One holds objects that survived earlier collections; the other is empty, waiting to receive survivors at the next minor GC. They swap roles each time. | Lesson 04 |
| Minor GC | A collection of the young generation only, run when Eden fills. Copies live objects to a survivor space (or promotes them) and empties Eden in one step. Never reclaims anything in the old generation. | Lesson 04 |
| Object age | How many collections an object has survived, stored in a few bits of its header (four bits in HotSpot, so at most 15). | Lesson 04 |
| Promotion (tenuring) | Moving an object from the young generation into the old generation. | Lesson 04 |
| Tenuring threshold | The age at which an object is promoted. At most 15 in HotSpot; set with `-XX:MaxTenuringThreshold`, and the JVM may lower it on its own when survivor spaces get crowded. | Lesson 04 |
| Premature promotion | Short-lived objects pushed into the old generation too early — typically because the survivor space overflowed. Fills the old generation with garbage that is expensive to collect. | Lesson 04 |
| Old generation (tenured generation) | Where promoted, long-lived objects live. Fills slowly, collected rarely and expensively. | Lesson 04 |
| Card table / write barrier | How a minor GC finds old objects that point at young ones without scanning the whole old generation: a small piece of code the JIT adds to every reference store (the write barrier) marks that slice of the old generation "dirty" in a table (the card table), and only dirty slices are scanned. | Lesson 04 |
| Major GC / full GC | A major GC collects the old generation; a full GC collects the whole heap (often Metaspace too) and is usually the longest pause. The terms are used loosely in practice. | Lesson 04 |
| Weak reference (`WeakReference`) | A reference that points at an object without counting toward its reachability. Once only weak references remain, the collector may reclaim the object, and `get()` returns `null`. | Lesson 04 |
| `Reference.reachabilityFence` | A call (Java 9+) that keeps an object reachable up to that exact line. Needed because scope is a compile-time idea and the JIT may treat a variable as dead after its last use. | Lesson 04 |
| `WeakHashMap` | A JDK map whose keys are held weakly, so an entry disappears once nothing else references its key. Trap: a value that refers back to its own key keeps the entry forever. | Lesson 04 |
| `finalize()` / `Cleaner` | `finalize()` was meant to run when an object became garbage; it is unpredictable, slows the GC, and was deprecated for removal in Java 18 (JEP 421). `java.lang.ref.Cleaner` is the last-resort replacement; try-with-resources is the normal answer. | Lesson 04 |
| `GarbageCollectorMXBean` | The monitoring object for one collector, reporting how many collections it has run and how many milliseconds they took in total. | Lesson 04 |
| Serial collector | HotSpot's simplest collector (`-XX:+UseSerialGC`), single-threaded, whose layout — one Eden, two survivor spaces, one old generation — matches the textbook picture exactly. Its collectors report as `Copy` and `MarkSweepCompact`. | Lesson 04 |
| Throughput | The share of a program's run spent running its own code rather than collecting garbage. An hour-long job with 3 minutes of GC has 95% throughput. | Lesson 05 |
| Pause time (latency) | How long the application stands still during a stop-the-world pause — both typical and, more importantly, worst case. | Lesson 05 |
| Footprint | The extra memory and CPU a collector needs on top of the program: spare heap, bookkeeping tables, GC threads. | Lesson 05 |
| Mark-compact | Marking live objects from the roots, then sliding them together. What Serial and Parallel run, stop-the-world, on the old generation. | Lesson 05 |
| Parallel collector | `-XX:+UseParallelGC`: Serial's stop-the-world algorithms run on many GC threads. Highest throughput; pauses still grow with the heap. Default up to Java 8. | Lesson 05 |
| G1 (Garbage First) | `-XX:+UseG1GC`, the default since Java 9. Divides the heap into regions, marks the old generation concurrently, and collects the regions with the most garbage first, aiming at a pause goal. | Lesson 05 |
| Region (G1) | One of many equal-sized blocks (typically 1–32 MB) a G1 heap is divided into. Each has a role at any moment — Eden, survivor, old, humongous or free — and can change role after being emptied. | Lesson 05 |
| Concurrent marking | Marking live objects on background threads while the application keeps running, kept correct by write barriers. G1 finishes it with two short pauses, Remark and Cleanup. | Lesson 05 |
| Mixed collection | A G1 collection of all young regions plus a few of the old regions with the most garbage. | Lesson 05 |
| Collection set | The regions G1 picks to evacuate in one pause, sized to fit its pause goal. | Lesson 05 |
| Humongous object | In G1, an object at least half a region in size. It gets whole regions of its own, directly in the old generation. Logged as `G1 Humongous Allocation` when it triggers a collection. | Lesson 05 |
| `-XX:MaxGCPauseMillis` | G1's pause-time goal (default 200 ms). A goal G1 sizes its work to meet, not a guarantee. | Lesson 05 |
| ZGC | `-XX:+UseZGC`: marks AND moves objects concurrently, using load barriers, so pauses stay under a millisecond regardless of heap size, at the cost of throughput and headroom. Generational mode added in Java 21 (JEP 439, `-XX:+ZGenerational`). | Lesson 05 |
| Load barrier | A tiny check the JIT adds to every reference read. In ZGC it fixes up a reference whose object has been moved, before your code uses it. | Lesson 05 |
| Shenandoah | Another low-pause, concurrent-compacting collector, shipped in OpenJDK builds such as Temurin. | Lesson 05 |
| Unified logging (`-Xlog`) | The JVM's single logging system since Java 9: `-Xlog:WHAT:WHERE:DECORATIONS:OUTPUT-OPTIONS`, e.g. `-Xlog:gc:file=gc.log`. Replaced `-XX:+PrintGCDetails` and `-Xloggc`. | Lesson 05 |
| Log tag / decoration | A tag labels a message's subject (`gc`, `gc+age`, `class+load`); `*` includes every combination. Decorations are the bracketed prefix fields, by default `[uptime][level][tags]`. | Lesson 05 |
| Pause line | The one-line GC log record of a stop-the-world pause: uptime, `GC(n)`, `Pause Kind`, causes, heap before → after (committed), duration. | Lesson 05 |
| GC cause | The bracketed reason in a pause line: `Allocation Failure`, `G1 Evacuation Pause`, `Concurrent Start`, `Mixed`, `G1 Humongous Allocation`, `Metadata GC Threshold`, `Ergonomics`, `System.gc()`. | Lesson 05 |
| Regular expression (`Pattern` / `Matcher`) | A compact language for describing the shape of text and pulling named pieces out of it. Used by the project's GC log parser. | Lesson 05 |
| `@TempDir` | A JUnit 5 annotation giving a test a fresh empty folder that is deleted afterwards, so it can use real files without touching the project. | Lesson 05 |
| CMS | The old Concurrent Mark Sweep collector (`-XX:+UseConcMarkSweepGC`), removed in Java 14. Its flags are ignored with a warning on modern JVMs. | Lesson 05 |
| Profiling / profile | Collecting facts about running code — call counts, loop counts, which way branches went, which classes arrived at each call — to guide later compilation. The facts are the profile. | Lesson 06 |
| Back-edge | A jump back to the top of a loop; counted once per loop iteration, so long loops can trigger compilation even in a method called once. | Lesson 06 |
| Tier (0–4) | HotSpot's compilation levels: 0 interpreter, 1 C1 no profiling (trivial methods), 2 C1 light profiling, 3 C1 full profiling, 4 C2. The usual path is 0 → 3 → 4. | Lesson 06 |
| On-stack replacement (OSR) | Compiling a method while one of its loops is running, then moving the running loop's state into the compiled code mid-loop. Marked `%` in `-XX:+PrintCompilation`. | Lesson 06 |
| Inlining | Copying a called method's body into its caller, removing the call and letting the compiler optimise across the old boundary. Makes small methods free once code is hot. | Lesson 06 |
| Constant folding | Working out at compile time a value that can only ever be one thing, instead of computing it at run time. Often unlocked by inlining. | Lesson 06 |
| Monomorphic / bimorphic / megamorphic | A call site through an interface or superclass that has seen one, two, or three-plus receiver classes. The first two can be inlined behind cheap checks; megamorphic calls cannot. | Lesson 06 |
| Class hierarchy analysis (CHA) | C2's use of the classes loaded so far: if only one class implements an interface, calls can be inlined without a check — a bet recorded as a dependency and undone if another implementation loads. | Lesson 06 |
| Escape (an object escapes) | An object escapes a method if code outside it could ever reach it: returned, stored in a field or static, passed to a non-inlined method, or thrown. | Lesson 06 |
| Escape analysis | C2's check, after inlining, of whether each allocation can escape. If not, the object can be removed (scalar replacement) and its locks dropped (lock elision). | Lesson 06 |
| Scalar replacement | Replacing a non-escaping object by its separate fields held as plain local values — the object is never allocated at all. | Lesson 06 |
| Lock elision | Removing `synchronized` on an object no other thread can ever see. | Lesson 06 |
| Speculative optimisation | Compiling on the assumption that past behaviour continues — e.g. a never-taken branch stays untaken, a call site keeps seeing one class — protected by checks or recorded dependencies. | Lesson 06 |
| Deoptimization | Abandoning compiled code whose assumption broke: it is made not entrant, running frames are converted back to interpreter frames, and the method is later recompiled. Results never change. | Lesson 06 |
| Made not entrant | `-XX:+PrintCompilation`'s wording for retiring a compiled version. A tier 3 retirement is usually replacement by tier 4 (healthy); a tier 4 retirement is a deoptimization. | Lesson 06 |
| `-XX:+PrintCompilation` | HotSpot product flag printing one line per compilation or retirement: ms since start, id, attributes (`%` OSR…), tier, `Class::method`, `@ bci` for OSR, bytecode size. | Lesson 06 |
| Intrinsic | A JDK method HotSpot replaces with built-in, processor-tuned machine code instead of compiling its Java source — e.g. `System.arraycopy`, many `Math` methods. | Lesson 06 |
| Code cache | The memory region holding JIT-compiled machine code. If full, HotSpot stops compiling ("CodeCache is full. Compiler has been disabled") and runs new hot code interpreted. | Lesson 06 |
| Pattern matching for `instanceof` | `if (x instanceof Foo f)` — checks the type and gives the converted variable `f` in one step (Java 16+). | Lesson 06 |
| Byte order mark (BOM) | Marker bytes at the start of a text file announcing its encoding: `FF FE` for UTF-16LE (written by Windows PowerShell 5.1's `>`), `EF BB BF` for UTF-8. | Lesson 06 |
| `ThreadMXBean` allocated bytes | HotSpot's `com.sun.management.ThreadMXBean.getCurrentThreadAllocatedBytes()` — the running total of bytes the current thread has allocated; a HotSpot extension, not standard Java. | Lesson 06 |
| Process id (PID) | The number the operating system gives each running program. Every JDK diagnostic tool finds a JVM by it. In Java: `ProcessHandle.current().pid()`. | Lesson 07 |
| `jps` | The JDK tool that lists running JVMs with their PIDs (`-l` for full main-class names). | Lesson 07 |
| Attaching | Connecting a diagnostic tool to an already-running JVM through a local channel built into HotSpot. Only on the same machine, as the same operating-system user. | Lesson 07 |
| `jcmd` | The JDK's all-in-one diagnostic tool: `jcmd <pid> <command>` for version, flags, thread dumps, class histograms, heap dumps, logging and more. `jcmd <pid> help` lists everything. | Lesson 07 |
| Thread dump | A snapshot of every thread's name, state and stack of frames (`jstack <pid>` or `jcmd <pid> Thread.print`). Take three a few seconds apart. | Lesson 07 |
| Thread state | `RUNNABLE` (running or ready), `BLOCKED` (waiting to enter a `synchronized` block), `WAITING` (waiting indefinitely for a signal), `TIMED_WAITING` (waiting with a timeout, e.g. `sleep`). | Lesson 07 |
| Deadlock | Two or more threads each holding a lock another needs, so none can ever continue. HotSpot reports simple ones at the end of a thread dump ("Found one Java-level deadlock"). Fixed by a consistent lock order. | Lesson 07 |
| `jstack` / `jmap` | Older single-purpose tools: `jstack` prints thread dumps; `jmap -histo` prints class histograms and `jmap -dump` writes heap dumps. `jcmd` covers both. | Lesson 07 |
| Class histogram | Instances and total shallow bytes per class, largest first (`jcmd <pid> GC.class_histogram`, which runs a full GC first). Compare two, minutes apart, to find what grows. | Lesson 07 |
| Shallow size | The memory an object itself takes — its header and fields — not counting the objects its fields point at. What histograms report. | Lesson 07 |
| Heap dump (`.hprof`) | A file holding every object on the heap and every reference between them. Taken with `jcmd <pid> GC.heap_dump`, `-XX:+HeapDumpOnOutOfMemoryError`, or `HotSpotDiagnosticMXBean.dumpHeap`. Pauses the JVM, is as big as the live heap, and contains every value in memory. | Lesson 07 |
| Retained size | How much memory would be freed if one object were collected: the object plus everything reachable only through it. Points at the object holding a leak. | Lesson 07 |
| Dominator / dominator tree | An object dominates another if every path from the GC roots to it passes through it. Heap-dump tools sort objects by retained size in a dominator tree. | Lesson 07 |
| Path to GC roots | The chain of references from a GC root down to a chosen object — Lesson 04's reachability, as a query. Answers "why is this still alive?". | Lesson 07 |
| `-XX:+HeapDumpOnOutOfMemoryError` | Writes a heap dump at the moment an `OutOfMemoryError` occurs (to `-XX:HeapDumpPath`). Standard for production services. | Lesson 07 |
| JConsole / VisualVM / Eclipse MAT | Visual tools: JConsole ships with the JDK (live graphs); VisualVM is a separate download (monitoring, heap dumps); Eclipse MAT analyses large heap dumps (dominator tree, leak suspects). | Lesson 07 |
| JDK Flight Recorder (JFR) | An event recorder built into HotSpot, cheap enough for production, started with `jcmd <pid> JFR.start` and viewed in JDK Mission Control. | Lesson 07 |
