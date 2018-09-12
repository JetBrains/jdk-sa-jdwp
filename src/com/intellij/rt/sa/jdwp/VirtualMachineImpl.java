package com.intellij.rt.sa.jdwp;

import com.sun.jdi.connect.spi.Connection;

import java.io.IOException;

public class VirtualMachineImpl {
    private Connection myConnection;
    public com.intellij.rt.sa.jdi.VirtualMachineImpl vm;

    int sizeofFieldRef = 8;
    int sizeofMethodRef = 8;
    int sizeofObjectRef = 8;
    int sizeofClassRef = 8;
    int sizeofFrameRef = 8;

    public VirtualMachineImpl(Connection myConnection, com.intellij.rt.sa.jdi.VirtualMachineImpl vm) {
        this.myConnection = myConnection;
        this.vm = vm;
    }

    void sendToTarget(Packet pkt) {
        try {
            myConnection.writePacket(pkt.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}