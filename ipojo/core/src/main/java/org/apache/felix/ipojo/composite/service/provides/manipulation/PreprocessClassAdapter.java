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
package org.apache.felix.ipojo.composite.service.provides.manipulation;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Manipulate the class. - Add a component manager field (_cm) - Create getter
 * and setter for each fields - Store information about field - Store
 * information about implemented interfaces - Change GETFIELD and PUTFIELD to
 * called the getter and setter method
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 * 
 */
public class PreprocessClassAdapter extends ClassAdapter implements Opcodes {

    /**
     * Constant : POJO interface.
     */
    private static final String POJO_INTERFACE = "org/apache/felix/ipojo/Pojo";

    /**
     * The owner. m_owner : String
     */
    private String m_owner;

    /**
     * Constructor.
     * 
     * @param cv : Class visitor
     */
    public PreprocessClassAdapter(final ClassVisitor cv) {
        super(cv);
    }

    /**
     * The visit method. - Insert the _cm field - Create the _initialize method -
     * Create the _cm setter method
     * 
     * @see org.objectweb.asm.ClassVisitor#visit(int, int, String, String,
     * String, String[])
     * @param version : Version
     * @param access : Access modifier
     * @param name : name of the visited element
     * @param signature : singature of the visited element
     * @param superName : superclasses (extend clause)
     * @param interfaces : implement clause
     */
    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {

        m_owner = name;

        // Insert _cm field
        FieldVisitor fv = super.visitField(ACC_PRIVATE, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;", null, null);
        fv.visitEnd();

        // Create the _cmSetter(ComponentManager cm) method
        createComponentManagerSetter();

        // Add the getComponentInstance
        createGetComponentInstanceMethod();

        // Add the POJO interface to the interface list
        // Check that the POJO interface isnot already in the list
        boolean found = false;
        for (int i = 0; i < interfaces.length; i++) {
            if (interfaces[i].equals(POJO_INTERFACE)) {
                found = true;
            }
        }
        String[] itfs;
        if (!found) {
            itfs = new String[interfaces.length + 1];
            for (int i = 0; i < interfaces.length; i++) {
                itfs[i] = interfaces[i];
            }
            itfs[interfaces.length] = POJO_INTERFACE;
        } else {
            itfs = interfaces;
        }

        String str = "";
        for (int i = 0; i < itfs.length; i++) {
            str += itfs[i] + " ";
        }

        super.visit(version, access, name, signature, superName, itfs);
    }

    /**
     * visit method method :-).
     * 
     * @see org.objectweb.asm.ClassVisitor#visitMethod(int, java.lang.String,
     * java.lang.String, java.lang.String, java.lang.String[])
     * @param access : access modifier
     * @param name : name of the method
     * @param desc : descriptor of the method
     * @param signature : signature of the method
     * @param exceptions : exception launched by the method
     * @return MethodVisitor
     */
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        // The method is a constructor, adapt the constructor to add a
        // ComponentManager argument
        if (name.equals("<init>")) {
            // 1) change the constructor descriptor (add a component manager arg
            // as first argument)
            String newDesc = desc.substring(1);
            newDesc = "(Lorg/apache/felix/ipojo/InstanceManager;" + newDesc;

            // Insert the new constructor
            MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "<init>", newDesc, null, null);

            if (mv == null) {
                return null;
            } else {
                return new ConstructorCodeAdapter(mv, m_owner);
            }

        } else {
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
            if (mv == null) {
                return null;
            } else {
                return new PreprocessCodeAdapter(mv, m_owner);
            }
        }

    }

    /**
     * Create the setter method for the _cm field.
     */
    private void createComponentManagerSetter() {
        MethodVisitor mv = cv.visitMethod(ACC_PRIVATE, "_setComponentManager", "(Lorg/apache/felix/ipojo/InstanceManager;)V", null, null);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");

        mv.visitInsn(RETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Create the getComponentInstance method.
     */
    private void createGetComponentInstanceMethod() {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "getComponentInstance", "()Lorg/apache/felix/ipojo/ComponentInstance;", null, null);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");
        mv.visitInsn(ARETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * visit Field method.
     * 
     * @see org.objectweb.asm.ClassVisitor#visitField(int, java.lang.String,
     * java.lang.String, java.lang.String, java.lang.Object)
     * @param access : acces modifier
     * @param name : name of the field
     * @param desc : description of the field
     * @param signature : signature of the field
     * @param value : value of the field
     * @return FieldVisitor
     */
    public FieldVisitor visitField(final int access, final String name, final String desc, final String signature, final Object value) {
        if ((access & ACC_STATIC) == 0) {
            Type type = Type.getType(desc);

            // Keep the field in the code
            FieldVisitor fv = cv.visitField(access, name, desc, signature, value);

            if (type.getSort() == Type.ARRAY) {

                String gDesc = "()" + desc;
                createArrayGetter(name, gDesc);

                // Generates setter method
                String sDesc = "(" + desc + ")V";
                createArraySetter(name, sDesc);

            } else {

                // Generate the getter method
                String gDesc = "()" + desc;
                createSimpleGetter(name, gDesc, type);

                // Generates setter method
                String sDesc = "(" + desc + ")V";
                createSimpleSetter(name, sDesc, type);
            }

            return fv;

        }
        return super.visitField(access, name, desc, signature, value);
    }

    /**
     * Generate a setter method for an array field.
     * @param name : name of the field.
     * @param desc : description of the method
     */
    private void createArraySetter(String name, String desc) {
        MethodVisitor mv = cv.visitMethod(ACC_PRIVATE, "_set" + name, desc, null, null);

        String internalType = desc.substring(1);
        internalType = internalType.substring(0, internalType.length() - 2);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, m_owner, name, internalType);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");
        mv.visitLdcInsn(name);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", "setterCallback", "(Ljava/lang/String;Ljava/lang/Object;)V");

        mv.visitInsn(RETURN);

        // End
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Generate a getter method for an array field.
     * @param name : name of the field.
     * @param desc : description of the method
     */
    private void createArrayGetter(String name, String desc) {

        String methodName = "_get" + name;
        MethodVisitor mv = cv.visitMethod(ACC_PRIVATE, methodName, desc, null, null);

        String internalType = desc.substring(2);

        mv.visitVarInsn(ALOAD, 0);
        // mv.visitFieldInsn(GETFIELD, m_owner, name,
        // "["+type.getInternalName()+";");
        mv.visitFieldInsn(GETFIELD, m_owner, name, internalType);
        mv.visitVarInsn(ASTORE, 1);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");
        mv.visitLdcInsn(name);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", "getterCallback",
                "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;");
        mv.visitVarInsn(ASTORE, 2);

        mv.visitVarInsn(ALOAD, 2);
        mv.visitTypeInsn(CHECKCAST, internalType);
        mv.visitVarInsn(ASTORE, 3);

        Label l3a = new Label();
        mv.visitLabel(l3a);

        mv.visitVarInsn(ALOAD, 1);
        Label l4a = new Label();
        mv.visitJumpInsn(IFNULL, l4a);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z");

        Label l5a = new Label();
        mv.visitJumpInsn(IFNE, l5a);
        mv.visitLabel(l4a);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, m_owner, "_set" + name, "(" + internalType + ")V");
        mv.visitLabel(l5a);

        mv.visitVarInsn(ALOAD, 3);
        mv.visitInsn(ARETURN);

        // End
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Create the getter for service dependency.
     * 
     * @param name : field of the dependency
     * @param desc : description of the getter method
     * @param type : type to return
     */
    private void createSimpleGetter(String name, String desc, Type type) {

        String methodName = "_get" + name;
        MethodVisitor mv = cv.visitMethod(ACC_PRIVATE, methodName, desc, null, null);

        switch (type.getSort()) {
            // Generate the getter method for boolean, char, byte shot as for int.
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                String internalName = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][0];
                String boxingType = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][1];
                String unboxingMethod = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][2];

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, name, internalName);
                mv.visitVarInsn(type.getOpcode(ISTORE), 1);
                mv.visitTypeInsn(NEW, boxingType);
                mv.visitInsn(DUP);
                mv.visitVarInsn(type.getOpcode(ILOAD), 1);
                mv.visitMethodInsn(INVOKESPECIAL, boxingType, "<init>", "(" + internalName + ")V");
                mv.visitVarInsn(ASTORE, 2);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");
                mv.visitLdcInsn(name);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", "getterCallback",
                        "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;");
                mv.visitVarInsn(ASTORE, 3);
                mv.visitVarInsn(ALOAD, 3);
                mv.visitTypeInsn(CHECKCAST, boxingType);
                mv.visitVarInsn(ASTORE, 4);
                mv.visitVarInsn(ALOAD, 4);
                mv.visitMethodInsn(INVOKEVIRTUAL, boxingType, unboxingMethod, "()" + internalName);
                mv.visitVarInsn(type.getOpcode(ISTORE), 5);
                Label l5 = new Label();
                mv.visitLabel(l5);
                mv.visitVarInsn(type.getOpcode(ILOAD), 1);
                mv.visitVarInsn(type.getOpcode(ILOAD), 5);
                Label l6 = new Label();
                mv.visitJumpInsn(type.getOpcode(IF_ICMPEQ), l6);
                Label l7 = new Label();
                mv.visitLabel(l7);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(type.getOpcode(ILOAD), 5);
                mv.visitMethodInsn(INVOKEVIRTUAL, m_owner, "_set" + name, "(" + internalName + ")V");
                mv.visitLabel(l6);
                mv.visitVarInsn(type.getOpcode(ILOAD), 5);
                mv.visitInsn(type.getOpcode(IRETURN));
                break;

            // Generate the getter for long
            case Type.LONG:
                internalName = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][0];
                boxingType = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][1];
                unboxingMethod = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][2];
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, name, internalName);
                mv.visitVarInsn(LSTORE, 1);
                mv.visitTypeInsn(NEW, boxingType);
                mv.visitInsn(DUP);
                mv.visitVarInsn(LLOAD, 1);
                mv.visitMethodInsn(INVOKESPECIAL, boxingType, "<init>", "(" + internalName + ")V");
                mv.visitVarInsn(ASTORE, 3); // Double Space
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");
                mv.visitLdcInsn(name);
                mv.visitVarInsn(ALOAD, 3);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", "getterCallback",
                        "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;");
                mv.visitVarInsn(ASTORE, 4);
                mv.visitVarInsn(ALOAD, 4);
                mv.visitTypeInsn(CHECKCAST, boxingType);
                mv.visitVarInsn(ASTORE, 5);
                mv.visitVarInsn(ALOAD, 5);
                mv.visitMethodInsn(INVOKEVIRTUAL, boxingType, unboxingMethod, "()" + internalName);
                mv.visitVarInsn(LSTORE, 6);
                l5 = new Label();
                mv.visitLabel(l5);
                mv.visitVarInsn(LLOAD, 1);
                mv.visitVarInsn(LLOAD, 6);
                mv.visitInsn(LCMP);
                l6 = new Label();
                mv.visitJumpInsn(IFEQ, l6);
                l7 = new Label();
                mv.visitLabel(l7);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(LLOAD, 6);
                mv.visitMethodInsn(INVOKEVIRTUAL, m_owner, "_set" + name, "(" + internalName + ")V");
                mv.visitLabel(l6);
                mv.visitVarInsn(LLOAD, 6);
                mv.visitInsn(LRETURN);
                break;

            // Generate the getter for double
            case Type.DOUBLE:
                internalName = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][0];
                boxingType = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][1];
                unboxingMethod = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][2];
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, name, internalName);
                mv.visitVarInsn(DSTORE, 1);
                mv.visitTypeInsn(NEW, boxingType);
                mv.visitInsn(DUP);
                mv.visitVarInsn(DLOAD, 1);
                mv.visitMethodInsn(INVOKESPECIAL, boxingType, "<init>", "(" + internalName + ")V");
                mv.visitVarInsn(ASTORE, 3); // Double Space
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");
                mv.visitLdcInsn(name);
                mv.visitVarInsn(ALOAD, 3);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", "getterCallback",
                        "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;");
                mv.visitVarInsn(ASTORE, 4);
                mv.visitVarInsn(ALOAD, 4);
                mv.visitTypeInsn(CHECKCAST, boxingType);
                mv.visitVarInsn(ASTORE, 5);
                mv.visitVarInsn(ALOAD, 5);
                mv.visitMethodInsn(INVOKEVIRTUAL, boxingType, unboxingMethod, "()" + internalName);
                mv.visitVarInsn(DSTORE, 6);
                l5 = new Label();
                mv.visitLabel(l5);
                mv.visitVarInsn(DLOAD, 1);
                mv.visitVarInsn(DLOAD, 6);
                mv.visitInsn(DCMPL);
                l6 = new Label();
                mv.visitJumpInsn(IFEQ, l6);
                l7 = new Label();
                mv.visitLabel(l7);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(DLOAD, 6);
                mv.visitMethodInsn(INVOKEVIRTUAL, m_owner, "_set" + name, "(" + internalName + ")V");
                mv.visitLabel(l6);
                mv.visitVarInsn(DLOAD, 6);
                mv.visitInsn(DRETURN);
                break;

            // Generate for float.
            case Type.FLOAT:
                internalName = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][0];
                boxingType = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][1];
                unboxingMethod = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][2];

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, name, internalName);
                mv.visitVarInsn(FSTORE, 1);

                mv.visitTypeInsn(NEW, boxingType);
                mv.visitInsn(DUP);
                mv.visitVarInsn(FLOAD, 1);
                mv.visitMethodInsn(INVOKESPECIAL, boxingType, "<init>", "(" + internalName + ")V");
                mv.visitVarInsn(ASTORE, 2); // One Space

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");
                mv.visitLdcInsn(name);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", "getterCallback",
                        "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;");
                mv.visitVarInsn(ASTORE, 3);

                mv.visitVarInsn(ALOAD, 3);
                mv.visitTypeInsn(CHECKCAST, boxingType);
                mv.visitVarInsn(ASTORE, 4);

                mv.visitVarInsn(ALOAD, 4);
                mv.visitMethodInsn(INVOKEVIRTUAL, boxingType, unboxingMethod, "()" + internalName);
                mv.visitVarInsn(FSTORE, 5);

                l5 = new Label();
                mv.visitLabel(l5);
                
                mv.visitVarInsn(FLOAD, 1);
                mv.visitVarInsn(FLOAD, 5);
                mv.visitInsn(FCMPL);
                l6 = new Label();
                mv.visitJumpInsn(IFEQ, l6);

                l7 = new Label();
                mv.visitLabel(l7);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(FLOAD, 5);
                mv.visitMethodInsn(INVOKEVIRTUAL, m_owner, "_set" + name, "(" + internalName + ")V");
                mv.visitLabel(l6);

                mv.visitVarInsn(FLOAD, 5);
                mv.visitInsn(FRETURN);

                break;

            //Generate for array.
            case Type.OBJECT:

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, name, "L" + type.getInternalName() + ";");
                mv.visitVarInsn(ASTORE, 1);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");
                mv.visitLdcInsn(name);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", "getterCallback",
                        "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;");
                mv.visitVarInsn(ASTORE, 2);

                mv.visitVarInsn(ALOAD, 2);
                mv.visitTypeInsn(CHECKCAST, type.getInternalName());
                mv.visitVarInsn(ASTORE, 3);

                Label l3b = new Label();
                mv.visitLabel(l3b);

                mv.visitVarInsn(ALOAD, 1);
                Label l4b = new Label();
                mv.visitJumpInsn(IFNULL, l4b);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitVarInsn(ALOAD, 3);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z");

                Label l5b = new Label();
                mv.visitJumpInsn(IFNE, l5b);
                mv.visitLabel(l4b);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 3);
                mv.visitMethodInsn(INVOKEVIRTUAL, m_owner, "_set" + name, "(L" + type.getInternalName() + ";)V");
                mv.visitLabel(l5b);

                mv.visitVarInsn(ALOAD, 3);
                mv.visitInsn(ARETURN);

                break;

            default:
                break;
        }

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Create the setter method for one property. The name of the method is
     * _set+name of the field
     * 
     * @param name : name of the field representing a property
     * @param desc : description of the setter method
     * @param type : type of the property
     */
    private void createSimpleSetter(String name, String desc, Type type) {
        MethodVisitor mv = cv.visitMethod(ACC_PRIVATE, "_set" + name, desc, null, null);

        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
            case Type.FLOAT:
                String internalName = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][0];
                String boxingType = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][1];

                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(type.getOpcode(ILOAD), 1);
                mv.visitFieldInsn(PUTFIELD, m_owner, name, internalName);
                Label l1 = new Label();
                mv.visitLabel(l1);

                mv.visitTypeInsn(NEW, boxingType);
                mv.visitInsn(DUP);
                mv.visitVarInsn(type.getOpcode(ILOAD), 1);
                mv.visitMethodInsn(INVOKESPECIAL, boxingType, "<init>", "(" + internalName + ")V");
                mv.visitVarInsn(ASTORE, 2);

                Label l2 = new Label();
                mv.visitLabel(l2);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");
                mv.visitLdcInsn(name);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", "setterCallback", "(Ljava/lang/String;Ljava/lang/Object;)V");

                Label l3 = new Label();
                mv.visitLabel(l3);
                mv.visitInsn(RETURN);
                break;

            case Type.LONG:
            case Type.DOUBLE:
                internalName = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][0];
                boxingType = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][1];

                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(type.getOpcode(ILOAD), 1);
                mv.visitFieldInsn(PUTFIELD, m_owner, name, internalName);
                l1 = new Label();
                mv.visitLabel(l1);

                mv.visitTypeInsn(NEW, boxingType);
                mv.visitInsn(DUP);
                mv.visitVarInsn(type.getOpcode(ILOAD), 1);
                mv.visitMethodInsn(INVOKESPECIAL, boxingType, "<init>", "(" + internalName + ")V");
                mv.visitVarInsn(ASTORE, 3); // Double space

                l2 = new Label();
                mv.visitLabel(l2);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");
                mv.visitLdcInsn(name);
                mv.visitVarInsn(ALOAD, 3);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", "setterCallback", "(Ljava/lang/String;Ljava/lang/Object;)V");

                l3 = new Label();
                mv.visitLabel(l3);
                mv.visitInsn(RETURN);
                break;

            case Type.OBJECT:

                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitFieldInsn(PUTFIELD, m_owner, name, "L" + type.getInternalName() + ";");

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");
                mv.visitLdcInsn(name);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", "setterCallback", "(Ljava/lang/String;Ljava/lang/Object;)V");

                mv.visitInsn(RETURN);
                break;
            default:
                break;
        }

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
