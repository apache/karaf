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
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Check that a POJO is already manipulated or not.
 * Moreover it allows to get manipulation data about this class. 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ClassChecker implements ClassVisitor, Opcodes {

    /**
     * True if the class is already manipulated.
     */
    private boolean m_isAlreadyManipulated = false;

    /**
     * Interfaces implemented by the component.
     */
    private String[] m_itfs = new String[0];

    /**
     * Field map [field name, type] discovered in the component class.
     */
    private Map m_fields = new HashMap();

    /**
     * Method List of method descriptor discovered in the component class.
     */
    private List m_methods = new ArrayList()/* <MethodDesciptor> */;

    /**
     * Check if the _cm field already exists.
     * Update the field list.
     * @param access : access of the field
     * @param name : name of the field
     * @param desc : description of the field
     * @param signature : signature of the field
     * @param value : value of the field (for static field only)
     * @return the field visitor
     * @see org.objectweb.asm.ClassVisitor#visitField(int, java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
     */
    public FieldVisitor visitField(int access, String name, String desc,
            String signature, Object value) {

        if (access == ACC_PRIVATE && name.equals("_cm")
                && desc.equals("Lorg/apache/felix/ipojo/InstanceManager;")) {
            m_isAlreadyManipulated = true;
        }

        Type type = Type.getType(desc);
        if (type.getSort() == Type.ARRAY) {
            if (type.getInternalName().startsWith("L")) {
                String internalType = type.getInternalName().substring(1);
                String nameType = internalType.replace('/', '.');
                m_fields.put(name, nameType + "[]");
            } else {
                String nameType = type.getClassName().substring(0,
                        type.getClassName().length() - 2);
                m_fields.put(name, nameType + "[]");
            }
        } else {
            m_fields.put(name, type.getClassName());
        }

        return null;
    }

    /**
     * Check if the class was already manipulated.
     * @return true if the class is already manipulated.
     */
    public boolean isalreadyManipulated() {
        return m_isAlreadyManipulated;
    }

    /**
     * Visit the class.
     * Update the implemented interface list.
     * @param version : version of the class
     * @param access : access of the class
     * @param name : name of the class
     * @param signature : signature of the class
     * @param superName : super class of the class
     * @param interfaces : implemented interfaces.
     * @see org.objectweb.asm.ClassVisitor#visit(int, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
     */
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        // Store the interfaces :
        m_itfs = interfaces;
    }

    /**
     * Visit sources.
     * Do nothing
     * @param source : the name of the source file from which the class was compiled. May be null.
       @param debug : additional debug information to compute the relationship between source and compiled elements of the class. May be null.
     * @see org.objectweb.asm.ClassVisitor#visitSource(java.lang.String, java.lang.String)
     */
    public void visitSource(String source, String debug) { }

    /**
     * Visit an outer class.
     * @param owner - internal name of the enclosing class of the class.
     * @param name - the name of the method that contains the class, or null if the class is not enclosed in a method of its enclosing class.
     * @param desc - the descriptor of the method that contains the class, or null if the class is not enclosed in a method of its enclosing class.
     * @see org.objectweb.asm.ClassVisitor#visitOuterClass(java.lang.String, java.lang.String, java.lang.String)
     */
    public void visitOuterClass(String owner, String name, String desc) {
    }

    /**
     * Visit an annotation.
     * Do nothing.
     * @param desc - the class descriptor of the annotation class.
     * @param visible - true if the annotation is visible at runtime.
     * @return null.
     * @see org.objectweb.asm.ClassVisitor#visitAnnotation(java.lang.String, boolean)
     */
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return null;
    }

    /**
     * Visit a non standard attribute of the class.
     * @param attr - an attribute.
     * @see org.objectweb.asm.ClassVisitor#visitAttribute(org.objectweb.asm.Attribute)
     */
    public void visitAttribute(Attribute attr) {
    }

    /**
     * Visit an inner class.
     * @param name - the internal name of an inner class (see getInternalName).
     * @param outerName - the internal name of the class to which the inner class belongs (see getInternalName). May be null for not member classes.
     * @param innerName - the (simple) name of the inner class inside its enclosing class. May be null for anonymous inner classes.
     * @param access - the access flags of the inner class as originally declared in the enclosing class.
     * @see org.objectweb.asm.ClassVisitor#visitInnerClass(java.lang.String,
     *      java.lang.String, java.lang.String, int)
     */
    public void visitInnerClass(String name, String outerName, String innerName, int access) { }

    /**
     * Visit a method.
     * Update the method list (except if it init or clinit.
     * @param  access - the method's access flags (see Opcodes). This parameter also indicates if the method is synthetic and/or deprecated.
     * @param name - the method's name.
     * @param desc - the method's descriptor (see Type).
     * @param signature - the method's signature. May be null if the method parameters, return type and exceptions do not use generic types.
     * @param exceptions - the internal names of the method's exception classes (see getInternalName). May be null.
     * @return nothing.
     * @see org.objectweb.asm.ClassVisitor#visitMethod(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
     */
    public MethodVisitor visitMethod(int access, String name, String desc,
            String signature, String[] exceptions) {
        if (!name.equals("<init>") && !name.equals("<clinit>")) {
            m_methods.add(new MethodDescriptor(name, desc));
        }
        return null;
    }

    /**
     * End of the class visit.
     * @see org.objectweb.asm.ClassVisitor#visitEnd()
     */
    public void visitEnd() { }

    /**
     * Get collected interfaces.
     * @return the interfaces implemented by the component class.
     */
    public String[] getInterfaces() {
        return m_itfs;
    }

    /**
     * Get collected fields.
     * @return the field map [field_name, type].
     */
    public Map getFields() {
        return m_fields;
    }

    /**
     * Get collected methods.
     * @return the method list of [method, signature].
     */
    public List getMethods() {
        return m_methods;
    }

}
