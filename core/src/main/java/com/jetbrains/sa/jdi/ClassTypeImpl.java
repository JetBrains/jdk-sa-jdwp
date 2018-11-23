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
import com.sun.jdi.*;
import sun.jvm.hotspot.oops.InstanceKlass;

import java.lang.ref.SoftReference;
import java.util.*;

public class ClassTypeImpl extends ReferenceTypeImpl
    implements ClassType
{
    private SoftReference<List<InterfaceType>> interfacesCache    = null;
    private SoftReference<List<InterfaceType>> allInterfacesCache = null;
    private SoftReference<List<ClassType>> subclassesCache    = null;

    protected ClassTypeImpl(VirtualMachine aVm, InstanceKlass aRef) {
        super(aVm, aRef, JDWP.TypeTag.CLASS);
    }

    public ClassType superclass() {
        InstanceKlass kk = (InstanceKlass)ref().getSuper();
        if (kk == null) {
            return null;
        }
        return (ClassType) vm.referenceType(kk);
    }

    public List<InterfaceType> interfaces()  {
        List<InterfaceType> interfaces = (interfacesCache != null)? interfacesCache.get() : null;
        if (interfaces == null) {
            checkPrepared();
            interfaces = Collections.unmodifiableList(getInterfaces());
            interfacesCache = new SoftReference<List<InterfaceType>>(interfaces);
        }
        return interfaces;
    }

    void addInterfaces(List<InterfaceType> list) {
        List<InterfaceType> immediate = interfaces();

        HashSet<InterfaceType> hashList = new HashSet<InterfaceType>(list);
        hashList.addAll(immediate);
        list.clear();
        list.addAll(hashList);

        for (InterfaceType o : immediate) {
            InterfaceTypeImpl interfaze = (InterfaceTypeImpl) o;
            interfaze.addSuperinterfaces(list);
        }

        ClassTypeImpl superclass = (ClassTypeImpl)superclass();
        if (superclass != null) {
            superclass.addInterfaces(list);
        }
    }

    public List<InterfaceType> allInterfaces()  {
        List<InterfaceType> allinterfaces = (allInterfacesCache != null)? allInterfacesCache.get() : null;
        if (allinterfaces == null) {
            checkPrepared();
            allinterfaces = new ArrayList<InterfaceType>();
            addInterfaces(allinterfaces);
            allinterfaces = Collections.unmodifiableList(allinterfaces);
            allInterfacesCache = new SoftReference<List<InterfaceType>>(allinterfaces);
        }
        return allinterfaces;
    }

    public List<ClassType> subclasses() {
        List<ClassType> subclasses = (subclassesCache != null)? subclassesCache.get() : null;
        if (subclasses == null) {
            subclasses = new ArrayList<ClassType>(0);
            for (Object o : vm.allClasses()) {
                ReferenceType refType = (ReferenceType) o;
                if (refType instanceof ClassType) {
                    ClassType clazz = (ClassType) refType;
                    ClassType superclass = clazz.superclass();
                    if ((superclass != null) && superclass.equals(this)) {
                        subclasses.add(clazz);
                    }
                }
            }
            subclasses = Collections.unmodifiableList(subclasses);
            subclassesCache = new SoftReference<List<ClassType>>(subclasses);
        }
        return subclasses;
    }

    public Method concreteMethodByName(String name, String signature) {
        checkPrepared();
        for (Method candidate : visibleMethods()) {
            if (candidate.name().equals(name) && candidate.signature().equals(signature) && !candidate.isAbstract()) {
                return candidate;
            }
        }
        return null;
    }

   List<Method> getAllMethods() {
        ArrayList<Method> list = new ArrayList<Method>(methods());
        ClassType clazz = superclass();
        while (clazz != null) {
            list.addAll(clazz.methods());
            clazz = clazz.superclass();
        }
        /*
         * Avoid duplicate checking on each method by iterating through
         * duplicate-free allInterfaces() rather than recursing
         */
       for (InterfaceType interfaze : allInterfaces()) {
           list.addAll(interfaze.methods());
       }
        return list;
    }

    List<ReferenceType> inheritedTypes() {
        List<ReferenceType> inherited = new ArrayList<ReferenceType>(interfaces());
        if (superclass() != null) {
            inherited.add(0, superclass()); /* insert at front */
        }
        return inherited;
    }

    public boolean isEnum() {
        ClassTypeImpl superclass = (ClassTypeImpl) superclass();
        if (superclass != null) {
            return vm.javaLangEnum.equals(superclass.name());
        } else {
            return false;
        }
    }

    public void setValue(Field field, Value value) {
        vm.throwNotReadOnlyException("ClassType.setValue(...)");
    }


    public Value invokeMethod(ThreadReference threadIntf, Method methodIntf,
                              List<? extends Value> arguments, int options) {
        vm.throwNotReadOnlyException("ClassType.invokeMethod(...)");
        return null;
    }

    public ObjectReference newInstance(ThreadReference threadIntf,
                                       Method methodIntf,
                                       List<? extends Value> arguments, int options) {
        vm.throwNotReadOnlyException("ClassType.newInstance(...)");
        return null;
    }

    void addVisibleMethods(Map<String, Method> methodMap) {
        /*
         * Add methods from
         * parent types first, so that the methods in this class will
         * overwrite them in the hash table
         */

        for (InterfaceType interfaceType : interfaces()) {
            InterfaceTypeImpl interfaze = (InterfaceTypeImpl) interfaceType;
            interfaze.addVisibleMethods(methodMap);
        }

        ClassTypeImpl clazz = (ClassTypeImpl)superclass();
        if (clazz != null) {
            clazz.addVisibleMethods(methodMap);
        }

        addToMethodMap(methodMap, methods());
    }

    boolean isAssignableTo(ReferenceType type) {
        ClassTypeImpl superclazz = (ClassTypeImpl)superclass();
        if (this.equals(type)) {
            return true;
        } else if ((superclazz != null) && superclazz.isAssignableTo(type)) {
            return true;
        } else {
            List<InterfaceType> interfaces = interfaces();
            for (InterfaceType anInterface : interfaces) {
                InterfaceTypeImpl interfaze = (InterfaceTypeImpl) anInterface;
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
