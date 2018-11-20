/*
 * Copyright (C) 2018 JetBrains s.r.o.
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License v2 with Classpath Exception.
 * The text of the license is available in the file LICENSE.TXT.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See LICENSE.TXT for more details.
 *
 * You may contact JetBrains s.r.o. at Na Hřebenech II 1718/10, 140 00 Prague,
 * Czech Republic or at legal@jetbrains.com.
 */

package com.jetbrains.sa;

import com.jetbrains.sa.jdi.VirtualMachineImpl;
import com.jetbrains.sa.jdwp.JDWPProxy;
import com.sun.jdi.connect.spi.Connection;
import com.sun.jdi.connect.spi.TransportService;
import com.sun.tools.jdi.SocketTransportService;

import java.io.IOException;

public class SaJdwpListeningServer {
    static final String WAITING_FOR_DEBUGGER = "Waiting for debugger on: ";

    // do not allow instance creation
    private SaJdwpListeningServer() {
    }

    public static void main(String[] args) throws Exception {
        // By default SA agent classes prefer Windows process debugger
        // to windbg debugger. SA expects special properties to be set
        // to choose other debuggers. We will set those here before
        // attaching to SA agent.

        System.setProperty("sun.jvm.hotspot.debugger.useWindbgDebugger", "true");

        final VirtualMachineImpl vm = SaJdwpUtils.createVirtualMachine(args[0]);

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
                        } catch (IllegalArgumentException ignored) {
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
