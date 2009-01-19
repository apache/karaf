/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.ipojo.handler.temporal;


import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Generates proxy class delegating operation invocations thanks to a
 * a temporal dependency.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ProxyGenerator implements Opcodes {
    
    /**
     * The temporal dependency name. 
     */
    private static final String DEPENDENCY = "m_dependency";
    
    /**
     * The Temporal Dependency descriptor.
     */
    private static final String DEPENDENCY_DESC = Type.getDescriptor(TemporalDependency.class);
    
    /**
     * Temporal dependency internal class name. 
     */
    private static final String TEMPORAL_DEPENDENCY = "org/apache/felix/ipojo/handler/temporal/TemporalDependency";
    
    /**
     * Gets the internal names of the given class objects. 
     * @param classes the classes
     * @return the array containing internal names of the given class array. 
     */
    private static String[] getInternalClassNames(Class[] classes) {
        final String[] names = new String[classes.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = Type.getInternalName(classes[i]);
        }
        return names;
    }
    
    /**
     * Generates a proxy class.
     * @param spec the proxied service specification
     * @return the byte[] for the generated proxy class.
     */
    public static byte[] dumpProxy(Class spec) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        String internalClassName = Type.getInternalName(spec); // Specification class internal name.
        String[] itfs = new String[] {internalClassName};  // Implemented interface.
        String className = internalClassName + "$$Proxy"; // Unique name.
        Method[] methods = spec.getMethods(); // Method to delegate
        
        cw.visit(Opcodes.V1_3, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, className, null, "java/lang/Object", itfs);
        addDependencyField(cw);
        generateConstructor(cw, className);
        
        // For each method, create the delegator code.
        for (int i = 0; i < methods.length; i++) {
            if ((methods[i].getModifiers() & (Modifier.STATIC | Modifier.FINAL)) == 0) {
                generateDelegator(cw, methods[i], className, internalClassName);
            }
        }        
        
        cw.visitEnd();
        
        return cw.toByteArray();
        
    }

    /**
     * Generates a delegated method.
     * @param cw the class writer
     * @param method the method object to delegate
     * @param className the generated class name
     * @param itfName the internal specification class name
     */
    private static void generateDelegator(ClassWriter cw, Method method,
            String className, String itfName) {
        String methodName = method.getName();
        String desc = Type.getMethodDescriptor(method);
        String[] exceptions = getInternalClassNames(method.getExceptionTypes());
        int modifiers = method.getModifiers()
                & ~(Modifier.ABSTRACT | Modifier.NATIVE | Modifier.SYNCHRONIZED);
        Type[] types = Type.getArgumentTypes(method);

        int freeRoom = 1;
        for (int t = 0; t < types.length; t++) {
            freeRoom = freeRoom + types[t].getSize();
        }

        MethodVisitor mv = cw.visitMethod(modifiers, methodName, desc, null,
                exceptions);
        mv.visitCode();

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, DEPENDENCY, DEPENDENCY_DESC);  // The temporal dependency is on the stack.
        mv.visitMethodInsn(INVOKEVIRTUAL, TEMPORAL_DEPENDENCY, "getService", // Call getService
                "()Ljava/lang/Object;"); // The service object is on the stack.
        int varSvc = freeRoom; 
        freeRoom = freeRoom + 1; // Object Reference.
        mv.visitVarInsn(ASTORE, varSvc); // Store the service object.
        
        // Invoke the method on the service object.
        mv.visitVarInsn(ALOAD, varSvc);
        // Push argument on the stack.
        int i = 1; // Arguments. (non static method)
        for (int t = 0; t < types.length; t++) {
            mv.visitVarInsn(types[t].getOpcode(ILOAD), i); 
            i = i + types[t].getSize();
        }
        // Invocation
        mv.visitMethodInsn(INVOKEINTERFACE, itfName, methodName, desc);

        // Return the result
        Type returnType = Type.getReturnType(desc);
        if (returnType.getSort() != Type.VOID) {
            mv.visitInsn(returnType.getOpcode(IRETURN));
        } else {
            mv.visitInsn(RETURN);
        }

        // End of the method.
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Generates the constructors. The constructor receives a temporal dependency
     * and set the {@link ProxyGenerator#DEPENDENCY} field.
     * @param cw the class writer
     * @param className the generated class name.
     */
    private static void generateConstructor(ClassWriter cw, String className) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", '(' + DEPENDENCY_DESC + ")V", null, null);
        mv.visitCode();

        mv.visitVarInsn(ALOAD, 0); // Load this
        mv.visitInsn(DUP); // Dup
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V"); // Call  super
        mv.visitVarInsn(ALOAD, 1); // Load the argument
        mv.visitFieldInsn(PUTFIELD, className, DEPENDENCY, DEPENDENCY_DESC); // Assign the dependency field
        mv.visitInsn(RETURN); // Return void

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Adds the temporal dependency field {@link ProxyGenerator#DEPENDENCY}.
     * @param cw the class writer
     */
    private static void addDependencyField(ClassWriter cw) {
        cw.visitField(Opcodes.ACC_FINAL, DEPENDENCY, DEPENDENCY_DESC, null, null);
        cw.visitEnd();
    }
    
   

}
