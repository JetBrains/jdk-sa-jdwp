package com.jetbrains.sa;

import com.sun.tools.attach.VirtualMachine;

import java.io.*;
import java.util.*;

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
        startServer(args[0], args.length > 1 ? args[1] : "", true, true);
    }

    @SuppressWarnings("WeakerAccess")
    public static List<String> getServerProcessCommand(String pidString, String port, boolean server, String pathToJar) throws Exception {
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

        List<String> commands = new ArrayList<String>();
        if (version.startsWith("1.6") || version.startsWith("1.7") || version.startsWith("1.8")) {
            prepare6(commands, javaHome, pathToJar);
        } else {
            try {
                int v = Integer.parseInt(version);
                if (v >= 9) {
                    prepare9(commands, javaHome, pathToJar);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (commands.isEmpty()) {
            throw new IllegalStateException("Unable to start on version " + version);
        }

        String serverClassName = server ? SaJdwpListeningServer.class.getName() : SaJdwpAttachingServer.class.getName();
        Collections.addAll(commands, serverClassName, pidString, port);
        return commands;
    }

    static String startServer(String pidString, String port, boolean console, boolean server) throws Exception {
        List<String> commands = getServerProcessCommand(pidString, port, server, getJarPath());
        try {
            return startServer(commands, console, false);
        } catch (Exception e) {
            List<String> commandsWithSudo = createSudoCommand(commands);
            if (commandsWithSudo.equals(commands)) {
                throw e;
            }
            if (console) {
                System.out.println("Trying with sudo...");
            }
            return startServer(commandsWithSudo, console, true);
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.US).startsWith("windows");
    }

    private static void prepare6(List<String> commands, String javaHome, String pathToJar) throws Exception {
        // look for libs
        File toolsJar = new File(javaHome, "lib/tools.jar");
        if (!toolsJar.exists()) {
            toolsJar = new File(javaHome, "../lib/tools.jar");
            if (!toolsJar.exists()) {
                throw new IllegalStateException("JDK not detected, unable to find tools.jar in " + javaHome);
            }
        }
        File saJdiJar = new File(javaHome, "lib/sa-jdi.jar");
        if (!saJdiJar.exists()) {
            saJdiJar = new File(javaHome, "../lib/sa-jdi.jar");
            if (!saJdiJar.exists()) {
                throw new IllegalStateException("JDK not detected, unable to find sa-jdi.jar in " + javaHome);
            }
        }
        Collections.addAll(commands, javaHome + "/bin/java",
                "-cp", toolsJar.getCanonicalPath() + File.pathSeparatorChar
                        + saJdiJar.getCanonicalPath() + File.pathSeparatorChar
                        + pathToJar);
    }


    private static void prepare9(List<String> commands, String javaHome, String pathToJar) {
        // todo: determine if this is a jdk or jre better
        String javac = "javac";
        if (isWindows()) {
            javac += ".exe";
        }
        if (!new File(javaHome, "bin/" + javac).exists()) {
            if (!new File(javaHome, "../bin/" + javac).exists()) {
                throw new IllegalStateException("JDK not detected in " + javaHome + " , unable to attach");
            }
        }

        Collections.addAll(commands,javaHome + "/bin/java",
                "--add-modules", "jdk.hotspot.agent",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot=ALL-UNNAMED",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot.runtime=ALL-UNNAMED",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot.memory=ALL-UNNAMED",
                "--add-opens", "jdk.hotspot.agent/sun.jvm.hotspot.oops=ALL-UNNAMED",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot.utilities=ALL-UNNAMED",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot.debugger=ALL-UNNAMED",
                "--add-exports", "jdk.hotspot.agent/sun.jvm.hotspot.classfile=ALL-UNNAMED", // for jdk 10
                "-cp", pathToJar);
    }

    private static String getJarPath() {
//        return new File(SaJdwp.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath(); // does not work in IDEA
        String path = "/" + SaJdwp.class.getName().replace('.', '/') + ".class";
        String classResource = SaJdwp.class.getResource(path).getFile();
        String jarPath;
        if (classResource.startsWith("file:")) {
            jarPath = classResource.substring(5, classResource.length() - path.length() - 1);
        } else { //debug mode
            jarPath = "build/libs/sa-jdwp.jar";
        }
        return new File(jarPath).getAbsolutePath();
    }

    private static String startServer(List<String> cmds, boolean console, boolean sudo) throws Exception {
        if (console) {
            System.out.println("Running: ");
            for (String s : cmds) {
                System.out.print(s + " ");
            }
            System.out.println();
        }

        ProcessBuilder builder = new ProcessBuilder(cmds);
        builder.redirectErrorStream(true);

        final Process process = builder.start();

        if (console) {
            Runtime.getRuntime().addShutdownHook(new Thread(
                    new Runnable() {
                        public void run() {
                            process.destroy();
                        }
                    }));
        }

        if (!console && sudo) return ""; // sudo mode usually runs in a separate console

        BufferedReader stdOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String s;
        try {
            while ((s = stdOutput.readLine()) != null) {
                if (console) {
                    System.out.println(s);
                } else {
                    output.append('\n').append(s);
                    if (s.startsWith(SaJdwpListeningServer.WAITING_FOR_DEBUGGER)) {
                        return s.substring(SaJdwpListeningServer.WAITING_FOR_DEBUGGER.length());
                    } else if (s.startsWith(SaJdwpAttachingServer.SERVER_READY)) {
                        return "";
                    }
                }
            }
        } finally {
            stdOutput.close();
        }
        String error = "Unable to determine the attach address";
        if (output.length() > 0) {
            error += ", server output is:" + output.toString();
        }
        throw new IllegalStateException(error);
    }

    private static List<String> createSudoCommand(List<String> command) {
        if (isWindows()) {
            return command;
        }
        ArrayList<String> res = new ArrayList<String>(command.size() + 1);
        res.add("sudo");
        res.add("--");
        res.addAll(command);
        return res;
    }
}
