/*
 * Copyright (c) 2002, 2011, Oracle and/or its affiliates. All rights reserved.
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

import sun.jvm.hotspot.oops.FieldIdentifier;
import sun.jvm.hotspot.oops.Oop;
import sun.jvm.hotspot.oops.Symbol;

public class FieldImpl extends TypeComponentImpl {
    private sun.jvm.hotspot.oops.Field saField;

    FieldImpl( VirtualMachineImpl vm, ReferenceTypeImpl declaringType, sun.jvm.hotspot.oops.Field saField) {
        super(vm, declaringType);
        this.saField = saField;
        signature = saField.getSignature().asString();
    }

    sun.jvm.hotspot.oops.Field ref() {
        return saField;
    }

    public long uniqueID() {
        return hashCode(); // should be ok
    }

    // get the value of static field
    ValueImpl getValue() {
        return getValue(saField.getFieldHolder().getJavaMirror());
    }

    // get the value of this Field from a specific Oop
    ValueImpl getValue(Oop target) {
        ValueImpl valueImpl;
        sun.jvm.hotspot.oops.Field saField = ref();
        sun.jvm.hotspot.oops.FieldType ft = saField.getFieldType();
        if (ft.isArray()) {
            valueImpl = vm.objectMirror(((sun.jvm.hotspot.oops.OopField)saField).getValueAsOopHandle(target));
        } else if (ft.isObject()) {
            valueImpl = vm.objectMirror(((sun.jvm.hotspot.oops.OopField)saField).getValueAsOopHandle(target));
        } else if (ft.isByte()) {
            valueImpl = vm.mirrorOf(((sun.jvm.hotspot.oops.ByteField)saField).getValue(target));
        } else if (ft.isChar()) {
            valueImpl = vm.mirrorOf(((sun.jvm.hotspot.oops.CharField)saField).getValue(target));
        } else if (ft.isDouble()) {
            valueImpl = vm.mirrorOf(((sun.jvm.hotspot.oops.DoubleField)saField).getValue(target));
        } else if (ft.isFloat()) {
            valueImpl = vm.mirrorOf(((sun.jvm.hotspot.oops.FloatField)saField).getValue(target));
        } else if (ft.isInt()) {
            valueImpl = vm.mirrorOf(((sun.jvm.hotspot.oops.IntField)saField).getValue(target));
        } else if (ft.isLong()) {
            valueImpl = vm.mirrorOf(((sun.jvm.hotspot.oops.LongField)saField).getValue(target));
        } else if (ft.isShort()) {
            valueImpl = vm.mirrorOf(((sun.jvm.hotspot.oops.ShortField)saField).getValue(target));
        } else if (ft.isBoolean()) {
            valueImpl = vm.mirrorOf(((sun.jvm.hotspot.oops.BooleanField)saField).getValue(target));
        } else {
            throw new RuntimeException("Should not reach here");
        }
        return valueImpl;
    }

    public boolean equals(Object obj) {
        if ((obj instanceof FieldImpl)) {
            FieldImpl other = (FieldImpl)obj;
            return (declaringType().equals(other.declaringType())) &&
                (ref().equals(other.ref())) &&
                super.equals(obj);
        } else {
            return false;
        }
    }

    public String genericSignature() {
        Symbol genSig = saField.getGenericSignature();
        return (genSig != null)? genSig.asString() : null;
    }

    // from interface Mirror
    public String toString() {
        return declaringType().name() + '.' + name();
    }

    public String name() {
        FieldIdentifier myName =  saField.getID();
        return myName.getName();
    }

    // From interface Accessible
    public int modifiers() {
        return saField.getAccessFlagsObj().getStandardFlags();
    }

    public boolean isStatic() {
        return saField.isStatic();
    }

    public int hashCode() {
        return saField.hashCode();
    }
}
