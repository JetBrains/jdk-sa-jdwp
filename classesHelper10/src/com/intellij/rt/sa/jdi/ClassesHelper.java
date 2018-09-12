package com.intellij.rt.sa.jdi;

import com.sun.jdi.ReferenceType;
import sun.jvm.hotspot.classfile.ClassLoaderDataGraph;
import sun.jvm.hotspot.memory.SystemDictionary;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.VM;

import java.util.ArrayList;
import java.util.List;

class ClassesHelper {
    static List<Klass> allClasses(SystemDictionary systemDictionary, VM vm) {
        final List<Klass> saKlasses = new ArrayList<Klass>();
        ClassLoaderDataGraph.ClassVisitor visitor = new ClassLoaderDataGraph.ClassVisitor() {
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
        vm.getClassLoaderDataGraph().classesDo(visitor);

        // From SystemDictionary we do not get primitive single
        // dimensional array classes. add primitive single dimensional array
        // klasses from Universe.
//        vm.getUniverse().basicTypeClassesDo(visitor);
        ObjectHeap objectHeap = vm.getObjectHeap();

        visitor.visit(objectHeap.getBoolArrayKlassObj());
        visitor.visit(objectHeap.getByteArrayKlassObj());
        visitor.visit(objectHeap.getCharArrayKlassObj());
        visitor.visit(objectHeap.getIntArrayKlassObj());
        visitor.visit(objectHeap.getShortArrayKlassObj());
        visitor.visit(objectHeap.getLongArrayKlassObj());
        visitor.visit(objectHeap.getSingleArrayKlassObj());
        visitor.visit(objectHeap.getDoubleArrayKlassObj());

        return saKlasses;
    }

    static List<ReferenceType> visibleClasses(final Oop ref, final VirtualMachineImpl vm) {
        final List<ReferenceType> res = new ArrayList<ReferenceType>();

        // refer to getClassLoaderClasses in jvmtiGetLoadedClasses.cpp
        //  a. SystemDictionary::classes_do doesn't include arrays of primitive types (any dimensions)
        SystemDictionary sysDict = vm.saSystemDictionary();
        vm.saVM().getClassLoaderDataGraph().allEntriesDo(
                new ClassLoaderDataGraph.ClassAndLoaderVisitor() {
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
//        sysDict.primArrayClassesDo(
//                new ClassLoaderDataGraph.ClassAndLoaderVisitor() {
//                    public void visit(Klass k, Oop loader) {
//                        if (ref.equals(loader)) {
//                            res.add(vm.referenceType(k));
//                        }
//                    }
//                }
//        );

        // c. single dimensional primitive array klasses from Universe
        // these are not added to SystemDictionary
//        vm.saUniverse().basicTypeClassesDo(
//                new ClassLoaderDataGraph.ClassVisitor() {
//                    public void visit(Klass k) {
//                        res.add(vm.referenceType(k));
//                    }
//                }
//        );
        ObjectHeap objectHeap = vm.saObjectHeap();

        res.add(vm.referenceType(objectHeap.getBoolArrayKlassObj()));
        res.add(vm.referenceType(objectHeap.getByteArrayKlassObj()));
        res.add(vm.referenceType(objectHeap.getCharArrayKlassObj()));
        res.add(vm.referenceType(objectHeap.getIntArrayKlassObj()));
        res.add(vm.referenceType(objectHeap.getShortArrayKlassObj()));
        res.add(vm.referenceType(objectHeap.getLongArrayKlassObj()));
        res.add(vm.referenceType(objectHeap.getSingleArrayKlassObj()));
        res.add(vm.referenceType(objectHeap.getDoubleArrayKlassObj()));

        return res;
    }
}
