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

import java.util.logging.Level;

import org.apache.felix.ipojo.plugin.IPojoPluginConfiguration;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class PreprocessCodeAdapter extends MethodAdapter implements Opcodes {

    /** The owner class of the field.
     * m_owner : String
     */
    private String m_owner;

    /** PropertyCodeAdapter constructor.
     * A new FiledCodeAdapter should be create for each method visit.
     * @param mv MethodVisitor
     * @param owner Name of the class
     */
    public PreprocessCodeAdapter(final MethodVisitor mv, final String owner) {
        super(mv);
        m_owner = owner;
    }


    /** Visit Method for Filed instance (GETFIELD).
     * @see org.objectweb.asm.MethodVisitor#visitFieldInsn(int, String, String, String)
     * @param opcode : visited operation code
     * @param owner : owner of the field
     * @param name : name of the field
     * @param desc : decriptor of the field
     */
    public void visitFieldInsn(
            final int opcode,
            final String owner,
            final String name,
            final String desc) {
        if (owner.equals(m_owner)) {
            if (opcode == GETFIELD) {
            		IPojoPluginConfiguration.getLogger().log(Level.INFO, "Manipulate a GETFIELD on : " + name);
                    String gDesc = "()" + desc;
                    visitMethodInsn(INVOKEVIRTUAL, owner, "_get" + name, gDesc);
                    return;
            } else
            	if (opcode == PUTFIELD) {
            		IPojoPluginConfiguration.getLogger().log(Level.INFO, "Manipulate a PUTFIELD on : " + name);
            		String sDesc = "(" + desc + ")V";
            		visitMethodInsn(INVOKESPECIAL, owner, "_set" + name, sDesc);
            		return;
            	}
        }
        super.visitFieldInsn(opcode, owner, name, desc);
    }
    
}

