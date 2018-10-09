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

package com.jetbrains.sa.jdi;

import com.sun.jdi.ReferenceType;
import sun.jvm.hotspot.debugger.Address;
import sun.jvm.hotspot.memory.SystemDictionary;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.VM;

import java.util.ArrayList;
import java.util.List;

/**
 * @author egor
 */
class CompatibilityHelper6 implements Compatibility {
    @Override
    public Address getAddress(Method method) {
        return method.getHandle();
    }

    @Override
    public Address getAddress(Klass klass) {
        return klass.getHandle();
    }

    @Override
    public Klass asKlass(Oop ref) {
        return OopUtilities.classOopToKlass(ref);
    }

    @Override
    public List<InstanceKlass> getTransitiveInterfaces(InstanceKlass saKlass) {
        List<InstanceKlass> res = new ArrayList<InstanceKlass>();
        ObjArray interfaces = saKlass.getTransitiveInterfaces();
        long n = interfaces.getLength();
        for (int i = 0; i < n; i++) {
            res.add((InstanceKlass) interfaces.getObjAt(i));
        }
        return res;
    }

    @Override
    public String getSourceDebugExtension(InstanceKlass saKlass) {
        return saKlass.getSourceDebugExtension().asString();
    }

    @Override
    public InstanceKlass getMethodHandleKlass() {
        return null;//SystemDictionary.getMethodHandleKlass();
    }

    @Override
    public Klass getMethodHolder(Method method) {
        return method.getMethodHolder();
    }


    private static final long compressedKlassOffset;
    private static final long klassOffset;

    static {
        long coff = -1;
        long koff = -1;
        try {
            java.lang.reflect.Field field = Oop.class.getDeclaredField("klass");
            field.setAccessible(true);
            coff = ((Field) field.get(null)).getOffset();
            java.lang.reflect.Field field1 = Oop.class.getDeclaredField("compressedKlass");
            field1.setAccessible(true);
            koff = ((Field) field1.get(null)).getOffset();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        compressedKlassOffset = coff;
        klassOffset = koff;
    }

    @Override
    public Address getKlassAddress(Oop oop) {
        if (VM.getVM().isCompressedOopsEnabled()) {
            return oop.getHandle().getCompOopHandleAt(compressedKlassOffset);
        } else {
            return oop.getHandle().getOopHandleAt(klassOffset);
        }
    }

    @Override
    public List<Klass> allClasses(SystemDictionary systemDictionary, VM vm) {
        final List<Klass> saKlasses = new ArrayList<Klass>();
        SystemDictionary.ClassVisitor visitor = new SystemDictionary.ClassVisitor() {
            public void visit(Klass k) {
                for (Klass l = k; l != null; l = l.arrayKlassOrNull()) {
                    // for non-array classes filter out un-prepared classes
                    // refer to 'allClasses' in share/back/VirtualMachineImpl.c
                    if (l instanceof ArrayKlass) {
                        saKlasses.add(l);
                    } else {
                        int status = l.getClassStatus();
                        if ((status & JVMDIClassStatus.PREPARED) != 0) {
                            saKlasses.add(l);
                        }
                    }
                }
            }
        };

        // refer to jvmtiGetLoadedClasses.cpp - getLoadedClasses in VM code.

        // classes from SystemDictionary
        systemDictionary.classesDo(visitor);

        // From SystemDictionary we do not get primitive single
        // dimensional array classes. add primitive single dimensional array
        // klasses from Universe.
        vm.getUniverse().basicTypeClassesDo(visitor);

        return saKlasses;
    }

    @Override
    public List<ReferenceType> visibleClasses(final Oop ref, final VirtualMachineImpl vm) {
        final List<ReferenceType> res = new ArrayList<ReferenceType>();

        // refer to getClassLoaderClasses in jvmtiGetLoadedClasses.cpp
        //  a. SystemDictionary::classes_do doesn't include arrays of primitive types (any dimensions)
        SystemDictionary sysDict = vm.saSystemDictionary();
        sysDict.classesDo(
                new SystemDictionary.ClassAndLoaderVisitor() {
                    public void visit(Klass k, Oop loader) {
                        if (ref.equals(loader)) {
                            for (Klass l = k; l != null; l = l.arrayKlassOrNull()) {
                                res.add(vm.referenceType(l));
                            }
                        }
                    }
                }
        );

        // b. multi dimensional arrays of primitive types
        sysDict.primArrayClassesDo(
                new SystemDictionary.ClassAndLoaderVisitor() {
                    public void visit(Klass k, Oop loader) {
                        if (ref.equals(loader)) {
                            res.add(vm.referenceType(k));
                        }
                    }
                }
        );

        // c. single dimensional primitive array klasses from Universe
        // these are not added to SystemDictionary
        vm.saUniverse().basicTypeClassesDo(
                new SystemDictionary.ClassVisitor() {
                    public void visit(Klass k) {
                        res.add(vm.referenceType(k));
                    }
                }
        );

        return res;
    }
}
