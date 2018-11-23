/*
 * Copyright (c) 2002, 2003, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.jdi.ClassNotLoadedException;
import sun.jvm.hotspot.oops.Instance;

import java.util.ArrayList;
import java.util.List;

public class ClassLoaderReferenceImpl extends ObjectReferenceImpl {
     // because we work on process snapshot or core we can
     // cache visibleClasses & definedClasses always (i.e., no suspension)
     private List<ReferenceTypeImpl> visibleClassesCache;
     private List<ReferenceTypeImpl> definedClassesCache;

     ClassLoaderReferenceImpl(VirtualMachineImpl aVm, Instance oRef) {
         super(aVm, oRef);
     }

     protected String description() {
         return "ClassLoaderReference " + uniqueID();
     }

     public List<ReferenceTypeImpl> definedClasses() {
         if (definedClassesCache == null) {
             definedClassesCache = new ArrayList<ReferenceTypeImpl>();
             for (ReferenceTypeImpl type : vm.allClasses()) {
                 if (equals(type.classLoader())) {  /* thanks OTI */
                     definedClassesCache.add(type);
                 }
             }
         }
         return definedClassesCache;
     }

     public List<ReferenceTypeImpl> visibleClasses() {
         if (visibleClassesCache == null) {
             visibleClassesCache = CompatibilityHelper.INSTANCE.visibleClasses(ref(), vm);
         }
         return visibleClassesCache;
     }

     TypeImpl findType(String signature) throws ClassNotLoadedException {
         for (ReferenceTypeImpl type : visibleClasses()) {
             if (type.signature().equals(signature)) {
                 return type;
             }
         }
         JNITypeParser parser = new JNITypeParser(signature);
         throw new ClassNotLoadedException(parser.typeName(), "Class " + parser.typeName() + " not loaded");
     }

    @Override
    byte typeValueKey() {
        return JDWP.Tag.CLASS_LOADER;
    }
}
