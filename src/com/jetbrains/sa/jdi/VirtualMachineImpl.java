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

import com.sun.jdi.*;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.request.EventRequestManager;
import sun.jvm.hotspot.HotSpotAgent;
import sun.jvm.hotspot.debugger.Address;
import sun.jvm.hotspot.memory.SymbolTable;
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

public class VirtualMachineImpl extends MirrorImpl implements PathSearchingVirtualMachine {

    private HotSpotAgent     saAgent = new HotSpotAgent();
    private VM               saVM;
    private Universe         saUniverse;
    private SystemDictionary saSystemDictionary;
    private SymbolTable      saSymbolTable;
    private ObjectHeap       saObjectHeap;

    VM saVM() {
        return saVM;
    }

    SystemDictionary saSystemDictionary() {
        return saSystemDictionary;
    }

    SymbolTable saSymbolTable() {
        return saSymbolTable;
    }

    Universe saUniverse() {
        return saUniverse;
    }

    ObjectHeap saObjectHeap() {
        return saObjectHeap;
    }

    VirtualMachineManager vmmgr;

    private final ThreadGroup             threadGroupForJDI;

    // Per-vm singletons for primitive types and for void.
    // singleton-ness protected by "synchronized(this)".
    private BooleanType theBooleanType;
    private ByteType    theByteType;
    private CharType    theCharType;
    private ShortType   theShortType;
    private IntegerType theIntegerType;
    private LongType    theLongType;
    private FloatType   theFloatType;
    private DoubleType  theDoubleType;

    private VoidType    theVoidType;

    private VoidValue voidVal;
    private Map<Klass, ReferenceTypeImpl> typesByKlass;
    private Map<Long, ReferenceTypeImpl>  typesById;
    private List<ReferenceTypeImpl>       typesBySignature;
    private boolean   retrievedAllTypes = false;
    private List      bootstrapClasses;      // all bootstrap classes
    private ArrayList<ThreadReference> allThreads;
    private ArrayList topLevelGroups;
    final   int       sequenceNumber;

    // ObjectReference cache
    // "objectsByID" protected by "synchronized(this)".
    private final Map<Oop, SoftObjectReference>            objectsByID = new HashMap<Oop, SoftObjectReference>();
    private final ReferenceQueue referenceQueue = new ReferenceQueue();

    // names of some well-known classes to jdi
    private Symbol javaLangString;
    private Symbol javaLangThread;
    private Symbol javaLangThreadGroup;
    private Symbol javaLangClass;
    private Symbol javaLangClassLoader;

    // used in ReferenceTypeImpl.isThrowableBacktraceField
    private Symbol javaLangThrowable;

    // names of classes used in array assignment check
    // refer to ArrayTypeImpl.isAssignableTo
    private Symbol javaLangObject;
    private Symbol javaLangCloneable;
    private Symbol javaIoSerializable;

    // symbol used in ClassTypeImpl.isEnum check
    private Symbol javaLangEnum;

    Symbol javaLangObject() {
        return javaLangObject;
    }

    Symbol javaLangCloneable() {
        return javaLangCloneable;
    }

    Symbol javaIoSerializable() {
        return javaIoSerializable;
    }

    Symbol javaLangEnum() {
        return javaLangEnum;
    }

    Symbol javaLangThrowable() {
        return javaLangThrowable;
    }

    // name of the current default stratum
    private String defaultStratum;

    // initialize known class name symbols
    private void initClassNameSymbols() {
        SymbolTable st = saSymbolTable();
        javaLangString = st.probe("java/lang/String");
        javaLangThread = st.probe("java/lang/Thread");
        javaLangThreadGroup = st.probe("java/lang/ThreadGroup");
        javaLangClass = st.probe("java/lang/Class");
        javaLangClassLoader = st.probe("java/lang/ClassLoader");
        javaLangThrowable = st.probe("java/lang/Throwable");
        javaLangObject = st.probe("java/lang/Object");
        javaLangCloneable = st.probe("java/lang/Cloneable");
        javaIoSerializable = st.probe("java/io/Serializable");
        javaLangEnum = st.probe("java/lang/Enum");
    }

    private void init() {
        saVM = VM.getVM();
        saUniverse = saVM.getUniverse();
        saSystemDictionary = saVM.getSystemDictionary();
        saSymbolTable = saVM.getSymbolTable();
        saObjectHeap = saVM.getObjectHeap();
        initClassNameSymbols();
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


    VirtualMachineImpl(VirtualMachineManager mgr, int sequenceNumber)
        throws Exception {
        super(null);  // Can't use super(this)
        vm = this;

        this.sequenceNumber = sequenceNumber;
        this.vmmgr = mgr;

        /* Create ThreadGroup to be used by all threads servicing
         * this VM.
         */
        threadGroupForJDI = new ThreadGroup("JDI [" +
                                            this.hashCode() + "]");

        ((com.sun.tools.jdi.VirtualMachineManagerImpl)mgr).addVirtualMachine(this);

        // By default SA agent classes prefer Windows process debugger
        // to windbg debugger. SA expects special properties to be set
        // to choose other debuggers. We will set those here before
        // attaching to SA agent.

        System.setProperty("sun.jvm.hotspot.debugger.useWindbgDebugger", "true");
    }

    // we reflectively use newly spec'ed class because our ALT_BOOTDIR
    // is 1.4.2 and not 1.5.
    private static Class vmCannotBeModifiedExceptionClass = null;
    void throwNotReadOnlyException(String operation) {
        RuntimeException re = null;
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

    public List<ReferenceType> classesByName(String className) {
        String signature = JNITypeParser.typeNameToSignature(className);
        List<ReferenceType> list;
        if (!retrievedAllTypes) {
            retrieveAllClasses();
        }
        list = findReferenceTypes(signature);
        return Collections.unmodifiableList(list);
    }

    public List<ReferenceType> allClasses() {
        if (!retrievedAllTypes) {
            retrieveAllClasses();
        }
        ArrayList<ReferenceType> a;
        synchronized (this) {
            a = new ArrayList<ReferenceType>(typesBySignature);
        }
        return Collections.unmodifiableList(a);
    }

    // classes loaded by bootstrap loader
    List bootstrapClasses() {
        if (bootstrapClasses == null) {
            bootstrapClasses = new ArrayList();
            List all = allClasses();
            for (Iterator itr = all.iterator(); itr.hasNext();) {
               ReferenceType type = (ReferenceType) itr.next();
               if (type.classLoader() == null) {
                   bootstrapClasses.add(type);
               }
            }
        }
        return bootstrapClasses;
    }

    public synchronized List<ReferenceType> findReferenceTypes(String signature) {
        if (typesByKlass == null) {
            return new ArrayList<ReferenceType>(0);
        }

        // we haven't sorted types by signatures. But we can take
        // advantage of comparing symbols instead of name. In the worst
        // case, we will be comparing N addresses rather than N strings
        // where N being total no. of classes in allClasses() list.

        // The signature could be Lx/y/z; or [....
        // If it is Lx/y/z; the internal type name is x/y/x
        // for array klasses internal type name is same as
        // signature
        String typeName = null;
        if (signature.charAt(0) == 'L') {
            typeName = signature.substring(1, signature.length() - 1);
        } else {
            typeName = signature;
        }

        Symbol typeNameSym = saSymbolTable().probe(typeName);
        // if there is no symbol in VM, then we wouldn't have that type
        if (typeNameSym == null) {
            return new ArrayList<ReferenceType>(0);
        }

        Iterator iter = typesBySignature.iterator();
        List<ReferenceType> list = new ArrayList<ReferenceType>();
        while (iter.hasNext()) {
            // We have cached type name as symbol in reference type
            ReferenceTypeImpl type = (ReferenceTypeImpl)iter.next();
            if (typeNameSym.equals(type.typeNameAsSymbol())) {
                list.add(type);
            }
        }
        return list;
    }

    private void retrieveAllClasses() {
        final List<Klass> saKlasses = CompatibilityHelper.INSTANCE.allClasses(saSystemDictionary, saVM);

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
        ReferenceTypeImpl retType = null;
        if (typesByKlass != null) {
            retType = typesByKlass.get(kk);
        }
        if (retType == null) {
            retType = addReferenceType(kk);
        }
        return retType;
    }

    private void initReferenceTypes() {
        typesByKlass = new HashMap<Klass, ReferenceTypeImpl>();
        typesById = new HashMap<Long, ReferenceTypeImpl>();
        typesBySignature = new ArrayList<ReferenceTypeImpl>();
    }

    private synchronized ReferenceTypeImpl addReferenceType(Klass kk) {
        if (typesByKlass == null) {
            initReferenceTypes();
        }
        ReferenceTypeImpl newRefType = null;
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
        typesBySignature.add(newRefType);
        return newRefType;
    }

    ThreadGroup threadGroupForJDI() {
        return threadGroupForJDI;
    }

    public void redefineClasses(Map classToBytes) {
        throwNotReadOnlyException("VirtualMachineImpl.redefineClasses()");
    }

    private List<ThreadReference> getAllThreads() {
        if (allThreads == null) {
            allThreads = new ArrayList(10);  // Might be enough, might not be
            for (JavaThread thread =
                 saVM.getThreads().first(); thread != null;
                 thread = thread.next()) {
                // refer to JvmtiEnv::GetAllThreads in jvmtiEnv.cpp.
                // filter out the hidden-from-external-view threads.
                if (thread.isHiddenFromExternalView() == false) {
                    ThreadReferenceImpl myThread = threadMirror(thread);
                    allThreads.add(myThread);
                }
            }
        }
        return allThreads;
    }

    public List<ThreadReference> allThreads() { //fixme jjh
        return Collections.unmodifiableList(getAllThreads());
    }

    public void suspend() {
        throwNotReadOnlyException("VirtualMachineImpl.suspend()");
    }

    public void resume() {
        throwNotReadOnlyException("VirtualMachineImpl.resume()");
    }

    public List topLevelThreadGroups() { //fixme jjh
        // The doc for ThreadGroup says that The top-level thread group
        // is the only thread group whose parent is null.  This means there is
        // only one top level thread group.  There will be a thread in this
        // group so we will just find a thread whose threadgroup has no parent
        // and that will be it.

        if (topLevelGroups == null) {
            topLevelGroups = new ArrayList(1);
            Iterator myIt = getAllThreads().iterator();
            while (myIt.hasNext()) {
                ThreadReferenceImpl myThread = (ThreadReferenceImpl)myIt.next();
                ThreadGroupReference myGroup = myThread.threadGroup();
                ThreadGroupReference myParent = myGroup.parent();
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

    public BooleanValue mirrorOf(boolean value) {
        return new BooleanValueImpl(this,value);
    }

    public ByteValue mirrorOf(byte value) {
        return new ByteValueImpl(this,value);
    }

    public CharValue mirrorOf(char value) {
        return new CharValueImpl(this,value);
    }

    public ShortValue mirrorOf(short value) {
        return new ShortValueImpl(this,value);
    }

    public IntegerValue mirrorOf(int value) {
        return new IntegerValueImpl(this,value);
    }

    public LongValue mirrorOf(long value) {
        return new LongValueImpl(this,value);
    }

    public FloatValue mirrorOf(float value) {
        return new FloatValueImpl(this,value);
    }

    public DoubleValue mirrorOf(double value) {
        return new DoubleValueImpl(this,value);
    }

    public StringReference mirrorOf(String value) {
        throwNotReadOnlyException("VirtualMachinestop.mirrorOf(String)");
        return null;
    }

    public VoidValue mirrorOfVoid() {
        if (voidVal == null) {
            voidVal = new VoidValueImpl(this);
        }
        return voidVal;
    }


    public Process process() {
        throwNotReadOnlyException("VirtualMachine.process");
        return null;
    }

    // dispose observer for Class re-use. refer to ConnectorImpl.
    private Observer disposeObserver;

    // ConnectorImpl loaded by a different class loader can not access it.
    // i.e., runtime package of <ConnectorImpl, L1> is not the same that of
    // <VirtualMachineImpl, L2> when L1 != L2. So, package private method
    // can be called reflectively after using setAccessible(true).

    void setDisposeObserver(Observer observer) {
       disposeObserver = observer;
    }

    private void notifyDispose() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(disposeObserver != null, "null VM.dispose observer");
        }
        disposeObserver.update(null, null);
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
    public long[] instanceCounts(List classes) {
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
            if (!(rti.isAbstract() || (rti instanceof InterfaceType))) {
                allAbstractClasses = false;
            }
        }

        if (allAbstractClasses) {
            return new long[size];
        }

        final boolean compressedKlassPointersEnabled = CompatibilityHelper.INSTANCE.isCompressedKlassPointersEnabled(saVM());
        saObjectHeap.iterate(new DefaultHeapVisitor() {
            public boolean doObj(Oop oop) {
                Address klassAddress = CompatibilityHelper.INSTANCE.getKlassAddress(compressedKlassPointersEnabled, oop);
                Long current = instanceMap.get(klassAddress);
                if (current != null) {
                    instanceMap.put(klassAddress, current + 1);
                }
                return false;
            }
        });

        final long[] retValue = new long[size] ;
        for (int i = 0; i < retValue.length; i++) {
            retValue[i] = instanceMap.get(CompatibilityHelper.INSTANCE.getAddress(((ReferenceTypeImpl)classes.get(i)).ref()));
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
        StringBuffer sb = new StringBuffer();
        sb.append("JVM version ");
        sb.append(version());
        sb.append(" (");
        sb.append(saVM.getSystemProperty("java.vm.name"));
        sb.append(", ");
        sb.append(saVM.getSystemProperty("java.vm.info"));
        sb.append(")");
        return sb.toString();
    }

    public int jdwpMajor() {
        return vmmgr.majorInterfaceVersion();
    }

    public int jdwpMinor() {
        return vmmgr.minorInterfaceVersion();
    }

    // from interface Mirror
    public VirtualMachine virtualMachine() {
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
    public List/*<ObjectReference>*/ allObjects() {
        final List objects = new ArrayList(0);
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
    public List/*<ObjectReference>*/ objectsByType(ReferenceType type) {
        return objectsByType(type, true);
    }

    // returns objects of type exactly equal to given type
    private List/*<ObjectReference>*/ objectsByExactType(ReferenceType type) {
        final List objects = new ArrayList(0);
        final Klass givenKls = ((ReferenceTypeImpl)type).ref();
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
    private List/*<ObjectReference>*/ objectsBySubType(ReferenceType type) {
        final List objects = new ArrayList(0);
        final ReferenceType givenType = type;
        saObjectHeap.iterate(new DefaultHeapVisitor() {
                public boolean doObj(Oop oop) {
                    ReferenceTypeImpl curType = (ReferenceTypeImpl) referenceType(oop.getKlass());
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
    public List/*<ObjectReference>*/ objectsByType(ReferenceType type, boolean includeSubtypes) {
        Klass kls = ((ReferenceTypeImpl)type).ref();
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
                Type componentType = arrayType.componentType();
                if (componentType instanceof PrimitiveType) {
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

    Type findBootType(String signature) throws ClassNotLoadedException {
        List types = allClasses();
        Iterator iter = types.iterator();
        while (iter.hasNext()) {
            ReferenceType type = (ReferenceType)iter.next();
            if ((type.classLoader() == null) &&
                (type.signature().equals(signature))) {
                return type;
            }
        }
        JNITypeParser parser = new JNITypeParser(signature);
        throw new ClassNotLoadedException(parser.typeName(),
                                         "Type " + parser.typeName() + " not loaded");
    }

    BooleanType theBooleanType() {
        if (theBooleanType == null) {
            synchronized(this) {
                if (theBooleanType == null) {
                    theBooleanType = new BooleanTypeImpl(this);
                }
            }
        }
        return theBooleanType;
    }

    ByteType theByteType() {
        if (theByteType == null) {
            synchronized(this) {
                if (theByteType == null) {
                    theByteType = new ByteTypeImpl(this);
                }
            }
        }
        return theByteType;
    }

    CharType theCharType() {
        if (theCharType == null) {
            synchronized(this) {
                if (theCharType == null) {
                    theCharType = new CharTypeImpl(this);
                }
            }
        }
        return theCharType;
    }

    ShortType theShortType() {
        if (theShortType == null) {
            synchronized(this) {
                if (theShortType == null) {
                    theShortType = new ShortTypeImpl(this);
                }
            }
        }
        return theShortType;
    }

    IntegerType theIntegerType() {
        if (theIntegerType == null) {
            synchronized(this) {
                if (theIntegerType == null) {
                    theIntegerType = new IntegerTypeImpl(this);
                }
            }
        }
        return theIntegerType;
    }

    LongType theLongType() {
        if (theLongType == null) {
            synchronized(this) {
                if (theLongType == null) {
                    theLongType = new LongTypeImpl(this);
                }
            }
        }
        return theLongType;
    }

    FloatType theFloatType() {
        if (theFloatType == null) {
            synchronized(this) {
                if (theFloatType == null) {
                    theFloatType = new FloatTypeImpl(this);
                }
            }
        }
        return theFloatType;
    }

    DoubleType theDoubleType() {
        if (theDoubleType == null) {
            synchronized(this) {
                if (theDoubleType == null) {
                    theDoubleType = new DoubleTypeImpl(this);
                }
            }
        }
        return theDoubleType;
    }

    VoidType theVoidType() {
        if (theVoidType == null) {
            synchronized(this) {
                if (theVoidType == null) {
                    theVoidType = new VoidTypeImpl(this);
                }
            }
        }
        return theVoidType;
    }

    PrimitiveType primitiveTypeMirror(char tag) {
        switch (tag) {
        case 'Z':
                return theBooleanType();
        case 'B':
                return theByteType();
        case 'C':
                return theCharType();
        case 'S':
                return theShortType();
        case 'I':
                return theIntegerType();
        case 'J':
                return theLongType();
        case 'F':
                return theFloatType();
        case 'D':
                return theDoubleType();
        default:
                throw new IllegalArgumentException("Unrecognized primitive tag " + tag);
        }
    }

    private void processQueue() {
        Reference ref;
        while ((ref = referenceQueue.poll()) != null) {
            SoftObjectReference softRef = (SoftObjectReference)ref;
            removeObjectMirror(softRef);
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
                Symbol className = key.getKlass().getName();
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(className != null, "Null class name");
                }
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
                       className = kls.getName();
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
    static private class SoftObjectReference extends SoftReference {
       int count;
       Object key;

       SoftObjectReference(Object key, ObjectReferenceImpl mirror,
                           ReferenceQueue queue) {
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

       Object key() {
           return key;
       }

       ObjectReferenceImpl object() {
           return (ObjectReferenceImpl)get();
       }
   }

    public ThreadReferenceImpl getThreadById(long id) {
        for (ThreadReference thread : allThreads()) {
            if (thread.uniqueID() == id) {
                return (ThreadReferenceImpl) thread;
            }
        }
        throw new IllegalStateException("Thread with id " + id + " not found");
    }

    public synchronized ReferenceTypeImpl getReferenceTypeById(long id) {
        if (typesById != null) {
            return typesById.get(id);
        }
        throw new IllegalStateException("ReferenceType with id " + id + " not found");
    }

    public ThreadGroupReferenceImpl getThreadGroupReferenceById(long id) {
        for (ThreadReference thread : allThreads()) {
            ThreadGroupReference threadGroup = thread.threadGroup();
            if (threadGroup.uniqueID() == id) {
                return (ThreadGroupReferenceImpl) threadGroup;
            }
        }
        throw new IllegalStateException("ThreadGroup with id " + id + " not found");
    }
}
