package com.jetbrains.sa.jdi;

import com.sun.jdi.ReferenceType;
import sun.jvm.hotspot.memory.SystemDictionary;
import sun.jvm.hotspot.oops.ArrayKlass;
import sun.jvm.hotspot.oops.JVMDIClassStatus;
import sun.jvm.hotspot.oops.Klass;
import sun.jvm.hotspot.oops.Oop;
import sun.jvm.hotspot.runtime.VM;

import java.util.ArrayList;
import java.util.List;

class ClassesHelper {
    static List<Klass> allClasses(SystemDictionary systemDictionary, VM vm) {
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

    static List<ReferenceType> visibleClasses(final Oop ref, final VirtualMachineImpl vm) {
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
