package com.intellij.rt.sa;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.Transport;
import com.sun.jdi.connect.spi.Connection;
import com.sun.tools.jdi.SocketTransportService;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * @author egor
 */
public class SAJDWPAttachingConnector implements AttachingConnector {
    private SocketTransportService transportService = new SocketTransportService();

    public SAJDWPAttachingConnector() {
        super();
    }

    @Override
    public Map<String, Argument> defaultArguments() {
        Argument pidArgument = new PidArgument();
        return Collections.singletonMap(pidArgument.name(), pidArgument);
    }

    public VirtualMachine attach(Map<String, ? extends Argument> arguments) throws IOException,
            IllegalConnectorArgumentsException {
        int pid = 0;
        try {
            pid = Integer.parseInt(arguments.get("pid").value());
        } catch (NumberFormatException nfe) {
            throw (IllegalConnectorArgumentsException) new IllegalConnectorArgumentsException
                    (nfe.getMessage(), "pid").initCause(nfe);
        }

        try {
            String address = SaJdwp.startServer(String.valueOf(pid), "");
            Connection connection = transportService.attach(address, 0, 0);
            return Bootstrap.virtualMachineManager().createVirtualMachine(connection);
        } catch (Exception e) {
            throw new IOException("Exception while starting sa-jdwp server", e);
        }
    }

    public String name() {
        return SAJDWPAttachingConnector.class.getName();
    }

    public String description() {
        return "This connector allows you to attach to a Java process using the sa-jdwp";
    }

    public Transport transport() {
        return new Transport() {
            @Override
            public String name() {
                return transportService.name();
            }
        };
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
