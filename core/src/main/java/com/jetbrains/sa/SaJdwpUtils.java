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
import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachineManager;

import java.util.Locale;

class SaJdwpUtils {
    static VirtualMachineImpl createVirtualMachine(String target) throws Exception {
        VirtualMachineManager virtualMachineManager = Bootstrap.virtualMachineManager();
        try {
            // pid attach
            return VirtualMachineImpl.createVirtualMachineForPID(virtualMachineManager, Integer.parseInt(target), 0);
        } catch (NumberFormatException e) {
            // core attach
            String javaExeName = System.getProperty("java.home") + "/bin/java";
            if (isWindows()) {
                javaExeName += ".exe";
            }
            return VirtualMachineImpl.createVirtualMachineForCorefile(virtualMachineManager, javaExeName, target, 0);
        }
    }

    static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.US).startsWith("windows");
    }
}
