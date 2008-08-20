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

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Adapts a inner class in order to allow accessing outer class fields.
 * A manipulated inner class has access to the managed field of the outer class.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class InnerClassAdapter extends ClassAdapter implements Opcodes {
    
    /**
     * Implementation class name.
     */
    private String m_outer;
    
    /**
     * List of fields of the implementation class. 
     */
    private Set m_fields;

    /**
     * Creates the inner class adapter.
     * @param arg0 parent class visitor
     * @param outerClass outer class (implementation class)
     * @param fields fields of the implementation class
     */
    public InnerClassAdapter(ClassVisitor arg0, String outerClass, Set fields) {
        super(arg0);
        m_outer = outerClass;
        m_fields = fields;
    }
    
    /**
     * Visits a method.
     * This methods create a code visitor manipulating outer class field accesses.
     * @param access method visibility
     * @param name method name
     * @param desc method descriptor
     * @param signature method signature
     * @param exceptions list of exceptions thrown by the method
     * @return a code adapter manipulating field accesses
     * @see org.objectweb.asm.ClassAdapter#visitMethod(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
     */
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        return new MethodCodeAdapter(mv, m_outer, access, name, desc, m_fields);
    }    
    

}
