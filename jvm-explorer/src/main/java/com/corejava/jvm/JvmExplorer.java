// Every Java file starts by declaring which package it belongs to. A package is a
// named folder for classes; ours mirrors the Maven groupId plus the project purpose,
// and Maven REQUIRES the folder path on disk to match this line exactly.
package com.corejava.jvm;

// Lesson 02's module: reports which class loader defined a class, and the whole parent
// chain above it. An import is purely a compile-time convenience - it lets us write the
// short name below - and by itself it causes NO loading of anything at run time.
import com.corejava.jvm.loading.ClassLoaderReporter;
// The demonstration subjects and the witness that records which of them woke up.
import com.corejava.jvm.loading.ConstantHolder;
import com.corejava.jvm.loading.InitializationLog;
import com.corejava.jvm.loading.LazyHolder;
// Lesson 03's modules: where everything the JVM makes actually lives, and what happens
// at the two walls - the stack that runs out of frames and the heap that runs out of room.
import com.corejava.jvm.memory.MemoryRegions;
import com.corejava.jvm.memory.ReferenceDemo;
import com.corejava.jvm.memory.StackDepthProbe;

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

        // ---- Lesson 01: what this JVM is actually being fed -------------------
        // A blank line, then a heading, so the two halves of the report read as two
        // separate questions: "which engine is running?" and "what is it running?".
        System.out.println();
        System.out.println("--- class file headers: what javac actually produced ---");

        // Every class the JVM runs arrived as a .class file whose first eight bytes
        // announce the file type and the format version. Printing them for two of our
        // own classes shows that this is not theory: our compiled output really does
        // start with 0xCAFEBABE and really does carry the version number the JVM checks.
        System.out.println(ClassFileInspector.inspect(BytecodeSubject.class).describe());
        System.out.println(ClassFileInspector.inspect(JvmExplorer.class).describe());

        // The two numbers that must agree for anything to run at all: the version the
        // COMPILER wrote into the file, and the version the RUNNING ENGINE understands.
        // When these disagree the JVM refuses the file with UnsupportedClassVersionError,
        // so printing them side by side turns that error message into something readable.
        ClassFileHeader ownHeader = ClassFileInspector.inspect(JvmExplorer.class);
        System.out.println("Class files say : " + ownHeader.javaVersionName()
                + " (major " + ownHeader.majorVersion() + ")");
        System.out.println("Running JVM is  : Java " + Runtime.version().feature());

        // ---- Lesson 02: who found those files, and when? ----------------------
        // Lesson 01 asked the JVM for a class's own bytes and skipped past the question
        // of WHO located them. That "who" is a class loader, and there are three built
        // into every JVM, arranged in a parent chain. Printing the chain for classes
        // from three different places shows all three loaders at once.
        System.out.println();
        System.out.println("--- who loaded what: the class loader chain ---");

        // java.lang.String lives in java.base, the core module the JVM cannot run
        // without, so it is defined by the bootstrap loader - the one written in C++
        // inside the JVM itself, which has no Java object and shows up as null.
        System.out.println(ClassLoaderReporter.reportFor(String.class));

        // java.sql.Date lives in a JDK module that is NOT part of java.base. Such
        // modules are expected to be defined by the platform loader, one step below
        // bootstrap. Printed rather than asserted: confirm what your JDK reports.
        System.out.println(ClassLoaderReporter.reportFor(java.sql.Date.class));

        // Our own classes come off the class path, which belongs to the application
        // loader - the bottom of the chain, and the only one that ever sees our code.
        System.out.println(ClassLoaderReporter.reportFor(JvmExplorer.class));

        // The chain itself, independent of any one class: app -> platform -> bootstrap.
        // Every single class request walks UP this chain before anyone searches.
        System.out.println("Delegation chain : " + String.join("  ->  ", ClassLoaderReporter.builtInDelegationChain()));

        // ---- Lesson 02: nothing initializes until it must -----------------------
        // A class is loaded lazily and initialized even more lazily. The witness log
        // starts empty and only grows when the program does something that genuinely
        // requires a class's own code to have run.
        System.out.println();
        System.out.println("--- lazy initialization: nothing runs until it is needed ---");

        // Empty: importing ConstantHolder and LazyHolder above changed nothing at all.
        System.out.println("Initialized so far : " + InitializationLog.events());

        // A compile-time constant. javac copied the number 65 into THIS class's own
        // bytecode, so this line does not mention ConstantHolder at run time - and the
        // log below is still empty afterwards.
        System.out.println("Constant read      : " + ConstantHolder.PINNED_MAJOR_VERSION);
        System.out.println("Initialized so far : " + InitializationLog.events());

        // A static method call cannot happen without the class's initializer having
        // run first, so this is the line that finally wakes LazyHolder up.
        System.out.println("Static method call : " + LazyHolder.stamp());
        System.out.println("Initialized so far : " + InitializationLog.events());

        // ---- Lesson 03: where all of this actually lives ----------------------
        // Every class loaded above put a description of itself into Metaspace, and every
        // object this program has created sits on the heap. Both are now measurable
        // rather than theoretical, so we print the regions the JVM is currently using.
        System.out.println();
        System.out.println("--- runtime memory areas: heap, non-heap, Metaspace ---");
        System.out.println(MemoryRegions.describe());

        // A variable never holds an object; it holds a reference to one on the heap.
        // Printing this report proves it: the same method call can change the caller's
        // object and yet be unable to change the caller's variable.
        System.out.println();
        System.out.println("--- references: the variable is an arrow, the object is on the heap ---");
        System.out.println(ReferenceDemo.describe());

        // The stack is per thread and has a fixed size, so a chain of calls that never
        // returns must eventually run out of frames. The probe provokes exactly that,
        // catches the error, and reports how far it got - a number that will differ on
        // every machine, and changes when you pass -Xss.
        System.out.println();
        System.out.println("--- the stack has a floor: how many frames fit? ---");
        System.out.println("Frames before overflow : " + StackDepthProbe.measureMaxDepth());
        System.out.println("Ended with             : " + StackDepthProbe.lastErrorType());
        System.out.println("The heap's own wall is provoked separately: "
                + "java -Xmx32m -cp target/classes com.corejava.jvm.experiments.MemoryLimitsExperiment --oom");
    }
}
