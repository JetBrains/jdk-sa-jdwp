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

import com.sun.jdi.connect.spi.Connection;

import java.io.IOException;

public class VirtualMachineImpl {
    private Connection myConnection;
    public com.jetbrains.sa.jdi.VirtualMachineImpl vm;

    int sizeofFieldRef = 8;
    int sizeofMethodRef = 8;
    int sizeofObjectRef = 8;
    int sizeofClassRef = 8;
    int sizeofFrameRef = 8;

    public VirtualMachineImpl(Connection myConnection, com.jetbrains.sa.jdi.VirtualMachineImpl vm) {
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