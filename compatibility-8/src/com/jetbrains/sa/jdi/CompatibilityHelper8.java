package com.jetbrains.sa.jdi;

import com.sun.jdi.ReferenceType;
import sun.jvm.hotspot.debugger.Address;
import sun.jvm.hotspot.memory.SystemDictionary;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.VM;
import sun.jvm.hotspot.utilities.KlassArray;

import java.util.ArrayList;
import java.util.List;

/**
 * @author egor
 */
@SuppressWarnings("unused")
class CompatibilityHelper8 implements Compatibility {
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
        List<InstanceKlass> res = new ArrayList<>();
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
            coff = ((Field) field.get(null)).getOffset();
            java.lang.reflect.Field field1 = Oop.class.getDeclaredField("compressedKlass");
            field1.setAccessible(true);
            koff = ((Field) field1.get(null)).getOffset();
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
        final List<Klass> saKlasses = new ArrayList<>();
        SystemDictionary.ClassVisitor visitor = k -> {
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
        final List<ReferenceType> res = new ArrayList<>();

        // refer to getClassLoaderClasses in jvmtiGetLoadedClasses.cpp
        //  a. SystemDictionary::classes_do doesn't include arrays of primitive types (any dimensions)
        SystemDictionary sysDict = vm.saSystemDictionary();
        sysDict.classesDo((k, loader) -> {
            if (ref.equals(loader)) {
                for (Klass l = k; l != null; l = l.arrayKlassOrNull()) {
                    res.add(vm.referenceType(l));
                }
            }
        });

        // b. multi dimensional arrays of primitive types
        sysDict.primArrayClassesDo((k, loader) -> {
            if (ref.equals(loader)) {
                res.add(vm.referenceType(k));
            }
        });

        // c. single dimensional primitive array klasses from Universe
        // these are not added to SystemDictionary
        vm.saUniverse().basicTypeClassesDo(k -> res.add(vm.referenceType(k)));

        return res;
    }
}
