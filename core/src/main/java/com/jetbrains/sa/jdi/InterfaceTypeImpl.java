/*
 * Copyright (c) 2002, 2004, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.jdi.*;
import com.jetbrains.sa.jdwp.JDWP;
import sun.jvm.hotspot.oops.InstanceKlass;

import java.lang.ref.SoftReference;
import java.util.*;

public class InterfaceTypeImpl extends ReferenceTypeImpl
                               implements InterfaceType {
    private SoftReference<List<InterfaceType>> superInterfacesCache = null;
    private SoftReference<List<InterfaceType>> subInterfacesCache = null;
    private SoftReference<List<ClassType>> implementorsCache = null;

    protected InterfaceTypeImpl(VirtualMachine aVm, InstanceKlass aRef) {
        super(aVm, aRef);
    }

    public List<InterfaceType> superinterfaces() throws ClassNotPreparedException {
        List<InterfaceType> superinterfaces = (superInterfacesCache != null)? superInterfacesCache.get() : null;
        if (superinterfaces == null) {
            checkPrepared();
            superinterfaces = Collections.unmodifiableList(getInterfaces());
            superInterfacesCache = new SoftReference<List<InterfaceType>>(superinterfaces);
        }
        return superinterfaces;
    }

    public List<InterfaceType> subinterfaces() {
        List<InterfaceType> subinterfaces = (subInterfacesCache != null)? subInterfacesCache.get() : null;
        if (subinterfaces == null) {
            subinterfaces = new ArrayList<InterfaceType>();
            for (ReferenceType refType : vm.allClasses()) {
                if (refType instanceof InterfaceType) {
                    InterfaceType interfaze = (InterfaceType) refType;
                    if (interfaze.isPrepared() && interfaze.superinterfaces().contains(this)) {
                        subinterfaces.add(interfaze);
                    }
                }
            }
            subinterfaces = Collections.unmodifiableList(subinterfaces);
            subInterfacesCache = new SoftReference<List<InterfaceType>>(subinterfaces);
        }
        return subinterfaces;
    }

    public List<ClassType> implementors() {
        List<ClassType> implementors = (implementorsCache != null)? implementorsCache.get() : null;
        if (implementors == null) {
            implementors = new ArrayList<ClassType>();
            for (Object o : vm.allClasses()) {
                ReferenceType refType = (ReferenceType) o;
                if (refType instanceof ClassType) {
                    ClassType clazz = (ClassType) refType;
                    if (clazz.isPrepared() && clazz.interfaces().contains(this)) {
                        implementors.add(clazz);
                    }
                }
            }
            implementors = Collections.unmodifiableList(implementors);
            implementorsCache = new SoftReference<List<ClassType>>(implementors);
        }
        return implementors;
    }

    void addVisibleMethods(Map<String, Method> methodMap) {
        /*
         * Add methods from
         * parent types first, so that the methods in this class will
         * overwrite them in the hash table
         */
        for (Object o : superinterfaces()) {
            InterfaceTypeImpl interfaze = (InterfaceTypeImpl) o;
            interfaze.addVisibleMethods(methodMap);
        }

        addToMethodMap(methodMap, methods());
    }

    List<Method> getAllMethods() {
        ArrayList<Method> list = new ArrayList<Method>(methods());
        /*
         * It's more efficient if don't do this
         * recursively.
         */
        List<InterfaceType> interfaces = allSuperinterfaces();
        for (InterfaceType interfaze : interfaces) {
            list.addAll(interfaze.methods());
        }

        return list;
    }

    List<InterfaceType> allSuperinterfaces() {
        ArrayList<InterfaceType> list = new ArrayList<InterfaceType>();
        addSuperinterfaces(list);
        return list;
    }

    void addSuperinterfaces(List<InterfaceType> list) {
        /*
         * This code is a little strange because it
         * builds the list with a more suitable order than the
         * depth-first approach a normal recursive solution would
         * take. Instead, all direct superinterfaces precede all
         * indirect ones.
         */

        /*
         * Get a list of direct superinterfaces that's not already in the
         * list being built.
         */
        List<InterfaceType> immediate = new ArrayList<InterfaceType>(superinterfaces());
        Iterator<InterfaceType> iter = immediate.iterator();
        while (iter.hasNext()) {
            InterfaceType interfaze = (InterfaceType)iter.next();
            if (list.contains(interfaze)) {
                iter.remove();
            }
        }

        /*
         * Add all new direct superinterfaces
         */
        list.addAll(immediate);

        /*
         * Recurse for all new direct superinterfaces.
         */
        iter = immediate.iterator();
        while (iter.hasNext()) {
            InterfaceTypeImpl interfaze = (InterfaceTypeImpl)iter.next();
            interfaze.addSuperinterfaces(list);
        }
    }

    boolean isAssignableTo(ReferenceType type) {

        // Exact match?
        if (this.equals(type)) {
            return true;
        } else {
            // Try superinterfaces.
            for (InterfaceType aSuper : superinterfaces()) {
                InterfaceTypeImpl interfaze = (InterfaceTypeImpl) aSuper;
                if (interfaze.isAssignableTo(type)) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public byte tag() {
        return JDWP.TypeTag.INTERFACE;
    }

    List<InterfaceType> inheritedTypes() {
        return superinterfaces();
    }

    public boolean isInitialized() {
        return isPrepared();
    }

    public String toString() {
       return "interface " + name() + " (" + loaderString() + ")";
    }
}
