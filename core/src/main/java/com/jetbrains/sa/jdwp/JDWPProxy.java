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

package com.jetbrains.sa.jdwp;

import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.connect.spi.Connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author egor
 */
public class JDWPProxy {
    private static Map<Integer, Map<Integer, Command>> COMMANDS = new HashMap<Integer, Map<Integer, Command>>();

    static {
        try {
            Class<?>[] declaredClasses = JDWP.class.getDeclaredClasses();
            for (Class<?> declaredClass : declaredClasses) {
                try {
                    int setId = (Integer) declaredClass.getDeclaredField("COMMAND_SET").get(null);
                    Class<?>[] commandsClasses = declaredClass.getDeclaredClasses();
                    HashMap<Integer, Command> commandsMap = new HashMap<Integer, Command>();
                    COMMANDS.put(setId, commandsMap);
                    for (Class<?> commandsClass : commandsClasses) {
                        try {
                            int commandId = (Integer) commandsClass.getDeclaredField("COMMAND").get(null);
                            commandsMap.put(commandId, (Command) commandsClass.newInstance());
                        } catch (NoSuchFieldException ignored) {
                        }
                    }
                } catch (NoSuchFieldException ignored) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void reply(Connection connection, com.jetbrains.sa.jdi.VirtualMachineImpl vm) throws IOException {
        VirtualMachineImpl virtualMachine = new VirtualMachineImpl(connection, vm);

//        sendVMStart(virtualMachine);

        try {
            while (true) {
                byte[] b = connection.readPacket();
                Packet p = Packet.fromByteArray(b);
                int cmdSet = p.cmdSet;
                int cmd = p.cmd;
                PacketStream packetStream = new PacketStream(virtualMachine, p.id, cmdSet, cmd);
                Command command = COMMANDS.get(cmdSet).get(cmd);
                try {
                    command.reply(virtualMachine, packetStream, new PacketStream(virtualMachine, p));
                } catch (VMDisconnectedException vde) {
                    throw  vde;
                } catch (Exception e) {
                    e.printStackTrace();
                    packetStream.pkt.errorCode = JDWP.Error.INTERNAL;
                    packetStream.dataStream.reset();

                    // serialize the original exception as a utf8 string
                    try {
                        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                        PrintStream printStream = new PrintStream(byteStream, false, "UTF8");
                        e.printStackTrace(printStream);
                        printStream.close();
                        packetStream.writeString(byteStream.toString("UTF8"));
                    } catch (Exception ignored) {
                    }
                }
                packetStream.send();
            }
        } catch (VMDisconnectedException ignored) {
        } finally {
            connection.close();
            //todo: dispose breaks subsequent connections, need to investigate
            vm.dispose();
        }
    }

    private static void sendVMStart(VirtualMachineImpl virtualMachine) {
        PacketStream packetStream = new PacketStream(virtualMachine, 0, 64, 100);
        packetStream.pkt.flags = Packet.NoFlags;
        packetStream.writeByte((byte) JDWP.SuspendPolicy.ALL);
        packetStream.writeInt(1);
        packetStream.writeByte((byte) JDWP.EventKind.VM_START);
        packetStream.writeInt(0);
        packetStream.writeObjectRef(0);
        packetStream.send();
    }
}
