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

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;

/**
 * Constructor Adapter : add a component manager argument inside a constructor.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ConstructorCodeAdapter extends GeneratorAdapter implements Opcodes {

    /**
     * The owner class of the field. 
     * m_owner : String
     */
    private String m_owner;

    /**
     * Is the super call already detected ? 
     */
    private boolean m_superDetected;

    /**
     * ConstructorCodeAdapter constructor. 
     * @param mv : method visitor.
     * @param access : access level of the method.
     * @param desc : descriptor of the constructor. 
     * @param owner : onwer class
     */
    public ConstructorCodeAdapter(final MethodVisitor mv, int access,
            String desc, final String owner) {
        super(mv, access, "<init>", desc);
        m_owner = owner;
      //  m_superDetected = false;
    }

    /**
     * Visit a method instruction.
     * Inject the _setComponentManager invocation just of the super invocation.
     * @param opcode : opcode
     * @param owner : method owner class
     * @param name : method name
     * @param desc : method description
     * @see org.objectweb.asm.commons.AdviceAdapter#visitMethodInsn(int, java.lang.String, java.lang.String, java.lang.String)
     */
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        // A method call is detected, check if it is the super call :
        if (!m_superDetected) {
            m_superDetected = true;
            // The first invocation is the super call
            // 1) Visit the super constructor :
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(opcode, owner, name, desc); // Super constructor
                                                            // invocation

            // 2) Load the object and the component manager argument
            mv.visitVarInsn(ALOAD, 0);
            // mv.visitVarInsn(ALOAD,
            // Type.getArgumentTypes(m_constructorDesc).length);
            mv.visitVarInsn(ALOAD, 1); // CM is always the first argument
            // 3) Initialize the field
            mv.visitMethodInsn(INVOKESPECIAL, m_owner, "_setComponentManager",
                    "(Lorg/apache/felix/ipojo/InstanceManager;)V");
            // insertion finished
        } else {
            mv.visitMethodInsn(opcode, owner, name, desc);
        }
    }

    /**
     * Visit an instruction modifying a local variable.
     * Increment the variable number if != 0.
     * @param opcode : opcode of the instruction.
     * @param var : variable number.
     * @see org.objectweb.asm.commons.AdviceAdapter#visitVarInsn(int, int)
     */
    public void visitVarInsn(int opcode, int var) {
        if (var == 0) {
            mv.visitVarInsn(opcode, var); // ALOAD 0 (THIS)
        } else {
            mv.visitVarInsn(opcode, var + 1); // All other variable count
        } 
    }

    /**
     * Visit an increment instruction.
     * Increment the variable number.
     * @param var : incremented variable number.
     * @param increment : increment.
     * @see org.objectweb.asm.commons.LocalVariablesSorter#visitIincInsn(int, int)
     */
    public void visitIincInsn(int var, int increment) {
        if (var != 0) {
            mv.visitIincInsn(var + 1, increment);
        } else {
            mv.visitIincInsn(var, increment);
        }
    }

    /**
     * Visit local variable.
     * @param name : name of the variable.
     * @param desc : description of the variable.
     * @param signature : signature of the variable.
     * @param start : starting label.
     * @param end : ending label.
     * @param index : variable index.
     * @see org.objectweb.asm.commons.LocalVariablesSorter#visitLocalVariable(java.lang.String, java.lang.String, java.lang.String, org.objectweb.asm.Label, org.objectweb.asm.Label, int)
     */
    public void visitLocalVariable(String name, String desc, String signature,
            Label start, Label end, int index) {
        if (index == 0) {
            mv.visitLocalVariable(name, desc, signature, start, end, index);
            mv.visitLocalVariable("_manager", "Lorg/apache/felix/ipojo/InstanceManager;", null, start, end, 1);
        } else {
            mv.visitLocalVariable(name, desc, signature, start, end, index + 1);   
        }
    }

    /**
     * Manage a field operation.
     * Replace GETFIELD and PUTFILED by getter and setter invocation.
     * 
     * @param opcode : visited operation code
     * @param owner : owner of the field
     * @param name : name of the field
     * @param desc : decriptor of the field
     * @see org.objectweb.asm.commons.AdviceAdapter#visitFieldInsn(int, java.lang.String, java.lang.String, java.lang.String)
     */
    public void visitFieldInsn(final int opcode, final String owner,
            final String name, final String desc) {
        if (owner.equals(m_owner)) {
            if (opcode == GETFIELD) {
                String gDesc = "()" + desc;
                visitMethodInsn(INVOKEVIRTUAL, owner, "_get" + name, gDesc);
                return;
            } else if (opcode == PUTFIELD) {
                String sDesc = "(" + desc + ")V";
                visitMethodInsn(INVOKESPECIAL, owner, "_set" + name, sDesc);
                return;
            }
        }
        super.visitFieldInsn(opcode, owner, name, desc);
    }
}
