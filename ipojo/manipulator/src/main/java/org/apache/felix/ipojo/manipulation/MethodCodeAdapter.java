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

import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Insert code calling callbacks at the entry and before the exit of a method.
 * Moreover it replaces all GETFIELD and SETFIELD by getter and setter invocation.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MethodCodeAdapter extends AdviceAdapter implements Opcodes {

    /**
     * The owner class of the field. m_owner : String
     */
    private String m_owner;

    /**
     * Name of the method.
     */
    private String m_name;
    
    /**
     * Contained fields.
     */
    private Set m_fields;

    /**
     * MethodCodeAdapter constructor. 
     * @param mv : MethodVisitor
     * @param owner : Name of the class
     * @param access : Method access
     * @param name : Method name
     * @param desc : Method descriptor
     * @param fields : Contained fields
     */
    public MethodCodeAdapter(final MethodVisitor mv, final String owner, int access, String name, String desc, Set fields) {
        super(mv, access, name, desc);
        m_owner = owner;
        m_name = name;
        m_fields = fields;
    }

    /**
     * Visit an instruction modifying a method (GETFIELD/PUTFIELD).
     * @see org.objectweb.asm.MethodVisitor#visitFieldInsn(int, String, String, String)
     * @param opcode : visited operation code
     * @param owner : owner of the field
     * @param name : name of the field
     * @param desc : descriptor of the field
     */
    public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
        if (owner.equals(m_owner) && m_fields.contains(name)) {
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

    /**
     * Method injecting call at the entry of each method.
     * @see org.objectweb.asm.commons.AdviceAdapter#onMethodEnter()
     */
    protected void onMethodEnter() {
        Type[] args = Type.getArgumentTypes(methodDesc);
        String name = m_name;
        
        for (int i = 0; i < args.length; i++) {
            String cn = args[i].getClassName();
            if (cn.endsWith("[]")) {
                cn = cn.replace('[', '$');
                cn = cn.substring(0, cn.length() - 1);
            }
            cn = cn.replace('.', '_');
            name += cn;
        }

        String flag = "_M" + name;

        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, flag, "Z");
        Label l1 = new Label();
        mv.visitJumpInsn(IFEQ, l1);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");
        mv.visitLdcInsn(name);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", "entryCallback", "(Ljava/lang/String;)V");
        mv.visitLabel(l1);
    }

    /**
     * Method injecting call at the exit of each method.
     * @param opcode : returned opcode (ARETURN, IRETURN, ATHROW ...)
     * @see org.objectweb.asm.commons.AdviceAdapter#onMethodExit(int)
     */
    protected void onMethodExit(int opcode) {
        Type[] args = Type.getArgumentTypes(methodDesc);
        String name = m_name;
       
        for (int i = 0; i < args.length; i++) {
            String cn = args[i].getClassName();
            if (cn.endsWith("[]")) {
                cn = cn.replace('[', '$');
                cn = cn.substring(0, cn.length() - 1);
            }
            cn = cn.replace('.', '_');
            name += cn;
        }

        String flag = "_M" + name;

        int local = newLocal(Type.getType(Object.class));
        if (opcode == RETURN) {
            visitInsn(ACONST_NULL);
        } else if (opcode != ARETURN && opcode != ATHROW) {
            box(Type.getReturnType(this.methodDesc));
        }
        
        mv.visitVarInsn(ASTORE, local);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, flag, "Z");
        Label l5 = new Label();
        mv.visitJumpInsn(IFEQ, l5);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");
        mv.visitLdcInsn(name);
        mv.visitVarInsn(ALOAD, local);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", "exitCallback", "(Ljava/lang/String;Ljava/lang/Object;)V");

        mv.visitLabel(l5);
        if (opcode == ARETURN || opcode == ATHROW) {
            mv.visitVarInsn(ALOAD, local);
        } else if (opcode != RETURN) {
            mv.visitVarInsn(ALOAD, local);
            unbox(Type.getReturnType(this.methodDesc));
        }
    }

    /**
     * Compute max local and max stack size.
     * @param maxStack : new stack size.
     * @param maxLocals : max local (do not modified, super will update it automatically).
     * @see org.objectweb.asm.commons.LocalVariablesSorter#visitMaxs(int, int)
     */
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack + 1, maxLocals);
    }

}
