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

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.request.EventRequestManager;
import sun.jvm.hotspot.HotSpotAgent;
import sun.jvm.hotspot.debugger.Address;
import sun.jvm.hotspot.memory.SystemDictionary;
import sun.jvm.hotspot.memory.Universe;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.JavaThread;
import sun.jvm.hotspot.runtime.VM;
import sun.jvm.hotspot.utilities.Assert;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.*;

public class VirtualMachineImpl extends MirrorImpl {

    private HotSpotAgent     saAgent = new HotSpotAgent();
    private VM               saVM;
    private Universe         saUniverse;
    private SystemDictionary saSystemDictionary;
    private ObjectHeap       saObjectHeap;

    VM saVM() {
        return saVM;
    }

    SystemDictionary saSystemDictionary() {
        return saSystemDictionary;
    }
    
    Universe saUniverse() {
        return saUniverse;
    }

    ObjectHeap saObjectHeap() {
        return saObjectHeap;
    }

    VirtualMachineManager vmmgr;

    // Per-vm singletons for primitive types and for void.
    final PrimitiveTypeImpl theBooleanType;
    final PrimitiveTypeImpl theByteType;
    final PrimitiveTypeImpl theCharType;
    final PrimitiveTypeImpl theShortType;
    final PrimitiveTypeImpl theIntegerType;
    final PrimitiveTypeImpl theLongType;
    final PrimitiveTypeImpl theFloatType;
    final PrimitiveTypeImpl theDoubleType;

    final VoidTypeImpl theVoidType;

    private final VoidValueImpl voidVal;

    private final Map<Klass, ReferenceTypeImpl> typesByKlass = new HashMap<Klass, ReferenceTypeImpl>();
    private final Map<Long, ReferenceTypeImpl>  typesById = new HashMap<Long, ReferenceTypeImpl>();
    private boolean   retrievedAllTypes = false;
    private List<ReferenceTypeImpl>      bootstrapClasses;      // all bootstrap classes
    private ArrayList<ThreadReferenceImpl> allThreads;
    private ArrayList<ThreadGroupReferenceImpl> topLevelGroups;
    final   int       sequenceNumber;

    // ObjectReference cache
    // "objectsByID" protected by "synchronized(this)".
    private final Map<Oop, SoftObjectReference>            objectsByID = new HashMap<Oop, SoftObjectReference>();
    private final ReferenceQueue referenceQueue = new ReferenceQueue();

    // names of some well-known classes to jdi
    final String javaLangString = "java/lang/String";
    final String javaLangThread = "java/lang/Thread";
    final String javaLangThreadGroup = "java/lang/ThreadGroup";
    final String javaLangClass = "java/lang/Class";
    final String javaLangClassLoader = "java/lang/ClassLoader";

    // used in ReferenceTypeImpl.isThrowableBacktraceField
    final String javaLangThrowable = "java/lang/Throwable";

    // names of classes used in array assignment check
    // refer to ArrayTypeImpl.isAssignableTo
    final String javaLangObject = "java/lang/Object";
    final String javaLangCloneable = "java/lang/Cloneable";
    final String javaIoSerializable = "java/io/Serializable";

    // symbol used in ClassTypeImpl.isEnum check
    final String javaLangEnum = "java/lang/Enum";

    // name of the current default stratum
    private String defaultStratum;

    private void init() {
        saVM = VM.getVM();
        saUniverse = saVM.getUniverse();
        saSystemDictionary = saVM.getSystemDictionary();
        saObjectHeap = saVM.getObjectHeap();
    }

    static public VirtualMachineImpl createVirtualMachineForCorefile(VirtualMachineManager mgr,
                                                                     String javaExecutableName,
                                                                     String coreFileName,
                                                                     int sequenceNumber)
        throws Exception {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(coreFileName != null, "SA VirtualMachineImpl: core filename = null is not yet implemented");
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(javaExecutableName != null, "SA VirtualMachineImpl: java executable = null is not yet implemented");
        }

        VirtualMachineImpl myvm = new VirtualMachineImpl(mgr, sequenceNumber);
        try {
            myvm.saAgent.attach(javaExecutableName, coreFileName);
            myvm.init();
        } catch (Exception ee) {
            myvm.saAgent.detach();
            throw ee;
        }
        return myvm;
    }

    static public VirtualMachineImpl createVirtualMachineForPID(VirtualMachineManager mgr,
                                                                int pid,
                                                                int sequenceNumber)
        throws Exception {

        VirtualMachineImpl myvm = new VirtualMachineImpl(mgr, sequenceNumber);
        try {
            myvm.saAgent.attach(pid);
            myvm.init();
        } catch (Exception ee) {
            myvm.saAgent.detach();
            throw ee;
        }
        return myvm;
    }

    static public VirtualMachineImpl createVirtualMachineForServer(VirtualMachineManager mgr,
                                                                String server,
                                                                int sequenceNumber)
        throws Exception {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(server != null, "SA VirtualMachineImpl: DebugServer = null is not yet implemented");
        }

        VirtualMachineImpl myvm = new VirtualMachineImpl(mgr, sequenceNumber);
        try {
            myvm.saAgent.attach(server);
            myvm.init();
        } catch (Exception ee) {
            myvm.saAgent.detach();
            throw ee;
        }
        return myvm;
    }


    VirtualMachineImpl(VirtualMachineManager mgr, int sequenceNumber) {
        super(null);  // Can't use super(this)
        vm = this;

        this.sequenceNumber = sequenceNumber;
        this.vmmgr = mgr;

        // By default SA agent classes prefer Windows process debugger
        // to windbg debugger. SA expects special properties to be set
        // to choose other debuggers. We will set those here before
        // attaching to SA agent.

        System.setProperty("sun.jvm.hotspot.debugger.useWindbgDebugger", "true");

        theBooleanType = new PrimitiveTypeImpl.Boolean(this);
        theByteType = new PrimitiveTypeImpl.Byte(this);
        theCharType = new PrimitiveTypeImpl.Char(this);
        theShortType = new PrimitiveTypeImpl.Short(this);
        theIntegerType = new PrimitiveTypeImpl.Integer(this);
        theLongType = new PrimitiveTypeImpl.Long(this);
        theFloatType = new PrimitiveTypeImpl.Float(this);
        theDoubleType = new PrimitiveTypeImpl.Double(this);

        theVoidType = new VoidTypeImpl(this);
        voidVal = new VoidValueImpl(this);
    }

    // we reflectively use newly spec'ed class because our ALT_BOOTDIR
    // is 1.4.2 and not 1.5.
    private static Class vmCannotBeModifiedExceptionClass = null;
    void throwNotReadOnlyException(String operation) {
        RuntimeException re;
        if (vmCannotBeModifiedExceptionClass == null) {
            try {
                vmCannotBeModifiedExceptionClass = Class.forName("com.sun.jdi.VMCannotBeModifiedException");
            } catch (ClassNotFoundException cnfe) {
                vmCannotBeModifiedExceptionClass = UnsupportedOperationException.class;
            }
        }
        try {
            re = (RuntimeException) vmCannotBeModifiedExceptionClass.newInstance();
        } catch (Exception exp) {
            re = new RuntimeException(exp.getMessage());
        }
        throw re;
    }

    public boolean equals(Object obj) {
        // Oh boy; big recursion troubles if we don't have this!
        // See MirrorImpl.equals
        return this == obj;
    }

    public int hashCode() {
        // big recursion if we don't have this. See MirrorImpl.hashCode
        return System.identityHashCode(this);
    }

    public List<ReferenceTypeImpl> classesByName(String className) {
        String signature = JNITypeParser.typeNameToSignature(className);
        List<ReferenceTypeImpl> list;
        if (!retrievedAllTypes) {
            retrieveAllClasses();
        }
        list = findReferenceTypes(signature);
        return Collections.unmodifiableList(list);
    }

    public List<ReferenceTypeImpl> allClasses() {
        if (!retrievedAllTypes) {
            retrieveAllClasses();
        }
        ArrayList<ReferenceTypeImpl> a;
        synchronized (this) {
            a = new ArrayList<ReferenceTypeImpl>(typesById.values());
        }
        return Collections.unmodifiableList(a);
    }

    // classes loaded by bootstrap loader
    List<ReferenceTypeImpl> bootstrapClasses() {
        if (bootstrapClasses == null) {
            bootstrapClasses = new ArrayList<ReferenceTypeImpl>();
            for (Object o : allClasses()) {
                ReferenceTypeImpl type = (ReferenceTypeImpl) o;
                if (type.classLoader() == null) {
                    bootstrapClasses.add(type);
                }
            }
        }
        return bootstrapClasses;
    }

    public synchronized List<ReferenceTypeImpl> findReferenceTypes(String signature) {
        // The signature could be Lx/y/z; or [....
        // If it is Lx/y/z; the internal type name is x/y/x
        // for array klasses internal type name is same as
        // signature
        String typeName;
        if (signature.charAt(0) == 'L') {
            typeName = signature.substring(1, signature.length() - 1);
        } else {
            typeName = signature;
        }

        List<ReferenceTypeImpl> list = new ArrayList<ReferenceTypeImpl>(1);
        for (ReferenceTypeImpl type : typesById.values()) {
            if (type.name().equals(typeName)) {
                list.add(type);
            }
        }
        return list;
    }

    private void retrieveAllClasses() {
        final Collection<Klass> saKlasses = CompatibilityHelper.INSTANCE.allClasses(saSystemDictionary, saVM);

        // Hold lock during processing to improve performance
        // and to have safe check/set of retrievedAllTypes
        synchronized (this) {
            if (!retrievedAllTypes) {
                // Number of classes
                for (Klass saKlass : saKlasses) {
                    referenceType(saKlass);
                }
                retrievedAllTypes = true;
            }
        }
    }

    synchronized ReferenceTypeImpl referenceType(Klass kk) {
        ReferenceTypeImpl retType = typesByKlass.get(kk);
        if (retType == null) {
            retType = addReferenceType(kk);
        }
        return retType;
    }

    private synchronized ReferenceTypeImpl addReferenceType(Klass kk) {
        ReferenceTypeImpl newRefType;
        if (kk instanceof ObjArrayKlass || kk instanceof TypeArrayKlass) {
            newRefType = new ArrayTypeImpl(this, (ArrayKlass)kk);
        } else if (kk instanceof InstanceKlass) {
            if (kk.isInterface()) {
                newRefType = new InterfaceTypeImpl(this, (InstanceKlass)kk);
            } else {
                newRefType = new ClassTypeImpl(this, (InstanceKlass)kk);
            }
        } else {
            throw new RuntimeException("should not reach here:" + kk);
        }

        typesByKlass.put(kk, newRefType);
        typesById.put(newRefType.uniqueID(), newRefType);
        return newRefType;
    }

    private List<ThreadReferenceImpl> getAllThreads() {
        if (allThreads == null) {
            allThreads = new ArrayList<ThreadReferenceImpl>(10);  // Might be enough, might not be
            for (JavaThread thread = saVM.getThreads().first(); thread != null; thread = thread.next()) {
                // refer to JvmtiEnv::GetAllThreads in jvmtiEnv.cpp.
                // filter out the hidden-from-external-view threads.
                if (!thread.isHiddenFromExternalView()) {
                    ThreadReferenceImpl myThread = threadMirror(thread);
                    allThreads.add(myThread);
                }
            }
        }
        return allThreads;
    }

    public List<ThreadReferenceImpl> allThreads() { //fixme jjh
        return Collections.unmodifiableList(getAllThreads());
    }

    public void suspend() {
        throwNotReadOnlyException("VirtualMachineImpl.suspend()");
    }

    public void resume() {
        throwNotReadOnlyException("VirtualMachineImpl.resume()");
    }

    public List<ThreadGroupReferenceImpl> topLevelThreadGroups() { //fixme jjh
        // The doc for ThreadGroup says that The top-level thread group
        // is the only thread group whose parent is null.  This means there is
        // only one top level thread group.  There will be a thread in this
        // group so we will just find a thread whose threadgroup has no parent
        // and that will be it.

        if (topLevelGroups == null) {
            topLevelGroups = new ArrayList<ThreadGroupReferenceImpl>(1);
            for (ThreadReferenceImpl threadReference : getAllThreads()) {
                ThreadReferenceImpl myThread = threadReference;
                ThreadGroupReferenceImpl myGroup = myThread.threadGroup();
                ThreadGroupReferenceImpl myParent = myGroup.parent();
                if (myGroup.parent() == null) {
                    topLevelGroups.add(myGroup);
                    break;
                }
            }
        }
        return  Collections.unmodifiableList(topLevelGroups);
    }

    public EventQueue eventQueue() {
        throwNotReadOnlyException("VirtualMachine.eventQueue()");
        return null;
    }

    public EventRequestManager eventRequestManager() {
        throwNotReadOnlyException("VirtualMachineImpl.eventRequestManager()");
        return null;
    }

    public BooleanValueImpl mirrorOf(boolean value) {
        return new BooleanValueImpl(this,value);
    }

    public ByteValueImpl mirrorOf(byte value) {
        return new ByteValueImpl(this,value);
    }

    public CharValueImpl mirrorOf(char value) {
        return new CharValueImpl(this,value);
    }

    public ShortValueImpl mirrorOf(short value) {
        return new ShortValueImpl(this,value);
    }

    public IntegerValueImpl mirrorOf(int value) {
        return new IntegerValueImpl(this,value);
    }

    public LongValueImpl mirrorOf(long value) {
        return new LongValueImpl(this,value);
    }

    public FloatValueImpl mirrorOf(float value) {
        return new FloatValueImpl(this,value);
    }

    public DoubleValueImpl mirrorOf(double value) {
        return new DoubleValueImpl(this,value);
    }

    public StringReferenceImpl mirrorOf(String value) {
        throwNotReadOnlyException("VirtualMachinestop.mirrorOf(String)");
        return null;
    }

    public VoidValueImpl mirrorOfVoid() {
        return voidVal;
    }


    public Process process() {
        throwNotReadOnlyException("VirtualMachine.process");
        return null;
    }

    public void dispose() {
        saAgent.detach();
//        notifyDispose();
    }

    public void exit(int exitCode) {
        throwNotReadOnlyException("VirtualMachine.exit(int)");
    }

    public boolean canBeModified() {
        return false;
    }

    public boolean canWatchFieldModification() {
        return false;
    }

    public boolean canWatchFieldAccess() {
        return false;
    }

    public boolean canGetBytecodes() {
        return true;
    }

    public boolean canGetSyntheticAttribute() {
        return true;
    }

    // FIXME: For now, all monitor capabilities are disabled
    public boolean canGetOwnedMonitorInfo() {
        return false;
    }

    public boolean canGetCurrentContendedMonitor() {
        return false;
    }

    public boolean canGetMonitorInfo() {
        return false;
    }

    // because this SA works only with 1.5 and update releases
    // this should always succeed unlike JVMDI/JDI.
    public boolean canGet1_5LanguageFeatures() {
        return true;
    }

    public boolean canUseInstanceFilters() {
        return false;
    }

    public boolean canRedefineClasses() {
        return false;
    }

    public boolean canAddMethod() {
        return false;
    }

    public boolean canUnrestrictedlyRedefineClasses() {
        return false;
    }

    public boolean canPopFrames() {
        return false;
    }

    public boolean canGetSourceDebugExtension() {
        // We can use InstanceKlass.getSourceDebugExtension only if
        // ClassFileParser parsed the info. But, ClassFileParser parses
        // SourceDebugExtension attribute only if corresponding JVMDI/TI
        // capability is set to true. Currently, vmStructs does not expose
        // JVMDI/TI capabilities and hence we conservatively assume false.
        return false;
    }

    public boolean canRequestVMDeathEvent() {
        return false;
    }

    // new method since 1.6
    public boolean canForceEarlyReturn() {
        return false;
    }

    // new method since 1.6
    public boolean canGetConstantPool() {
        return true;
    }

    // new method since 1.6
    public boolean canGetClassFileVersion() {
        return true;
    }

    // new method since 1.6.
    public boolean canGetMethodReturnValues() {
        return false;
    }

    // new method since 1.6
    // Real body will be supplied later.
    public boolean canGetInstanceInfo() {
        return true;
    }

    // new method since 1.6
    public boolean canUseSourceNameFilters() {
        return false;
    }

    // new method since 1.6.
    public boolean canRequestMonitorEvents() {
        return false;
    }

    // new method since 1.6.
    public boolean canGetMonitorFrameInfo() {
        return true;
    }

    // new method since 1.6
    // Real body will be supplied later.
    public long[] instanceCounts(List<? extends ReferenceTypeImpl> classes) {
        if (!canGetInstanceInfo()) {
            throw new UnsupportedOperationException(
                      "target does not support getting instances");
        }

        int size = classes.size();
        final Map<Address, Long> instanceMap = new HashMap<Address, Long>(size);

        boolean allAbstractClasses = true;
        for (Object aClass : classes) {
            ReferenceTypeImpl rti = (ReferenceTypeImpl) aClass;
            instanceMap.put(CompatibilityHelper.INSTANCE.getAddress(rti.ref()), 0L);
            if (!(rti.isAbstract() || (rti instanceof InterfaceTypeImpl))) {
                allAbstractClasses = false;
            }
        }

        if (allAbstractClasses) {
            return new long[size];
        }

        saObjectHeap.iterate(new DefaultHeapVisitor() {
            public boolean doObj(Oop oop) {
                Address klassAddress = CompatibilityHelper.INSTANCE.getKlassAddress(oop);
                Long current = instanceMap.get(klassAddress);
                if (current != null) {
                    instanceMap.put(klassAddress, current + 1);
                }
                return false;
            }
        });

        final long[] retValue = new long[size] ;
        for (int i = 0; i < retValue.length; i++) {
            retValue[i] = instanceMap.get(CompatibilityHelper.INSTANCE.getAddress(classes.get(i).ref()));
        }

        return retValue;
    }

    private List<String> getPath (String pathName) {
        String cp = saVM.getSystemProperty(pathName);
        if (cp == null) {
            return Collections.emptyList();
        }
        String pathSep = saVM.getSystemProperty("path.separator");
        ArrayList<String> al = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(cp, pathSep);
        while (st.hasMoreTokens()) {
            al.add(st.nextToken());
        }
        al.trimToSize();
        return al;
    }

    public List<String> classPath() {
        return getPath("java.class.path");
    }

    public List<String> bootClassPath() {
        return getPath("sun.boot.class.path");
    }

    public String baseDirectory() {
        return saVM.getSystemProperty("user.dir");
    }

    public void setDefaultStratum(String stratum) {
        defaultStratum = stratum;
    }

    public String getDefaultStratum() {
        return defaultStratum;
    }

    public String description() {
//        String version_format = ResourceBundle.getBundle("com.sun.tools.jdi.resources.jdi").getString("version_format");
        String version_format = "Java Debug Interface (Reference Implementation) version {0}.{1} \\n{2}";
        return java.text.MessageFormat.format(version_format,
                                              "" + vmmgr.majorInterfaceVersion(),
                                              "" + vmmgr.minorInterfaceVersion(),
                                              name());
    }

    public String version() {
        return saVM.getSystemProperty("java.version");
    }

    public String name() {
        return "JVM version " + version() +
                " (" + saVM.getSystemProperty("java.vm.name") + ", " + saVM.getSystemProperty("java.vm.info") + ")";
    }

    public int jdwpMajor() {
        return vmmgr.majorInterfaceVersion();
    }

    public int jdwpMinor() {
        return vmmgr.minorInterfaceVersion();
    }

    // from interface Mirror
    public VirtualMachineImpl virtualMachine() {
        return this;
    }

    public String toString() {
        return name();
    }

    public void setDebugTraceMode(int traceFlags) {
        // spec. says output is implementation dependent
        // and trace mode may be ignored. we ignore it :-)
    }

    // heap walking API

    // capability check
    public boolean canWalkHeap() {
        return true;
    }

    // return a list of all objects in heap
    public List<ObjectReferenceImpl> allObjects() {
        final List<ObjectReferenceImpl> objects = new ArrayList<ObjectReferenceImpl>(0);
        saObjectHeap.iterate(
                new DefaultHeapVisitor() {
                    public boolean doObj(Oop oop) {
                        objects.add(objectMirror(oop));
                        return false;
                    }
                });
        return objects;
    }

    // equivalent to objectsByType(type, true)
    public List<ObjectReferenceImpl> objectsByType(ReferenceTypeImpl type) {
        return objectsByType(type, true);
    }

    // returns objects of type exactly equal to given type
    private List<ObjectReferenceImpl> objectsByExactType(ReferenceTypeImpl type) {
        final List<ObjectReferenceImpl> objects = new ArrayList<ObjectReferenceImpl>(0);
        final Klass givenKls = type.ref();
        saObjectHeap.iterate(new DefaultHeapVisitor() {
            public boolean doObj(Oop oop) {
                if (givenKls.equals(oop.getKlass())) {
                    objects.add(objectMirror(oop));
                }
                return false;
            }
        });
        return objects;
    }

    // returns objects of given type as well as it's subtypes
    private List<ObjectReferenceImpl> objectsBySubType(ReferenceTypeImpl type) {
        final List<ObjectReferenceImpl> objects = new ArrayList<ObjectReferenceImpl>(0);
        final ReferenceTypeImpl givenType = type;
        saObjectHeap.iterate(new DefaultHeapVisitor() {
            public boolean doObj(Oop oop) {
                ReferenceTypeImpl curType = referenceType(oop.getKlass());
                if (curType.isAssignableTo(givenType)) {
                    objects.add(objectMirror(oop));
                }
                return false;
            }
        });
        return objects;
    }

    // includeSubtypes - do you want to include subclass/subtype instances of given
    // ReferenceType or do we want objects of exact type only?
    public List<ObjectReferenceImpl> objectsByType(ReferenceTypeImpl type, boolean includeSubtypes) {
        Klass kls = type.ref();
        if (kls instanceof InstanceKlass) {
            InstanceKlass ik = (InstanceKlass) kls;
            // if the Klass is final or if there are no subklasses loaded yet
            if (ik.getAccessFlagsObj().isFinal() || ik.getSubklassKlass() == null) {
                includeSubtypes = false;
            }
        } else {
            // no subtypes for primitive array types
            ArrayTypeImpl arrayType = (ArrayTypeImpl) type;
            try {
                TypeImpl componentType = arrayType.componentType();
                if (componentType instanceof PrimitiveTypeImpl) {
                    includeSubtypes = false;
                }
            } catch (ClassNotLoadedException cnle) {
                // ignore. component type not yet loaded
            }
        }

        if (includeSubtypes) {
            return objectsBySubType(type);
        } else {
            return objectsByExactType(type);
        }
    }

    TypeImpl findBootType(String signature) throws ClassNotLoadedException {
        for (ReferenceTypeImpl type : allClasses()) {
            if ((type.classLoader() == null) && (type.signature().equals(signature))) {
                return type;
            }
        }
        JNITypeParser parser = new JNITypeParser(signature);
        throw new ClassNotLoadedException(parser.typeName(), "Type " + parser.typeName() + " not loaded");
    }

    PrimitiveTypeImpl primitiveTypeMirror(char tag) {
        switch (tag) {
        case 'Z':
                return theBooleanType;
        case 'B':
                return theByteType;
        case 'C':
                return theCharType;
        case 'S':
                return theShortType;
        case 'I':
                return theIntegerType;
        case 'J':
                return theLongType;
        case 'F':
                return theFloatType;
        case 'D':
                return theDoubleType;
        default:
                throw new IllegalArgumentException("Unrecognized primitive tag " + tag);
        }
    }

    private void processQueue() {
        Reference ref;
        while ((ref = referenceQueue.poll()) != null) {
            removeObjectMirror((SoftObjectReference)ref);
        }
    }

    // Address value is used as uniqueID by ObjectReferenceImpl
    long getAddressValue(Address address) {
        return vm.saVM.getDebugger().getAddressValue(address);
    }

    public synchronized ObjectReferenceImpl objectMirror(long id) {
        for (SoftObjectReference value : objectsByID.values()) {
            if (value != null) {
                ObjectReferenceImpl object = value.object();
                if (object.uniqueID() == id) {
                    return object;
                }
            }
        }
        throw new IllegalStateException("Object with id " + id + " not found");
    }

    synchronized ObjectReferenceImpl objectMirror(Oop key) {

        // Handle any queue elements that are not strongly reachable
        processQueue();

        if (key == null) {
            return null;
        }
        ObjectReferenceImpl object = null;

        /*
         * Attempt to retrieve an existing object object reference
         */
        SoftObjectReference ref = objectsByID.get(key);
        if (ref != null) {
            object = ref.object();
        }

        /*
         * If the object wasn't in the table, or it's soft reference was
         * cleared, create a new instance.
         */
        if (object == null) {
            if (key instanceof Instance) {
                // look for well-known classes
                Symbol classNameSymbol = key.getKlass().getName();
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(classNameSymbol != null, "Null class name");
                }
                String className = classNameSymbol.asString();
                Instance inst = (Instance) key;
                if (className.equals(javaLangString)) {
                    object = new StringReferenceImpl(this, inst);
                } else if (className.equals(javaLangThread)) {
                    object = new ThreadReferenceImpl(this, inst);
                } else if (className.equals(javaLangThreadGroup)) {
                    object = new ThreadGroupReferenceImpl(this, inst);
                } else if (className.equals(javaLangClass)) {
                    object = new ClassObjectReferenceImpl(this, inst);
                } else if (className.equals(javaLangClassLoader)) {
                    object = new ClassLoaderReferenceImpl(this, inst);
                } else {
                    // not a well-known class. But the base class may be
                    // one of the known classes.
                    Klass kls = key.getKlass().getSuper();
                    while (kls != null) {
                       className = kls.getName().asString();
                       // java.lang.Class and java.lang.String are final classes
                       if (className.equals(javaLangThread)) {
                          object = new ThreadReferenceImpl(this, inst);
                          break;
                       } else if(className.equals(javaLangThreadGroup)) {
                          object = new ThreadGroupReferenceImpl(this, inst);
                          break;
                       } else if (className.equals(javaLangClassLoader)) {
                          object = new ClassLoaderReferenceImpl(this, inst);
                          break;
                       }
                       kls = kls.getSuper();
                    }

                    if (object == null) {
                       // create generic object reference
                       object = new ObjectReferenceImpl(this, inst);
                    }
                }
            } else if (key instanceof TypeArray) {
                object = new ArrayReferenceImpl(this, (Array) key);
            } else if (key instanceof ObjArray) {
                object = new ArrayReferenceImpl(this, (Array) key);
            } else {
                throw new RuntimeException("unexpected object type " + key);
            }
            ref = new SoftObjectReference(key, object, referenceQueue);

            /*
             * If there was no previous entry in the table, we add one here
             * If the previous entry was cleared, we replace it here.
             */
            objectsByID.put(key, ref);
        } else {
            ref.incrementCount();
        }

        return object;
    }

    synchronized void removeObjectMirror(SoftObjectReference ref) {
        /*
         * This will remove the soft reference if it has not been
         * replaced in the cache.
         */
        objectsByID.remove(ref.key());
    }

    StringReferenceImpl stringMirror(Instance id) {
        return (StringReferenceImpl) objectMirror(id);
    }

    ArrayReferenceImpl arrayMirror(Array id) {
       return (ArrayReferenceImpl) objectMirror(id);
    }

    ThreadReferenceImpl threadMirror(Instance id) {
        return (ThreadReferenceImpl) objectMirror(id);
    }

    ThreadReferenceImpl threadMirror(JavaThread jt) {
        return (ThreadReferenceImpl) objectMirror(jt.getThreadObj());
    }

    ThreadGroupReferenceImpl threadGroupMirror(Instance id) {
        return (ThreadGroupReferenceImpl) objectMirror(id);
    }

    ClassLoaderReferenceImpl classLoaderMirror(Instance id) {
        return (ClassLoaderReferenceImpl) objectMirror(id);
    }

    ClassObjectReferenceImpl classObjectMirror(Instance id) {
        return (ClassObjectReferenceImpl) objectMirror(id);
    }

    // Use of soft refs and caching stuff here has to be re-examined.
    //  It might not make sense for JDI - SA.
    static private class SoftObjectReference extends SoftReference<ObjectReferenceImpl> {
       int count;
       Oop key;

       SoftObjectReference(Oop key, ObjectReferenceImpl mirror, ReferenceQueue queue) {
           super(mirror, queue);
           this.count = 1;
           this.key = key;
       }

       int count() {
           return count;
       }

       void incrementCount() {
           count++;
       }

       Oop key() {
           return key;
       }

       ObjectReferenceImpl object() {
           return get();
       }
   }

    public ThreadReferenceImpl getThreadById(long id) {
        for (ThreadReferenceImpl thread : allThreads()) {
            if (thread.uniqueID() == id) {
                return thread;
            }
        }
        throw new IllegalStateException("Thread with id " + id + " not found");
    }

    public synchronized ReferenceTypeImpl getReferenceTypeById(long id) {
        ReferenceTypeImpl res = typesById.get(id);
        if (res == null) {
            throw new IllegalStateException("ReferenceType with id " + id + " not found");
        }
        return res;
    }

    public ThreadGroupReferenceImpl getThreadGroupReferenceById(long id) {
        for (ThreadReferenceImpl thread : allThreads()) {
            ThreadGroupReferenceImpl threadGroup = thread.threadGroup();
            if (threadGroup.uniqueID() == id) {
                return threadGroup;
            }
        }
        throw new IllegalStateException("ThreadGroup with id " + id + " not found");
    }
}
