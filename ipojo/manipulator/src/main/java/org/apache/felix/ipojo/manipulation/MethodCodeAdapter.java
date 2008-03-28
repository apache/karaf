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

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;

/**
 * Insert code calling callbacks at the entry and before the exit of a method.
 * Moreover it replaces all GETFIELD and SETFIELD by getter and setter invocation.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MethodCodeAdapter extends GeneratorAdapter implements Opcodes {

    /**
     * The owner class of the field. m_owner : String
     */
    private String m_owner;
    
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
                visitMethodInsn(INVOKESPECIAL, owner, "__get" + name, gDesc);
                return;
            } else if (opcode == PUTFIELD) {
                String sDesc = "(" + desc + ")V";
                visitMethodInsn(INVOKESPECIAL, owner, "__set" + name, sDesc);
                return;
            }
        }
        super.visitFieldInsn(opcode, owner, name, desc);
    }

}
