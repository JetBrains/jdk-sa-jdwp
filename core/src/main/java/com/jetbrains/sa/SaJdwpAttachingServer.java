package com.jetbrains.sa;

import com.jetbrains.sa.jdi.VirtualMachineImpl;
import com.jetbrains.sa.jdwp.JDWPProxy;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.connect.spi.Connection;
import com.sun.tools.jdi.SocketTransportService;

public class SaJdwpAttachingServer {
    // do not allow instance creation
    private SaJdwpAttachingServer() {
    }

    public static void main(String[] args) throws Exception {
        // By default SA agent classes prefer Windows process debugger
        // to windbg debugger. SA expects special properties to be set
        // to choose other debuggers. We will set those here before
        // attaching to SA agent.

        System.setProperty("sun.jvm.hotspot.debugger.useWindbgDebugger", "true");

        final VirtualMachineImpl vm = VirtualMachineImpl.createVirtualMachineForPID(Bootstrap.virtualMachineManager(), Integer.parseInt(args[0]), 0);

        String address = args[1];
        System.out.println("Connecting to " + address);

        final SocketTransportService socketTransportService = new SocketTransportService();
        final Connection connection = socketTransportService.attach(address, 0, 0);

        System.out.println("Connected to " + address);

        // shutdown hook to clean-up the server in case of forced exit.
        Runtime.getRuntime().addShutdownHook(new Thread(
                new Runnable() {
                    public void run() {
                        try {
                            vm.dispose();
                            connection.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }));

        JDWPProxy.reply(connection, vm);
    }
}
