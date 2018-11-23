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

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotLoadedException;
import sun.jvm.hotspot.oops.Symbol;

import java.util.ArrayList;
import java.util.List;

public abstract class MethodImpl extends TypeComponentImpl {
    private JNITypeParser signatureParser;
    protected sun.jvm.hotspot.oops.Method saMethod;

    public abstract int argSlotCount();
    abstract List<LocationImpl> allLineLocations(SDE.Stratum stratum, String sourceName) throws AbsentInformationException;
    abstract List<LocationImpl> locationsOfLine(SDE.Stratum stratum, String sourceName, int lineNumber) throws AbsentInformationException;

    static MethodImpl createMethodImpl(VirtualMachineImpl vm, ReferenceTypeImpl declaringType,
                                       sun.jvm.hotspot.oops.Method saMethod) {
        // Someday might have to add concrete and non-concrete subclasses.
        if (saMethod.isNative() || saMethod.isAbstract()) {
            return new NonConcreteMethodImpl(vm, declaringType, saMethod);
        } else {
            return new ConcreteMethodImpl(vm, declaringType, saMethod);
        }
    }

    MethodImpl(VirtualMachineImpl vm, ReferenceTypeImpl declaringType,
               sun.jvm.hotspot.oops.Method saMethod ) {
        super(vm, declaringType);
        this.saMethod = saMethod;
        getParser();
    }

    private JNITypeParser getParser() {
        if (signatureParser == null) {
            Symbol sig1 = saMethod.getSignature();
            signature = sig1.asString();
            signatureParser = new JNITypeParser(signature);
        }
        return signatureParser;
    }

    // Object ref() {
    public sun.jvm.hotspot.oops.Method ref() {
        return saMethod;
    }

    public long uniqueID() {
        return vm.getAddressValue(CompatibilityHelper.INSTANCE.getAddress(saMethod));
    }

    public String genericSignature() {
        Symbol genSig = saMethod.getGenericSignature();
        return (genSig != null)? genSig.asString() : null;
    }

    public String returnTypeName() {
        return getParser().typeName();
    }

    public TypeImpl returnType() throws ClassNotLoadedException {
        return findType(getParser().signature());
    }

    private TypeImpl findType(String signature) throws ClassNotLoadedException {
        ReferenceTypeImpl enclosing = declaringType();
        return enclosing.findType(signature);
    }

    public List<String> argumentTypeNames() {
        return getParser().argumentTypeNames();
    }

    List<String> argumentSignatures() {
        return getParser().argumentSignatures();
    }

    TypeImpl argumentType(int index) throws ClassNotLoadedException {
        ReferenceTypeImpl enclosing = declaringType();
        String signature = argumentSignatures().get(index);
        return enclosing.findType(signature);
    }

    public List<TypeImpl> argumentTypes() throws ClassNotLoadedException {
        int size = argumentSignatures().size();
        ArrayList<TypeImpl> types = new ArrayList<TypeImpl>(size);
        for (int i = 0; i < size; i++) {
            TypeImpl type = argumentType(i);
            types.add(type);
        }
        return types;
    }

    public boolean isAbstract() {
        return saMethod.isAbstract();
    }

    public boolean isBridge() {
        return saMethod.isBridge();
    }

    public boolean isSynchronized() {
        return saMethod.isSynchronized();
    }

    public boolean isNative() {
        return saMethod.isNative();
    }

    public boolean isVarArgs() {
        return saMethod.isVarArgs();
    }

    public boolean isConstructor() {
        return saMethod.isConstructor();
    }

    public boolean isStaticInitializer() {
        return saMethod.isStaticInitializer();
    }

    public boolean isObsolete() {
        return saMethod.isObsolete();
    }

    public final List<LocationImpl> allLineLocations()
                           throws AbsentInformationException {
        return allLineLocations(vm.getDefaultStratum(), null);
    }

    public List<LocationImpl> allLineLocations(String stratumID,
                                 String sourceName)
                           throws AbsentInformationException {
        return allLineLocations(declaringType.stratum(stratumID), sourceName);
    }

    public final List<LocationImpl> locationsOfLine(int lineNumber)
                           throws AbsentInformationException {
        return locationsOfLine(vm.getDefaultStratum(), null, lineNumber);
    }

    public List<LocationImpl> locationsOfLine(String stratumID,
                                String sourceName,
                                int lineNumber)
                           throws AbsentInformationException {
        return locationsOfLine(declaringType.stratum(stratumID), sourceName, lineNumber);
    }

    LineInfo codeIndexToLineInfo(SDE.Stratum stratum,
                                 long codeIndex) {
        if (stratum.isJava()) {
            return new BaseLineInfo(-1, declaringType);
        } else {
            return new StratumLineInfo(stratum.id(), -1, null, null);
        }
    }

    public boolean equals(Object obj) {
        if ((obj instanceof MethodImpl)) {
            MethodImpl other = (MethodImpl)obj;
            return (declaringType().equals(other.declaringType())) &&
                (ref().equals(other.ref())) &&
                super.equals(obj);
        } else {
            return false;
        }
    }

    // From interface Comparable
    public int compareTo(MethodImpl method) {
        ReferenceTypeImpl declaringType = declaringType();
         int rc = declaringType.compareTo(method.declaringType());
         if (rc == 0) {
           rc = declaringType.indexOf(this) -
               declaringType.indexOf(method);
         }
         return rc;
    }

    // from interface Mirror
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(declaringType().name());
        sb.append(".");
        sb.append(name());
        sb.append("(");
        boolean first = true;
        for (Object o : argumentTypeNames()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append((String) o);
            first = false;
        }
        sb.append(")");
        return sb.toString();
    }

    public String name() {
        Symbol myName = saMethod.getName();
        return myName.asString();
    }

    public int modifiers() {
        return saMethod.getAccessFlagsObj().getStandardFlags();
    }

    public boolean isPackagePrivate() {
        return saMethod.isPackagePrivate();
    }

    public boolean isPrivate() {
        return saMethod.isPrivate();
    }

    public boolean isProtected() {
        return saMethod.isProtected();
    }

    public boolean isPublic() {
        return saMethod.isPublic();
    }

    public boolean isStatic() {
        return saMethod.isStatic();
    }

    public boolean isSynthetic() {
        return saMethod.isSynthetic();
    }

    public boolean isFinal() {
        return saMethod.isFinal();
    }

    public int hashCode() {
        return saMethod.hashCode();
    }

    abstract public List<LocalVariableImpl> variables() throws AbsentInformationException;

    abstract public byte[] bytecodes();
}
