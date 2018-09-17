package com.jetbrains.sa.jdi;

import com.sun.jdi.ReferenceType;
import sun.jvm.hotspot.debugger.Address;
import sun.jvm.hotspot.memory.SystemDictionary;
import sun.jvm.hotspot.oops.InstanceKlass;
import sun.jvm.hotspot.oops.Klass;
import sun.jvm.hotspot.oops.Method;
import sun.jvm.hotspot.oops.Oop;
import sun.jvm.hotspot.runtime.VM;

import java.util.List;

/**
 * @author egor
 */
public interface Compatibility {
    Address getAddress(Method method);

    Address getAddress(Klass klass);

    boolean isCompressedKlassPointersEnabled(VM vm);

    Klass asKlass(Oop ref);

    List<InstanceKlass> getTransitiveInterfaces(InstanceKlass saKlass);

    String getSourceDebugExtension(InstanceKlass saKlass);

    InstanceKlass getMethodHandleKlass();

    Klass getMethodHolder(Method method);

    Address getKlassAddress(boolean compressedKlassPointersEnabled, Oop oop);

    List<Klass> allClasses(SystemDictionary systemDictionary, VM vm);

    List<ReferenceType> visibleClasses(final Oop ref, final VirtualMachineImpl vm);
}
