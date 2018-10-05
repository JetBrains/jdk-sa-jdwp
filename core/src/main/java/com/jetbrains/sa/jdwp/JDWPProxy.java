package com.jetbrains.sa.jdwp;

import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.connect.spi.Connection;

import java.io.IOException;
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
