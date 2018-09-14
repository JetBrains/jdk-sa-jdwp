package com.jetbrains.sa;

import com.sun.tools.attach.VirtualMachine;

import java.io.*;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

public class SaJdwp {
    // do not allow instance creation
    private SaJdwp() {
    }

    private static void usage() {
        System.out.println("Usage: java -jar sa-jdwp.jar <pid> (port)");
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            usage();
        }
        startServer(args[0], args.length > 1 ? args[1] : "", true);
    }

    static String startServer(String pidString, String port, boolean console) throws Exception {
        VirtualMachine vm = VirtualMachine.attach(pidString);
        String javaHome;
        String version;
        try {
            Properties systemProperties = vm.getSystemProperties();
            javaHome = systemProperties.getProperty("java.home");
            version = systemProperties.getProperty("java.specification.version");
        }
        finally {
            vm.detach();
        }

        // todo: determine if this is a jdk or jre better
        boolean windows = System.getProperty("os.name").toLowerCase(Locale.US).startsWith("windows");
        String javac = "javac";
        if (windows) {
            javac += ".exe";
        }
        if (!new File(javaHome, "bin/" + javac).exists()) {
            if (!new File(javaHome, "../bin/" + javac).exists()) {
                throw new IllegalStateException("JDK not detected, unable to attach");
            }
        }

        ProcessBuilder builder = null;
        if (version.startsWith("1.8")) {
            builder = prepare8(javaHome);
        } else if (version.startsWith("9")) {
            builder = prepare9(javaHome);
        } else if (version.startsWith("10")) {
            builder = prepare10(javaHome);
        }

        if (builder != null) {
            builder.command().addAll(Arrays.asList(SaJdwpServer.class.getName(), pidString, port));
            return startServer(builder, console);
        }

        throw new IllegalStateException("Unable to start on version " + version);
    }

    private static ProcessBuilder prepare8(String javaHome) throws Exception {
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
        return new ProcessBuilder(javaHome + "/bin/java",
                "-cp", "\"" + toolsJar.getCanonicalPath() + ";" + saJdiJar.getCanonicalPath() + ";" + getJarPath() + "\"");
    }


    private static ProcessBuilder prepare9(String javaHome) throws Exception {
        return new ProcessBuilder(javaHome + "/bin/java",
                "--add-modules", "jdk.hotspot.agent",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot=ALL-UNNAMED",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot.runtime=ALL-UNNAMED",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot.memory=ALL-UNNAMED",
                "--add-opens", "jdk.hotspot.agent/sun.jvm.hotspot.oops=ALL-UNNAMED",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot.utilities=ALL-UNNAMED",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot.debugger=ALL-UNNAMED",
                "-cp", "\"" + getJarPath() + "\"");
    }

    private static ProcessBuilder prepare10(String javaHome) throws Exception {
        return new ProcessBuilder(javaHome + "/bin/java",
                "--add-modules", "jdk.hotspot.agent",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot=ALL-UNNAMED",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot.runtime=ALL-UNNAMED",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot.memory=ALL-UNNAMED",
                "--add-opens", "jdk.hotspot.agent/sun.jvm.hotspot.oops=ALL-UNNAMED",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot.utilities=ALL-UNNAMED",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot.debugger=ALL-UNNAMED",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot.classfile=ALL-UNNAMED",
                "-cp", "\"" + getJarPath() + "\"");
    }

    private static String getJarPath() {
//        return new File(SaJdwp.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath(); // does not work in IDEA
        String path = "/" + SaJdwp.class.getName().replace('.', '/') + ".class";
        String classResource = SaJdwp.class.getResource(path).getFile();
        String jarPath;
        if (classResource.startsWith("file:")) {
            jarPath = classResource.substring(5, classResource.length() - path.length() - 1);
        } else { //debug mode
            jarPath = "out/artifacts/sa-jdwp.jar";
        }
        return new File(jarPath).getAbsolutePath();
    }

    private static String startServer(ProcessBuilder builder, boolean console) throws IOException {
        builder.redirectErrorStream(true);
        if (console) {
            System.out.println("Running: ");
            for (String s : builder.command()) {
                System.out.print(s + " ");
            }
            System.out.println();
        }
        final Process process = builder.start();

        if (console) {
            Runtime.getRuntime().addShutdownHook(new Thread(
                    new Runnable() {
                        public void run() {
                            process.destroy();
                        }
                    }));
        }

        BufferedReader stdOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String s;
        try {
            while ((s = stdOutput.readLine()) != null) {
                if (console) {
                    System.out.println(s);
                } else if (s.startsWith(SaJdwpServer.WAITING_FOR_DEBUGGER)) {
                    return s.substring(SaJdwpServer.WAITING_FOR_DEBUGGER.length());
                }
            }
        } finally {
            stdOutput.close();
        }
        if (!console) {
            throw new IllegalStateException("Unable to determine the attach address");
        }
        return "";
    }
}
