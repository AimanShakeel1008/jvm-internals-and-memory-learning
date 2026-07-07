// Every Java file starts by declaring which package it belongs to. A package is a
// named folder for classes; ours mirrors the Maven groupId plus the project purpose,
// and Maven REQUIRES the folder path on disk to match this line exactly.
package com.corejava.jvm;

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
    }
}
