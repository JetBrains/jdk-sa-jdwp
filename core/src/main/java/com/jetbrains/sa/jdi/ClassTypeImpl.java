/*
 * Copyright (c) 2002, 2007, Oracle and/or its affiliates. All rights reserved.
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
import sun.jvm.hotspot.oops.InstanceKlass;

import java.lang.ref.SoftReference;
import java.util.*;

public class ClassTypeImpl extends ReferenceTypeImpl {
    private SoftReference<List<InterfaceTypeImpl>> interfacesCache    = null;
    private SoftReference<List<InterfaceTypeImpl>> allInterfacesCache = null;
    private SoftReference<List<ClassTypeImpl>> subclassesCache    = null;

    protected ClassTypeImpl(VirtualMachineImpl aVm, InstanceKlass aRef) {
        super(aVm, aRef, JDWP.TypeTag.CLASS);
    }

    public ClassTypeImpl superclass() {
        InstanceKlass kk = (InstanceKlass)ref().getSuper();
        if (kk == null) {
            return null;
        }
        return (ClassTypeImpl) vm.referenceType(kk);
    }

    public List<InterfaceTypeImpl> interfaces()  {
        List<InterfaceTypeImpl> interfaces = (interfacesCache != null)? interfacesCache.get() : null;
        if (interfaces == null) {
            checkPrepared();
            interfaces = Collections.unmodifiableList(getInterfaces());
            interfacesCache = new SoftReference<List<InterfaceTypeImpl>>(interfaces);
        }
        return interfaces;
    }

    void addInterfaces(List<InterfaceTypeImpl> list) {
        List<InterfaceTypeImpl> immediate = interfaces();

        HashSet<InterfaceTypeImpl> hashList = new HashSet<InterfaceTypeImpl>(list);
        hashList.addAll(immediate);
        list.clear();
        list.addAll(hashList);

        for (InterfaceTypeImpl o : immediate) {
            InterfaceTypeImpl interfaze = o;
            interfaze.addSuperinterfaces(list);
        }

        ClassTypeImpl superclass = superclass();
        if (superclass != null) {
            superclass.addInterfaces(list);
        }
    }

    public List<InterfaceTypeImpl> allInterfaces()  {
        List<InterfaceTypeImpl> allinterfaces = (allInterfacesCache != null)? allInterfacesCache.get() : null;
        if (allinterfaces == null) {
            checkPrepared();
            allinterfaces = new ArrayList<InterfaceTypeImpl>();
            addInterfaces(allinterfaces);
            allinterfaces = Collections.unmodifiableList(allinterfaces);
            allInterfacesCache = new SoftReference<List<InterfaceTypeImpl>>(allinterfaces);
        }
        return allinterfaces;
    }

    public List<ClassTypeImpl> subclasses() {
        List<ClassTypeImpl> subclasses = (subclassesCache != null)? subclassesCache.get() : null;
        if (subclasses == null) {
            subclasses = new ArrayList<ClassTypeImpl>(0);
            for (Object o : vm.allClasses()) {
                ReferenceTypeImpl refType = (ReferenceTypeImpl) o;
                if (refType instanceof ClassTypeImpl) {
                    ClassTypeImpl clazz = (ClassTypeImpl) refType;
                    ClassTypeImpl superclass = clazz.superclass();
                    if ((superclass != null) && superclass.equals(this)) {
                        subclasses.add(clazz);
                    }
                }
            }
            subclasses = Collections.unmodifiableList(subclasses);
            subclassesCache = new SoftReference<List<ClassTypeImpl>>(subclasses);
        }
        return subclasses;
    }

    public MethodImpl concreteMethodByName(String name, String signature) {
        checkPrepared();
        for (MethodImpl candidate : visibleMethods()) {
            if (candidate.name().equals(name) && candidate.signature().equals(signature) && !candidate.isAbstract()) {
                return candidate;
            }
        }
        return null;
    }

   List<MethodImpl> getAllMethods() {
        ArrayList<MethodImpl> list = new ArrayList<MethodImpl>(methods());
        ClassTypeImpl clazz = superclass();
        while (clazz != null) {
            list.addAll(clazz.methods());
            clazz = clazz.superclass();
        }
        /*
         * Avoid duplicate checking on each method by iterating through
         * duplicate-free allInterfaces() rather than recursing
         */
       for (InterfaceTypeImpl interfaze : allInterfaces()) {
           list.addAll(interfaze.methods());
       }
        return list;
    }

    List<ReferenceTypeImpl> inheritedTypes() {
        List<ReferenceTypeImpl> inherited = new ArrayList<ReferenceTypeImpl>(interfaces());
        if (superclass() != null) {
            inherited.add(0, superclass()); /* insert at front */
        }
        return inherited;
    }

    public boolean isEnum() {
        ClassTypeImpl superclass = superclass();
        if (superclass != null) {
            return vm.javaLangEnum.equals(superclass.name());
        } else {
            return false;
        }
    }

    public void setValue(FieldImpl field, ValueImpl value) {
        vm.throwNotReadOnlyException("ClassType.setValue(...)");
    }


    public ValueImpl invokeMethod(ThreadReferenceImpl threadIntf, MethodImpl methodIntf,
                              List<? extends ValueImpl> arguments, int options) {
        vm.throwNotReadOnlyException("ClassType.invokeMethod(...)");
        return null;
    }

    public ObjectReferenceImpl newInstance(ThreadReferenceImpl threadIntf,
                                       MethodImpl methodIntf,
                                       List<? extends ValueImpl> arguments, int options) {
        vm.throwNotReadOnlyException("ClassType.newInstance(...)");
        return null;
    }

    void addVisibleMethods(Map<String, MethodImpl> methodMap) {
        /*
         * Add methods from
         * parent types first, so that the methods in this class will
         * overwrite them in the hash table
         */

        for (InterfaceTypeImpl interfaceType : interfaces()) {
            InterfaceTypeImpl interfaze = interfaceType;
            interfaze.addVisibleMethods(methodMap);
        }

        ClassTypeImpl clazz = superclass();
        if (clazz != null) {
            clazz.addVisibleMethods(methodMap);
        }

        addToMethodMap(methodMap, methods());
    }

    boolean isAssignableTo(ReferenceTypeImpl type) {
        ClassTypeImpl superclazz = superclass();
        if (this.equals(type)) {
            return true;
        } else if ((superclazz != null) && superclazz.isAssignableTo(type)) {
            return true;
        } else {
            List<InterfaceTypeImpl> interfaces = interfaces();
            for (InterfaceTypeImpl anInterface : interfaces) {
                InterfaceTypeImpl interfaze = anInterface;
                if (interfaze.isAssignableTo(type)) {
                    return true;
                }
            }
            return false;
        }
    }

    public String toString() {
       return "class " + name() + "(" + loaderString() + ")";
    }
}
