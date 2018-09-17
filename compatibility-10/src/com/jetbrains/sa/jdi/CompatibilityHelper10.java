package com.jetbrains.sa.jdi;

import com.sun.jdi.ReferenceType;
import sun.jvm.hotspot.classfile.ClassLoaderDataGraph;
import sun.jvm.hotspot.memory.SystemDictionary;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.VM;
import sun.jvm.hotspot.debugger.Address;
import sun.jvm.hotspot.utilities.KlassArray;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
class CompatibilityHelper10 implements Compatibility {
    @Override
    public Address getAddress(Method method) {
        return method.getAddress();
    }

    @Override
    public Address getAddress(Klass klass) {
        return klass.getAddress();
    }

    @Override
    public Klass asKlass(Oop ref) {
        return java_lang_Class.asKlass(ref);
    }

    @Override
    public List<InstanceKlass> getTransitiveInterfaces(InstanceKlass saKlass) {
        List<InstanceKlass> res = new ArrayList<InstanceKlass>();
        KlassArray interfaces = saKlass.getTransitiveInterfaces();
        int n = interfaces.length();
        for (int i = 0; i < n; i++) {
            res.add((InstanceKlass) interfaces.getAt(i));
        }
        return res;
    }

    @Override
    public String getSourceDebugExtension(InstanceKlass saKlass) {
        return saKlass.getSourceDebugExtension();
    }

    @Override
    public InstanceKlass getMethodHandleKlass() {
        return SystemDictionary.getMethodHandleKlass();
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
            coff = ((MetadataField) field.get(null)).getOffset();
            java.lang.reflect.Field field1 = Oop.class.getDeclaredField("compressedKlass");
            field1.setAccessible(true);
            koff = ((MetadataField) field1.get(null)).getOffset();
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
        compressedKlassOffset = coff;
        klassOffset = koff;
    }

    @Override
    public Address getKlassAddress(Oop oop) {
        if (VM.getVM().isCompressedKlassPointersEnabled()) {
            return oop.getHandle().getCompKlassAddressAt(compressedKlassOffset);
        } else {
            return oop.getHandle().getAddressAt(klassOffset);
        }
    }

    @Override
    public List<Klass> allClasses(SystemDictionary systemDictionary, VM vm) {
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

    @Override
    public List<ReferenceType> visibleClasses(final Oop ref, final VirtualMachineImpl vm) {
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
