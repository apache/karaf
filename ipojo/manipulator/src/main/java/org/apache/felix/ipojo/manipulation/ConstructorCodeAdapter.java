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
import org.objectweb.asm.commons.GeneratorAdapter;


/**
 * Constructor Adapter : add a component manager argument inside a constructor.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ConstructorCodeAdapter extends GeneratorAdapter implements Opcodes {

    /** 
     * The class containing the field.
     * m_owner : String
     */
    private String m_owner;
    
    /**
     * Is the super call detected ?
     */
    private boolean m_superDetected;
    
    /**
     * Set of contained fields.
     */
    private Set m_fields;
    
    /**
     * Constructor description.
     */
    private String m_desc;

    /** 
     * PropertyCodeAdapter constructor.
     * A new FiledCodeAdapter should be create for each method visit.
     * @param mv MethodVisitor
     * @param owner Name of the class
     * @param fields List of contained fields
     * @param access Access
     * @param desc : constructor descriptor
     * @param name : name
     */
    public ConstructorCodeAdapter(final MethodVisitor mv, final String owner, Set fields, int access, String name, String desc) {
        super(mv, access, name, desc);
        m_owner = owner;
        m_superDetected = false;
        m_fields = fields;
        m_desc = desc;
    }


    /** 
     * Visit Method for Field instance (GETFIELD).
     * @see org.objectweb.asm.MethodVisitor#visitFieldInsn(int, String, String, String)
     * @param opcode : visited operation code
     * @param owner : owner of the field
     * @param name : name of the field
     * @param desc : descriptor of the field
     */
    public void visitFieldInsn(
            final int opcode,
            final String owner,
            final String name,
            final String desc) {
        if (m_fields.contains(name) && m_owner.equals(owner)) {
            if (opcode == GETFIELD) {
                String gDesc = "()" + desc;
                visitMethodInsn(INVOKEVIRTUAL, owner, "_get" + name, gDesc);
                return;
            } else
                if (opcode == PUTFIELD) {
                    String sDesc = "(" + desc + ")V";
                    visitMethodInsn(INVOKESPECIAL, owner, "_set" + name, sDesc);
                    return;
                }
        }
        super.visitFieldInsn(opcode, owner, name, desc);
    }
    
    /**
     * Visit a method invocation instruction.
     * After the super constructor invocation, insert the _setComponentManager invocation.
     * @param opcode : opcode
     * @param owner : method owning class
     * @param name : method name
     * @param desc : method descriptor
     * @see org.objectweb.asm.MethodAdapter#visitMethodInsn(int, java.lang.String, java.lang.String, java.lang.String)
     */
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        // A method call is detected, check if it is the super call :
        if (!m_superDetected) {
            m_superDetected = true; 
            // The first invocation is the super call
            // 1) Visit the super constructor :
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(opcode, owner, name, desc); // Super constructor invocation
            
            // 2) Load the object and the component manager argument 
            mv.visitVarInsn(ALOAD, 0);
            //mv.visitVarInsn(ALOAD, Type.getArgumentTypes(m_constructorDesc).length);
            mv.visitVarInsn(ALOAD, 1);  // CM is always the first argument
            // 3) Initialize the field 
            mv.visitMethodInsn(INVOKESPECIAL, m_owner, "_setComponentManager", "(Lorg/apache/felix/ipojo/InstanceManager;)V");
            
            // Add the entry callback call.
            methodEntry();
            
            // insertion finished   
        } else { 
            mv.visitMethodInsn(opcode, owner, name, desc); 
        }
    }
    
    /**
     * Visit an instruction implying a variable.
     * For all non-this variable, increment the variable index.
     * @param opcode : opcode
     * @param var : variable index  
     * @see org.objectweb.asm.MethodAdapter#visitVarInsn(int, int)
     */
    public void visitVarInsn(int opcode, int var) {
        if (!m_superDetected) { 
            return; // Do nothing the ALOAD 0 will be injected by visitMethodInsn
        } else { 
            if (var == 0) { 
                mv.visitVarInsn(opcode, var); // ALOAD 0 (THIS)
            } else { 
                mv.visitVarInsn(opcode, var + 1); // All other variable count 
            }     
        }
    }
    
    /**
     * Visit an increment instruction.
     * If incrementing a variable, increment the variable index.
     * @param var : variable index
     * @param increment : increment
     * @see org.objectweb.asm.MethodAdapter#visitIincInsn(int, int)
     */
    public void visitIincInsn(int var, int increment) {
        if (var != 0) { 
            mv.visitIincInsn(var + 1, increment); 
        } else { 
            mv.visitIincInsn(var, increment); // Increment the current object ???
        } 
    }
    
    /**
     * Visit a local variable.
     * Add _manager and increment variable index.
     * @param name : variable name
     * @param desc : variable descriptor
     * @param signature : variable signature
     * @param start : beginning label 
     * @param end : ending label
     * @param index :variable index
     * @see org.objectweb.asm.MethodAdapter#visitLocalVariable(java.lang.String, java.lang.String, java.lang.String, org.objectweb.asm.Label, org.objectweb.asm.Label, int)
     */
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        if (index == 0) {
            mv.visitLocalVariable(name, desc, signature, start, end, index);
            mv.visitLocalVariable("_manager", "Lorg/apache/felix/ipojo/InstanceManager;", null, start, end, 1);
        }
        mv.visitLocalVariable(name, desc, signature, start, end, index + 1);
    }
    
    /**
     * Visit max method.
     * @param maxStack : stack size.
     * @param maxLocals : local number.
     * @see org.objectweb.asm.MethodAdapter#visitMaxs(int, int)
     */
    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitMaxs(maxStack + 1, maxLocals + 2);
    }
    
    /**
     * Visit zero argument instruction.
     * This allow to catch RETURN instruction in order to insert the invocation to exit callback.
     * @param opcode : instruction code
     * @see org.objectweb.asm.MethodAdapter#visitInsn(int)
     */
    public void visitInsn(int opcode) {
        switch(opcode) {
            case RETURN:
                onMethodExit(opcode);
                break;
            default :
                break;
        }
        super.visitInsn(opcode);
    }
    
    /**
     * Method injecting call at the entry of each method.
     */
    private void methodEntry() {
        Type[] args = Type.getArgumentTypes(m_desc);
        String name = "$init";
        
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
        Type[] args = Type.getArgumentTypes(m_desc);
        String name = "$init";
       
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
        local++;
        visitInsn(ACONST_NULL);
        
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
    }


    /**
     * Do nothing.
     * @see org.objectweb.asm.commons.AdviceAdapter#onMethodEnter()
     */
    protected void onMethodEnter() { }

}



