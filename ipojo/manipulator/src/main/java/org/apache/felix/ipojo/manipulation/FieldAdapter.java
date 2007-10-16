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
package org.apache.felix.ipojo.manipulation;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Create getter and setter for each fields .
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FieldAdapter extends ClassAdapter implements Opcodes {

    /**
     * The owner.
     * m_owner : String
     */
    private String m_owner;

    /**
     * Constructor.
     * @param cv : Class visitor
     */
    public FieldAdapter(final ClassVisitor cv) {
        super(cv);
    }

    /**
     * The visit method. - Insert the _cm field - Create the _initialize method - Create the _cm setter method
     * 
     * @see org.objectweb.asm.ClassVisitor#visit(int, int, String, String, String, String[])
     * @param version : Version
     * @param access : Access modifier
     * @param name : name of the visited element
     * @param signature : signature of the visited element
     * @param superName : superclass (extend clause)
     * @param interfaces : implement clause
     */
    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
        m_owner = name;
    }

    /**
     * Visit a Field.
     * Inject the getter and the setter method for this field.
     * @see org.objectweb.asm.ClassVisitor#visitField(int, java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
     * @param access : access modifier
     * @param name : name of the field
     * @param desc : description of the field
     * @param signature : signature of the field
     * @param value : value of the field
     * @return FieldVisitor : null
     */
    public FieldVisitor visitField(final int access, final String name, final String desc, final String signature, final Object value) {

        if ((access & ACC_STATIC) == 0) {
            //ManipulationProperty.getLogger().log(Level.INFO, "Manipulate the field declaration of " + name);
            Type type = Type.getType(desc);

            if (type.getSort() == Type.ARRAY) {
                String gDesc = "()" + desc;
                createArrayGetter(name, gDesc, type);

                // Generates setter method
                String sDesc = "(" + desc + ")V";
                createArraySetter(name, sDesc, type);

            } else {
                // Generate the getter method
                String gDesc = "()" + desc;
                createSimpleGetter(name, gDesc, type);

                // Generates setter method
                String sDesc = "(" + desc + ")V";
                createSimpleSetter(name, sDesc, type);
            }

        }

        return null;
    }

    /**
     * Create a getter method for an array.
     * @param name : field name
     * @param desc : method description
     * @param type : contained type (inside the array)
     */
    private void createArraySetter(String name, String desc, Type type) {
        MethodVisitor mv = cv.visitMethod(ACC_PRIVATE, "_set" + name, desc, null, null);

        String internalType = desc.substring(1);
        internalType = internalType.substring(0, internalType.length() - 2);

        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, "_F" + name, "Z");
        Label l2 = new Label();
        mv.visitJumpInsn(IFNE, l2);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, m_owner, name, internalType);
        mv.visitInsn(RETURN);
        mv.visitLabel(l2);

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
     * Create a setter method for an array.
     * @param name : field name
     * @param desc : method description
     * @param type : contained type (inside the array)
     */
    private void createArrayGetter(String name, String desc, Type type) {

        String methodName = "_get" + name;
        MethodVisitor mv = cv.visitMethod(ACC_PRIVATE, methodName, desc, null, null);

        String internalType = desc.substring(2);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, "_F" + name, "Z");
        Label l1 = new Label();
        mv.visitJumpInsn(IFNE, l1);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, name, internalType);
        mv.visitInsn(ARETURN);
        mv.visitLabel(l1);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");
        mv.visitLdcInsn(name);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", "getterCallback", "(Ljava/lang/String;)Ljava/lang/Object;");
        mv.visitTypeInsn(CHECKCAST, internalType);
        mv.visitInsn(ARETURN);

        // End
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Create the getter for a field.
     * @param name : field of the dependency
     * @param desc : description of the getter method
     * @param type : type to return
     */
    private void createSimpleGetter(String name, String desc, Type type) {

        String methodName = "_get" + name;
        MethodVisitor mv = cv.visitMethod(ACC_PRIVATE, methodName, desc, null, null);

        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:

                String internalName = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][0];
                String boxingType = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][1];
                String unboxingMethod = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][2];

                Label l0 = new Label();
                mv.visitLabel(l0);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, "_F" + name, "Z");
                Label l1 = new Label();
                mv.visitJumpInsn(IFNE, l1);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, name, internalName);
                mv.visitInsn(IRETURN);

                mv.visitLabel(l1);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");
                mv.visitLdcInsn(name);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", "getterCallback", "(Ljava/lang/String;)Ljava/lang/Object;");
                mv.visitVarInsn(ASTORE, 1);

                mv.visitVarInsn(ALOAD, 1);
                mv.visitTypeInsn(CHECKCAST, boxingType);
                mv.visitVarInsn(ASTORE, 2);

                mv.visitVarInsn(ALOAD, 2);
                mv.visitMethodInsn(INVOKEVIRTUAL, boxingType, unboxingMethod, "()" + internalName);
                mv.visitInsn(type.getOpcode(IRETURN));
                break;

            case Type.LONG:
                internalName = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][0];
                boxingType = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][1];
                unboxingMethod = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][2];

                l0 = new Label();
                mv.visitLabel(l0);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, "_F" + name, "Z");
                l1 = new Label();
                mv.visitJumpInsn(IFNE, l1);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, name, internalName);
                mv.visitInsn(LRETURN);
                mv.visitLabel(l1);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");
                mv.visitLdcInsn(name);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", "getterCallback", "(Ljava/lang/String;)Ljava/lang/Object;");
                mv.visitVarInsn(ASTORE, 1);

                mv.visitVarInsn(ALOAD, 1);
                mv.visitTypeInsn(CHECKCAST, boxingType);
                mv.visitVarInsn(ASTORE, 2);

                mv.visitVarInsn(ALOAD, 2);
                mv.visitMethodInsn(INVOKEVIRTUAL, boxingType, unboxingMethod, "()" + internalName);
                mv.visitInsn(LRETURN);

                break;

            case Type.DOUBLE:
                internalName = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][0];
                boxingType = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][1];
                unboxingMethod = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][2];

                l0 = new Label();
                mv.visitLabel(l0);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, "_F" + name, "Z");
                l1 = new Label();
                mv.visitJumpInsn(IFNE, l1);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, name, internalName);
                mv.visitInsn(DRETURN);
                mv.visitLabel(l1);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");
                mv.visitLdcInsn(name);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", "getterCallback", "(Ljava/lang/String;)Ljava/lang/Object;");
                mv.visitVarInsn(ASTORE, 1);

                mv.visitVarInsn(ALOAD, 1);
                mv.visitTypeInsn(CHECKCAST, boxingType);
                mv.visitVarInsn(ASTORE, 2);

                mv.visitVarInsn(ALOAD, 2);
                mv.visitMethodInsn(INVOKEVIRTUAL, boxingType, unboxingMethod, "()" + internalName);
                mv.visitInsn(DRETURN);

                break;

            case Type.FLOAT:
                internalName = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][0];
                boxingType = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][1];
                unboxingMethod = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][2];

                l0 = new Label();
                mv.visitLabel(l0);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, "_F" + name, "Z");
                l1 = new Label();
                mv.visitJumpInsn(IFNE, l1);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, name, internalName);
                mv.visitInsn(FRETURN);
                mv.visitLabel(l1);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");
                mv.visitLdcInsn(name);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", "getterCallback", "(Ljava/lang/String;)Ljava/lang/Object;");
                mv.visitVarInsn(ASTORE, 1);

                mv.visitVarInsn(ALOAD, 1);
                mv.visitTypeInsn(CHECKCAST, boxingType);
                mv.visitVarInsn(ASTORE, 2);

                mv.visitVarInsn(ALOAD, 2);
                mv.visitMethodInsn(INVOKEVIRTUAL, boxingType, unboxingMethod, "()" + internalName);
                mv.visitInsn(FRETURN);

                break;

            case Type.OBJECT:
                l0 = new Label();
                mv.visitLabel(l0);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, "_F" + name, "Z");
                l1 = new Label();
                mv.visitJumpInsn(IFNE, l1);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, name, "L" + type.getInternalName() + ";");
                mv.visitInsn(ARETURN);
                mv.visitLabel(l1);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");
                mv.visitLdcInsn(name);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", "getterCallback", "(Ljava/lang/String;)Ljava/lang/Object;");
                mv.visitTypeInsn(CHECKCAST, type.getInternalName());
                mv.visitInsn(ARETURN);

                break;

            default:
                ManipulationProperty.getLogger().log(ManipulationProperty.SEVERE, "Manipulation problem in " + m_owner + " : a type is not implemented : " + type);
                break;
        }

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Create the setter method for one property. The name of the method is _set+name of the field
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

                Label l1 = new Label();
                mv.visitLabel(l1);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, "_F" + name, "Z");
                Label l22 = new Label();
                mv.visitJumpInsn(IFNE, l22);
                
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(type.getOpcode(ILOAD), 1);
                mv.visitFieldInsn(PUTFIELD, m_owner, name, internalName);
                mv.visitInsn(RETURN);
                mv.visitLabel(l22);

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

                l1 = new Label();
                mv.visitLabel(l1);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, "_F" + name, "Z");
                Label l23 = new Label();
                mv.visitJumpInsn(IFNE, l23);
           
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(type.getOpcode(ILOAD), 1);
                mv.visitFieldInsn(PUTFIELD, m_owner, name, internalName);
                mv.visitInsn(RETURN);
                mv.visitLabel(l23);

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
                mv.visitFieldInsn(GETFIELD, m_owner, "_F" + name, "Z");
                Label l24 = new Label();
                mv.visitJumpInsn(IFNE, l24);
           
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitFieldInsn(PUTFIELD, m_owner, name, "L" + type.getInternalName() + ";");
                mv.visitInsn(RETURN);
                mv.visitLabel(l24);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");
                mv.visitLdcInsn(name);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", "setterCallback", "(Ljava/lang/String;Ljava/lang/Object;)V");

                mv.visitInsn(RETURN);
                break;
            default:
                ManipulationProperty.getLogger().log(ManipulationProperty.SEVERE, "Manipulation Error : Cannot create the setter method for the field : " + name + " (" + type + ")");
                break;
        }

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
