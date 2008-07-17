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
package org.apache.felix.ipojo.composite.service.provides;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.felix.ipojo.Handler;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Create the Proxy class.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class POJOWriter implements Opcodes {
    
    //TODO : merge this class with another class only static methods.

    /**
     * Create a class.
     * @param cw : class writer
     * @param className : class name
     * @param spec : implemented specification
     */
    private static void createClass(ClassWriter cw, String className, String spec) {
        // Create the class
        cw.visit(V1_2, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object", new String[] { spec.replace('.', '/') });
    }

    /**
     * Inject field in the current class.
     * @param cw : class writer.
     * @param fields : list of field to inject.
     */
    private static void injectFields(ClassWriter cw, List fields) {
        // Inject fields
        for (int i = 0; i < fields.size(); i++) {
            FieldMetadata field = (FieldMetadata) fields.get(i);
            if (field.isUseful()) {
                SpecificationMetadata spec = field.getSpecification();
                String fieldName = field.getName();
                String desc = "";
                if (field.isAggregate()) {
                    desc = "[L" + spec.getName().replace('.', '/') + ";";
                } else {
                    desc = "L" + spec.getName().replace('.', '/') + ";";
                }

                cw.visitField(Opcodes.ACC_PRIVATE, fieldName, desc, null, null);
            }
        }
    }
    
    /**
     * Generates an empty constructor.
     * this constructor just call the java.lang.Object constructor.
     * @param cw class writer
     */
    private static void generateConstructor(ClassWriter cw) {
        // Inject a constructor <INIT>()V
        MethodVisitor cst = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        cst.visitVarInsn(ALOAD, 0);
        cst.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        cst.visitInsn(RETURN);
        cst.visitMaxs(0, 0);
        cst.visitEnd();
    }

    /**
     * Return the proxy 'classname' for the contract 'contractname' by delegating on available service.
     * @param clazz : Specification class
     * @param className : The class name to create
     * @param fields : the list of fields on which delegate
     * @param methods : the list of method on which delegate
     * @param handler : handler object used to access the logger
     * @return byte[] : the build class
     */
    public static byte[] dump(Class clazz, String className, List fields, List methods, Handler handler) {
        Method[] itfmethods = clazz.getMethods();

        // Create the class
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        className = className.replace('.', '/');
        createClass(cw, className, clazz.getName());

        // Inject fields inside the POJO
        injectFields(cw, fields);

        generateConstructor(cw);
        
        for (int i = 0; i < itfmethods.length; ++i) {
            Method method = itfmethods[i];

            // Get the field for this method
            // 1) find the MethodMetadata
            FieldMetadata delegator = null; // field to delegate
            MethodMetadata methodDelegator = null; // field to delegate
            for (int j = 0; j < methods.size(); j++) {
                MethodMetadata methodMeta = (MethodMetadata) methods.get(j);
                if (methodMeta.equals(method)) {
                    delegator = methodMeta.getDelegation();
                    methodDelegator = methodMeta;
                }
            }

            generateMethod(cw, className, methodDelegator, method, delegator, handler);

        }

        // End process
        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * Generate on method.
     * @param cw : class writer
     * @param className : the current class name
     * @param method : the method to generate
     * @param sign : method signature to generate
     * @param delegator : the field on which delegate
     * @param handler : the handler (used to acess the logger)
     */
    private static void generateMethod(ClassWriter cw, String className, MethodMetadata method, Method sign, FieldMetadata delegator, Handler handler) {
        String desc = Type.getMethodDescriptor(sign);
        String name = sign.getName();
        String[] exc = new String[sign.getExceptionTypes().length];
        for (int i = 0; i < sign.getExceptionTypes().length; i++) {
            exc[i] = Type.getType(sign.getExceptionTypes()[i]).getInternalName();
        }

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, name, desc, null, exc);

        if (delegator.isOptional()) {
            if (!delegator.isAggregate()) {
                generateOptionalCase(mv, delegator, className);
            }
            if (delegator.isAggregate() /*&& method.getPolicy() == MethodMetadata.ONE_POLICY*/) {
                generateOptionalAggregateCase(mv, delegator, className);
            }
        }

        if (delegator.isAggregate()) {
            if (method.getPolicy() == MethodMetadata.ONE_POLICY) {
                // Aggregate and One Policy
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, delegator.getName(), "[L" + delegator.getSpecification().getName().replace('.', '/') + ";");
                mv.visitInsn(ICONST_0); // Use the first one
                mv.visitInsn(AALOAD);

                loadArgs(mv, ACC_PUBLIC, Type.getArgumentTypes(desc));

                // Invoke
                mv.visitMethodInsn(INVOKEINTERFACE, delegator.getSpecification().getName().replace('.', '/'), name, desc);

                // Return
                mv.visitInsn(Type.getReturnType(desc).getOpcode(Opcodes.IRETURN));

            } else { // All policy
                if (Type.getReturnType(desc).getSort() != Type.VOID) {
                    handler.error("All policy cannot be used on method which does not return void");
                }

                Type[] args = Type.getArgumentTypes(desc);
                int index = args.length + 1;

                // Init
                mv.visitInsn(ICONST_0);
                mv.visitVarInsn(ISTORE, index);
                Label l1b = new Label();
                mv.visitLabel(l1b);
                Label l2b = new Label();
                mv.visitJumpInsn(GOTO, l2b);

                // Loop
                Label l3b = new Label();
                mv.visitLabel(l3b);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, delegator.getName(), "[L" + delegator.getSpecification().getName().replace('.', '/') + ";");
                mv.visitVarInsn(ILOAD, index);
                mv.visitInsn(AALOAD);

                loadArgs(mv, ACC_PUBLIC, Type.getArgumentTypes(desc));

                mv.visitMethodInsn(INVOKEINTERFACE, delegator.getSpecification().getName().replace('.', '/'), name, desc);

                Label l4b = new Label();
                mv.visitLabel(l4b);
                mv.visitIincInsn(index, 1); // i++;

                // Condition
                mv.visitLabel(l2b);
                mv.visitVarInsn(ILOAD, index);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, delegator.getName(), "[L" + delegator.getSpecification().getName().replace('.', '/') + ";");
                mv.visitInsn(ARRAYLENGTH);
                mv.visitJumpInsn(IF_ICMPLT, l3b);

                Label l5b = new Label();
                mv.visitLabel(l5b);
                mv.visitInsn(RETURN);
            }
        } else {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, delegator.getName(), "L" + delegator.getSpecification().getName().replace('.', '/') + ";");

            loadArgs(mv, ACC_PUBLIC, Type.getArgumentTypes(desc));

            // Invoke
            if (delegator.getSpecification().isInterface()) {
                mv.visitMethodInsn(INVOKEINTERFACE, delegator.getSpecification().getName().replace('.', '/'), name, desc);
            } else {
                mv.visitMethodInsn(INVOKEVIRTUAL, delegator.getSpecification().getName().replace('.', '/'), name, desc);
            }

            // Return
            mv.visitInsn(Type.getReturnType(desc).getOpcode(IRETURN));
        }

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Generate Optional Case for aggregate field.
     * @param mv : method visitor
     * @param delegator : Field on which delegate
     * @param className : current class name
     */
    private static void generateOptionalAggregateCase(MethodVisitor mv, FieldMetadata delegator, String className) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, delegator.getName(), "[L" + delegator.getSpecification().getName().replace('.', '/') + ";");
        mv.visitInsn(ARRAYLENGTH);
        Label l1a = new Label();
        mv.visitJumpInsn(IFNE, l1a);
        Label l2a = new Label();
        mv.visitLabel(l2a);
        mv.visitTypeInsn(NEW, "java/lang/UnsupportedOperationException");
        mv.visitInsn(DUP);
        mv.visitLdcInsn("Operation not supported");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/UnsupportedOperationException", "<init>", "(Ljava/lang/String;)V");
        mv.visitInsn(ATHROW);
        mv.visitLabel(l1a);
    }

    /**
     * Generate Optional case for non aggregate fields.
     * 
     * @param mv : the method visitor
     * @param delegator : the field on which delegate.
     * @param className : the name of the current class.
     */
    private static void generateOptionalCase(MethodVisitor mv, FieldMetadata delegator, String className) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, delegator.getName(), "L" + delegator.getSpecification().getName().replace('.', '/') + ";");
        mv.visitTypeInsn(INSTANCEOF, "org/apache/felix/ipojo/Nullable");
        Label end = new Label();
        mv.visitJumpInsn(IFEQ, end);
        Label begin = new Label();
        mv.visitLabel(begin);
        mv.visitTypeInsn(NEW, "java/lang/UnsupportedOperationException");
        mv.visitInsn(DUP);
        mv.visitLdcInsn("Operation not supported");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/UnsupportedOperationException", "<init>", "(Ljava/lang/String;)V");
        mv.visitInsn(ATHROW);
        mv.visitLabel(end);
    }
    
    /**
     * Load on stack the method arguments.
     * @param mv method visitor
     * @param access access level of the method
     * @param args argument types array
     */
    private static void loadArgs(MethodVisitor mv, int access, Type[] args) {
        int i = 0;
        int j = args.length;
        int k = getArgIndex(access, args, i);
        for (int l = 0; l < j; l++) {
            Type type = args[i + l];
            mv.visitVarInsn(type.getOpcode(ILOAD), k);
            k += type.getSize();
        }
    }
    
    /**
     * Gets the index of the argument 'i'.
     * This method manages double-spaces.
     * @param access method access (mostly public)
     * @param args argument type array
     * @param i wanted index
     * @return the real index
     */
    private static int getArgIndex(int access, Type[] args, int i) {
        int j = (access & 8) != 0 ? 0 : 1;
        for (int k = 0; k < i; k++) {
            j += args[k].getSize();
        }
        return j;
    }

}
