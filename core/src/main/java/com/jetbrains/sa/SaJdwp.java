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
        String port = args.length > 1 ? args[1] : "";
        List<String> commands = getServerProcessCommand(args[0], port, true, getJarPath());
        try {
            startServer(commands);
        } catch (Exception e) {
            List<String> commandsWithSudo = createSudoCommand(commands);
            if (commandsWithSudo.equals(commands)) {
                throw e;
            }
            System.out.println("Trying with sudo...");
            startServer(commandsWithSudo);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static List<String> getServerProcessCommand(String pidString, String port, boolean server, String pathToJar) throws Exception {
        Properties systemProperties;
        VirtualMachine vm = VirtualMachine.attach(pidString);
        try {
            systemProperties = vm.getSystemProperties();
        } finally {
            vm.detach();
        }
        return getServerProcessCommand(systemProperties, pidString, port, server, pathToJar);
    }

    @SuppressWarnings("WeakerAccess")
    public static List<String> getServerProcessCommand(Properties systemProperties, String pidString, String port, boolean server, String pathToJar) throws Exception {
        String javaHome = systemProperties.getProperty("java.home");
        String version = systemProperties.getProperty("java.specification.version");

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

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.US).startsWith("windows");
    }

    private static void prepare6(List<String> commands, String javaHome, String pathToJar) throws IOException {
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

    private static void startServer(List<String> cmds) throws Exception {
        System.out.println("Running: ");
        for (String s : cmds) {
            System.out.print(s + " ");
        }
        System.out.println();

        ProcessBuilder builder = new ProcessBuilder(cmds);
        builder.redirectErrorStream(true);

        final Process process = builder.start();

        Runtime.getRuntime().addShutdownHook(new Thread(
                new Runnable() {
                    public void run() {
                        process.destroy();
                    }
                }));

        boolean success = false;
        BufferedReader stdOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String s;
        try {
            while ((s = stdOutput.readLine()) != null) {
                System.out.println(s);
                if (s.startsWith(SaJdwpListeningServer.WAITING_FOR_DEBUGGER) || s.startsWith(SaJdwpAttachingServer.SERVER_READY)) {
                    success = true;
                }
            }
        } finally {
            stdOutput.close();
        }
        if (!success) {
            throw new IllegalStateException("Unable to start");
        }
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
