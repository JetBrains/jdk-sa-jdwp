package com.jetbrains.sa;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.tools.jdi.SocketListeningConnector;

import java.io.IOException;
import java.util.Map;

/**
 * @author egor
 */
public class SAJDWPListeningConnector extends SocketListeningConnector {
    private String myCurrentAddress;

    @Override
    public Map defaultArguments() {
        Map map = super.defaultArguments();
        Argument pidArgument = new PidArgument();
        map.put(pidArgument.name(), pidArgument);
        return map;
    }

    @Override
    public boolean supportsMultipleConnections() {
        return false;
    }

    @Override
    public String startListening(Map<String, ? extends Argument> arguments) throws IOException, IllegalConnectorArgumentsException {
        myCurrentAddress = super.startListening(null, arguments);
        return myCurrentAddress;
    }

    @Override
    public VirtualMachine accept(Map<String, ? extends Argument> map) throws IOException, IllegalConnectorArgumentsException {
        int pid = 0;
        try {
            pid = Integer.parseInt(map.get("pid").value());
        } catch (NumberFormatException nfe) {
            throw (IllegalConnectorArgumentsException) new IllegalConnectorArgumentsException
                    (nfe.getMessage(), "pid").initCause(nfe);
        }

        try {
            SaJdwp.startServer(String.valueOf(pid), myCurrentAddress, false, false);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Unable to start sa-jdwp server", e);
        }

        return super.accept(map);
    }

    public String name() {
        return SAJDWPListeningConnector.class.getName();
    }

    public String description() {
        return "This connector allows you to attach to a Java process using the sa-jdwp";
    }

    @Override
    public String toString() {
        return super.toString();
    }

    private static class PidArgument implements StringArgument {
        private String value;

        @Override
        public boolean isValid(String s) {
            return true;
        }

        @Override
        public String name() {
            return "pid";
        }

        @Override
        public String label() {
            return "PID";
        }

        @Override
        public String description() {
            return "PID of a Java process";
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public void setValue(String s) {
            value = s;
        }

        @Override
        public boolean mustSpecify() {
            return true;
        }
    }
}
