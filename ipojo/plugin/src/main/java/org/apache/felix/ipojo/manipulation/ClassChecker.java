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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ClassChecker implements ClassVisitor, Opcodes {
	
	/**
	 * True if the class is already manipulated.
	 */
	private boolean isAlreadyManipulated = false;
	
    /**
     * Interfaces implemented by the component.
     */
    private String[] m_itfs = new String[0];

	/**
	 * Field hashmap [field name, type] discovered in the component class.
	 */
	private HashMap m_fields = new HashMap();
	
	/**
	 * Method List of method descriptor discovered in the component class. 
	 */
	private List m_methods = new ArrayList()/*<MethodDesciptor>*/;

	/** Check if the _cm field already exists.
	 * @see org.objectweb.asm.ClassVisitor#visitField(int, java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
	 */
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		
		if (access == ACC_PRIVATE && name.equals("_cm") && desc.equals("Lorg/apache/felix/ipojo/InstanceManager;")) {
			isAlreadyManipulated = true;
		}
		
		Type type = Type.getType(desc);
		if (type.getSort() == Type.ARRAY) {
        	if (type.getInternalName().startsWith("L")) {
        		String internalType = type.getInternalName().substring(1);
        		String nameType = internalType.replace('/', '.');
                m_fields.put(name, nameType + "[]");
        	}
        	else {
        		String nameType = type.getClassName().substring(0, type.getClassName().length() - 2);
        		m_fields.put(name, nameType + "[]");
        	}
		} else { m_fields.put(name, type.getClassName()); }
		
		return null;
	}
	
	public boolean isalreadyManipulated() { return isAlreadyManipulated; }
	
	/**
     * @see org.objectweb.asm.ClassVisitor#visit(int, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
     */
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)  {
    	//Store the interfaces :
    	m_itfs = interfaces;
    }

    /**
     * @see org.objectweb.asm.ClassVisitor#visitSource(java.lang.String, java.lang.String)
     */
    public void visitSource(String arg0, String arg1) { }

    /**
     * @see org.objectweb.asm.ClassVisitor#visitOuterClass(java.lang.String, java.lang.String, java.lang.String)
     */
    public void visitOuterClass(String arg0, String arg1, String arg2) { }

    /**
     * @see org.objectweb.asm.ClassVisitor#visitAnnotation(java.lang.String, boolean)
     */
    public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) { return null; }

    /**
     * @see org.objectweb.asm.ClassVisitor#visitAttribute(org.objectweb.asm.Attribute)
     */
    public void visitAttribute(Attribute arg0) { }

    /**
     * @see org.objectweb.asm.ClassVisitor#visitInnerClass(java.lang.String, java.lang.String, java.lang.String, int)
     */
    public void visitInnerClass(String arg0, String arg1, String arg2, int arg3) { }
    
    /**
     * @see org.objectweb.asm.ClassVisitor#visitMethod(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
     */
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) { 
    	if(!name.equals("<init>")) { m_methods.add(new MethodDescriptor(name, desc)); }
    	return null; 
    }

    /**
     * @see org.objectweb.asm.ClassVisitor#visitEnd()
     */
    public void visitEnd() { }
    
    /**
     * @return the interfaces implemented by the component class.
     */
    public String[] getInterfaces() {
        return m_itfs;
    }

	/**
	 * @return the field hashmap [field_name, type].
	 */
	public HashMap getFields() {
		return m_fields;
	}
	
	
	/**
	 * @return the method list of [method, signature].
	 */
	public List getMethods() {
		return m_methods;
	}

}
