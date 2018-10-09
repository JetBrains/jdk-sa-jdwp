/*
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

package com.jetbrains.sa.jdwp;


import com.jetbrains.sa.jdi.*;
import com.sun.jdi.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Java(tm) Debug Wire Protocol
 */
@SuppressWarnings("unused")
public class JDWP {

    static class VirtualMachine {
        static final int COMMAND_SET = 1;
        private VirtualMachine() {}  // hide constructor

        /**
         * Returns the JDWP version implemented by the target VM.
         * The version string format is implementation dependent.
         */
        static class Version implements Command  {
            static final int COMMAND = 1;


            /**
             * Text information on the VM version
             */
            //final String description;

            /**
             * Major JDWP Version number
             */
            //final int jdwpMajor;

            /**
             * Minor JDWP Version number
             */
            //final int jdwpMinor;

            /**
             * Target VM JRE version, as in the java.version property
             */
            //final String vmVersion;

            /**
             * Target VM name, as in the java.vm.name property
             */
            //final String vmName;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                //description = answer.readString();
                answer.writeString(vm.vm.description());
                //jdwpMajor = answer.readInt();
                answer.writeInt(vm.vm.jdwpMajor());
                //jdwpMinor = answer.readInt();
                answer.writeInt(vm.vm.jdwpMinor());
                //vmVersion = answer.readString();
                answer.writeString(vm.vm.version());
                //vmName = answer.readString();
                answer.writeString(vm.vm.name());
            }
        }

        /**
         * Returns reference types for all the classes loaded by the target VM
         * which match the given signature.
         * Multple reference types will be returned if two or more class
         * loaders have loaded a class of the same name.
         * The search is confined to loaded classes only; no attempt is made
         * to load a class of the given signature.
         */
        static class ClassesBySignature implements Command  {
            static final int COMMAND = 2;

            static class ClassInfo {

                /**
                 * <a href="#JDWP_TypeTag">Kind</a> 
                 * of following reference type. 
                 */
                //final byte refTypeTag;

                /**
                 * Matching loaded reference type
                 */
                //final long typeID;

                /**
                 * The current class
                 * <a href="#JDWP_ClassStatus">status.</a>
                 */
                //final int status;

                public static void write(ReferenceTypeImpl referenceType, VirtualMachineImpl vm, PacketStream answer) {
                    //refTypeTag = answer.readByte();
                    answer.writeByte(referenceType.tag());
                    //typeID = answer.readClassRef();
                    answer.writeClassRef(referenceType.uniqueID());
                    //status = answer.readInt();
                    answer.writeInt(referenceType.ref().getClassStatus());
                }
            }


            /**
             * Number of reference types that follow.
             */
            //final ClassInfo[] classes;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                String signature = command.readString();
                List<com.sun.jdi.ReferenceType> referenceTypes = vm.vm.findReferenceTypes(signature);
                answer.writeInt(referenceTypes.size());
                for (com.sun.jdi.ReferenceType referenceType : referenceTypes) {
                    ClassInfo.write(((ReferenceTypeImpl) referenceType), vm, answer);
                }
//                //int classesCount = answer.readInt();
                //classes = new ClassInfo[classesCount];
                //for (int i = 0; i < classesCount; i++) {;
                    //classes[i] = new ClassInfo(vm, ps);
                //}
            }
        }

        /**
         * Returns reference types for all classes currently loaded by the
         * target VM.
         */
        static class AllClasses implements Command  {
            static final int COMMAND = 3;

            static class ClassInfo {

                /**
                 * <a href="#JDWP_TypeTag">Kind</a> 
                 * of following reference type. 
                 */
                //final byte refTypeTag;

                /**
                 * Loaded reference type
                 */
                //final long typeID;

                /**
                 * The JNI signature of the loaded reference type
                 */
                //final String signature;

                /**
                 * The current class
                 * <a href="#JDWP_ClassStatus">status.</a>
                 */
                //final int status;

                public static void write(ReferenceTypeImpl referenceType, VirtualMachineImpl vm, PacketStream answer) {
                    //refTypeTag = answer.readByte();
                    answer.writeByte(referenceType.tag());
                    //typeID = answer.readClassRef();
                    answer.writeClassRef(referenceType.uniqueID());
                    //signature = answer.readString();
                    answer.writeString(referenceType.signature());
                    //status = answer.readInt();
                    answer.writeInt(referenceType.ref().getClassStatus());
                }
            }


            /**
             * Number of reference types that follow.
             */
            //final ClassInfo[] classes;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                List<com.sun.jdi.ReferenceType> referenceTypes = vm.vm.allClasses();
                answer.writeInt(referenceTypes.size());
                for (com.sun.jdi.ReferenceType referenceType : referenceTypes) {
                    ClassInfo.write((ReferenceTypeImpl) referenceType, vm, answer);
                }
//                //int classesCount = answer.readInt();
                //classes = new ClassInfo[classesCount];
                //for (int i = 0; i < classesCount; i++) {;
                    //classes[i] = new ClassInfo(vm, ps);
                //}
            }
        }

        /**
         * Returns all threads currently running in the target VM .
         * The returned list contains threads created through
         * java.lang.Thread, all native threads attached to
         * the target VM through JNI, and system threads created
         * by the target VM. Threads that have not yet been started
         * and threads that have completed their execution are not
         * included in the returned list.
         */
        static class AllThreads implements Command  {
            static final int COMMAND = 4;


            /**
             * Number of threads that follow.
             */
            //final ThreadReferenceImpl[] threads;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                List<com.sun.jdi.ThreadReference> allThreads = vm.vm.allThreads();
//                //int threadsCount = answer.readInt();
                answer.writeInt(allThreads.size());
                for (com.sun.jdi.ThreadReference thread : allThreads) {
                    answer.writeObjectRef(thread.uniqueID());
                }
                //threads = new ThreadReferenceImpl[threadsCount];
                //for (int i = 0; i < threadsCount; i++) {;
                    //threads[i] = answer.readThreadReference();
                //}
            }
        }

        /**
         * Returns all thread groups that do not have a parent. This command
         * may be used as the first step in building a tree (or trees) of the
         * existing thread grouanswer.
         */
        static class TopLevelThreadGroups implements Command  {
            static final int COMMAND = 5;


            /**
             * Number of thread groups that follow.
             */
            //final ThreadGroupReferenceImpl[] groups;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                List list = vm.vm.topLevelThreadGroups();
//                //int groupsCount = answer.readInt();
                answer.writeInt(list.size());
                for (Object group : list) {
                    com.sun.jdi.ThreadGroupReference g = (com.sun.jdi.ThreadGroupReference) group;
                    answer.writeObjectRef(g.uniqueID());
                }
                //groups = new ThreadGroupReferenceImpl[groupsCount];
                //for (int i = 0; i < groupsCount; i++) {;
                    //groups[i] = answer.readThreadGroupReference();
                //}
            }
        }

        /**
         * Invalidates this virtual machine mirror.
         * The communication channel to the target VM is closed, and
         * the target VM prepares to accept another subsequent connection
         * from this debugger or another debugger, including the
         * following tasks:
         * <ul>
         * <li>All event requests are cancelled.
         * <li>All threads suspended by the thread-level
         * <a href="#JDWP_ThreadReference_Resume">resume</a> command
         * or the VM-level
         * <a href="#JDWP_VirtualMachine_Resume">resume</a> command
         * are resumed as many times as necessary for them to run.
         * <li>Garbage collection is re-enabled in all cases where it was
         * <a href="#JDWP_ObjectReference_DisableCollection">disabled</a>
         * </ul>
         * Any current method invocations executing in the target VM
         * are continued after the disconnection. Upon completion of any such
         * method invocation, the invoking thread continues from the
         * location where it was originally stopped.
         * <p>
         * Resources originating in
         * this VirtualMachine (ObjectReferences, ReferenceTypes, etc.)
         * will become invalid.
         */
        static class Dispose implements Command  {
            static final int COMMAND = 6;


            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                throw new VMDisconnectedException();
            }
        }

        /**
         * Returns the sizes of variably-sized data types in the target VM.
         * The returned values indicate the number of bytes used by the
         * identifiers in command and reply packets.
         */
        static class IDSizes implements Command  {
            static final int COMMAND = 7;


            /**
             * fieldID size in bytes 
             */
            //final int fieldIDSize;

            /**
             * methodID size in bytes 
             */
            //final int methodIDSize;

            /**
             * objectID size in bytes 
             */
            //final int objectIDSize;

            /**
             * referenceTypeID size in bytes 
             */
            //final int referenceTypeIDSize;

            /**
             * frameID size in bytes
             */
            //final int frameIDSize;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                //fieldIDSize = answer.readInt();
                answer.writeInt(vm.sizeofFieldRef);
                //methodIDSize = answer.readInt();
                answer.writeInt(vm.sizeofMethodRef);
                //objectIDSize = answer.readInt();
                answer.writeInt(vm.sizeofObjectRef);
                //referenceTypeIDSize = answer.readInt();
                answer.writeInt(vm.sizeofClassRef);
                //frameIDSize = answer.readInt();
                answer.writeInt(vm.sizeofFrameRef);
            }
        }

        /**
         * Suspends the execution of the application running in the target
         * VM. All Java threads currently running will be suspended.
         * <p>
         * Unlike java.lang.Thread.suspend,
         * suspends of both the virtual machine and individual threads are
         * counted. Before a thread will run again, it must be resumed through
         * the <a href="#JDWP_VirtualMachine_Resume">VM-level resume</a> command
         * or the <a href="#JDWP_ThreadReference_Resume">thread-level resume</a> command
         * the same number of times it has been suspended.
         */
        static class Suspend implements Command  {
            static final int COMMAND = 8;


            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
            }
        }

        /**
         * Resumes execution of the application after the suspend
         * command or an event has stopped it.
         * Suspensions of the Virtual Machine and individual threads are
         * counted. If a particular thread is suspended n times, it must
         * resumed n times before it will continue.
         */
        static class Resume implements Command  {
            static final int COMMAND = 9;


            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
            }
        }

        /**
         * Terminates the target VM with the given exit code.
         * On some platforms, the exit code might be truncated, for
         * example, to the low order 8 bits.
         * All ids previously returned from the target VM become invalid.
         * Threads running in the VM are abruptly terminated.
         * A thread death exception is not thrown and
         * finally blocks are not run.
         */
        static class Exit implements Command  {
            static final int COMMAND = 10;


            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
            }
        }

        /**
         * Creates a new string object in the target VM and returns
         * its id.
         */
        static class CreateString implements Command  {
            static final int COMMAND = 11;


            /**
             * Created string (instance of java.lang.String)
             */
            //final StringReferenceImpl stringObject;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                //stringObject = answer.readStringReference();
            }
        }

        /**
         * Retrieve this VM's capabilities. The capabilities are returned
         * as booleans, each indicating the presence or absence of a
         * capability. The commands associated with each capability will
         * return the NOT_IMPLEMENTED error if the cabability is not
         * available.
         */
        static class Capabilities implements Command  {
            static final int COMMAND = 12;


            /**
             * Can the VM watch field modification, and therefore 
             * can it send the Modification Watchpoint Event?
             */
            //final boolean canWatchFieldModification;

            /**
             * Can the VM watch field access, and therefore 
             * can it send the Access Watchpoint Event?
             */
            //final boolean canWatchFieldAccess;

            /**
             * Can the VM get the bytecodes of a given method? 
             */
            //final boolean canGetBytecodes;

            /**
             * Can the VM determine whether a field or method is 
             * synthetic? (that is, can the VM determine if the 
             * method or the field was invented by the compiler?) 
             */
            //final boolean canGetSyntheticAttribute;

            /**
             * Can the VM get the owned monitors infornation for 
             * a thread?
             */
            //final boolean canGetOwnedMonitorInfo;

            /**
             * Can the VM get the current contended monitor of a thread?
             */
            //final boolean canGetCurrentContendedMonitor;

            /**
             * Can the VM get the monitor information for a given object?
             */
            //final boolean canGetMonitorInfo;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                //canWatchFieldModification = answer.readBoolean();
                answer.writeBoolean(vm.vm.canWatchFieldModification());
                //canWatchFieldAccess = answer.readBoolean();
                answer.writeBoolean(vm.vm.canWatchFieldAccess());
                //canGetBytecodes = answer.readBoolean();
                answer.writeBoolean(vm.vm.canGetBytecodes());
                //canGetSyntheticAttribute = answer.readBoolean();
                answer.writeBoolean(vm.vm.canGetSyntheticAttribute());
                //canGetOwnedMonitorInfo = answer.readBoolean();
                answer.writeBoolean(vm.vm.canGetOwnedMonitorInfo());
                //canGetCurrentContendedMonitor = answer.readBoolean();
                answer.writeBoolean(vm.vm.canGetCurrentContendedMonitor());
                //canGetMonitorInfo = answer.readBoolean();
                answer.writeBoolean(vm.vm.canGetMonitorInfo());
            }
        }

        /**
         * Retrieve the classpath and bootclasspath of the target VM.
         * If the classpath is not defined, returns an empty list. If the
         * bootclasspath is not defined returns an empty list.
         */
        static class ClassPaths implements Command  {
            static final int COMMAND = 13;


            /**
             * Base directory used to resolve relative 
             * paths in either of the following lists.
             */
            //final String baseDir;

            /**
             * Number of paths in classpath.
             */
            //final String[] classpaths;

            /**
             * Number of paths in bootclasspath.
             */
            //final String[] bootclasspaths;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                //baseDir = answer.readString();
                answer.writeString(vm.vm.baseDirectory());

                List<String> classPath = vm.vm.classPath();
//                //int classpathsCount = answer.readInt();
                answer.writeInt(classPath.size());
                for (String s : classPath) {
                    answer.writeString(s);
                }
                //classpaths = new String[classpathsCount];
                //for (int i = 0; i < classpathsCount; i++) {;
                    //classpaths[i] = answer.readString();
                //}
//                //int bootclasspathsCount = answer.readInt();
                List<String> bootClassPath = vm.vm.bootClassPath();
                answer.writeInt(bootClassPath.size());
                for (String s : bootClassPath) {
                    answer.writeString(s);
                }
                //bootclasspaths = new String[bootclasspathsCount];
                //for (int i = 0; i < bootclasspathsCount; i++) {;
                    //bootclasspaths[i] = answer.readString();
                //}
            }
        }

        /**
         * Releases a list of object IDs. For each object in the list, the
         * following applies.
         * The count of references held by the back-end (the reference
         * count) will be decremented by refCnt.
         * If thereafter the reference count is less than
         * or equal to zero, the ID is freed.
         * Any back-end resources associated with the freed ID may
         * be freed, and if garbage collection was
         * disabled for the object, it will be re-enabled.
         * The sender of this command
         * promises that no further commands will be sent
         * referencing a freed ID.
         * <p>
         * Use of this command is not required. If it is not sent,
         * resources associated with each ID will be freed by the back-end
         * at some time after the corresponding object is garbage collected.
         * It is most useful to use this command to reduce the load on the
         * back-end if a very large number of
         * objects has been retrieved from the back-end (a large array,
         * for example) but may not be garbage collected any time soon.
         * <p>
         * IDs may be re-used by the back-end after they
         * have been freed with this command.
         * This description assumes reference counting,
         * a back-end may use any implementation which operates
         * equivalently.
         */
        static class DisposeObjects implements Command  {
            static final int COMMAND = 14;


            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
            }
        }

        /**
         * Tells the target VM to stop sending events. Events are not discarded;
         * they are held until a subsequent ReleaseEvents command is sent.
         * This command is useful to control the number of events sent
         * to the debugger VM in situations where very large numbers of events
         * are generated.
         * While events are held by the debugger back-end, application
         * execution may be frozen by the debugger back-end to prevent
         * buffer overflows on the back end.
         * Responses to commands are never held and are not affected by this
         * command. If events are already being held, this command is
         * ignored.
         */
        static class HoldEvents implements Command  {
            static final int COMMAND = 15;


            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
            }
        }

        /**
         * Tells the target VM to continue sending events. This command is
         * used to restore normal activity after a HoldEvents command. If
         * there is no current HoldEvents command in effect, this command is
         * ignored.
         */
        static class ReleaseEvents implements Command  {
            static final int COMMAND = 16;


            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
            }
        }

        /**
         * Retrieve all of this VM's capabilities. The capabilities are returned
         * as booleans, each indicating the presence or absence of a
         * capability. The commands associated with each capability will
         * return the NOT_IMPLEMENTED error if the cabability is not
         * available.
         * Since JDWP version 1.4.
         */
        static class CapabilitiesNew implements Command  {
            static final int COMMAND = 17;


            /**
             * Can the VM watch field modification, and therefore
             * can it send the Modification Watchpoint Event?
             */
            //final boolean canWatchFieldModification;

            /**
             * Can the VM watch field access, and therefore 
             * can it send the Access Watchpoint Event?
             */
            //final boolean canWatchFieldAccess;

            /**
             * Can the VM get the bytecodes of a given method? 
             */
            //final boolean canGetBytecodes;

            /**
             * Can the VM determine whether a field or method is 
             * synthetic? (that is, can the VM determine if the 
             * method or the field was invented by the compiler?) 
             */
            //final boolean canGetSyntheticAttribute;

            /**
             * Can the VM get the owned monitors infornation for 
             * a thread?
             */
            //final boolean canGetOwnedMonitorInfo;

            /**
             * Can the VM get the current contended monitor of a thread?
             */
            //final boolean canGetCurrentContendedMonitor;

            /**
             * Can the VM get the monitor information for a given object? 
             */
            //final boolean canGetMonitorInfo;

            /**
             * Can the VM redefine classes?
             */
            //final boolean canRedefineClasses;

            /**
             * Can the VM add methods when redefining 
             * classes?
             */
            //final boolean canAddMethod;

            /**
             * Can the VM redefine classes
             * in arbitrary ways?
             */
            //final boolean canUnrestrictedlyRedefineClasses;

            /**
             * Can the VM pop stack frames?
             */
            //final boolean canPopFrames;

            /**
             * Can the VM filter events by specific object?
             */
            //final boolean canUseInstanceFilters;

            /**
             * Can the VM get the source debug extension?
             */
            //final boolean canGetSourceDebugExtension;

            /**
             * Can the VM request VM death events?
             */
            //final boolean canRequestVMDeathEvent;

            /**
             * Can the VM set a default stratum?
             */
            //final boolean canSetDefaultStratum;

            /**
             * Can the VM return instances, counts of instances of classes 
             * and referring objects?
             */
            //final boolean canGetInstanceInfo;

            /**
             * Can the VM request monitor events?
             */
            //final boolean canRequestMonitorEvents;

            /**
             * Can the VM get monitors with frame depth info?
             */
            //final boolean canGetMonitorFrameInfo;

            /**
             * Can the VM filter class prepare events by source name?
             */
            //final boolean canUseSourceNameFilters;

            /**
             * Can the VM return the constant pool information?
             */
            //final boolean canGetConstantPool;

            /**
             * Can the VM force early return from a method?
             */
            //final boolean canForceEarlyReturn;

            /**
             * Reserved for future capability
             */
            //final boolean reserved22;

            /**
             * Reserved for future capability
             */
            //final boolean reserved23;

            /**
             * Reserved for future capability
             */
            //final boolean reserved24;

            /**
             * Reserved for future capability
             */
            //final boolean reserved25;

            /**
             * Reserved for future capability
             */
            //final boolean reserved26;

            /**
             * Reserved for future capability
             */
            //final boolean reserved27;

            /**
             * Reserved for future capability
             */
            //final boolean reserved28;

            /**
             * Reserved for future capability
             */
            //final boolean reserved29;

            /**
             * Reserved for future capability
             */
            //final boolean reserved30;

            /**
             * Reserved for future capability
             */
            //final boolean reserved31;

            /**
             * Reserved for future capability
             */
            //final boolean reserved32;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                //canWatchFieldModification = answer.readBoolean();
                answer.writeBoolean(vm.vm.canWatchFieldModification());
                //canWatchFieldAccess = answer.readBoolean();
                answer.writeBoolean(vm.vm.canWatchFieldAccess());
                //canGetBytecodes = answer.readBoolean();
                answer.writeBoolean(vm.vm.canGetBytecodes());
                //canGetSyntheticAttribute = answer.readBoolean();
                answer.writeBoolean(vm.vm.canGetSyntheticAttribute());
                //canGetOwnedMonitorInfo = answer.readBoolean();
                answer.writeBoolean(vm.vm.canGetOwnedMonitorInfo());
                //canGetCurrentContendedMonitor = answer.readBoolean();
                answer.writeBoolean(vm.vm.canGetCurrentContendedMonitor());
                //canGetMonitorInfo = answer.readBoolean();
                answer.writeBoolean(vm.vm.canGetMonitorInfo());
                //canRedefineClasses = answer.readBoolean();
                answer.writeBoolean(vm.vm.canRedefineClasses());
                //canAddMethod = answer.readBoolean();
                answer.writeBoolean(vm.vm.canAddMethod());
                //canUnrestrictedlyRedefineClasses = answer.readBoolean();
                answer.writeBoolean(vm.vm.canUnrestrictedlyRedefineClasses());
                //canPopFrames = answer.readBoolean();
                answer.writeBoolean(vm.vm.canPopFrames());
                //canUseInstanceFilters = answer.readBoolean();
                answer.writeBoolean(vm.vm.canUseInstanceFilters());
                //canGetSourceDebugExtension = answer.readBoolean();
                answer.writeBoolean(vm.vm.canGetSourceDebugExtension());
                //canRequestVMDeathEvent = answer.readBoolean();
                answer.writeBoolean(vm.vm.canRequestVMDeathEvent());
                //canSetDefaultStratum = answer.readBoolean();
                answer.writeBoolean(false);
                //canGetInstanceInfo = answer.readBoolean();
                answer.writeBoolean(vm.vm.canGetInstanceInfo());
                //canRequestMonitorEvents = answer.readBoolean();
                answer.writeBoolean(vm.vm.canRequestMonitorEvents());
                //canGetMonitorFrameInfo = answer.readBoolean();
                answer.writeBoolean(vm.vm.canGetMonitorFrameInfo());
                //canUseSourceNameFilters = answer.readBoolean();
                answer.writeBoolean(vm.vm.canUseSourceNameFilters());
                //canGetConstantPool = answer.readBoolean();
                answer.writeBoolean(vm.vm.canGetConstantPool());
                //canForceEarlyReturn = answer.readBoolean();
                answer.writeBoolean(vm.vm.canForceEarlyReturn());
                //reserved22 = answer.readBoolean();
                answer.writeBoolean(false);
                //reserved23 = answer.readBoolean();
                answer.writeBoolean(false);
                //reserved24 = answer.readBoolean();
                answer.writeBoolean(false);
                //reserved25 = answer.readBoolean();
                answer.writeBoolean(false);
                //reserved26 = answer.readBoolean();
                answer.writeBoolean(false);
                //reserved27 = answer.readBoolean();
                answer.writeBoolean(false);
                //reserved28 = answer.readBoolean();
                answer.writeBoolean(false);
                //reserved29 = answer.readBoolean();
                answer.writeBoolean(false);
                //reserved30 = answer.readBoolean();
                answer.writeBoolean(false);
                //reserved31 = answer.readBoolean();
                answer.writeBoolean(false);
                //reserved32 = answer.readBoolean();
                answer.writeBoolean(false);
            }
        }

        /**
         * Installs new class definitions.
         * If there are active stack frames in methods of the redefined classes in the
         * target VM then those active frames continue to run the bytecodes of the
         * original method. These methods are considered obsolete - see
         * <a href="#JDWP_Method_IsObsolete">IsObsolete</a>. The methods in the
         * redefined classes will be used for new invokes in the target VM.
         * The original method ID refers to the redefined method.
         * All breakpoints in the redefined classes are cleared.
         * If resetting of stack frames is desired, the
         * <a href="#JDWP_StackFrame_PopFrames">PopFrames</a> command can be used
         * to pop frames with obsolete methods.
         * <p>
         * Requires canRedefineClasses capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         * In addition to the canRedefineClasses capability, the target VM must
         * have the canAddMethod capability to add methods when redefining classes,
         * or the canUnrestrictedlyRedefineClasses to redefine classes in arbitrary
         * ways.
         */
        static class RedefineClasses implements Command  {
            static final int COMMAND = 18;


            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                notImplemented(answer);
            }
        }

        /**
         * Set the default stratum. Requires canSetDefaultStratum capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class SetDefaultStratum implements Command  {
            static final int COMMAND = 19;


            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                notImplemented(answer);
            }
        }

        /**
         * Returns reference types for all classes currently loaded by the
         * target VM.
         * Both the JNI signature and the generic signature are
         * returned for each class.
         * Generic signatures are described in the signature attribute
         * section in
         * <cite>The Java&trade; Virtual Machine Specification</cite>.
         * Since JDWP version 1.5.
         */
        static class AllClassesWithGeneric implements Command  {
            static final int COMMAND = 20;

            static class ClassInfo {

                /**
                 * <a href="#JDWP_TypeTag">Kind</a> 
                 * of following reference type. 
                 */
                //final byte refTypeTag;

                /**
                 * Loaded reference type
                 */
                //final long typeID;

                /**
                 * The JNI signature of the loaded reference type.
                 */
                //final String signature;

                /**
                 * The generic signature of the loaded reference type 
                 * or an empty string if there is none.
                 */
                //final String genericSignature;

                /**
                 * The current class
                 * <a href="#JDWP_ClassStatus">status.</a>
                 */
                //final int status;

                public static void write(ReferenceTypeImpl cls, VirtualMachineImpl vm, PacketStream answer) {
                    //refTypeTag = answer.readByte();
                    answer.writeByte(cls.tag());
                    //typeID = answer.readClassRef();
                    answer.writeClassRef(cls.uniqueID());
                    //signature = answer.readString();
                    answer.writeString(cls.signature());
                    //genericSignature = answer.readString();
                    answer.writeStringOrEmpty(cls.genericSignature());
                    //status = answer.readInt();
                    answer.writeInt(cls.ref().getClassStatus());
                }
            }


            /**
             * Number of reference types that follow.
             */
            //final ClassInfo[] classes;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                List allClasses = vm.vm.allClasses();
//                //int classesCount = answer.readInt();
                answer.writeInt(allClasses.size());
                //classes = new ClassInfo[classesCount];
                for (Object cls : allClasses) {
                    ClassInfo.write((ReferenceTypeImpl) cls, vm, answer);
                }
                //for (int i = 0; i < classesCount; i++) {;
                    //classes[i] = new ClassInfo(vm, ps);
                //}
            }
        }

        /**
         * Returns the number of instances of each reference type in the input list.
         * Only instances that are reachable for the purposes of
         * garbage collection are counted.  If a reference type is invalid,
         * eg. it has been unloaded, zero is returned for its instance count.
         * <p>Since JDWP version 1.6. Requires canGetInstanceInfo capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class InstanceCounts implements Command  {
            static final int COMMAND = 21;


            /**
             * The number of counts that follow.
             */
            //final long[] counts;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                int count = command.readInt();
                List<ReferenceTypeImpl> refs = new ArrayList<ReferenceTypeImpl>(count);
                for (int i = 0; i < count; i++) {
                    refs.add(command.readReferenceType());
                }
                long[] counts = vm.vm.instanceCounts(refs);
//                //int countsCount = answer.readInt();
                answer.writeInt(counts.length);
                //counts = new long[countsCount];
                for (long l : counts) {
                    answer.writeLong(l);
                }
                //for (int i = 0; i < countsCount; i++) {;
                    //counts[i] = answer.readLong();
                //}
            }
        }

        /**
         * Returns all modules in the target VM.
         * <p>Since JDWP version 9.
         */
        static class AllModules implements Command  {
            static final int COMMAND = 22;


            /**
             * The number of the modules that follow.
             */
            //final ModuleReferenceImpl[] modules;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
//                //int modulesCount = answer.readInt();
                //modules = new ModuleReferenceImpl[modulesCount];
                //for (int i = 0; i < modulesCount; i++) {;
                    //modules[i] = answer.readModule();
                //}
            }
        }
    }

    static class ReferenceType {
        static final int COMMAND_SET = 2;
        private ReferenceType() {}  // hide constructor

        /**
         * Returns the JNI signature of a reference type.
         * JNI signature formats are described in the
         * <a href="http://java.sun.com/products/jdk/1.2/docs/guide/jni/index.html">Java Native Inteface Specification</a>
         * <p>
         * For primitive classes
         * the returned signature is the signature of the corresponding primitive
         * type; for example, "I" is returned as the signature of the class
         * represented by java.lang.Integer.TYPE.
         */
        static class Signature implements Command  {
            static final int COMMAND = 1;


            /**
             * The JNI signature for the reference type.
             */
            //final String signature;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl referenceType = command.readReferenceType();
                //signature = answer.readString();
                answer.writeString(referenceType.signature());
            }
        }

        /**
         * Returns the instance of java.lang.ClassLoader which loaded
         * a given reference type. If the reference type was loaded by the
         * system class loader, the returned object ID is null.
         */
        static class ClassLoader implements Command  {
            static final int COMMAND = 2;


            /**
             * The class loader for the reference type.
             */
            //final ClassLoaderReferenceImpl classLoader;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl referenceType = command.readReferenceType();
                //classLoader = answer.readClassLoaderReference();
                answer.writeClassLoaderReference((ClassLoaderReferenceImpl) referenceType.classLoader());
            }
        }

        /**
         * Returns the modifiers (also known as access flags) for a reference type.
         * The returned bit mask contains information on the declaration
         * of the reference type. If the reference type is an array or
         * a primitive class (for example, java.lang.Integer.TYPE), the
         * value of the returned bit mask is undefined.
         */
        static class Modifiers implements Command  {
            static final int COMMAND = 3;


            /**
             * Modifier bits as defined in Chapter 4 of
             * <cite>The Java&trade; Virtual Machine Specification</cite>
             */
            //final int modBits;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl referenceType = command.readReferenceType();
                //modBits = answer.readInt();
                answer.writeInt(referenceType.modifiers());
            }
        }

        /**
         * Returns information for each field in a reference type.
         * Inherited fields are not included.
         * The field list will include any synthetic fields created
         * by the compiler.
         * Fields are returned in the order they occur in the class file.
         */
        static class Fields implements Command  {
            static final int COMMAND = 4;

            static class FieldInfo {

                /**
                 * Field ID.
                 */
                //final long fieldID;

                /**
                 * Name of field.
                 */
                //final String name;

                /**
                 * JNI Signature of field.
                 */
                //final String signature;

                /**
                 * The modifier bit flags (also known as access flags)
                 * which provide additional information on the
                 * field declaration. Individual flag values are
                 * defined in Chapter 4 of
                 * <cite>The Java&trade; Virtual Machine Specification</cite>.
                 * In addition, The <code>0xf0000000</code> bit identifies
                 * the field as synthetic, if the synthetic attribute
                 * <a href="#JDWP_VirtualMachine_Capabilities">capability</a> is available.
                 */
                //final int modBits;

                public static void write(FieldImpl field, VirtualMachineImpl vm, PacketStream answer) {
                    //fieldID = answer.readFieldRef();
                    answer.writeFieldRef(field.uniqueID());
                    //name = answer.readString();
                    answer.writeString(field.name());
                    //signature = answer.readString();
                    answer.writeString(field.signature());
                    //modBits = answer.readInt();
                    answer.writeInt(field.modifiers());
                }
            }


            /**
             * Number of declared fields.
             */
            //final FieldInfo[] declared;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl type = command.readReferenceType();
                List<com.sun.jdi.Field> fields = type.fields();
//                //int declaredCount = answer.readInt();
                answer.writeInt(fields.size());
                for (com.sun.jdi.Field field : fields) {
                    FieldInfo.write((FieldImpl) field, vm, answer);
                }
//                //int declaredCount = answer.readInt();
                //declared = new FieldInfo[declaredCount];
                //for (int i = 0; i < declaredCount; i++) {;
                    //declared[i] = new FieldInfo(vm, ps);
                //}
            }
        }

        /**
         * Returns information for each method in a reference type.
         * Inherited methods are not included. The list of methods will
         * include constructors (identified with the name "&lt;init&gt;"),
         * the initialization method (identified with the name "&lt;clinit&gt;")
         * if present, and any synthetic methods created by the compiler.
         * Methods are returned in the order they occur in the class file.
         */
        static class Methods implements Command  {
            static final int COMMAND = 5;

            static class MethodInfo {

                /**
                 * Method ID.
                 */
                //final long methodID;

                /**
                 * Name of method.
                 */
                //final String name;

                /**
                 * JNI signature of method.
                 */
                //final String signature;

                /**
                 * The modifier bit flags (also known as access flags)
                 * which provide additional information on the
                 * method declaration. Individual flag values are
                 * defined in Chapter 4 of
                 * <cite>The Java&trade; Virtual Machine Specification</cite>.
                 * In addition, The <code>0xf0000000</code> bit identifies
                 * the method as synthetic, if the synthetic attribute
                 * <a href="#JDWP_VirtualMachine_Capabilities">capability</a> is available.
                 */
                //final int modBits;

                public static void write(MethodImpl method, VirtualMachineImpl vm, PacketStream answer) {
                    //methodID = answer.readMethodRef();
                    answer.writeMethodRef(method.uniqueID());
                    //name = answer.readString();
                    answer.writeString(method.name());
                    //signature = answer.readString();
                    answer.writeString(method.signature());
                    //modBits = answer.readInt();
                    answer.writeInt(method.modifiers());
                }
            }


            /**
             * Number of declared methods.
             */
            //final MethodInfo[] declared;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl type = command.readReferenceType();
                List<com.sun.jdi.Method> methods = type.methods();
//                //int declaredCount = answer.readInt();
                answer.writeInt(methods.size());
                for (com.sun.jdi.Method method : methods) {
                    MethodInfo.write((MethodImpl) method, vm, answer);
                }
//                //int declaredCount = answer.readInt();
                //declared = new MethodInfo[declaredCount];
                //for (int i = 0; i < declaredCount; i++) {;
                    //declared[i] = new MethodInfo(vm, ps);
                //}
            }
        }

        /**
         * Returns the value of one or more static fields of the
         * reference type. Each field must be member of the reference type
         * or one of its superclasses, superinterfaces, or implemented interfaces.
         * Access control is not enforced; for example, the values of private
         * fields can be obtained.
         */
        static class GetValues implements Command  {
            static final int COMMAND = 6;


            /**
             * The number of values returned, always equal to fields,
             * the number of values to get.
             */
            //final ValueImpl[] values;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl referenceType = command.readReferenceType();
                int size = command.readInt();
                answer.writeInt(size);
                for (int i = 0; i < size; i++) {
                    answer.writeValue(referenceType.getValue(referenceType.fieldById(command.readFieldRef())));
                }
//                //int valuesCount = answer.readInt();
                //values = new ValueImpl[valuesCount];
                //for (int i = 0; i < valuesCount; i++) {;
                    //values[i] = answer.readValue();
                //}
            }
        }

        /**
         * Returns the name of source file in which a reference type was
         * declared.
         */
        static class SourceFile implements Command  {
            static final int COMMAND = 7;


            /**
             * The source file name. No path information
             * for the file is included
             */
            //final String sourceFile;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl type = command.readReferenceType();
                //sourceFile = answer.readString();
                try {
                    answer.writeString(type.baseSourceName());
                } catch (AbsentInformationException e) {
                    answer.pkt.errorCode = Error.ABSENT_INFORMATION;
                }
            }
        }

        /**
         * Returns the classes and interfaces directly nested within this type.
         * Types further nested within those types are not included.
         */
        static class NestedTypes implements Command  {
            static final int COMMAND = 8;

            static class TypeInfo {

                /**
                 * <a href="#JDWP_TypeTag">Kind</a> 
                 * of following reference type. 
                 */
                //final byte refTypeTag;

                /**
                 * The nested class or interface ID.
                 */
                //final long typeID;

                public static void write(ReferenceTypeImpl type, VirtualMachineImpl vm, PacketStream answer) {
                    //refTypeTag = answer.readByte();
                    answer.writeByte(type.tag());
                    //typeID = answer.readClassRef();
                    answer.writeClassRef(type.uniqueID());
                }
            }


            /**
             * The number of nested classes and interfaces
             */
            //final TypeInfo[] classes;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl type = command.readReferenceType();
                List<com.sun.jdi.ReferenceType> nestedTypes = type.nestedTypes();
//                //int classesCount = answer.readInt();
                answer.writeInt(nestedTypes.size());
                for (com.sun.jdi.ReferenceType nestedType : nestedTypes) {
                    TypeInfo.write((ReferenceTypeImpl) nestedType, vm, answer);
                }
                //classes = new TypeInfo[classesCount];
                //for (int i = 0; i < classesCount; i++) {;
                    //classes[i] = new TypeInfo(vm, ps);
                //}
            }
        }

        /**
         * Returns the current status of the reference type. The status
         * indicates the extent to which the reference type has been
         * initialized, as described in section 2.1.6 of
         * <cite>The Java&trade; Virtual Machine Specification</cite>.
         * If the class is linked the PREPARED and VERIFIED bits in the returned status bits
         * will be set. If the class is initialized the INITIALIZED bit in the returned
         * status bits will be set. If an error occured during initialization then the
         * ERROR bit in the returned status bits will be set.
         * The returned status bits are undefined for array types and for
         * primitive classes (such as java.lang.Integer.TYPE).
         */
        static class Status implements Command  {
            static final int COMMAND = 9;


            /**
             * <a href="#JDWP_ClassStatus">Status</a> bits:
             * See <a href="#JDWP_ClassStatus">JDWP.ClassStatus</a>
             */
            //final int status;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl type = command.readReferenceType();
                //status = answer.readInt();
                answer.writeInt(type.ref().getClassStatus());
            }
        }

        /**
         * Returns the interfaces declared as implemented by this class.
         * Interfaces indirectly implemented (extended by the implemented
         * interface or implemented by a superclass) are not included.
         */
        static class Interfaces implements Command  {
            static final int COMMAND = 10;


            /**
             * The number of implemented interfaces
             */
            //final InterfaceTypeImpl[] interfaces;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl type = command.readReferenceType();
                List<com.sun.jdi.InterfaceType> interfaces;
                if (type instanceof com.sun.jdi.ClassType) {
                    interfaces = ((com.sun.jdi.ClassType) type).interfaces();
                }
                else if (type instanceof InterfaceTypeImpl) {
                    interfaces = ((InterfaceTypeImpl) type).superinterfaces();
                }
                else {
                    answer.pkt.errorCode = Error.INVALID_CLASS;
                    return;
                }
//                //int interfacesCount = answer.readInt();
                answer.writeInt(interfaces.size());
                for (com.sun.jdi.InterfaceType iface : interfaces) {
                    answer.writeClassRef(((InterfaceTypeImpl) iface).uniqueID());
                }
                //interfaces = new InterfaceTypeImpl[interfacesCount];
                //for (int i = 0; i < interfacesCount; i++) {;
                    //interfaces[i] = vm.interfaceType(answer.readClassRef());
                //}
            }
        }

        /**
         * Returns the class object corresponding to this type.
         */
        static class ClassObject implements Command  {
            static final int COMMAND = 11;


            /**
             * class object.
             */
            //final ClassObjectReferenceImpl classObject;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl type = command.readReferenceType();
                //classObject = answer.readClassObjectReference();
                answer.writeClassObjectReference((ClassObjectReferenceImpl) type.classObject());
            }
        }

        /**
         * Returns the value of the SourceDebugExtension attribute.
         * Since JDWP version 1.4. Requires canGetSourceDebugExtension capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class SourceDebugExtension implements Command  {
            static final int COMMAND = 12;


            /**
             * extension attribute
             */
            //final String extension;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl type = command.readReferenceType();
                //extension = answer.readString();
                try {
                    answer.writeString(type.sourceDebugExtension());
                } catch (AbsentInformationException e) {
                    answer.pkt.errorCode = Error.ABSENT_INFORMATION;
                }
            }
        }

        /**
         * Returns the JNI signature of a reference type along with the
         * generic signature if there is one.
         * Generic signatures are described in the signature attribute
         * section in
         * <cite>The Java&trade; Virtual Machine Specification</cite>.
         * Since JDWP version 1.5.
         * <p>
         */
        static class SignatureWithGeneric implements Command  {
            static final int COMMAND = 13;


            /**
             * The JNI signature for the reference type.
             */
            //final String signature;

            /**
             * The generic signature for the reference type or an empty
             * string if there is none.
             */
            //final String genericSignature;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl type = command.readReferenceType();
                //signature = answer.readString();
                answer.writeString(type.signature());
                //genericSignature = answer.readString();
                answer.writeStringOrEmpty(type.genericSignature());
            }
        }

        /**
         * Returns information, including the generic signature if any,
         * for each field in a reference type.
         * Inherited fields are not included.
         * The field list will include any synthetic fields created
         * by the compiler.
         * Fields are returned in the order they occur in the class file.
         * Generic signatures are described in the signature attribute
         * section in
         * <cite>The Java&trade; Virtual Machine Specification</cite>.
         * Since JDWP version 1.5.
         */
        static class FieldsWithGeneric implements Command  {
            static final int COMMAND = 14;

            static class FieldInfo {

                /**
                 * Field ID.
                 */
                //final long fieldID;

                /**
                 * The name of the field.
                 */
                //final String name;

                /**
                 * The JNI signature of the field.
                 */
                //final String signature;

                /**
                 * The generic signature of the 
                 * field, or an empty string if there is none.
                 */
                //final String genericSignature;

                /**
                 * The modifier bit flags (also known as access flags)
                 * which provide additional information on the
                 * field declaration. Individual flag values are
                 * defined in Chapter 4 of
                 * <cite>The Java&trade; Virtual Machine Specification</cite>.
                 * In addition, The <code>0xf0000000</code> bit identifies
                 * the field as synthetic, if the synthetic attribute
                 * <a href="#JDWP_VirtualMachine_Capabilities">capability</a> is available.
                 */
                //final int modBits;

                public static void write(FieldImpl field, VirtualMachineImpl vm, PacketStream answer) {
                    //fieldID = answer.readFieldRef();
                    answer.writeFieldRef(field.uniqueID());
                    //name = answer.readString();
                    answer.writeString(field.name());
                    //signature = answer.readString();
                    answer.writeString(field.signature());
                    //genericSignature = answer.readString();
                    answer.writeStringOrEmpty(field.genericSignature());
                    //modBits = answer.readInt();
                    answer.writeInt(field.modifiers());
                }
            }


            /**
             * Number of declared fields.
             */
            //final FieldInfo[] declared;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl type = command.readReferenceType();
                List<com.sun.jdi.Field> fields = type.fields();
//                //int declaredCount = answer.readInt();
                answer.writeInt(fields.size());
                for (com.sun.jdi.Field field : fields) {
                    FieldInfo.write((FieldImpl) field, vm, answer);
                }
                //declared = new FieldInfo[declaredCount];
                //for (int i = 0; i < declaredCount; i++) {;
                    //declared[i] = new FieldInfo(vm, ps);
                //}
            }
        }

        /**
         * Returns information, including the generic signature if any,
         * for each method in a reference type.
         * Inherited methodss are not included. The list of methods will
         * include constructors (identified with the name "&lt;init&gt;"),
         * the initialization method (identified with the name "&lt;clinit&gt;")
         * if present, and any synthetic methods created by the compiler.
         * Methods are returned in the order they occur in the class file.
         * Generic signatures are described in the signature attribute
         * section in
         * <cite>The Java&trade; Virtual Machine Specification</cite>.
         * Since JDWP version 1.5.
         */
        static class MethodsWithGeneric implements Command  {
            static final int COMMAND = 15;

            static class MethodInfo {

                /**
                 * Method ID.
                 */
                //final long methodID;

                /**
                 * The name of the method.
                 */
                //final String name;

                /**
                 * The JNI signature of the method.
                 */
                //final String signature;

                /**
                 * The generic signature of the method, or 
                 * an empty string if there is none.
                 */
                //final String genericSignature;

                /**
                 * The modifier bit flags (also known as access flags)
                 * which provide additional information on the
                 * method declaration. Individual flag values are
                 * defined in Chapter 4 of
                 * <cite>The Java&trade; Virtual Machine Specification</cite>.
                 * In addition, The <code>0xf0000000</code> bit identifies
                 * the method as synthetic, if the synthetic attribute
                 * <a href="#JDWP_VirtualMachine_Capabilities">capability</a> is available.
                 */
                //final int modBits;

                public static void write(MethodImpl method, VirtualMachineImpl vm, PacketStream answer) {
                    //methodID = answer.readMethodRef();
                    answer.writeMethodRef(method.uniqueID());
                    //name = answer.readString();
                    answer.writeString(method.name());
                    //signature = answer.readString();
                    answer.writeString(method.signature());
                    //genericSignature = answer.readString();
                    answer.writeStringOrEmpty(method.genericSignature());
                    //modBits = answer.readInt();
                    answer.writeInt(method.modifiers());
                }
            }


            /**
             * Number of declared methods.
             */
            //final MethodInfo[] declared;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl type = command.readReferenceType();
                List<com.sun.jdi.Method> methods = type.methods();
//                //int declaredCount = answer.readInt();
                answer.writeInt(methods.size());
                for (com.sun.jdi.Method method : methods) {
                    MethodInfo.write((MethodImpl) method, vm, answer);
                }
                //declared = new MethodInfo[declaredCount];
                //for (int i = 0; i < declaredCount; i++) {;
                    //declared[i] = new MethodInfo(vm, ps);
                //}
            }
        }

        /**
         * Returns instances of this reference type.
         * Only instances that are reachable for the purposes of
         * garbage collection are returned.
         * <p>Since JDWP version 1.6. Requires canGetInstanceInfo capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class Instances implements Command  {
            static final int COMMAND = 16;


            /**
             * The number of instances that follow.
             */
            //final ObjectReferenceImpl[] instances;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl type = command.readReferenceType();
                List<com.sun.jdi.ObjectReference> instances = type.instances(command.readInt());
//                //int instancesCount = answer.readInt();
                answer.writeInt(instances.size());
                for (com.sun.jdi.ObjectReference instance : instances) {
                    answer.writeTaggedObjectReference(instance);
                }
                //instances = new ObjectReferenceImpl[instancesCount];
                //for (int i = 0; i < instancesCount; i++) {;
                    //instances[i] = answer.readTaggedObjectReference();
                //}
            }
        }

        /**
         * Returns the class file major and minor version numbers, as defined in the class
         * file format of the Java Virtual Machine specification.
         * <p>Since JDWP version 1.6.
         */
        static class ClassFileVersion implements Command  {
            static final int COMMAND = 17;


            /**
             * Major version number
             */
            //final int majorVersion;

            /**
             * Minor version number
             */
            //final int minorVersion;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl type = command.readReferenceType();
                //majorVersion = answer.readInt();
                answer.writeInt(type.majorVersion());
                //minorVersion = answer.readInt();
                answer.writeInt(type.minorVersion());
            }
        }

        /**
         * Return the raw bytes of the constant pool in the format of the
         * constant_pool item of the Class File Format in
         * <cite>The Java&trade; Virtual Machine Specification</cite>.
         * <p>Since JDWP version 1.6. Requires canGetConstantPool capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         *
         */
        static class ConstantPool implements Command  {
            static final int COMMAND = 18;


            /**
             * Total number of constant pool entries plus one. This
             * corresponds to the constant_pool_count item of the
             * Class File Format in
             * <cite>The Java&trade; Virtual Machine Specification</cite>.
             */
            //final int count;

            //final byte[] bytes;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl type = command.readReferenceType();
                byte[] bytes = type.constantPool();
                //count = answer.readInt();
                answer.writeInt(type.constantPoolCount());
//                //int bytesCount = answer.readInt();
                answer.writeInt(bytes.length);
                //bytes = new byte[bytesCount];
                answer.writeByteArray(bytes);
                //for (int i = 0; i < bytesCount; i++) {;
                    //bytes[i] = answer.readByte();
                //}
            }
        }

        /**
         * Returns the module that this reference type belongs to.
         * <p>Since JDWP version 9.
         */
        static class Module implements Command  {
            static final int COMMAND = 19;


            /**
             * The module this reference type belongs to.
             */
            //final ModuleReferenceImpl module;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                //module = answer.readModule();
            }
        }
    }

    static class ClassType {
        static final int COMMAND_SET = 3;
        private ClassType() {}  // hide constructor

        /**
         * Returns the immediate superclass of a class.
         */
        static class Superclass implements Command  {
            static final int COMMAND = 1;


            /**
             * The superclass (null if the class ID for java.lang.Object is specified).
             */
            //final ClassTypeImpl superclass;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl type = command.readReferenceType();
                if (type instanceof com.sun.jdi.ClassType) {
                    //superclass = vm.classType(answer.readClassRef());
                    com.sun.jdi.ClassType superclass = ((com.sun.jdi.ClassType) type).superclass();
                    if (superclass != null) {
                        answer.writeClassRef(((ClassTypeImpl) superclass).uniqueID());
                    }
                    else {
                        answer.writeNullObjectRef();
                    }
                }
                else {
                    answer.pkt.errorCode = Error.INVALID_CLASS;
                }
            }
        }

        /**
         * Sets the value of one or more static fields.
         * Each field must be member of the class type
         * or one of its superclasses, superinterfaces, or implemented interfaces.
         * Access control is not enforced; for example, the values of private
         * fields can be set. Final fields cannot be set.
         * For primitive values, the value's type must match the
         * field's type exactly. For object values, there must exist a
         * widening reference conversion from the value's type to the
         * field's type and the field's type must be loaded.
         */
        static class SetValues implements Command  {
            static final int COMMAND = 2;


            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                notImplemented(answer);
            }
        }

        /**
         * Invokes a static method.
         * The method must be member of the class type
         * or one of its superclasses.
         * Access control is not enforced; for example, private
         * methods can be invoked.
         * <p>
         * The method invocation will occur in the specified thread.
         * Method invocation can occur only if the specified thread
         * has been suspended by an event.
         * Method invocation is not supported
         * when the target VM has been suspended by the front-end.
         * <p>
         * The specified method is invoked with the arguments in the specified
         * argument list.
         * The method invocation is synchronous; the reply packet is not
         * sent until the invoked method returns in the target VM.
         * The return value (possibly the void value) is
         * included in the reply packet.
         * If the invoked method throws an exception, the
         * exception object ID is set in the reply packet; otherwise, the
         * exception object ID is null.
         * <p>
         * For primitive arguments, the argument value's type must match the
         * argument's type exactly. For object arguments, there must exist a
         * widening reference conversion from the argument value's type to the
         * argument's type and the argument's type must be loaded.
         * <p>
         * By default, all threads in the target VM are resumed while
         * the method is being invoked if they were previously
         * suspended by an event or by command.
         * This is done to prevent the deadlocks
         * that will occur if any of the threads own monitors
         * that will be needed by the invoked method. It is possible that
         * breakpoints or other events might occur during the invocation.
         * Note, however, that this implicit resume acts exactly like
         * the ThreadReference resume command, so if the thread's suspend
         * count is greater than 1, it will remain in a suspended state
         * during the invocation. By default, when the invocation completes,
         * all threads in the target VM are suspended, regardless their state
         * before the invocation.
         * <p>
         * The resumption of other threads during the invoke can be prevented
         * by specifying the INVOKE_SINGLE_THREADED
         * bit flag in the <code>options</code> field; however,
         * there is no protection against or recovery from the deadlocks
         * described above, so this option should be used with great caution.
         * Only the specified thread will be resumed (as described for all
         * threads above). Upon completion of a single threaded invoke, the invoking thread
         * will be suspended once again. Note that any threads started during
         * the single threaded invocation will not be suspended when the
         * invocation completes.
         * <p>
         * If the target VM is disconnected during the invoke (for example, through
         * the VirtualMachine dispose command) the method invocation continues.
         */
        static class InvokeMethod implements Command  {
            static final int COMMAND = 3;


            /**
             * The returned value.
             */
            //final ValueImpl returnValue;

            /**
             * The thrown exception.
             */
            //final ObjectReferenceImpl exception;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                //returnValue = answer.readValue();
                //exception = answer.readTaggedObjectReference();
                notImplemented(answer);
            }
        }

        /**
         * Creates a new object of this type, invoking the specified
         * constructor. The constructor method ID must be a member of
         * the class type.
         * <p>
         * Instance creation will occur in the specified thread.
         * Instance creation can occur only if the specified thread
         * has been suspended by an event.
         * Method invocation is not supported
         * when the target VM has been suspended by the front-end.
         * <p>
         * The specified constructor is invoked with the arguments in the specified
         * argument list.
         * The constructor invocation is synchronous; the reply packet is not
         * sent until the invoked method returns in the target VM.
         * The return value (possibly the void value) is
         * included in the reply packet.
         * If the constructor throws an exception, the
         * exception object ID is set in the reply packet; otherwise, the
         * exception object ID is null.
         * <p>
         * For primitive arguments, the argument value's type must match the
         * argument's type exactly. For object arguments, there must exist a
         * widening reference conversion from the argument value's type to the
         * argument's type and the argument's type must be loaded.
         * <p>
         * By default, all threads in the target VM are resumed while
         * the method is being invoked if they were previously
         * suspended by an event or by command.
         * This is done to prevent the deadlocks
         * that will occur if any of the threads own monitors
         * that will be needed by the invoked method. It is possible that
         * breakpoints or other events might occur during the invocation.
         * Note, however, that this implicit resume acts exactly like
         * the ThreadReference resume command, so if the thread's suspend
         * count is greater than 1, it will remain in a suspended state
         * during the invocation. By default, when the invocation completes,
         * all threads in the target VM are suspended, regardless their state
         * before the invocation.
         * <p>
         * The resumption of other threads during the invoke can be prevented
         * by specifying the INVOKE_SINGLE_THREADED
         * bit flag in the <code>options</code> field; however,
         * there is no protection against or recovery from the deadlocks
         * described above, so this option should be used with great caution.
         * Only the specified thread will be resumed (as described for all
         * threads above). Upon completion of a single threaded invoke, the invoking thread
         * will be suspended once again. Note that any threads started during
         * the single threaded invocation will not be suspended when the
         * invocation completes.
         * <p>
         * If the target VM is disconnected during the invoke (for example, through
         * the VirtualMachine dispose command) the method invocation continues.
         */
        static class NewInstance implements Command  {
            static final int COMMAND = 4;


            /**
             * The newly created object, or null 
             * if the constructor threw an exception.
             */
            //final ObjectReferenceImpl newObject;

            /**
             * The thrown exception, if any; otherwise, null.
             */
            //final ObjectReferenceImpl exception;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                //newObject = answer.readTaggedObjectReference();
                //exception = answer.readTaggedObjectReference();
                notImplemented(answer);
            }
        }
    }

    static class ArrayType {
        static final int COMMAND_SET = 4;
        private ArrayType() {}  // hide constructor

        /**
         * Creates a new array object of this type with a given length.
         */
        static class NewInstance implements Command  {
            static final int COMMAND = 1;


            /**
             * The newly created array object.
             */
            //final ObjectReferenceImpl newArray;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                //newArray = answer.readTaggedObjectReference();
                notImplemented(answer);
            }
        }
    }

    static class InterfaceType {
        static final int COMMAND_SET = 5;
        private InterfaceType() {}  // hide constructor

        /**
         * Invokes a static method.
         * The method must not be a static initializer.
         * The method must be a member of the interface type.
         * <p>Since JDWP version 1.8
         * <p>
         * The method invocation will occur in the specified thread.
         * Method invocation can occur only if the specified thread
         * has been suspended by an event.
         * Method invocation is not supported
         * when the target VM has been suspended by the front-end.
         * <p>
         * The specified method is invoked with the arguments in the specified
         * argument list.
         * The method invocation is synchronous; the reply packet is not
         * sent until the invoked method returns in the target VM.
         * The return value (possibly the void value) is
         * included in the reply packet.
         * If the invoked method throws an exception, the
         * exception object ID is set in the reply packet; otherwise, the
         * exception object ID is null.
         * <p>
         * For primitive arguments, the argument value's type must match the
         * argument's type exactly. For object arguments, there must exist a
         * widening reference conversion from the argument value's type to the
         * argument's type and the argument's type must be loaded.
         * <p>
         * By default, all threads in the target VM are resumed while
         * the method is being invoked if they were previously
         * suspended by an event or by a command.
         * This is done to prevent the deadlocks
         * that will occur if any of the threads own monitors
         * that will be needed by the invoked method. It is possible that
         * breakpoints or other events might occur during the invocation.
         * Note, however, that this implicit resume acts exactly like
         * the ThreadReference resume command, so if the thread's suspend
         * count is greater than 1, it will remain in a suspended state
         * during the invocation. By default, when the invocation completes,
         * all threads in the target VM are suspended, regardless their state
         * before the invocation.
         * <p>
         * The resumption of other threads during the invoke can be prevented
         * by specifying the INVOKE_SINGLE_THREADED
         * bit flag in the <code>options</code> field; however,
         * there is no protection against or recovery from the deadlocks
         * described above, so this option should be used with great caution.
         * Only the specified thread will be resumed (as described for all
         * threads above). Upon completion of a single threaded invoke, the invoking thread
         * will be suspended once again. Note that any threads started during
         * the single threaded invocation will not be suspended when the
         * invocation completes.
         * <p>
         * If the target VM is disconnected during the invoke (for example, through
         * the VirtualMachine dispose command) the method invocation continues.
         */
        static class InvokeMethod implements Command  {
            static final int COMMAND = 1;


            /**
             * The returned value.
             */
            //final ValueImpl returnValue;

            /**
             * The thrown exception.
             */
            //final ObjectReferenceImpl exception;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                //returnValue = answer.readValue();
                //exception = answer.readTaggedObjectReference();
                notImplemented(answer);
            }
        }
    }

    static class Method {
        static final int COMMAND_SET = 6;
        private Method() {}  // hide constructor

        /**
         * Returns line number information for the method, if present.
         * The line table maps source line numbers to the initial code index
         * of the line. The line table
         * is ordered by code index (from lowest to highest). The line number
         * information is constant unless a new class definition is installed
         * using <a href="#JDWP_VirtualMachine_RedefineClasses">RedefineClasses</a>.
         */
        static class LineTable implements Command  {
            static final int COMMAND = 1;

            static class LineInfo {

                /**
                 * Initial code index of the line, 
                 * start <= lineCodeIndex < end
                 */
                //final long lineCodeIndex;

                /**
                 * Line number.
                 */
                //final int lineNumber;

                public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                    //lineCodeIndex = answer.readLong();
                    //lineNumber = answer.readInt();
                }
            }


            /**
             * Lowest valid code index for the method, >=0, or -1 if the method is native 
             */
            //final long start;

            /**
             * Highest valid code index for the method, >=0, or -1 if the method is native
             */
            //final long end;

            /**
             * The number of entries in the line table for this method.
             */
            //final LineInfo[] lines;
            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl referenceType = command.readReferenceType();
                MethodImpl method = referenceType.methodById(command.readMethodRef());

                if (method.isNative()) {
                    answer.pkt.errorCode = Error.NATIVE_METHOD;
                    return;
                }

                List<Location> locations = Collections.emptyList();
                try {
                    locations = method.allLineLocations();
                } catch (AbsentInformationException ignored) {
                }
                sun.jvm.hotspot.oops.Method ref = method.ref();
                long start = 0;
                long end = ref.getCodeSize();
                if (end == 0) {
                    start = -1;
                }
                //start = answer.readLong();
                answer.writeLong(start);
                //end = answer.readLong();
                answer.writeLong(end);
//                //int linesCount = answer.readInt();
                answer.writeInt(locations.size());
                for (Location location : locations) {
                    //lineCodeIndex = answer.readLong();
                    answer.writeLong(location.codeIndex());
                    //lineNumber = answer.readInt();
                    answer.writeInt(location.lineNumber());
                }
                //lines = new LineInfo[linesCount];
                //for (int i = 0; i < linesCount; i++) {;
                //lines[i] = new LineInfo(vm, ps);
                //}
            }
        }

        /**
         * Returns variable information for the method. The variable table
         * includes arguments and locals declared within the method. For
         * instance methods, the "this" reference is included in the
         * table. Also, synthetic variables may be present.
         */
        static class VariableTable implements Command  {
            static final int COMMAND = 2;

            /**
             * Information about the variable.
             */
            static class SlotInfo {

                /**
                 * First code index at which the variable is visible (unsigned). 
                 * Used in conjunction with <code>length</code>. 
                 * The variable can be get or set only when the current 
                 * <code>codeIndex</code> <= current frame code index < <code>codeIndex + length</code> 
                 */
                //final long codeIndex;

                /**
                 * The variable's name.
                 */
                //final String name;

                /**
                 * The variable type's JNI signature.
                 */
                //final String signature;

                /**
                 * Unsigned value used in conjunction with <code>codeIndex</code>. 
                 * The variable can be get or set only when the current 
                 * <code>codeIndex</code> <= current frame code index < <code>code index + length</code> 
                 */
                //final int length;

                /**
                 * The local variable's index in its frame
                 */
                //final int slot;

                public static void write(LocalVariableImpl var, VirtualMachineImpl vm, PacketStream answer) {
                    //codeIndex = answer.readLong();
                    answer.writeLong(var.getStart());
                    //name = answer.readString();
                    answer.writeString(var.name());
                    //signature = answer.readString();
                    answer.writeString(var.signature());
                    //length = answer.readInt();
                    answer.writeInt(var.getLength());
                    //slot = answer.readInt();
                    answer.writeInt(var.slot());
                }
            }


            /**
             * The number of words in the frame used by arguments. 
             * Eight-byte arguments use two words; all others use one. 
             */
            //final int argCnt;

            /**
             * The number of variables.
             */
            //final SlotInfo[] slots;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl referenceType = command.readReferenceType();
                MethodImpl method = referenceType.methodById(command.readMethodRef());
                try {
                    List<LocalVariable> variables = method.variables();
                    //argCnt = answer.readInt();
                    answer.writeInt(method.argSlotCount());
                    //int slotsCount = answer.readInt();
                    answer.writeInt(variables.size());
                    for (LocalVariable variable : variables) {
                        SlotInfo.write((LocalVariableImpl) variable, vm, answer);
                    }
                    //slots = new SlotInfo[slotsCount];
                    //for (int i = 0; i < slotsCount; i++) {;
                    //slots[i] = new SlotInfo(vm, ps);
                    //}

                } catch (AbsentInformationException e) {
                    answer.pkt.errorCode = Error.ABSENT_INFORMATION;
                }
                //argCnt = answer.readInt();
//                //int slotsCount = answer.readInt();
                //slots = new SlotInfo[slotsCount];
                //for (int i = 0; i < slotsCount; i++) {;
                    //slots[i] = new SlotInfo(vm, ps);
                //}
            }
        }

        /**
         * Retrieve the method's bytecodes as defined in
         * <cite>The Java&trade; Virtual Machine Specification</cite>.
         * Requires canGetBytecodes capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class Bytecodes implements Command  {
            static final int COMMAND = 3;


            //final byte[] bytes;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl referenceType = command.readReferenceType();
                MethodImpl method = referenceType.methodById(command.readMethodRef());
                byte[] bytecodes = method.bytecodes();
                answer.writeInt(bytecodes.length);
                answer.writeByteArray(bytecodes);
//                //int bytesCount = answer.readInt();
                //bytes = new byte[bytesCount];
                //for (int i = 0; i < bytesCount; i++) {;
                    //bytes[i] = answer.readByte();
                //}
            }
        }

        /**
         * Determine if this method is obsolete. A method is obsolete if it has been replaced
         * by a non-equivalent method using the
         * <a href="#JDWP_VirtualMachine_RedefineClasses">RedefineClasses</a> command.
         * The original and redefined methods are considered equivalent if their bytecodes are
         * the same except for indices into the constant pool and the referenced constants are
         * equal.
         */
        static class IsObsolete implements Command  {
            static final int COMMAND = 4;


            /**
             * true if this method has been replaced
             * by a non-equivalent method using
             * the RedefineClasses command.
             */
            //final boolean isObsolete;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl referenceType = command.readReferenceType();
                MethodImpl method = referenceType.methodById(command.readMethodRef());
                //isObsolete = answer.readBoolean();
                answer.writeBoolean(method.isObsolete());
            }
        }

        /**
         * Returns variable information for the method, including
         * generic signatures for the variables. The variable table
         * includes arguments and locals declared within the method. For
         * instance methods, the "this" reference is included in the
         * table. Also, synthetic variables may be present.
         * Generic signatures are described in the signature attribute
         * section in
         * <cite>The Java&trade; Virtual Machine Specification</cite>.
         * Since JDWP version 1.5.
         */
        static class VariableTableWithGeneric implements Command  {
            static final int COMMAND = 5;

            /**
             * Information about the variable.
             */
            static class SlotInfo {

                /**
                 * First code index at which the variable is visible (unsigned). 
                 * Used in conjunction with <code>length</code>. 
                 * The variable can be get or set only when the current 
                 * <code>codeIndex</code> <= current frame code index < <code>codeIndex + length</code> 
                 */
                //final long codeIndex;

                /**
                 * The variable's name.
                 */
                //final String name;

                /**
                 * The variable type's JNI signature.
                 */
                //final String signature;

                /**
                 * The variable type's generic 
                 * signature or an empty string if there is none.
                 */
                //final String genericSignature;

                /**
                 * Unsigned value used in conjunction with <code>codeIndex</code>. 
                 * The variable can be get or set only when the current 
                 * <code>codeIndex</code> <= current frame code index < <code>code index + length</code> 
                 */
                //final int length;

                /**
                 * The local variable's index in its frame
                 */
                //final int slot;

                public static void write(LocalVariableImpl var, VirtualMachineImpl vm, PacketStream answer) {
                    //codeIndex = answer.readLong();
                    answer.writeLong(var.getStart());
                    //name = answer.readString();
                    answer.writeString(var.name());
                    //signature = answer.readString();
                    answer.writeString(var.signature());
                    //genericSignature = answer.readString();
                    answer.writeStringOrEmpty(var.genericSignature());
                    //length = answer.readInt();
                    answer.writeInt(var.getLength());
                    //slot = answer.readInt();
                    answer.writeInt(var.slot());
                }
            }


            /**
             * The number of words in the frame used by arguments. 
             * Eight-byte arguments use two words; all others use one. 
             */
            //final int argCnt;

            /**
             * The number of variables.
             */
            //final SlotInfo[] slots;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ReferenceTypeImpl referenceType = command.readReferenceType();
                MethodImpl method = referenceType.methodById(command.readMethodRef());
                try {
                    List<LocalVariable> variables = method.variables();
                    //argCnt = answer.readInt();
                    answer.writeInt(method.argSlotCount());
                    //int slotsCount = answer.readInt();
                    answer.writeInt(variables.size());
                    for (LocalVariable variable : variables) {
                        SlotInfo.write((LocalVariableImpl) variable, vm, answer);
                    }
                    //slots = new SlotInfo[slotsCount];
                    //for (int i = 0; i < slotsCount; i++) {;
                    //slots[i] = new SlotInfo(vm, ps);
                    //}

                } catch (AbsentInformationException e) {
                    answer.pkt.errorCode = Error.ABSENT_INFORMATION;
                }
            }
        }
    }

    static class Field {
        static final int COMMAND_SET = 8;
        private Field() {}  // hide constructor
    }

    static class ObjectReference {
        static final int COMMAND_SET = 9;
        private ObjectReference() {}  // hide constructor

        /**
         * Returns the runtime type of the object.
         * The runtime type will be a class or an array.
         */
        static class ReferenceType implements Command  {
            static final int COMMAND = 1;


            /**
             * <a href="#JDWP_TypeTag">Kind</a> 
             * of following reference type. 
             */
            //final byte refTypeTag;

            /**
             * The runtime reference type.
             */
            //final long typeID;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ObjectReferenceImpl objectReference = vm.vm.objectMirror(command.readObjectRef());
                //refTypeTag = answer.readByte();
                ReferenceTypeImpl referenceType = (ReferenceTypeImpl) objectReference.referenceType();
                answer.writeByte(referenceType.tag());
                //typeID = answer.readClassRef();
                answer.writeClassRef(referenceType.uniqueID());
            }
        }

        /**
         * Returns the value of one or more instance fields.
         * Each field must be member of the object's type
         * or one of its superclasses, superinterfaces, or implemented interfaces.
         * Access control is not enforced; for example, the values of private
         * fields can be obtained.
         */
        static class GetValues implements Command  {
            static final int COMMAND = 2;


            /**
             * The number of values returned, always equal to 'fields',
             * the number of values to get. Field values are ordered
             * in the reply in the same order as corresponding fieldIDs
             * in the command.
             */
//            final ValueImpl[] values;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ObjectReferenceImpl objectReference = vm.vm.objectMirror(command.readObjectRef());
                ReferenceTypeImpl referenceType = (ReferenceTypeImpl) objectReference.referenceType();
                int count = command.readInt();
                answer.writeInt(count);
                for (int i = 0; i < count; i++) {
                    long id = command.readFieldRef();
                    answer.writeValue(objectReference.getValue(referenceType.fieldById(id)));
                }
//                //int valuesCount = answer.readInt();
                //values = new ValueImpl[valuesCount];
                //for (int i = 0; i < valuesCount; i++) {;
                    //values[i] = answer.readValue();
                //}
            }
        }

        /**
         * Sets the value of one or more instance fields.
         * Each field must be member of the object's type
         * or one of its superclasses, superinterfaces, or implemented interfaces.
         * Access control is not enforced; for example, the values of private
         * fields can be set.
         * For primitive values, the value's type must match the
         * field's type exactly. For object values, there must be a
         * widening reference conversion from the value's type to the
         * field's type and the field's type must be loaded.
         */
        static class SetValues implements Command  {
            static final int COMMAND = 3;


            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                notImplemented(answer);
            }
        }

        /**
         * Returns monitor information for an object. All threads int the VM must
         * be suspended.
         * Requires canGetMonitorInfo capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class MonitorInfo implements Command  {
            static final int COMMAND = 5;


            /**
             * The monitor owner, or null if it is not currently owned.
             */
            //final ThreadReferenceImpl owner;

            /**
             * The number of times the monitor has been entered.
             */
            //final int entryCount;

            /**
             * The number of threads that are waiting for the monitor
             * 0 if there is no current owner
             */
            //final ThreadReferenceImpl[] waiters;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ObjectReferenceImpl objectReference = vm.vm.objectMirror(command.readObjectRef());
                //owner = answer.readThreadReference();
                answer.writeThreadReference(objectReference.owningThread());
                //entryCount = answer.readInt();
                answer.writeInt(objectReference.entryCount());
//                //int waitersCount = answer.readInt();
                List<com.sun.jdi.ThreadReference> waiting = objectReference.waitingThreads();
                answer.writeInt(waiting.size());
                for (com.sun.jdi.ThreadReference threadReference : waiting) {
                    answer.writeThreadReference(threadReference);
                }
                //waiters = new ThreadReferenceImpl[waitersCount];
                //for (int i = 0; i < waitersCount; i++) {;
                    //waiters[i] = answer.readThreadReference();
                //}
            }
        }

        /**
         * Invokes a instance method.
         * The method must be member of the object's type
         * or one of its superclasses, superinterfaces, or implemented interfaces.
         * Access control is not enforced; for example, private
         * methods can be invoked.
         * <p>
         * The method invocation will occur in the specified thread.
         * Method invocation can occur only if the specified thread
         * has been suspended by an event.
         * Method invocation is not supported
         * when the target VM has been suspended by the front-end.
         * <p>
         * The specified method is invoked with the arguments in the specified
         * argument list.
         * The method invocation is synchronous; the reply packet is not
         * sent until the invoked method returns in the target VM.
         * The return value (possibly the void value) is
         * included in the reply packet.
         * If the invoked method throws an exception, the
         * exception object ID is set in the reply packet; otherwise, the
         * exception object ID is null.
         * <p>
         * For primitive arguments, the argument value's type must match the
         * argument's type exactly. For object arguments, there must be a
         * widening reference conversion from the argument value's type to the
         * argument's type and the argument's type must be loaded.
         * <p>
         * By default, all threads in the target VM are resumed while
         * the method is being invoked if they were previously
         * suspended by an event or by a command.
         * This is done to prevent the deadlocks
         * that will occur if any of the threads own monitors
         * that will be needed by the invoked method. It is possible that
         * breakpoints or other events might occur during the invocation.
         * Note, however, that this implicit resume acts exactly like
         * the ThreadReference resume command, so if the thread's suspend
         * count is greater than 1, it will remain in a suspended state
         * during the invocation. By default, when the invocation completes,
         * all threads in the target VM are suspended, regardless their state
         * before the invocation.
         * <p>
         * The resumption of other threads during the invoke can be prevented
         * by specifying the INVOKE_SINGLE_THREADED
         * bit flag in the <code>options</code> field; however,
         * there is no protection against or recovery from the deadlocks
         * described above, so this option should be used with great caution.
         * Only the specified thread will be resumed (as described for all
         * threads above). Upon completion of a single threaded invoke, the invoking thread
         * will be suspended once again. Note that any threads started during
         * the single threaded invocation will not be suspended when the
         * invocation completes.
         * <p>
         * If the target VM is disconnected during the invoke (for example, through
         * the VirtualMachine dispose command) the method invocation continues.
         */
        static class InvokeMethod implements Command  {
            static final int COMMAND = 6;


            /**
             * The returned value, or null if an exception is thrown.
             */
            //final ValueImpl returnValue;

            /**
             * The thrown exception, if any.
             */
            //final ObjectReferenceImpl exception;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                //returnValue = answer.readValue();
                //exception = answer.readTaggedObjectReference();
                notImplemented(answer);
            }
        }

        /**
         * Prevents garbage collection for the given object. By
         * default all objects in back-end replies may be
         * collected at any time the target VM is running. A call to
         * this command guarantees that the object will not be
         * collected. The
         * <a href="#JDWP_ObjectReference_EnableCollection">EnableCollection</a>
         * command can be used to
         * allow collection once again.
         * <p>
         * Note that while the target VM is suspended, no garbage
         * collection will occur because all threads are suspended.
         * The typical examination of variables, fields, and arrays
         * during the suspension is safe without explicitly disabling
         * garbage collection.
         * <p>
         * This method should be used sparingly, as it alters the
         * pattern of garbage collection in the target VM and,
         * consequently, may result in application behavior under the
         * debugger that differs from its non-debugged behavior.
         */
        static class DisableCollection implements Command  {
            static final int COMMAND = 7;


            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                notImplemented(answer);
            }
        }

        /**
         * Permits garbage collection for this object. By default all
         * objects returned by JDWP may become unreachable in the target VM,
         * and hence may be garbage collected. A call to this command is
         * necessary only if garbage collection was previously disabled with
         * the <a href="#JDWP_ObjectReference_DisableCollection">DisableCollection</a>
         * command.
         */
        static class EnableCollection implements Command  {
            static final int COMMAND = 8;


            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                notImplemented(answer);
            }
        }

        /**
         * Determines whether an object has been garbage collected in the
         * target VM.
         */
        static class IsCollected implements Command  {
            static final int COMMAND = 9;


            /**
             * true if the object has been collected; false otherwise
             */
            //final boolean isCollected;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                //isCollected = answer.readBoolean();
                answer.writeBoolean(false);
            }
        }

        /**
         * Returns objects that directly reference this object.
         * Only objects that are reachable for the purposes
         * of garbage collection are returned.
         * Note that an object can also be referenced in other ways,
         * such as from a local variable in a stack frame, or from a JNI global
         * reference.  Such non-object referrers are not returned by this command.
         * <p>Since JDWP version 1.6. Requires canGetInstanceInfo capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class ReferringObjects implements Command  {
            static final int COMMAND = 10;


            /**
             * The number of objects that follow.
             */
            //final ObjectReferenceImpl[] referringObjects;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ObjectReferenceImpl objectReference = vm.vm.objectMirror(command.readObjectRef());
                List<com.sun.jdi.ObjectReference> refs = objectReference.referringObjects(command.readInt());
//                //int referringObjectsCount = answer.readInt();
                answer.writeInt(refs.size());
                for (com.sun.jdi.ObjectReference ref : refs) {
                    answer.writeTaggedObjectReference(ref);
                }
                //referringObjects = new ObjectReferenceImpl[referringObjectsCount];
                //for (int i = 0; i < referringObjectsCount; i++) {;
                    //referringObjects[i] = answer.readTaggedObjectReference();
                //}
            }
        }
    }

    static class StringReference {
        static final int COMMAND_SET = 10;
        private StringReference() {}  // hide constructor

        /**
         * Returns the characters contained in the string.
         */
        static class Value implements Command  {
            static final int COMMAND = 1;


            /**
             * UTF-8 representation of the string value.
             */
            //final String stringValue;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ObjectReferenceImpl objectReference = command.readObjectReference();
                if (objectReference instanceof com.sun.jdi.StringReference) {
                    //stringValue = answer.readString();
                    answer.writeString(((com.sun.jdi.StringReference) objectReference).value());
                }
                else {
                    answer.pkt.errorCode = Error.INVALID_STRING;
                }
            }
        }
    }

    static class ThreadReference {
        static final int COMMAND_SET = 11;
        private ThreadReference() {}  // hide constructor

        /**
         * Returns the thread name.
         */
        static class Name implements Command  {
            static final int COMMAND = 1;


            /**
             * The thread name.
             */
            //final String threadName;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                answer.writeString(command.readThreadReference().name());
            }
        }

        /**
         * Suspends the thread.
         * <p>
         * Unlike java.lang.Thread.suspend(), suspends of both
         * the virtual machine and individual threads are counted. Before
         * a thread will run again, it must be resumed the same number
         * of times it has been suspended.
         * <p>
         * Suspending single threads with command has the same
         * dangers java.lang.Thread.suspend(). If the suspended
         * thread holds a monitor needed by another running thread,
         * deadlock is possible in the target VM (at least until the
         * suspended thread is resumed again).
         * <p>
         * The suspended thread is guaranteed to remain suspended until
         * resumed through one of the JDI resume methods mentioned above;
         * the application in the target VM cannot resume the suspended thread
         * through {@link Thread#resume}.
         * <p>
         * Note that this doesn't change the status of the thread (see the
         * <a href="#JDWP_ThreadReference_Status">ThreadStatus</a> command.)
         * For example, if it was
         * Running, it will still appear running to other threads.
         */
        static class Suspend implements Command  {
            static final int COMMAND = 2;


            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                notImplemented(answer);
            }
        }

        /**
         * Resumes the execution of a given thread. If this thread was
         * not previously suspended by the front-end,
         * calling this command has no effect.
         * Otherwise, the count of pending suspends on this thread is
         * decremented. If it is decremented to 0, the thread will
         * continue to execute.
         */
        static class Resume implements Command  {
            static final int COMMAND = 3;


            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                notImplemented(answer);
            }
        }

        /**
         * Returns the current status of a thread. The thread status
         * reply indicates the thread status the last time it was running.
         * the suspend status provides information on the thread's
         * suspension, if any.
         */
        static class Status implements Command  {
            static final int COMMAND = 4;


            /**
             * One of the thread status codes
             * See <a href="#JDWP_ThreadStatus">JDWP.ThreadStatus</a>
             */
            //final int threadStatus;

            /**
             * One of the suspend status codes
             * See <a href="#JDWP_SuspendStatus">JDWP.SuspendStatus</a>
             */
            //final int suspendStatus;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ThreadReferenceImpl thread = command.readThreadReference();
                //threadStatus = answer.readInt();
                answer.writeInt(thread.status());
                //suspendStatus = answer.readInt();
                answer.writeInt(thread.suspendCount());
            }
        }

        /**
         * Returns the thread group that contains a given thread.
         */
        static class ThreadGroup implements Command  {
            static final int COMMAND = 5;


            /**
             * The thread group of this thread.
             */
            //final ThreadGroupReferenceImpl group;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ThreadReferenceImpl thread = command.readThreadReference();
                //group = answer.readThreadGroupReference();
                answer.writeObjectRef(thread.threadGroup().uniqueID());
            }
        }

        /**
         * Returns the current call stack of a suspended thread.
         * The sequence of frames starts with
         * the currently executing frame, followed by its caller,
         * and so on. The thread must be suspended, and the returned
         * frameID is valid only while the thread is suspended.
         */
        static class Frames implements Command  {
            static final int COMMAND = 6;

            static class Frame {

                /**
                 * The ID of this frame.
                 */
                //final long frameID;

                /**
                 * The current location of this frame
                 */
                //final Location location;

                public static void write(com.sun.jdi.StackFrame frame, VirtualMachineImpl vm, PacketStream answer) {
                    //frameID = answer.readFrameRef();
                    answer.writeFrameRef(((StackFrameImpl) frame).id());
                    //location = answer.readLocation();
                    answer.writeLocation((LocationImpl) frame.location());
                }
            }


            /**
             * The number of frames retreived
             */
            //final Frame[] frames;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ThreadReferenceImpl thread = command.readThreadReference();
                try {
                    List<com.sun.jdi.StackFrame> frames = thread.frames();
//                //int framesCount = answer.readInt();
                    answer.writeInt(frames.size());
                    for (com.sun.jdi.StackFrame frame : frames) {
                        Frame.write(frame,vm, answer);
                    }
                    //frames = new Frame[framesCount];
                    //for (int i = 0; i < framesCount; i++) {;
                    //frames[i] = new Frame(vm, ps);
                    //}
                } catch (IncompatibleThreadStateException e) {
                    answer.pkt.errorCode = Error.INVALID_THREAD;
                }
            }
        }

        /**
         * Returns the count of frames on this thread's stack.
         * The thread must be suspended, and the returned
         * count is valid only while the thread is suspended.
         * Returns JDWP.Error.errorThreadNotSuspended if not suspended.
         */
        static class FrameCount implements Command  {
            static final int COMMAND = 7;


            /**
             * The count of frames on this thread's stack.
             */
            //final int frameCount;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ThreadReferenceImpl thread = command.readThreadReference();
                //frameCount = answer.readInt();
                try {
                    answer.writeInt(thread.frameCount());
                } catch (IncompatibleThreadStateException e) {
                    answer.pkt.errorCode = Error.INVALID_THREAD;
                }
            }
        }

        /**
         * Returns the objects whose monitors have been entered by this thread.
         * The thread must be suspended, and the returned information is
         * relevant only while the thread is suspended.
         * Requires canGetOwnedMonitorInfo capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class OwnedMonitors implements Command  {
            static final int COMMAND = 8;


            /**
             * The number of owned monitors
             */
            //final ObjectReferenceImpl[] owned;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ThreadReferenceImpl thread = command.readThreadReference();
                List<com.sun.jdi.ObjectReference> ownedMonitors;
                try {
                    ownedMonitors = thread.ownedMonitors();
                } catch (IncompatibleThreadStateException e) {
                    answer.pkt.errorCode = Error.INVALID_THREAD;
                    return;
                }
//                //int ownedCount = answer.readInt();
                answer.writeInt(ownedMonitors.size());
                for (com.sun.jdi.ObjectReference ownedMonitor : ownedMonitors) {
                    answer.writeTaggedObjectReference(ownedMonitor);
                }
                //owned = new ObjectReferenceImpl[ownedCount];
                //for (int i = 0; i < ownedCount; i++) {;
                    //owned[i] = answer.readTaggedObjectReference();
                //}
            }
        }

        /**
         * Returns the object, if any, for which this thread is waiting. The
         * thread may be waiting to enter a monitor, or it may be waiting, via
         * the java.lang.Object.wait method, for another thread to invoke the
         * notify method.
         * The thread must be suspended, and the returned information is
         * relevant only while the thread is suspended.
         * Requires canGetCurrentContendedMonitor capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class CurrentContendedMonitor implements Command  {
            static final int COMMAND = 9;


            /**
             * The contended monitor, or null if
             * there is no current contended monitor.
             */
            //final ObjectReferenceImpl monitor;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ThreadReferenceImpl thread = command.readThreadReference();
                //monitor = answer.readTaggedObjectReference();
                try {
                    answer.writeTaggedObjectReference(thread.currentContendedMonitor());
                } catch (IncompatibleThreadStateException e) {
                    answer.pkt.errorCode = Error.INVALID_THREAD;
                }
            }
        }

        /**
         * Stops the thread with an asynchronous exception, as if done by
         * java.lang.Thread.stop
         */
        static class Stop implements Command  {
            static final int COMMAND = 10;


            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                notImplemented(answer);
            }
        }

        /**
         * Interrupt the thread, as if done by java.lang.Thread.interrupt
         */
        static class Interrupt implements Command  {
            static final int COMMAND = 11;


            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                notImplemented(answer);
            }
        }

        /**
         * Get the suspend count for this thread. The suspend count is the
         * number of times the thread has been suspended through the
         * thread-level or VM-level suspend commands without a corresponding resume
         */
        static class SuspendCount implements Command  {
            static final int COMMAND = 12;


            /**
             * The number of outstanding suspends of this thread.
             */
            //final int suspendCount;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                //suspendCount = answer.readInt();
                answer.writeInt(1);
            }
        }

        /**
         * Returns monitor objects owned by the thread, along with stack depth at which
         * the monitor was acquired. Returns stack depth of -1  if
         * the implementation cannot determine the stack depth
         * (e.g., for monitors acquired by JNI MonitorEnter).
         * The thread must be suspended, and the returned information is
         * relevant only while the thread is suspended.
         * Requires canGetMonitorFrameInfo capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         * <p>Since JDWP version 1.6.
         */
        static class OwnedMonitorsStackDepthInfo implements Command  {
            static final int COMMAND = 13;

            static class monitor {

                /**
                 * An owned monitor
                 */
                //final ObjectReferenceImpl monitor;

                /**
                 * Stack depth location where monitor was acquired
                 */
                //final int stack_depth;

                public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                    //monitor = answer.readTaggedObjectReference();
                    //stack_depth = answer.readInt();
                }
            }


            /**
             * The number of owned monitors
             */
            //final monitor[] owned;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ThreadReferenceImpl thread = command.readThreadReference();
                List list;
                try {
                    list = thread.ownedMonitorsAndFrames();
                } catch (IncompatibleThreadStateException e) {
                    answer.pkt.errorCode = Error.INVALID_THREAD;
                    return;
                }
//                //int ownedCount = answer.readInt();
                answer.writeInt(list.size());
                for (Object o : list) {
                    answer.writeTaggedObjectReference(((MonitorInfo) o).monitor());
                    answer.writeInt(((MonitorInfo) o).stackDepth());
                }
                //owned = new monitor[ownedCount];
                //for (int i = 0; i < ownedCount; i++) {;
                    //owned[i] = new monitor(vm, ps);
                //}
            }
        }

        /**
         * Force a method to return before it reaches a return
         * statement.
         * <p>
         * The method which will return early is referred to as the
         * called method. The called method is the current method (as
         * defined by the Frames section in
         * <cite>The Java&trade; Virtual Machine Specification</cite>)
         * for the specified thread at the time this command
         * is received.
         * <p>
         * The specified thread must be suspended.
         * The return occurs when execution of Java programming
         * language code is resumed on this thread. Between sending this
         * command and resumption of thread execution, the
         * state of the stack is undefined.
         * <p>
         * No further instructions are executed in the called
         * method. Specifically, finally blocks are not executed. Note:
         * this can cause inconsistent states in the application.
         * <p>
         * A lock acquired by calling the called method (if it is a
         * synchronized method) and locks acquired by entering
         * synchronized blocks within the called method are
         * released. Note: this does not apply to JNI locks or
         * java.util.concurrent.locks locks.
         * <p>
         * Events, such as MethodExit, are generated as they would be in
         * a normal return.
         * <p>
         * The called method must be a non-native Java programming
         * language method. Forcing return on a thread with only one
         * frame on the stack causes the thread to exit when resumed.
         * <p>
         * For void methods, the value must be a void value.
         * For methods that return primitive values, the value's type must
         * match the return type exactly.  For object values, there must be a
         * widening reference conversion from the value's type to the
         * return type type and the return type must be loaded.
         * <p>
         * Since JDWP version 1.6. Requires canForceEarlyReturn capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class ForceEarlyReturn implements Command  {
            static final int COMMAND = 14;


            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                notImplemented(answer);
            }
        }
    }

    static class ThreadGroupReference {
        static final int COMMAND_SET = 12;
        private ThreadGroupReference() {}  // hide constructor

        /**
         * Returns the thread group name.
         */
        static class Name implements Command  {
            static final int COMMAND = 1;


            /**
             * The thread group's name.
             */
            //final String groupName;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ThreadGroupReferenceImpl group = command.readThreadGroupReference();
                //groupName = answer.readString();
                answer.writeString(group.name());
            }
        }

        /**
         * Returns the thread group, if any, which contains a given thread group.
         */
        static class Parent implements Command  {
            static final int COMMAND = 2;


            /**
             * The parent thread group object, or
             * null if the given thread group
             * is a top-level thread group
             */
            //final ThreadGroupReferenceImpl parentGroup;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ThreadGroupReferenceImpl group = command.readThreadGroupReference();
                //parentGroup = answer.readThreadGroupReference();
                answer.writeThreadGroupReference(group.parent());

            }
        }

        /**
         * Returns the live threads and active thread groups directly contained
         * in this thread group. Threads and thread groups in child
         * thread groups are not included.
         * A thread is alive if it has been started and has not yet been stopped.
         * See <a href=../../../api/java/lang/ThreadGroup.html>java.lang.ThreadGroup </a>
         * for information about active ThreadGrouanswer.
         */
        static class Children implements Command  {
            static final int COMMAND = 3;


            /**
             * The number of live child threads.
             */
            //final ThreadReferenceImpl[] childThreads;

            /**
             * The number of active child thread grouanswer.
             */
            //final ThreadGroupReferenceImpl[] childGroups;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ThreadGroupReferenceImpl group = command.readThreadGroupReference();
                List<com.sun.jdi.ThreadReference> threads = group.threads();
//                //int childThreadsCount = answer.readInt();
                answer.writeInt(threads.size());
                for (com.sun.jdi.ThreadReference thread : threads) {
                    answer.writeThreadReference(thread);
                }
                //childThreads = new ThreadReferenceImpl[childThreadsCount];
                //for (int i = 0; i < childThreadsCount; i++) {;
                    //childThreads[i] = answer.readThreadReference();
                //}
                List<com.sun.jdi.ThreadGroupReference> threadGroups = group.threadGroups();
//                //int childGroupsCount = answer.readInt();
                answer.writeInt(threadGroups.size());
                for (com.sun.jdi.ThreadGroupReference threadGroup : threadGroups) {
                    answer.writeThreadGroupReference(threadGroup);
                }
                //childGroups = new ThreadGroupReferenceImpl[childGroupsCount];
                //for (int i = 0; i < childGroupsCount; i++) {;
                    //childGroups[i] = answer.readThreadGroupReference();
                //}
            }
        }
    }

    static class ArrayReference {
        static final int COMMAND_SET = 13;
        private ArrayReference() {}  // hide constructor

        /**
         * Returns the number of components in a given array.
         */
        static class Length implements Command  {
            static final int COMMAND = 1;


            /**
             * The length of the array.
             */
            //final int arrayLength;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ArrayReferenceImpl arrayReference = command.readArrayReference();
                //arrayLength = answer.readInt();
                answer.writeInt(arrayReference.length());
            }
        }

        /**
         * Returns a range of array components. The specified range must
         * be within the bounds of the array.
         */
        static class GetValues implements Command  {
            static final int COMMAND = 2;


            /**
             * The retrieved values. If the values
             * are objects, they are tagged-values;
             * otherwise, they are untagged-values
             */
            //final List<?> values;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ArrayReferenceImpl arrayReference = command.readArrayReference();
                int start = command.readInt();
                int length = command.readInt();

                byte tag;
                try {
                    tag = ((TypeImpl)((com.sun.jdi.ArrayType) arrayReference.type()).componentType()).tag();
                } catch (ClassNotLoadedException e) { // fallback to the first element type
                    tag = ValueImpl.typeValueKey(arrayReference.getValue(0));
                }

                //values = answer.readArrayRegion();
                answer.writeArrayRegion(arrayReference.getValues(start, length), tag);
            }
        }

        /**
         * Sets a range of array components. The specified range must
         * be within the bounds of the array.
         * For primitive values, each value's type must match the
         * array component type exactly. For object values, there must be a
         * widening reference conversion from the value's type to the
         * array component type and the array component type must be loaded.
         */
        static class SetValues implements Command  {
            static final int COMMAND = 3;


            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                notImplemented(answer);
            }
        }
    }

    static class ClassLoaderReference {
        static final int COMMAND_SET = 14;
        private ClassLoaderReference() {}  // hide constructor

        /**
         * Returns a list of all classes which this class loader has
         * been requested to load. This class loader is considered to be
         * an <i>initiating</i> class loader for each class in the returned
         * list. The list contains each
         * reference type defined by this loader and any types for which
         * loading was delegated by this class loader to another class loader.
         * <p>
         * The visible class list has useful properties with respect to
         * the type namespace. A particular type name will occur at most
         * once in the list. Each field or variable declared with that
         * type name in a class defined by
         * this class loader must be resolved to that single type.
         * <p>
         * No ordering of the returned list is guaranteed.
         */
        static class VisibleClasses implements Command  {
            static final int COMMAND = 1;

            static class ClassInfo {

                /**
                 * <a href="#JDWP_TypeTag">Kind</a>
                 * of following reference type.
                 */
                //final byte refTypeTag;

                /**
                 * A class visible to this class loader.
                 */
                //final long typeID;

                public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                    //refTypeTag = answer.readByte();
                    //typeID = answer.readClassRef();
                }
            }


            /**
             * The number of visible classes.
             */
            //final ClassInfo[] classes;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ClassLoaderReferenceImpl classLoaderReference = command.readClassLoaderReference();
                List<com.sun.jdi.ReferenceType> visibleClasses = classLoaderReference.visibleClasses();
//                //int classesCount = answer.readInt();
                answer.writeInt(visibleClasses.size());
                for (com.sun.jdi.ReferenceType visibleClass : visibleClasses) {
                    //refTypeTag = answer.readByte();
                    answer.writeByte(((ReferenceTypeImpl) visibleClass).tag());
                    //typeID = answer.readClassRef();
                    answer.writeClassRef(((ReferenceTypeImpl) visibleClass).uniqueID());
                }
                //classes = new ClassInfo[classesCount];
                //for (int i = 0; i < classesCount; i++) {;
                    //classes[i] = new ClassInfo(vm, ps);
                //}
            }
        }
    }

    static class EventRequest {
        static final int COMMAND_SET = 15;
        private EventRequest() {}  // hide constructor

        /**
         * Set an event request. When the event described by this request
         * occurs, an <a href="#JDWP_Event">event</a> is sent from the
         * target VM. If an event occurs that has not been requested then it is not sent
         * from the target VM. The two exceptions to this are the VM Start Event and
         * the VM Death Event which are automatically generated events - see
         * <a href="#JDWP_Event_Composite">Composite Command</a> for further details.
         */
        static class Set implements Command  {
            static final int COMMAND = 1;


            /**
             * ID of created request
             */
            //final int requestID;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                //requestID = answer.readInt();
                answer.writeInt(0); // to allow jdi VirtualMachineImpl to initialize
            }
        }

        /**
         * Clear an event request. See <a href="#JDWP_EventKind">JDWP.EventKind</a>
         * for a complete list of events that can be cleared. Only the event request matching
         * the specified event kind and requestID is cleared. If there isn't a matching event
         * request the command is a no-op and does not result in an error. Automatically
         * generated events do not have a corresponding event request and may not be cleared
         * using this command.
         */
        static class Clear implements Command  {
            static final int COMMAND = 2;


            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
//                notImplemented(answer); // to allow jdb and other debuggers to initialize
            }
        }

        /**
         * Removes all set breakpoints, a no-op if there are no breakpoints set.
         */
        static class ClearAllBreakpoints implements Command  {
            static final int COMMAND = 3;


            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                notImplemented(answer);
            }
        }
    }

    static class StackFrame {
        static final int COMMAND_SET = 16;
        private StackFrame() {}  // hide constructor

        /**
         * Returns the value of one or more local variables in a
         * given frame. Each variable must be visible at the frame's code index.
         * Even if local variable information is not available, values can
         * be retrieved if the front-end is able to
         * determine the correct local variable index. (Typically, this
         * index can be determined for method arguments from the method
         * signature without access to the local variable table information.)
         */
        static class GetValues implements Command  {
            static final int COMMAND = 1;


            /**
             * The number of values retrieved, always equal to slots,
             * the number of values to get.
             */
            //final ValueImpl[] values;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ThreadReferenceImpl thread = command.readThreadReference();
                try {
                    StackFrameImpl frame = thread.frame((int) command.readFrameRef());
                    int slots = command.readInt();
                    if (slots >= frame.getMaxSlot()) {
                        answer.pkt.errorCode = Error.INVALID_SLOT;
                        return;
                    }
                    //int valuesCount = answer.readInt();
                    answer.writeInt(slots);
                    for (int i = 0; i < slots; i++) {
                        Value slotValue = frame.getSlotValue(command.readInt(), command.readByte());
                        answer.writeValue(slotValue);
                    }

                } catch (IncompatibleThreadStateException e) {
                    e.printStackTrace();
                }
                //values = new ValueImpl[valuesCount];
                //for (int i = 0; i < valuesCount; i++) {;
                    //values[i] = answer.readValue();
                //}
            }
        }

        /**
         * Sets the value of one or more local variables.
         * Each variable must be visible at the current frame code index.
         * For primitive values, the value's type must match the
         * variable's type exactly. For object values, there must be a
         * widening reference conversion from the value's type to the
         * variable's type and the variable's type must be loaded.
         * <p>
         * Even if local variable information is not available, values can
         * be set, if the front-end is able to
         * determine the correct local variable index. (Typically, this
         * index can be determined for method arguments from the method
         * signature without access to the local variable table information.)
         */
        static class SetValues implements Command  {
            static final int COMMAND = 2;


            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                notImplemented(answer);
            }
        }

        /**
         * Returns the value of the 'this' reference for this frame.
         * If the frame's method is static or native, the reply
         * will contain the null object reference.
         */
        static class ThisObject implements Command  {
            static final int COMMAND = 3;


            /**
             * The 'this' object for this frame.
             */
            //final ObjectReferenceImpl objectThis;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ThreadReferenceImpl thread = command.readThreadReference();
                try {
                    StackFrameImpl frame = thread.frame((int) command.readFrameRef());
                    //objectThis = answer.readTaggedObjectReference();
                    answer.writeTaggedObjectReference(frame.thisObject());
                } catch (IncompatibleThreadStateException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Pop the top-most stack frames of the thread stack, up to, and including 'frame'.
         * The thread must be suspended to perform this command.
         * The top-most stack frames are discarded and the stack frame previous to 'frame'
         * becomes the current frame. The operand stack is restored -- the argument values
         * are added back and if the invoke was not <code>invokestatic</code>,
         * <code>objectref</code> is added back as well. The Java virtual machine
         * program counter is restored to the opcode of the invoke instruction.
         * <p>
         * Since JDWP version 1.4. Requires canPopFrames capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class PopFrames implements Command  {
            static final int COMMAND = 4;


            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                notImplemented(answer);
            }
        }
    }

    static class ClassObjectReference {
        static final int COMMAND_SET = 17;
        private ClassObjectReference() {}  // hide constructor

        /**
         * Returns the reference type reflected by this class object.
         */
        static class ReflectedType implements Command  {
            static final int COMMAND = 1;


            /**
             * <a href="#JDWP_TypeTag">Kind</a>
             * of following reference type.
             */
            //final byte refTypeTag;

            /**
             * reflected reference type
             */
            //final long typeID;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                ClassObjectReferenceImpl reference = command.readClassObjectReference();
                ReferenceTypeImpl type = (ReferenceTypeImpl) reference.reflectedType();
                //refTypeTag = answer.readByte();
                answer.writeByte(type.tag());
                //typeID = answer.readClassRef();
                answer.writeClassRef(type.uniqueID());
            }
        }
    }

    static class ModuleReference {
        static final int COMMAND_SET = 18;
        private ModuleReference() {}  // hide constructor

        /**
         * Returns the name of this module.
         * <p>Since JDWP version 9.
         */
        static class Name implements Command  {
            static final int COMMAND = 1;


            /**
             * The module's name.
             */
            //final String name;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                //name = answer.readString();
            }
        }

        /**
         * Returns the class loader of this module.
         * <p>Since JDWP version 9.
         */
        static class ClassLoader implements Command  {
            static final int COMMAND = 2;


            /**
             * The module's class loader.
             */
            //final ClassLoaderReferenceImpl classLoader;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                //classLoader = answer.readClassLoaderReference();
            }
        }
    }

    static class Event {
        static final int COMMAND_SET = 64;
        private Event() {}  // hide constructor

        /**
         * Several events may occur at a given time in the target VM.
         * For example, there may be more than one breakpoint request
         * for a given location
         * or you might single step to the same location as a
         * breakpoint request.  These events are delivered
         * together as a composite event.  For uniformity, a
         * composite event is always used
         * to deliver events, even if there is only one event to report.
         * <P>
         * The events that are grouped in a composite event are restricted in the
         * following ways:
         * <P>
         * <UL>
         * <LI>Only with other thread start events for the same thread:
         *     <UL>
         *     <LI>Thread Start Event
         *     </UL>
         * <LI>Only with other thread death events for the same thread:
         *     <UL>
         *     <LI>Thread Death Event
         *     </UL>
         * <LI>Only with other class prepare events for the same class:
         *     <UL>
         *     <LI>Class Prepare Event
         *     </UL>
         * <LI>Only with other class unload events for the same class:
         *     <UL>
         *     <LI>Class Unload Event
         *     </UL>
         * <LI>Only with other access watchpoint events for the same field access:
         *     <UL>
         *     <LI>Access Watchpoint Event
         *     </UL>
         * <LI>Only with other modification watchpoint events for the same field
         * modification:
         *     <UL>
         *     <LI>Modification Watchpoint Event
         *     </UL>
         * <LI>Only with other Monitor contended enter events for the same monitor object:
         *     <UL>
         *     <LI>Monitor Contended Enter Event
         *     </UL>
         * <LI>Only with other Monitor contended entered events for the same monitor object:
         *     <UL>
         *     <LI>Monitor Contended Entered Event
         *     </UL>
         * <LI>Only with other Monitor wait events for the same monitor object:
         *     <UL>
         *     <LI>Monitor Wait Event
         *     </UL>
         * <LI>Only with other Monitor waited events for the same monitor object:
         *     <UL>
         *     <LI>Monitor Waited Event
         *     </UL>
         * <LI>Only with other ExceptionEvents for the same exception occurrance:
         *     <UL>
         *     <LI>ExceptionEvent
         *     </UL>
         * <LI>Only with other members of this group, at the same location
         * and in the same thread:
         *     <UL>
         *     <LI>Breakpoint Event
         *     <LI>Step Event
         *     <LI>Method Entry Event
         *     <LI>Method Exit Event
         *     </UL>
         * </UL>
         * <P>
         * The VM Start Event and VM Death Event are automatically generated events.
         * This means they do not need to be requested using the
         * <a href="#JDWP_EventRequest_Set">EventRequest.Set</a> command.
         * The VM Start event signals the completion of VM initialization. The VM Death
         * event signals the termination of the VM.
         * If there is a debugger connected at the time when an automatically generated
         * event occurs it is sent from the target VM. Automatically generated events may
         * also be requested using the EventRequest.Set command and thus multiple events
         * of the same event kind will be sent from the target VM when an event occurs.
         * Automatically generated events are sent with the requestID field
         * in the Event Data set to 0. The value of the suspendPolicy field in the
         * Event Data depends on the event. For the automatically generated VM Start
         * Event the value of suspendPolicy is not defined and is therefore implementation
         * or configuration specific. In the Sun implementation, for example, the
         * suspendPolicy is specified as an option to the JDWP agent at launch-time.
         * The automatically generated VM Death Event will have the suspendPolicy set to
         * NONE.
         */
        static class Composite implements Command  {
            static final int COMMAND = 100;

            static class Events {
                abstract static class EventsCommon {
                    abstract byte eventKind();
                }

                /**
                 * Event kind selector
                 */
                //final byte eventKind;
                EventsCommon aEventsCommon;

                public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                    //eventKind = answer.readByte();
                    //switch (eventKind) {
                        //case JDWP.EventKind.VM_START:
                            // aEventsCommon = new VMStart(vm, ps);
                            //break;
                        //case JDWP.EventKind.SINGLE_STEP:
                            // aEventsCommon = new SingleStep(vm, ps);
                            //break;
                        //case JDWP.EventKind.BREAKPOINT:
                            // aEventsCommon = new Breakpoint(vm, ps);
                            //break;
                        //case JDWP.EventKind.METHOD_ENTRY:
                            // aEventsCommon = new MethodEntry(vm, ps);
                            //break;
                        //case JDWP.EventKind.METHOD_EXIT:
                            // aEventsCommon = new MethodExit(vm, ps);
                            //break;
                        //case JDWP.EventKind.METHOD_EXIT_WITH_RETURN_VALUE:
                            // aEventsCommon = new MethodExitWithReturnValue(vm, ps);
                            //break;
                        //case JDWP.EventKind.MONITOR_CONTENDED_ENTER:
                            // aEventsCommon = new MonitorContendedEnter(vm, ps);
                            //break;
                        //case JDWP.EventKind.MONITOR_CONTENDED_ENTERED:
                            // aEventsCommon = new MonitorContendedEntered(vm, ps);
                            //break;
                        //case JDWP.EventKind.MONITOR_WAIT:
                            // aEventsCommon = new MonitorWait(vm, ps);
                            //break;
                        //case JDWP.EventKind.MONITOR_WAITED:
                            // aEventsCommon = new MonitorWaited(vm, ps);
                            //break;
                        //case JDWP.EventKind.EXCEPTION:
                            // aEventsCommon = new Exception(vm, ps);
                            //break;
                        //case JDWP.EventKind.THREAD_START:
                            // aEventsCommon = new ThreadStart(vm, ps);
                            //break;
                        //case JDWP.EventKind.THREAD_DEATH:
                            // aEventsCommon = new ThreadDeath(vm, ps);
                            //break;
                        //case JDWP.EventKind.CLASS_PREPARE:
                            // aEventsCommon = new ClassPrepare(vm, ps);
                            //break;
                        //case JDWP.EventKind.CLASS_UNLOAD:
                            // aEventsCommon = new ClassUnload(vm, ps);
                            //break;
                        //case JDWP.EventKind.FIELD_ACCESS:
                            // aEventsCommon = new FieldAccess(vm, ps);
                            //break;
                        //case JDWP.EventKind.FIELD_MODIFICATION:
                            // aEventsCommon = new FieldModification(vm, ps);
                            //break;
                        //case JDWP.EventKind.VM_DEATH:
                            // aEventsCommon = new VMDeath(vm, ps);
                            //break;
                    //}
                }

                /**
                 * Notification of initialization of a target VM.  This event is
                 * received before the main thread is started and before any
                 * application code has been executed. Before this event occurs
                 * a significant amount of system code has executed and a number
                 * of system classes have been loaded.
                 * This event is always generated by the target VM, even
                 * if not explicitly requested.
                 */
                static class VMStart extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.VM_START;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event (or 0 if this
                     * event is automatically generated.
                     */
                    //final int requestID;

                    /**
                     * Initial thread
                     */
                    //final ThreadReferenceImpl thread;

                    public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                        //requestID = answer.readInt();
                        //thread = answer.readThreadReference();
                    }
                }

                /**
                 * Notification of step completion in the target VM. The step event
                 * is generated before the code at its location is executed.
                 */
                static class SingleStep extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.SINGLE_STEP;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    //final int requestID;

                    /**
                     * Stepped thread
                     */
                    //final ThreadReferenceImpl thread;

                    /**
                     * Location stepped to
                     */
                    //final Location location;

                    public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                        //requestID = answer.readInt();
                        //thread = answer.readThreadReference();
                        //location = answer.readLocation();
                    }
                }

                /**
                 * Notification of a breakpoint in the target VM. The breakpoint event
                 * is generated before the code at its location is executed.
                 */
                static class Breakpoint extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.BREAKPOINT;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    //final int requestID;

                    /**
                     * Thread which hit breakpoint
                     */
                    //final ThreadReferenceImpl thread;

                    /**
                     * Location hit
                     */
                    //final Location location;

                    public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                        //requestID = answer.readInt();
                        //thread = answer.readThreadReference();
                        //location = answer.readLocation();
                    }
                }

                /**
                 * Notification of a method invocation in the target VM. This event
                 * is generated before any code in the invoked method has executed.
                 * Method entry events are generated for both native and non-native
                 * methods.
                 * <P>
                 * In some VMs method entry events can occur for a particular thread
                 * before its thread start event occurs if methods are called
                 * as part of the thread's initialization.
                 */
                static class MethodEntry extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.METHOD_ENTRY;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    //final int requestID;

                    /**
                     * Thread which entered method
                     */
                    //final ThreadReferenceImpl thread;

                    /**
                     * The initial executable location in the method.
                     */
                    //final Location location;

                    public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                        //requestID = answer.readInt();
                        //thread = answer.readThreadReference();
                        //location = answer.readLocation();
                    }
                }

                /**
                 * Notification of a method return in the target VM. This event
                 * is generated after all code in the method has executed, but the
                 * location of this event is the last executed location in the method.
                 * Method exit events are generated for both native and non-native
                 * methods. Method exit events are not generated if the method terminates
                 * with a thrown exception.
                 */
                static class MethodExit extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.METHOD_EXIT;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    //final int requestID;

                    /**
                     * Thread which exited method
                     */
                    //final ThreadReferenceImpl thread;

                    /**
                     * Location of exit
                     */
                    //final Location location;

                    public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                        //requestID = answer.readInt();
                        //thread = answer.readThreadReference();
                        //location = answer.readLocation();
                    }
                }

                /**
                 * Notification of a method return in the target VM. This event
                 * is generated after all code in the method has executed, but the
                 * location of this event is the last executed location in the method.
                 * Method exit events are generated for both native and non-native
                 * methods. Method exit events are not generated if the method terminates
                 * with a thrown exception. <p>Since JDWP version 1.6.
                 */
                static class MethodExitWithReturnValue extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.METHOD_EXIT_WITH_RETURN_VALUE;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    //final int requestID;

                    /**
                     * Thread which exited method
                     */
                    //final ThreadReferenceImpl thread;

                    /**
                     * Location of exit
                     */
                    //final Location location;

                    /**
                     * Value that will be returned by the method
                     */
                    //final ValueImpl value;

                    public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                        //requestID = answer.readInt();
                        //thread = answer.readThreadReference();
                        //location = answer.readLocation();
                        //value = answer.readValue();
                    }
                }

                /**
                 * Notification that a thread in the target VM is attempting
                 * to enter a monitor that is already acquired by another thread.
                 * Requires canRequestMonitorEvents capability - see
                 * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
                 * <p>Since JDWP version 1.6.
                 */
                static class MonitorContendedEnter extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.MONITOR_CONTENDED_ENTER;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    //final int requestID;

                    /**
                     * Thread which is trying to enter the monitor
                     */
                    //final ThreadReferenceImpl thread;

                    /**
                     * Monitor object reference
                     */
                    //final ObjectReferenceImpl object;

                    /**
                     * Location of contended monitor enter
                     */
                    //final Location location;

                    public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                        //requestID = answer.readInt();
                        //thread = answer.readThreadReference();
                        //object = answer.readTaggedObjectReference();
                        //location = answer.readLocation();
                    }
                }

                /**
                 * Notification of a thread in the target VM is entering a monitor
                 * after waiting for it to be released by another thread.
                 * Requires canRequestMonitorEvents capability - see
                 * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
                 * <p>Since JDWP version 1.6.
                 */
                static class MonitorContendedEntered extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.MONITOR_CONTENDED_ENTERED;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    //final int requestID;

                    /**
                     * Thread which entered monitor
                     */
                    //final ThreadReferenceImpl thread;

                    /**
                     * Monitor object reference
                     */
                    //final ObjectReferenceImpl object;

                    /**
                     * Location of contended monitor enter
                     */
                    //final Location location;

                    public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                        //requestID = answer.readInt();
                        //thread = answer.readThreadReference();
                        //object = answer.readTaggedObjectReference();
                        //location = answer.readLocation();
                    }
                }

                /**
                 * Notification of a thread about to wait on a monitor object.
                 * Requires canRequestMonitorEvents capability - see
                 * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
                 * <p>Since JDWP version 1.6.
                 */
                static class MonitorWait extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.MONITOR_WAIT;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    //final int requestID;

                    /**
                     * Thread which is about to wait
                     */
                    //final ThreadReferenceImpl thread;

                    /**
                     * Monitor object reference
                     */
                    //final ObjectReferenceImpl object;

                    /**
                     * Location at which the wait will occur
                     */
                    //final Location location;

                    /**
                     * Thread wait time in milliseconds
                     */
                    //final long timeout;

                    public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                        //requestID = answer.readInt();
                        //thread = answer.readThreadReference();
                        //object = answer.readTaggedObjectReference();
                        //location = answer.readLocation();
                        //timeout = answer.readLong();
                    }
                }

                /**
                 * Notification that a thread in the target VM has finished waiting on
                 * Requires canRequestMonitorEvents capability - see
                 * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
                 * a monitor object.
                 * <p>Since JDWP version 1.6.
                 */
                static class MonitorWaited extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.MONITOR_WAITED;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    //final int requestID;

                    /**
                     * Thread which waited
                     */
                    //final ThreadReferenceImpl thread;

                    /**
                     * Monitor object reference
                     */
                    //final ObjectReferenceImpl object;

                    /**
                     * Location at which the wait occured
                     */
                    //final Location location;

                    /**
                     * True if timed out
                     */
                    //final boolean timed_out;

                    public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                        //requestID = answer.readInt();
                        //thread = answer.readThreadReference();
                        //object = answer.readTaggedObjectReference();
                        //location = answer.readLocation();
                        //timed_out = answer.readBoolean();
                    }
                }

                /**
                 * Notification of an exception in the target VM.
                 * If the exception is thrown from a non-native method,
                 * the exception event is generated at the location where the
                 * exception is thrown.
                 * If the exception is thrown from a native method, the exception event
                 * is generated at the first non-native location reached after the exception
                 * is thrown.
                 */
                static class Exception extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.EXCEPTION;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    //final int requestID;

                    /**
                     * Thread with exception
                     */
                    //final ThreadReferenceImpl thread;

                    /**
                     * Location of exception throw
                     * (or first non-native location after throw if thrown from a native method)
                     */
                    //final Location location;

                    /**
                     * Thrown exception
                     */
                    //final ObjectReferenceImpl exception;

                    /**
                     * Location of catch, or 0 if not caught. An exception
                     * is considered to be caught if, at the point of the throw, the
                     * current location is dynamically enclosed in a try statement that
                     * handles the exception. (See the JVM specification for details).
                     * If there is such a try statement, the catch location is the
                     * first location in the appropriate catch clause.
                     * <p>
                     * If there are native methods in the call stack at the time of the
                     * exception, there are important restrictions to note about the
                     * returned catch location. In such cases,
                     * it is not possible to predict whether an exception will be handled
                     * by some native method on the call stack.
                     * Thus, it is possible that exceptions considered uncaught
                     * here will, in fact, be handled by a native method and not cause
                     * termination of the target VM. Furthermore, it cannot be assumed that the
                     * catch location returned here will ever be reached by the throwing
                     * thread. If there is
                     * a native frame between the current location and the catch location,
                     * the exception might be handled and cleared in that native method
                     * instead.
                     * <p>
                     * Note that compilers can generate try-catch blocks in some cases
                     * where they are not explicit in the source code; for example,
                     * the code generated for <code>synchronized</code> and
                     * <code>finally</code> blocks can contain implicit try-catch blocks.
                     * If such an implicitly generated try-catch is
                     * present on the call stack at the time of the throw, the exception
                     * will be considered caught even though it appears to be uncaught from
                     * examination of the source code.
                     */
                    //final Location catchLocation;

                    public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                        //requestID = answer.readInt();
                        //thread = answer.readThreadReference();
                        //location = answer.readLocation();
                        //exception = answer.readTaggedObjectReference();
                        //catchLocation = answer.readLocation();
                    }
                }

                /**
                 * Notification of a new running thread in the target VM.
                 * The new thread can be the result of a call to
                 * <code>java.lang.Thread.start</code> or the result of
                 * attaching a new thread to the VM though JNI. The
                 * notification is generated by the new thread some time before
                 * its execution starts.
                 * Because of this timing, it is possible to receive other events
                 * for the thread before this event is received. (Notably,
                 * Method Entry Events and Method Exit Events might occur
                 * during thread initialization.
                 * It is also possible for the
                 * <a href="#JDWP_VirtualMachine_AllThreads">VirtualMachine AllThreads</a>
                 * command to return
                 * a thread before its thread start event is received.
                 * <p>
                 * Note that this event gives no information
                 * about the creation of the thread object which may have happened
                 * much earlier, depending on the VM being debugged.
                 */
                static class ThreadStart extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.THREAD_START;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    //final int requestID;

                    /**
                     * Started thread
                     */
                    //final ThreadReferenceImpl thread;

                    public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                        //requestID = answer.readInt();
                        //thread = answer.readThreadReference();
                    }
                }

                /**
                 * Notification of a completed thread in the target VM. The
                 * notification is generated by the dying thread before it terminates.
                 * Because of this timing, it is possible
                 * for {@link com.sun.jdi.VirtualMachine#allThreads} to return this thread
                 * after this event is received.
                 * <p>
                 * Note that this event gives no information
                 * about the lifetime of the thread object. It may or may not be collected
                 * soon depending on what references exist in the target VM.
                 */
                static class ThreadDeath extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.THREAD_DEATH;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    //final int requestID;

                    /**
                     * Ending thread
                     */
                    //final ThreadReferenceImpl thread;

                    public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                        //requestID = answer.readInt();
                        //thread = answer.readThreadReference();
                    }
                }

                /**
                 * Notification of a class prepare in the target VM. See the JVM
                 * specification for a definition of class preparation. Class prepare
                 * events are not generated for primtiive classes (for example,
                 * java.lang.Integer.TYPE).
                 */
                static class ClassPrepare extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.CLASS_PREPARE;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    //final int requestID;

                    /**
                     * Preparing thread.
                     * In rare cases, this event may occur in a debugger system
                     * thread within the target VM. Debugger threads take precautions
                     * to prevent these events, but they cannot be avoided under some
                     * conditions, especially for some subclasses of
                     * java.lang.Error.
                     * If the event was generated by a debugger system thread, the
                     * value returned by this method is null, and if the requested
                     * <a href="#JDWP_SuspendPolicy">suspend policy</a>
                     * for the event was EVENT_THREAD
                     * all threads will be suspended instead, and the
                     * composite event's suspend policy will reflect this change.
                     * <p>
                     * Note that the discussion above does not apply to system threads
                     * created by the target VM during its normal (non-debug) operation.
                     */
                    //final ThreadReferenceImpl thread;

                    /**
                     * Kind of reference type.
                     * See <a href="#JDWP_TypeTag">JDWP.TypeTag</a>
                     */
                    //final byte refTypeTag;

                    /**
                     * Type being prepared
                     */
                    //final long typeID;

                    /**
                     * Type signature
                     */
                    //final String signature;

                    /**
                     * Status of type.
                     * See <a href="#JDWP_ClassStatus">JDWP.ClassStatus</a>
                     */
                    //final int status;

                    public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                        //requestID = answer.readInt();
                        //thread = answer.readThreadReference();
                        //refTypeTag = answer.readByte();
                        //typeID = answer.readClassRef();
                        //signature = answer.readString();
                        //status = answer.readInt();
                    }
                }

                /**
                 * Notification of a class unload in the target VM.
                 * <p>
                 * There are severe constraints on the debugger back-end during
                 * garbage collection, so unload information is greatly limited.
                 */
                static class ClassUnload extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.CLASS_UNLOAD;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    //final int requestID;

                    /**
                     * Type signature
                     */
                    //final String signature;

                    public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                        //requestID = answer.readInt();
                        //signature = answer.readString();
                    }
                }

                /**
                 * Notification of a field access in the target VM.
                 * Field modifications
                 * are not considered field accesses.
                 * Requires canWatchFieldAccess capability - see
                 * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
                 */
                static class FieldAccess extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.FIELD_ACCESS;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    //final int requestID;

                    /**
                     * Accessing thread
                     */
                    //final ThreadReferenceImpl thread;

                    /**
                     * Location of access
                     */
                    //final Location location;

                    /**
                     * Kind of reference type.
                     * See <a href="#JDWP_TypeTag">JDWP.TypeTag</a>
                     */
                    //final byte refTypeTag;

                    /**
                     * Type of field
                     */
                    //final long typeID;

                    /**
                     * Field being accessed
                     */
                    //final long fieldID;

                    /**
                     * Object being accessed (null=0 for statics
                     */
                    //final ObjectReferenceImpl object;

                    public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                        //requestID = answer.readInt();
                        //thread = answer.readThreadReference();
                        //location = answer.readLocation();
                        //refTypeTag = answer.readByte();
                        //typeID = answer.readClassRef();
                        //fieldID = answer.readFieldRef();
                        //object = answer.readTaggedObjectReference();
                    }
                }

                /**
                 * Notification of a field modification in the target VM.
                 * Requires canWatchFieldModification capability - see
                 * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
                 */
                static class FieldModification extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.FIELD_MODIFICATION;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    //final int requestID;

                    /**
                     * Modifying thread
                     */
                    //final ThreadReferenceImpl thread;

                    /**
                     * Location of modify
                     */
                    //final Location location;

                    /**
                     * Kind of reference type.
                     * See <a href="#JDWP_TypeTag">JDWP.TypeTag</a>
                     */
                    //final byte refTypeTag;

                    /**
                     * Type of field
                     */
                    //final long typeID;

                    /**
                     * Field being modified
                     */
                    //final long fieldID;

                    /**
                     * Object being modified (null=0 for statics
                     */
                    //final ObjectReferenceImpl object;

                    /**
                     * Value to be assigned
                     */
                    //final ValueImpl valueToBe;

                    public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                        //requestID = answer.readInt();
                        //thread = answer.readThreadReference();
                        //location = answer.readLocation();
                        //refTypeTag = answer.readByte();
                        //typeID = answer.readClassRef();
                        //fieldID = answer.readFieldRef();
                        //object = answer.readTaggedObjectReference();
                        //valueToBe = answer.readValue();
                    }
                }

                static class VMDeath extends EventsCommon {
                    static final byte ALT_ID = JDWP.EventKind.VM_DEATH;
                    byte eventKind() {
                        return ALT_ID;
                    }

                    /**
                     * Request that generated event
                     */
                    //final int requestID;

                    public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                        //requestID = answer.readInt();
                    }
                }
            }


            /**
             * Which threads where suspended by this composite event?
             */
            //final byte suspendPolicy;

            /**
             * Events in set.
             */
            //final Events[] events;

            public void reply(VirtualMachineImpl vm, PacketStream answer, PacketStream command) {
                //suspendPolicy = answer.readByte();
//                //int eventsCount = answer.readInt();
                //events = new Events[eventsCount];
                //for (int i = 0; i < eventsCount; i++) {;
                    //events[i] = new Events(vm, ps);
                //}
            }
        }
    }

    interface Error {
        int NONE = 0;
        int INVALID_THREAD = 10;
        int INVALID_THREAD_GROUP = 11;
        int INVALID_PRIORITY = 12;
        int THREAD_NOT_SUSPENDED = 13;
        int THREAD_SUSPENDED = 14;
        int THREAD_NOT_ALIVE = 15;
        int INVALID_OBJECT = 20;
        int INVALID_CLASS = 21;
        int CLASS_NOT_PREPARED = 22;
        int INVALID_METHODID = 23;
        int INVALID_LOCATION = 24;
        int INVALID_FIELDID = 25;
        int INVALID_FRAMEID = 30;
        int NO_MORE_FRAMES = 31;
        int OPAQUE_FRAME = 32;
        int NOT_CURRENT_FRAME = 33;
        int TYPE_MISMATCH = 34;
        int INVALID_SLOT = 35;
        int DUPLICATE = 40;
        int NOT_FOUND = 41;
        int INVALID_MODULE = 42;
        int INVALID_MONITOR = 50;
        int NOT_MONITOR_OWNER = 51;
        int INTERRUPT = 52;
        int INVALID_CLASS_FORMAT = 60;
        int CIRCULAR_CLASS_DEFINITION = 61;
        int FAILS_VERIFICATION = 62;
        int ADD_METHOD_NOT_IMPLEMENTED = 63;
        int SCHEMA_CHANGE_NOT_IMPLEMENTED = 64;
        int INVALID_TYPESTATE = 65;
        int HIERARCHY_CHANGE_NOT_IMPLEMENTED = 66;
        int DELETE_METHOD_NOT_IMPLEMENTED = 67;
        int UNSUPPORTED_VERSION = 68;
        int NAMES_DONT_MATCH = 69;
        int CLASS_MODIFIERS_CHANGE_NOT_IMPLEMENTED = 70;
        int METHOD_MODIFIERS_CHANGE_NOT_IMPLEMENTED = 71;
        int NOT_IMPLEMENTED = 99;
        int NULL_POINTER = 100;
        int ABSENT_INFORMATION = 101;
        int INVALID_EVENT_TYPE = 102;
        int ILLEGAL_ARGUMENT = 103;
        int OUT_OF_MEMORY = 110;
        int ACCESS_DENIED = 111;
        int VM_DEAD = 112;
        int INTERNAL = 113;
        int UNATTACHED_THREAD = 115;
        int INVALID_TAG = 500;
        int ALREADY_INVOKING = 502;
        int INVALID_INDEX = 503;
        int INVALID_LENGTH = 504;
        int INVALID_STRING = 506;
        int INVALID_CLASS_LOADER = 507;
        int INVALID_ARRAY = 508;
        int TRANSPORT_LOAD = 509;
        int TRANSPORT_INIT = 510;
        int NATIVE_METHOD = 511;
        int INVALID_COUNT = 512;
    }

    interface EventKind {
        int SINGLE_STEP = 1;
        int BREAKPOINT = 2;
        int FRAME_POP = 3;
        int EXCEPTION = 4;
        int USER_DEFINED = 5;
        int THREAD_START = 6;
        int THREAD_DEATH = 7;
        int THREAD_END = 7;
        int CLASS_PREPARE = 8;
        int CLASS_UNLOAD = 9;
        int CLASS_LOAD = 10;
        int FIELD_ACCESS = 20;
        int FIELD_MODIFICATION = 21;
        int EXCEPTION_CATCH = 30;
        int METHOD_ENTRY = 40;
        int METHOD_EXIT = 41;
        int METHOD_EXIT_WITH_RETURN_VALUE = 42;
        int MONITOR_CONTENDED_ENTER = 43;
        int MONITOR_CONTENDED_ENTERED = 44;
        int MONITOR_WAIT = 45;
        int MONITOR_WAITED = 46;
        int VM_START = 90;
        int VM_INIT = 90;
        int VM_DEATH = 99;
        int VM_DISCONNECTED = 100;
    }

    interface ThreadStatus {
        int ZOMBIE = 0;
        int RUNNING = 1;
        int SLEEPING = 2;
        int MONITOR = 3;
        int WAIT = 4;
    }

    interface SuspendStatus {
        int SUSPEND_STATUS_SUSPENDED = 0x1;
    }

    interface ClassStatus {
        int VERIFIED = 1;
        int PREPARED = 2;
        int INITIALIZED = 4;
        int ERROR = 8;
    }

    public interface TypeTag {
        int CLASS = 1;
        int INTERFACE = 2;
        int ARRAY = 3;
    }

    public interface Tag {
        int ARRAY = 91;
        int BYTE = 66;
        int CHAR = 67;
        int OBJECT = 76;
        int FLOAT = 70;
        int DOUBLE = 68;
        int INT = 73;
        int LONG = 74;
        int SHORT = 83;
        int VOID = 86;
        int BOOLEAN = 90;
        int STRING = 115;
        int THREAD = 116;
        int THREAD_GROUP = 103;
        int CLASS_LOADER = 108;
        int CLASS_OBJECT = 99;
    }

    interface StepDepth {
        int INTO = 0;
        int OVER = 1;
        int OUT = 2;
    }

    interface StepSize {
        int MIN = 0;
        int LINE = 1;
    }

    interface SuspendPolicy {
        int NONE = 0;
        int EVENT_THREAD = 1;
        int ALL = 2;
    }

    /**
     * The invoke options are a combination of zero or more of the following bit flags:
     */
    interface InvokeOptions {
        int INVOKE_SINGLE_THREADED = 0x01;
        int INVOKE_NONVIRTUAL = 0x02;
    }

    private static void notImplemented(PacketStream answer) {
        answer.pkt.errorCode = Error.NOT_IMPLEMENTED;
    }
}
