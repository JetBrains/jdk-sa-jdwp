package com.jetbrains.sa;

import com.jetbrains.sa.jdi.VirtualMachineImpl;
import com.jetbrains.sa.jdwp.JDWPProxy;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.connect.spi.Connection;
import com.sun.jdi.connect.spi.TransportService;
import com.sun.tools.jdi.SocketTransportService;

import java.io.IOException;

public class SaJdwpServer {
    public static final String WAITING_FOR_DEBUGGER = "Waiting for debugger on: ";

    // do not allow instance creation
    private SaJdwpServer() {
    }

    public static void main(String[] args) throws Exception {
        // By default SA agent classes prefer Windows process debugger
        // to windbg debugger. SA expects special properties to be set
        // to choose other debuggers. We will set those here before
        // attaching to SA agent.

        System.setProperty("sun.jvm.hotspot.debugger.useWindbgDebugger", "true");

        final VirtualMachineImpl vm = VirtualMachineImpl.createVirtualMachineForPID(Bootstrap.virtualMachineManager(), Integer.parseInt(args[0]), 0);

        final SocketTransportService socketTransportService = new SocketTransportService();
        final TransportService.ListenKey listenKey = socketTransportService.startListening(args.length > 1 ? args[1] : null);

        System.err.println(WAITING_FOR_DEBUGGER + listenKey.address());

        // shutdown hook to clean-up the server in case of forced exit.
        Runtime.getRuntime().addShutdownHook(new Thread(
                new Runnable() {
                    public void run() {
                        try {
                            vm.dispose();
                            socketTransportService.stopListening(listenKey);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }));

        Connection connection = socketTransportService.accept(listenKey, 0, 0);
        socketTransportService.stopListening(listenKey);
        JDWPProxy.reply(connection, vm);
    }
}
