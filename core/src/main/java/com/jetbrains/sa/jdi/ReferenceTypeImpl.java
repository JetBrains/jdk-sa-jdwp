/*
 * Copyright (c) 2002, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.jetbrains.sa.jdi;

import com.sun.jdi.Field;
import com.sun.jdi.Method;
import com.sun.jdi.*;
import sun.jvm.hotspot.debugger.Address;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.utilities.Assert;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.*;

public abstract class ReferenceTypeImpl extends TypeImpl
implements ReferenceType {
    protected Klass       saKlass;          // This can be an InstanceKlass or an ArrayKlass
    protected Symbol      typeNameSymbol;   // This is used in vm.classesByName to speedup search
    private int           modifiers = -1;
    private String        signature = null;
    private SoftReference sdeRef = null;
    private SoftReference fieldsCache;
    private SoftReference allFieldsCache;
    private SoftReference methodsCache;
    private SoftReference allMethodsCache;
    private SoftReference nestedTypesCache;
    private SoftReference methodInvokesCache;

    /* to mark when no info available */
    static final SDE NO_SDE_INFO_MARK = new SDE();

    protected ReferenceTypeImpl(VirtualMachine aVm, Klass klass) {
        super(aVm);
        saKlass = klass;
        typeNameSymbol = saKlass.getName();
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(typeNameSymbol != null, "null type name for a Klass");
        }
    }

    Symbol typeNameAsSymbol() {
        return typeNameSymbol;
    }

    Method getMethodMirror(sun.jvm.hotspot.oops.Method ref) {
        // SA creates new Method objects when they are referenced which means
        // that the incoming object might not be the same object as on our
        // even though it is the same method. So do an address compare by
        // calling equals rather than just reference compare.
        for (Method method1 : methods()) {
            MethodImpl method = (MethodImpl) method1;
            if (ref.equals(method.ref())) {
                return method;
            }
        }
        if (ref.getMethodHolder().equals(CompatibilityHelper.INSTANCE.getMethodHandleKlass())) {
          // invoke methods are generated as needed, so make mirrors as needed
          List mis;
          if (methodInvokesCache == null) {
            mis = new ArrayList();
            methodInvokesCache = new SoftReference(mis);
          } else {
            mis = (List)methodInvokesCache.get();
          }
            for (Object mi : mis) {
                MethodImpl method = (MethodImpl) mi;
                if (ref.equals(method.ref())) {
                    return method;
                }
            }

          MethodImpl method = MethodImpl.createMethodImpl(vm, this, ref);
          mis.add(method);
          return method;
        }
        throw new IllegalArgumentException("Invalid method id: " + ref);
    }

    public boolean equals(Object obj) {
        if ((obj instanceof ReferenceTypeImpl)) {
            ReferenceTypeImpl other = (ReferenceTypeImpl)obj;
            return (ref().equals(other.ref())) &&
                (vm.equals(other.virtualMachine()));
        } else {
            return false;
        }
    }

    public int hashCode() {
        return saKlass.hashCode();
    }

    public int compareTo(ReferenceType refType) {
        /*
         * Note that it is critical that compareTo() == 0
         * implies that equals() == true. Otherwise, TreeSet
         * will collapse classes.
         *
         * (Classes of the same name loaded by different class loaders
         * or in different VMs must not return 0).
         */
        ReferenceTypeImpl other = (ReferenceTypeImpl)refType;
        int comp = name().compareTo(other.name());
        if (comp == 0) {
            Klass rf1 = ref();
            Klass rf2 = other.ref();
            // optimize for typical case: refs equal and VMs equal
            if (rf1.equals(rf2)) {
                // sequenceNumbers are always positive
                comp = vm.sequenceNumber -
                 ((VirtualMachineImpl)(other.virtualMachine())).sequenceNumber;
            } else {
                comp = CompatibilityHelper.INSTANCE.getAddress(rf1).minus(CompatibilityHelper.INSTANCE.getAddress(rf2)) < 0? -1 : 1;
            }
        }
        return comp;
    }

    public String signature() {
        if (signature == null) {
            signature = saKlass.signature();
        }
        return signature;
    }

    // refer to JvmtiEnv::GetClassSignature.
    // null is returned for array klasses.
    public String genericSignature() {
        if (saKlass instanceof ArrayKlass) {
            return null;
        } else {
            Symbol genSig = ((InstanceKlass)saKlass).getGenericSignature();
            return (genSig != null)? genSig.asString() : null;
        }
    }

    public ClassLoaderReference classLoader() {
      Instance xx = (Instance)(((InstanceKlass)saKlass).getClassLoader());
      return vm.classLoaderMirror(xx);
    }

    public boolean isPublic() {
        return((modifiers() & VMModifiers.PUBLIC) != 0);
    }

    public boolean isProtected() {
        return((modifiers() & VMModifiers.PROTECTED) != 0);
    }

    public boolean isPrivate() {
        return((modifiers() & VMModifiers.PRIVATE) != 0);
    }

    public boolean isPackagePrivate() {
        return !isPublic() && !isPrivate() && !isProtected();
    }

    public boolean isAbstract() {
        return((modifiers() & VMModifiers.ABSTRACT) != 0);
    }

    public boolean isFinal() {
        return((modifiers() & VMModifiers.FINAL) != 0);
    }

    public boolean isStatic() {
        return((modifiers() & VMModifiers.STATIC) != 0);
    }

    public boolean isPrepared() {
        return (saKlass.getClassStatus() & JVMDIClassStatus.PREPARED) != 0;
    }

    final void checkPrepared() throws ClassNotPreparedException {
        if (! isPrepared()) {
            throw new ClassNotPreparedException();
        }
    }

    public boolean isVerified() {
        return (saKlass.getClassStatus() & JVMDIClassStatus.VERIFIED) != 0;
    }

    public boolean isInitialized() {
        return (saKlass.getClassStatus() & JVMDIClassStatus.INITIALIZED) != 0;
    }

    public boolean failedToInitialize() {
        return (saKlass.getClassStatus() & JVMDIClassStatus.ERROR) != 0;
    }

    private boolean isThrowableBacktraceField(sun.jvm.hotspot.oops.Field fld) {
        // refer to JvmtiEnv::GetClassFields in jvmtiEnv.cpp.
        // We want to filter out java.lang.Throwable.backtrace (see 4446677).
        // It contains some Method*s that aren't quite real Objects.
        return fld.getFieldHolder().getName().equals(vm.javaLangThrowable()) &&
                fld.getID().getName().equals("backtrace");
    }

    public final FieldImpl fieldById(long id) throws ClassNotPreparedException {
        for (Field field : allFields()) {
            if (((FieldImpl) field).uniqueID() == id) {
                return (FieldImpl) field;
            }
        }
        throw new IllegalStateException("Field with id " + id + " not found in " + name());
    }

    public final List<Field> fields() throws ClassNotPreparedException {
        List fields = (fieldsCache != null)? (List) fieldsCache.get() : null;
        if (fields == null) {
            checkPrepared();
            if (saKlass instanceof ArrayKlass) {
                fields = new ArrayList(0);
            } else {
                // Get a list of the sa Field types
                List saFields = ((InstanceKlass)saKlass).getImmediateFields();

                // Create a list of our Field types
                int len = saFields.size();
                fields = new ArrayList(len);
                for (Object saField : saFields) {
                    sun.jvm.hotspot.oops.Field curField = (sun.jvm.hotspot.oops.Field) saField;
                    if (!isThrowableBacktraceField(curField)) {
                        fields.add(new FieldImpl(vm, this, curField));
                    }
                }
            }
            fields = Collections.unmodifiableList(fields);
            fieldsCache = new SoftReference(fields);
        }
        return fields;
    }

    public final List<Field> allFields() throws ClassNotPreparedException {
        List<Field> allFields = (allFieldsCache != null)? (List) allFieldsCache.get() : null;
        if (allFields == null) {
            checkPrepared();
            if (saKlass instanceof ArrayKlass) {
                // is 'length' a field of array klasses? To maintain
                // consistency with JVMDI-JDI we return 0 size.
                allFields = new ArrayList<Field>(0);
            } else {

                // Get a list of the sa Field types

                // getAllFields() is buggy and does not return all super classes
//                saFields = ((InstanceKlass)saKlass).getAllFields();

                InstanceKlass saKlass = (InstanceKlass) this.saKlass;
                List saFields = saKlass.getImmediateFields();

                // transitiveInterfaces contains all interfaces implemented
                // by this class and its superclass chain with no duplicates.

                for (InstanceKlass intf1 : CompatibilityHelper.INSTANCE.getTransitiveInterfaces(saKlass)) {
                    if (Assert.ASSERTS_ENABLED) {
                        Assert.that(intf1.isInterface(), "just checking type");
                    }
                    saFields.addAll(intf1.getImmediateFields());
                }

                // Get all fields in the superclass, recursively.  But, don't
                // include fields in interfaces implemented by superclasses;
                // we already have all those.
                if (!saKlass.isInterface()) {
                    InstanceKlass supr = saKlass;
                    while  ( (supr = (InstanceKlass) supr.getSuper()) != null) {
                        saFields.addAll(supr.getImmediateFields());
                    }
                }

                // Create a list of our Field types
                allFields = new ArrayList<Field>(saFields.size());
                for (Object saField : saFields) {
                    sun.jvm.hotspot.oops.Field curField = (sun.jvm.hotspot.oops.Field) saField;
                    if (!isThrowableBacktraceField(curField)) {
                        allFields.add(new FieldImpl(vm, vm.referenceType(curField.getFieldHolder()), curField));
                    }
                }
            }
            allFields = Collections.unmodifiableList(allFields);
            allFieldsCache = new SoftReference(allFields);
        }
        return allFields;
    }

    abstract List inheritedTypes();

    void addVisibleFields(List visibleList, Map visibleTable, List ambiguousNames) {
        List list = visibleFields();
        for (Object o : list) {
            Field field = (Field) o;
            String name = field.name();
            if (!ambiguousNames.contains(name)) {
                Field duplicate = (Field) visibleTable.get(name);
                if (duplicate == null) {
                    visibleList.add(field);
                    visibleTable.put(name, field);
                } else if (!field.equals(duplicate)) {
                    ambiguousNames.add(name);
                    visibleTable.remove(name);
                    visibleList.remove(duplicate);
                } else {
                    // identical field from two branches; do nothing
                }
            }
        }
    }

    public final List visibleFields() throws ClassNotPreparedException {
        checkPrepared();
        /*
         * Maintain two different collections of visible fields. The
         * list maintains a reasonable order for return. The
         * hash map provides an efficient way to lookup visible fields
         * by name, important for finding hidden or ambiguous fields.
         */
        List visibleList = new ArrayList();
        Map  visibleTable = new HashMap();

        /* Track fields removed from above collection due to ambiguity */
        List ambiguousNames = new ArrayList();

        /* Add inherited, visible fields */
        List types = inheritedTypes();
        Iterator iter = types.iterator();
        while (iter.hasNext()) {
            /*
             * TO DO: Be defensive and check for cyclic interface inheritance
             */
            ReferenceTypeImpl type = (ReferenceTypeImpl)iter.next();
            type.addVisibleFields(visibleList, visibleTable, ambiguousNames);
        }

        /*
         * Insert fields from this type, removing any inherited fields they
         * hide.
         */
        List retList = new ArrayList(fields());
        iter = retList.iterator();
        while (iter.hasNext()) {
            Field field = (Field)iter.next();
            Field hidden = (Field)visibleTable.get(field.name());
            if (hidden != null) {
                visibleList.remove(hidden);
            }
        }
        retList.addAll(visibleList);
        return retList;
    }

   public final Field fieldByName(String fieldName) throws ClassNotPreparedException {
        List searchList;
        Field f;

        // visibleFields calls checkPrepared
        searchList = visibleFields();

       for (Object o : searchList) {
           f = (Field) o;

           if (f.name().equals(fieldName)) {
               return f;
           }
       }
        //throw new NoSuchFieldException("Field '" + fieldName + "' not found in " + name());
        return null;
    }

    public final MethodImpl methodById(long id) throws ClassNotPreparedException {
        for (Method method : methods()) {
            if (((MethodImpl) method).uniqueID() == id) {
                return (MethodImpl) method;
            }
        }
        throw new IllegalStateException("Method with id " + id + " not found in " + name());
    }

    public final List<Method> methods() throws ClassNotPreparedException {
        List<Method> methods = (methodsCache != null)? (List) methodsCache.get() : null;
        if (methods == null) {
            checkPrepared();
            if (saKlass instanceof ArrayKlass) {
                methods = new ArrayList<Method>(0);
            } else {
                List saMethods;
                // Get a list of the SA Method types
                saMethods = ((InstanceKlass)saKlass).getImmediateMethods();

                // Create a list of our MethodImpl types
                int len = saMethods.size();
                methods = new ArrayList<Method>(len);
                for (Object saMethod : saMethods) {
                    methods.add(MethodImpl.createMethodImpl(vm, this, (sun.jvm.hotspot.oops.Method) saMethod));
                }
            }
            methods = Collections.unmodifiableList(methods);
            methodsCache = new SoftReference(methods);
        }
        return methods;
    }

    abstract List getAllMethods();
    public final List allMethods() throws ClassNotPreparedException {
        List allMethods = (allMethodsCache != null)? (List) allMethodsCache.get() : null;
        if (allMethods == null) {
            checkPrepared();
            allMethods = Collections.unmodifiableList(getAllMethods());
            allMethodsCache = new SoftReference(allMethods);
        }
        return allMethods;
    }

    /*
     * Utility method used by subclasses to build lists of visible
     * methods.
     */
    void addToMethodMap(Map methodMap, List methodList) {
        for (Object o : methodList) {
            Method method = (Method) o;
            methodMap.put(method.name().concat(method.signature()), method);
        }
    }

    abstract void addVisibleMethods(Map methodMap);
    public final List visibleMethods() throws ClassNotPreparedException {
        checkPrepared();
        /*
         * Build a collection of all visible methods. The hash
         * map allows us to do this efficiently by keying on the
         * concatenation of name and signature.
         */
        //System.out.println("jj: RTI: Calling addVisibleMethods for:" + this);
        Map map = new HashMap();
        addVisibleMethods(map);

        /*
         * ... but the hash map destroys order. Methods should be
         * returned in a sensible order, as they are in allMethods().
         * So, start over with allMethods() and use the hash map
         * to filter that ordered collection.
         */
        //System.out.println("jj: RTI: Calling allMethods for:" + this);

        List list = new ArrayList(allMethods());
        //System.out.println("jj: allMethods = " + jjstr(list));
        //System.out.println("jj: map = " + map.toString());
        //System.out.println("jj: map = " + jjstr(map.values()));
        list.retainAll(map.values());
        //System.out.println("jj: map = " + jjstr(list));
        //System.exit(0);
        return list;
    }

    static Object prev;

    static public String jjstr(Collection cc) {
        StringBuilder buf = new StringBuilder();
        buf.append("[");
        Iterator i = cc.iterator();
        boolean hasNext = i.hasNext();
        while (hasNext) {
            Object o = i.next();
            if (prev == null) {
                prev = o;
            } else {
                System.out.println("prev == curr?" + prev.equals(o));
                System.out.println("prev == curr?" + (prev == o));
            }
            buf.append(o).append("@").append(o.hashCode());
            //buf.append( ((Object)o).toString());
            hasNext = i.hasNext();
            if (hasNext)
                buf.append(", ");
        }

        buf.append("]");
        return buf.toString();
    }

    public final List methodsByName(String name) throws ClassNotPreparedException {
        // visibleMethods calls checkPrepared
        List methods = visibleMethods();
        ArrayList retList = new ArrayList(methods.size());
        for (Object method : methods) {
            Method candidate = (Method) method;
            if (candidate.name().equals(name)) {
                retList.add(candidate);
            }
        }
        retList.trimToSize();
        return retList;
    }

    public final List methodsByName(String name, String signature) throws ClassNotPreparedException {
        // visibleMethods calls checkPrepared
        List methods = visibleMethods();
        ArrayList retList = new ArrayList(methods.size());
        for (Object method : methods) {
            Method candidate = (Method) method;
            if (candidate.name().equals(name) &&
                    candidate.signature().equals(signature)) {
                retList.add(candidate);
            }
        }
        retList.trimToSize();
        return retList;
    }


    List<InterfaceTypeImpl> getInterfaces() {
        if (saKlass instanceof ArrayKlass) {
            // Actually, JLS says arrays implement Cloneable and Serializable
            // But, JVMDI-JDI just returns 0 interfaces for arrays. We follow
            // the same for consistency.
            return Collections.emptyList();
        }

        // Get a list of the sa InstanceKlass types
        List saInterfaces = ((InstanceKlass)saKlass).getDirectImplementedInterfaces();

        // Create a list of our InterfaceTypes
        List<InterfaceTypeImpl> myInterfaces = new ArrayList<InterfaceTypeImpl>(saInterfaces.size());
        for (Object saInterface : saInterfaces) {
            myInterfaces.add((InterfaceTypeImpl) vm.referenceType((Klass) saInterface));
        }
        return myInterfaces;
    }

    public final List<ReferenceType> nestedTypes() {
        List<ReferenceType> nestedTypes = (nestedTypesCache != null)? (List) nestedTypesCache.get() : null;
        if (nestedTypes == null) {
            if (saKlass instanceof ArrayKlass) {
                nestedTypes = new ArrayList<ReferenceType>(0);
            } else {
                ClassLoaderReference cl = classLoader();
                List classes;
                if (cl != null) {
                   classes = cl.visibleClasses();
                } else {
                   classes = vm.bootstrapClasses();
                }
                nestedTypes = new ArrayList<ReferenceType>();
                for (Object aClass : classes) {
                    ReferenceTypeImpl refType = (ReferenceTypeImpl) aClass;
                    Symbol candidateName = refType.ref().getName();
                    if (((InstanceKlass) saKlass).isInnerOrLocalClassName(candidateName)) {
                        nestedTypes.add(refType);
                    }
                }
            }
            nestedTypes = Collections.unmodifiableList(nestedTypes);
            nestedTypesCache = new SoftReference<List<ReferenceType>>(nestedTypes);
        }
        return nestedTypes;
    }

    public Value getValue(Field sig) {
        List list = new ArrayList(1);
        list.add(sig);
        Map map = getValues(list);
        return(Value)map.get(sig);
    }

    /**
     * Returns a map of field values
     */
    public Map getValues(List theFields) {
        //validateMirrors();
        int size = theFields.size();
        Map map = new HashMap(size);
        for (Object theField : theFields) {
            FieldImpl fieldImpl = (FieldImpl) theField;

            validateFieldAccess(fieldImpl);
            // Do more validation specific to ReferenceType field getting
            if (!fieldImpl.isStatic()) {
                throw new IllegalArgumentException(
                        "Attempt to use non-static field with ReferenceType: " +
                                fieldImpl.name());
            }
            map.put(fieldImpl, fieldImpl.getValue());
        }
        return map;
    }

    void validateFieldAccess(Field field) {
       /*
        * Field must be in this object's class, a superclass, or
        * implemented interface
        */
        ReferenceTypeImpl declType = (ReferenceTypeImpl)field.declaringType();
        if (!declType.isAssignableFrom(this)) {
            throw new IllegalArgumentException("Invalid field");
        }
    }

    public ClassObjectReference classObject() {
        return vm.classObjectMirror(ref().getJavaMirror());
    }

    SDE.Stratum stratum(String stratumID) {
        SDE sde = sourceDebugExtensionInfo();
        if (!sde.isValid()) {
            sde = NO_SDE_INFO_MARK;
        }
        return sde.stratum(stratumID);
    }

    public String sourceName() throws AbsentInformationException {
        return (String)(sourceNames(vm.getDefaultStratum()).get(0));
    }

    public List sourceNames(String stratumID)
                                throws AbsentInformationException {
        SDE.Stratum stratum = stratum(stratumID);
        if (stratum.isJava()) {
            List result = new ArrayList(1);
            result.add(baseSourceName());
            return result;
        }
        return stratum.sourceNames(this);
    }

    public List sourcePaths(String stratumID)
                                throws AbsentInformationException {
        SDE.Stratum stratum = stratum(stratumID);
        if (stratum.isJava()) {
            List result = new ArrayList(1);
            result.add(baseSourceDir() + baseSourceName());
            return result;
        }
        return stratum.sourcePaths(this);
    }

    public String baseSourceName() throws AbsentInformationException {
      if (saKlass instanceof ArrayKlass) {
            throw new AbsentInformationException();
      }
      Symbol sym = ((InstanceKlass)saKlass).getSourceFileName();
      if (sym != null) {
          return sym.asString();
      } else {
          throw new AbsentInformationException();
      }
    }

    String baseSourcePath() throws AbsentInformationException {
        return baseSourceDir() + baseSourceName();
    }

    String baseSourceDir() {
        String typeName = name();
        StringBuilder sb = new StringBuilder(typeName.length() + 10);
        int index = 0;
        int nextIndex;

        while ((nextIndex = typeName.indexOf('.', index)) > 0) {
            sb.append(typeName, index, nextIndex);
            sb.append(File.separatorChar);
            index = nextIndex + 1;
        }
        return sb.toString();
    }

    public String sourceDebugExtension()
                           throws AbsentInformationException {
        if (!vm.canGetSourceDebugExtension()) {
            throw new UnsupportedOperationException();
        }
        SDE sde = sourceDebugExtensionInfo();
        if (sde == NO_SDE_INFO_MARK) {
            throw new AbsentInformationException();
        }
        return sde.sourceDebugExtension;
    }

    private SDE sourceDebugExtensionInfo() {
        if (!vm.canGetSourceDebugExtension()) {
            return NO_SDE_INFO_MARK;
        }
        SDE sde;
        sde = (sdeRef == null) ?  null : (SDE)sdeRef.get();
        if (sde == null) {
           String extension = null;
           if (saKlass instanceof InstanceKlass) {
              extension = CompatibilityHelper.INSTANCE.getSourceDebugExtension((InstanceKlass)saKlass);
           }
           if (extension == null) {
              sde = NO_SDE_INFO_MARK;
           } else {
              sde = new SDE(extension);
           }
           sdeRef = new SoftReference(sde);
        }
        return sde;
    }

    public List availableStrata() {
        SDE sde = sourceDebugExtensionInfo();
        if (sde.isValid()) {
            return sde.availableStrata();
        } else {
            List strata = new ArrayList();
            strata.add(SDE.BASE_STRATUM_NAME);
            return strata;
        }
    }

    /**
     * Always returns non-null stratumID
     */
    public String defaultStratum() {
        SDE sdei = sourceDebugExtensionInfo();
        if (sdei.isValid()) {
            return sdei.defaultStratumId;
        } else {
            return SDE.BASE_STRATUM_NAME;
        }
    }

    public final int modifiers() {
        if (modifiers == -1) {
            modifiers = getModifiers();
        }
        return modifiers;
    }

    // new method since 1.6.
    // Real body will be supplied later.
    public List<ObjectReference> instances(long maxInstances) {
        if (!vm.canGetInstanceInfo()) {
            throw new UnsupportedOperationException(
                      "target does not support getting instances");
        }

        if (maxInstances < 0) {
            throw new IllegalArgumentException("maxInstances is less than zero: "
                                              + maxInstances);
        }

        final List<ObjectReference> objects = new ArrayList<ObjectReference>(0);
        if (isAbstract() || (this instanceof InterfaceType)) {
            return objects;
        }

        final Address givenKls = CompatibilityHelper.INSTANCE.getAddress(saKlass);
        final long max = maxInstances;
        vm.saObjectHeap().iterate(new DefaultHeapVisitor() {
                private long instCount = 0;
                public boolean doObj(Oop oop) {
                    if (givenKls.equals(CompatibilityHelper.INSTANCE.getKlassAddress(oop))) {
                        objects.add(vm.objectMirror(oop));
                        instCount++;
                    }
                    return max > 0 && instCount >= max;
                }
            });
        return objects;
    }

    int getModifiers() {
        return (int) saKlass.getClassModifiers();
    }

    public List allLineLocations()
                            throws AbsentInformationException {
        return allLineLocations(vm.getDefaultStratum(), null);
    }

    public List allLineLocations(String stratumID, String sourceName)
                            throws AbsentInformationException {
        checkPrepared();
        boolean someAbsent = false; // A method that should have info, didn't
        SDE.Stratum stratum = stratum(stratumID);
        List list = new ArrayList();  // location list

        for (Method method1 : methods()) {
            MethodImpl method = (MethodImpl) method1;
            try {
                list.addAll(
                        method.allLineLocations(stratum.id(), sourceName));
            } catch (AbsentInformationException exc) {
                someAbsent = true;
            }
        }

        // If we retrieved no line info, and at least one of the methods
        // should have had some (as determined by an
        // AbsentInformationException being thrown) then we rethrow
        // the AbsentInformationException.
        if (someAbsent && list.size() == 0) {
            throw new AbsentInformationException();
        }
        return list;
    }

    public List locationsOfLine(int lineNumber)
                           throws AbsentInformationException {
        return locationsOfLine(vm.getDefaultStratum(),
                               null,
                               lineNumber);
    }

    public List locationsOfLine(String stratumID,
                                String sourceName,
                                int lineNumber)
                           throws AbsentInformationException {
        checkPrepared();
        // A method that should have info, didn't
        boolean someAbsent = false;
        // A method that should have info, did
        boolean somePresent = false;
        List methods = methods();
        SDE.Stratum stratum = stratum(stratumID);

        List list = new ArrayList();

        for (Object method1 : methods) {
            MethodImpl method = (MethodImpl) method1;
            // eliminate native and abstract to eliminate
            // false positives
            if (!method.isAbstract() &&
                    !method.isNative()) {
                try {
                    list.addAll(
                            method.locationsOfLine(stratum.id(),
                                    sourceName,
                                    lineNumber));
                    somePresent = true;
                } catch (AbsentInformationException exc) {
                    someAbsent = true;
                }
            }
        }
        if (someAbsent && !somePresent) {
            throw new AbsentInformationException();
        }
        return list;
    }

    public Klass ref() {
        return saKlass;
    }

    /*
     * Return true if an instance of this type
     * can be assigned to a variable of the given type
     */
    abstract boolean isAssignableTo(ReferenceType type);

    boolean isAssignableFrom(ReferenceType type) {
        return ((ReferenceTypeImpl)type).isAssignableTo(this);
    }

    boolean isAssignableFrom(ObjectReference object) {
        return object == null ||
            isAssignableFrom(object.referenceType());
    }

    int indexOf(Method method) {
        // Make sure they're all here - the obsolete method
        // won't be found and so will have index -1
        return methods().indexOf(method);
    }

    int indexOf(Field field) {
        // Make sure they're all here
        return fields().indexOf(field);
    }

    private static boolean isPrimitiveArray(String signature) {
        int i = signature.lastIndexOf('[');
        /*
         * TO DO: Centralize JNI signature knowledge.
         *
         * Ref:
         *  jdk1.4/doc/guide/jpda/jdi/com/sun/jdi/doc-files/signature.html
         */
        boolean isPA;
        if (i < 0) {
            isPA = false;
        } else {
            char c = signature.charAt(i + 1);
            isPA = (c != 'L');
        }
        return isPA;
    }

    Type findType(String signature) throws ClassNotLoadedException {
        Type type;
        if (signature.length() == 1) {
            /* OTI FIX: Must be a primitive type or the void type */
            char sig = signature.charAt(0);
            if (sig == 'V') {
                type = vm.theVoidType();
            } else {
                type = vm.primitiveTypeMirror(sig);
            }
        } else {
            // Must be a reference type.
            ClassLoaderReferenceImpl loader =
                       (ClassLoaderReferenceImpl)classLoader();
            if ((loader == null) ||
                (isPrimitiveArray(signature)) //Work around 4450091
                ) {
                // Caller wants type of boot class field
                type = vm.findBootType(signature);
            } else {
                // Caller wants type of non-boot class field
                type = loader.findType(signature);
            }
        }
        return type;
    }

    String loaderString() {
        if (classLoader() != null) {
            return "loaded by " + classLoader().toString();
        } else {
            return "loaded by bootstrap loader";
        }
    }

    public long uniqueID() {
        return vm.getAddressValue(CompatibilityHelper.INSTANCE.getAddress(saKlass));
    }

    // new method since 1.6
    public int majorVersion() {
        if (!vm.canGetClassFileVersion()) {
            throw new UnsupportedOperationException("Cannot get class file version");
        }
        return (int)((InstanceKlass)saKlass).majorVersion();
    }

    // new method since 1.6
    public int minorVersion() {
        if (!vm.canGetClassFileVersion()) {
            throw new UnsupportedOperationException("Cannot get class file version");
        }
        return (int)((InstanceKlass)saKlass).minorVersion();
    }

    // new method since 1.6
    public int constantPoolCount() {
        if (!vm.canGetConstantPool()) {
            throw new UnsupportedOperationException("Cannot get constant pool");
        }
        if (saKlass instanceof ArrayKlass) {
            return 0;
        } else {
            return ((InstanceKlass)saKlass).getConstants().getLength();
        }
    }

    // new method since 1.6
    public byte[] constantPool() {
        if (!vm.canGetConstantPool()) {
            throw new UnsupportedOperationException("Cannot get constant pool");
        }
        if (this instanceof ArrayType || this instanceof PrimitiveType) {
            return new byte[0];
        } else {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            try {
                ((InstanceKlass)saKlass).getConstants().writeBytes(bs);
            } catch (IOException ex) {
                                ex.printStackTrace();
                return new byte[0];
            }
            return bs.toByteArray();
        }
    }

    public abstract byte tag();
}
