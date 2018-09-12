package com.intellij.rt.sa.jdwp;

/**
 * @author egor
 */
public interface Command {
    void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command);
}
