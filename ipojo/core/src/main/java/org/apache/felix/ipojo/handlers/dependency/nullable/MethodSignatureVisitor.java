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
package org.apache.felix.ipojo.handlers.dependency.nullable;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** This class implement a class visitor. It store all method signature of
 * the visited interface. Then it returns these class signatures for the proxy
 * generation or the nullable class.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MethodSignatureVisitor implements ClassVisitor, Opcodes {

    /**
     * Array of method signature.
     */
    private MethodSignature[] m_methods;


    /**
     * Constructor.
     */
    public MethodSignatureVisitor() { }

    /**
     * Visit a method, store the information about the method.
     * @see org.objectweb.asm.ClassVisitor#visitMethod(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
     * @param access : Access modifier
     * @param name : name of the visited method
     * @param signature : singature of the visited element (null if not generic)
     * @param desc : descriptor of the method
     * @param exceptions : execption clause
     * @return always null (not code visitor needed)
     */
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        //Store method signature for each visited method
        m_methods = addMethod(m_methods, new MethodSignature(name, desc, signature, exceptions));
        return null;
    }

    /**
     * Get the method signatures list.
     * @return the method signature array.
     */
    public MethodSignature[] getMethods() {
        return m_methods;
    }

    /**
     * Return the new array of Method Signature by adding the given list and the given element.
     * @param list : the current array
     * @param method : the element to add
     * @return the new array
     */
    public static MethodSignature[] addMethod(MethodSignature[] list, MethodSignature method) {
        if (list != null) {
            MethodSignature[] newList = new MethodSignature[list.length + 1];
            System.arraycopy(list, 0, newList, 0, list.length);
            newList[list.length] = method;
            return newList;
        } else {
            list = new MethodSignature[] {method};
            return list;
        }

    }

    /**
     * Visit a method.
     * @see org.objectweb.asm.ClassVisitor#visit(int, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
     * @param arg0 : version
     * @param arg1 : access
     * @param arg2 : name
     * @param arg3 : signature
     * @param arg4 : superName
     * @param arg5 : interfaces 
     */
    public void visit(int arg0, int arg1, String arg2, String arg3, String arg4, String[] arg5) { }

    /**
     * Visit sources.
     * @see org.objectweb.asm.ClassVisitor#visitSource(java.lang.String, java.lang.String)
     * @param arg0 : source
     * @param arg1 : debug
     */
    public void visitSource(String arg0, String arg1) { }

    /**
     * Visit an outer class.
     * @see org.objectweb.asm.ClassVisitor#visitOuterClass(java.lang.String, java.lang.String, java.lang.String)
     * @param arg0 : owner
     * @param arg1 : name
     * @param arg2 : desc
     */
    public void visitOuterClass(String arg0, String arg1, String arg2) { }

    /**
     * Visit an annotation.
     * @see org.objectweb.asm.ClassVisitor#visitAnnotation(java.lang.String, boolean)
     * @param arg0 : desc
     * @param arg1 : visible
     * @return annotationvisitor
     */
    public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) { return null; }

    /**
     * Visit an attribute.
     * @see org.objectweb.asm.ClassVisitor#visitAttribute(org.objectweb.asm.Attribute)
     * @param arg0 : Attribute
     */
    public void visitAttribute(Attribute arg0) { }

    /**
     * Visit an inner class.
     * @see org.objectweb.asm.ClassVisitor#visitInnerClass(java.lang.String, java.lang.String, java.lang.String, int)
     * @param arg0 : name
     * @param arg1 : outerclass
     * @param arg2 : innerclass
     * @param arg3 : access
     */
    public void visitInnerClass(String arg0, String arg1, String arg2, int arg3) { }

    /**
     * Visit a field.
     * @see org.objectweb.asm.ClassVisitor#visitField(int, java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
     * @param arg0 : access
     * @param arg1 : name
     * @param arg2 : desc
     * @param arg3 : signature
     * @param arg4 : value
     * @return the field visitor
     */
    public FieldVisitor visitField(int arg0, String arg1, String arg2, String arg3, Object arg4) { return null; }

    /**
     * End of the visit.
     * @see org.objectweb.asm.ClassVisitor#visitEnd()
     */
    public void visitEnd() { }


}
