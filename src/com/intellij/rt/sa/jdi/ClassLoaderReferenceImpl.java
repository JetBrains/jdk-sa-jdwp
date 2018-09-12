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
 */

package com.intellij.rt.sa.jdi;

import com.sun.jdi.*;
import com.intellij.rt.sa.jdwp.JDWP;
import sun.jvm.hotspot.oops.Instance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ClassLoaderReferenceImpl
    extends ObjectReferenceImpl
    implements ClassLoaderReference
{
     // because we work on process snapshot or core we can
     // cache visibleClasses & definedClasses always (i.e., no suspension)
     private List<ReferenceType> visibleClassesCache;
     private List<ReferenceType> definedClassesCache;

     ClassLoaderReferenceImpl(VirtualMachine aVm, Instance oRef) {
         super(aVm, oRef);
     }

     protected String description() {
         return "ClassLoaderReference " + uniqueID();
     }

     public List<ReferenceType> definedClasses() {
         if (definedClassesCache == null) {
             definedClassesCache = new ArrayList<ReferenceType>();
             for (ReferenceType type : vm.allClasses()) {
                 if (equals(type.classLoader())) {  /* thanks OTI */
                     definedClassesCache.add(type);
                 }
             }
         }
         return definedClassesCache;
     }

     public List<ReferenceType> visibleClasses() {
         if (visibleClassesCache == null) {
             visibleClassesCache = ClassesHelper.visibleClasses(ref(), vm);
         }
         return visibleClassesCache;
     }

     Type findType(String signature) throws ClassNotLoadedException {
         List types = visibleClasses();
         Iterator iter = types.iterator();
         while (iter.hasNext()) {
             ReferenceType type = (ReferenceType)iter.next();
             if (type.signature().equals(signature)) {
                 return type;
             }
         }
         JNITypeParser parser = new JNITypeParser(signature);
         throw new ClassNotLoadedException(parser.typeName(),
                                          "Class " + parser.typeName() + " not loaded");
     }

    @Override
    byte typeValueKey() {
        return JDWP.Tag.CLASS_LOADER;
    }
}
