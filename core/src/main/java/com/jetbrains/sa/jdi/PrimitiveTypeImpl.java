/*
 * Copyright (c) 2002, 2003, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
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

package com.jetbrains.sa.jdi;

import com.jetbrains.sa.jdwp.JDWP;
import com.sun.jdi.*;

class PrimitiveTypeImpl extends TypeImpl implements PrimitiveType {
    private final String signature;

    PrimitiveTypeImpl(VirtualMachine vm, String signature, byte tag) {
        super(vm, tag);
        this.signature = signature;
    }

    @Override
    public String signature() {
        return signature;
    }

    public String toString() {
        return name();
    }

    static class Boolean extends PrimitiveTypeImpl implements com.sun.jdi.BooleanType {
        Boolean(VirtualMachine vm) {
            super(vm, "Z", JDWP.Tag.BOOLEAN);
        }
    }

    static class Byte extends PrimitiveTypeImpl implements com.sun.jdi.ByteType {
        Byte(VirtualMachine vm) {
            super(vm, "B", JDWP.Tag.BYTE);
        }
    }

    static class Char extends PrimitiveTypeImpl implements com.sun.jdi.CharType {
        Char(VirtualMachine vm) {
            super(vm, "C", JDWP.Tag.CHAR);
        }
    }

    static class Double extends PrimitiveTypeImpl implements DoubleType {
        Double(VirtualMachine vm) {
            super(vm, "D", JDWP.Tag.DOUBLE);
        }
    }

    static class Float extends PrimitiveTypeImpl implements FloatType {
        Float(VirtualMachine vm) {
            super(vm, "F", JDWP.Tag.FLOAT);
        }
    }

    static class Integer extends PrimitiveTypeImpl implements IntegerType {
        Integer(VirtualMachine vm) {
            super(vm, "I", JDWP.Tag.INT);
        }
    }

    static class Long extends PrimitiveTypeImpl implements LongType {
        Long(VirtualMachine vm) {
            super(vm, "J", JDWP.Tag.LONG);
        }
    }

    static class Short extends PrimitiveTypeImpl implements ShortType {
        Short(VirtualMachine vm) {
            super(vm, "S", JDWP.Tag.SHORT);
        }
    }
}
