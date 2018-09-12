package com.intellij.rt.sa;

import com.sun.tools.attach.VirtualMachine;

import java.io.*;
import java.util.Locale;
import java.util.Properties;

public class SaJdwp {
    // do not allow instance creation
    private SaJdwp() {
    }

    private static void usage() {
        PrintStream out = System.out;
        out.println("Usage: java -jar sa-jdwp.jar <pid>");
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            usage();
        }
        String pidString = args[0];
        VirtualMachine vm = VirtualMachine.attach(pidString);
        Properties systemProperties = vm.getSystemProperties();
        String javaHome = systemProperties.getProperty("java.home");
        String version = systemProperties.getProperty("java.specification.version");
        vm.detach();

        // todo: determine if this is a jdk or jre better
        boolean windows = System.getProperty("os.name").toLowerCase(Locale.US).startsWith("windows");
        String javac = "javac";
        if (windows) {
            javac += ".exe";
        }
        if (!new File(javaHome, "bin/" + javac).exists()) {
            if (!new File(javaHome, "../bin/" + javac).exists()) {
                System.out.println("JDK not detected, unable to attach");
                return;
            }
        }

        if (version.startsWith("1.6") || version.startsWith("1.7") || version.startsWith("1.8")) {
            start678(javaHome, pidString);
            return;
        } else if (version.startsWith("9")) {
            start9(javaHome, pidString);
            return;
        } else if (version.startsWith("10")) {
            start10(javaHome, pidString);
            return;
        }
        System.out.println("Unable to start on version " + version);
    }

    private static void start678(String javaHome, String pidString) throws Exception {
        // look for libs
        File toolsJar = new File(javaHome, "lib/tools.jar");
        if (!toolsJar.exists()) {
            toolsJar = new File(javaHome, "../lib/tools.jar");
            if (!toolsJar.exists()) {
                System.out.println("Unable to find tools.jar in " + javaHome);
                System.exit(1);
            }
        }
        File saJdiJar = new File(javaHome, "lib/sa-jdi.jar");
        if (!saJdiJar.exists()) {
            saJdiJar = new File(javaHome, "../lib/sa-jdi.jar");
            if (!saJdiJar.exists()) {
                System.out.println("Unable to find sa-jdi.jar in " + javaHome);
                System.exit(1);
            }
        }
        ProcessBuilder builder = new ProcessBuilder(javaHome + "/bin/java",
                "-cp", "\"" + toolsJar.getCanonicalPath() + ";" + saJdiJar.getCanonicalPath() + ";" + new File(SaJdwp.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath() + "\"",
                "SaJdwpServer", pidString);
        startServer(builder);
    }


    private static void start9(String javaHome, String pidString) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(javaHome + "/bin/java",
                "--add-modules", "jdk.hotspot.agent",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot=ALL-UNNAMED",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot.runtime=ALL-UNNAMED",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot.memory=ALL-UNNAMED",
                "--add-opens", "jdk.hotspot.agent/sun.jvm.hotspot.oops=ALL-UNNAMED",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot.utilities=ALL-UNNAMED",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot.debugger=ALL-UNNAMED",
                "-cp", "\"" + new File(SaJdwp.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath() + "\"",
                "SaJdwpServer", pidString);
        startServer(builder);
    }

    private static void start10(String javaHome, String pidString) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(javaHome + "/bin/java",
                "--add-modules", "jdk.hotspot.agent",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot=ALL-UNNAMED",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot.runtime=ALL-UNNAMED",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot.memory=ALL-UNNAMED",
                "--add-opens", "jdk.hotspot.agent/sun.jvm.hotspot.oops=ALL-UNNAMED",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot.utilities=ALL-UNNAMED",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot.debugger=ALL-UNNAMED",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot.classfile=ALL-UNNAMED",
                "-cp", "\"" + new File(SaJdwp.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath() + "\"",
                "SaJdwpServer", pidString);
        startServer(builder);
    }

    private static void startServer(ProcessBuilder builder) throws IOException {
        builder.redirectErrorStream(true);
        System.out.println("Running: ");
        for (String s : builder.command()) {
            System.out.print(s + " ");
        }
        System.out.println();
        Process process = builder.start();
        BufferedReader stdOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String s;
        try {
            while ((s = stdOutput.readLine()) != null) {
                System.out.println(s);
            }
        } finally {
            stdOutput.close();
        }
    }
}
