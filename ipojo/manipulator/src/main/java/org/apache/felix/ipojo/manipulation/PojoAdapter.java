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
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Manipulate a POJO class.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class PojoAdapter extends ClassAdapter implements Opcodes {

    /**
     * POJO interface.
     */
    private final String m_pojo = "org/apache/felix/ipojo/Pojo";

    /**
     * The owner. m_owner : String
     */
    private String m_owner;

    /**
     * Field list.
     */
    private List m_fields = new ArrayList();

    /**
     * Method list.
     */
    private List m_methods = new ArrayList();

    /**
     * Getter/Setter methods creator.
     */
    private FieldAdapter m_getterSetterCreator = new FieldAdapter(cv);

    /**
     * Constructor.
     * @param arg0 : class adapter on which delegate.
     */
    public PojoAdapter(ClassVisitor arg0) {
        super(arg0);
    }

    /**
     * Start visiting a class.
     * Initialize the getter/setter generator, add the _cm field, add the pojo interface.
     * @param version : class version
     * @param access : class access
     * @param name : class name 
     * @param signature : class signature
     * @param superName : class super class
     * @param interfaces : implemented interfaces
     * @see org.objectweb.asm.ClassAdapter#visit(int, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
     */
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        m_owner = name;
        m_getterSetterCreator.visit(version, access, name, signature, superName, interfaces);
        addCMField();
        addPOJOInterface(version, access, name, signature, superName, interfaces);
    }

    /**
     * Visit an annotation.
     * @param desc : annotation descriptor.
     * @param visible : is the annotation visible at runtime.
     * @return the annotation visitor.
     * @see org.objectweb.asm.ClassAdapter#visitAnnotation(java.lang.String, boolean)
     */
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return super.visitAnnotation(desc, visible);
    }

    /**
     * Visit an attribute.
     * @param attr : visited attribute
     * @see org.objectweb.asm.ClassAdapter#visitAttribute(org.objectweb.asm.Attribute)
     */
    public void visitAttribute(Attribute attr) {
        super.visitAttribute(attr);
    }

    /**
     * Visit end.
     * Create helper methods.
     * @see org.objectweb.asm.ClassAdapter#visitEnd()
     */
    public void visitEnd() {
        // Create the _cmSetter(ComponentManager cm) method
        createComponentManagerSetter();

        // Add the getComponentInstance
        createGetComponentInstanceMethod();

        m_methods.clear();
        m_fields.clear();

        cv.visitEnd();
    }

    /**
     * Visit a field.
     * Call the adapter generating getter and setter methods.
     * @param access : field access.
     * @param name : field name
     * @param desc : field descriptor
     * @param signature : field signature
     * @param value : field value (static field only)
     * @return the field visitor.
     * @see org.objectweb.asm.ClassAdapter#visitField(int, java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
     */
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        // Add the field to the list.
        if ((access & ACC_STATIC) == 0) {
            m_fields.add(name);
            addFlagField(name);
            m_getterSetterCreator.visitField(access, name, desc, signature, value);
        }

        return super.visitField(access, name, desc, signature, value);
    }

    /**
     * Visit an inner class.
     * @param name : class name
     * @param outerName : outer name
     * @param innerName : inner name
     * @param access : class access
     * @see org.objectweb.asm.ClassAdapter#visitInnerClass(java.lang.String, java.lang.String, java.lang.String, int)
     */
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        super.visitInnerClass(name, outerName, innerName, access);
    }

    /**
     * Visit a method.
     * Manipulate constructor and methods. Does nothing with clinit and class$
     * @param access : method access
     * @param name : method name
     * @param desc : method descriptor
     * @param signature : method signature
     * @param exceptions : method exceptions
     * @return the Method Visitor.
     * @see org.objectweb.asm.ClassAdapter#visitMethod(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
     */
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        // Avoid manipulating special method
		if (name.equals("<clinit>") || name.equals("class$")) {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
		// The constructor is manipulated separatly
        if (name.equals("<init>")) {
            // 1) change the constructor descriptor (add a component manager arg as first argument)
            String newDesc = desc.substring(1);
            newDesc = "(Lorg/apache/felix/ipojo/InstanceManager;" + newDesc;

            // Insert the new constructor
            MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "<init>", newDesc, null, null);

            if (mv == null) {
                return null;
            } else {
                //return new ConstructorCodeAdapter(mv, access, desc, m_owner);
                return new ConstructorCodeAdapter(mv, m_owner);
            }
        } else { // "Normal methods"

			// avoid manipulating static methods.
		  	if ((access & ACC_STATIC) == ACC_STATIC) {
		  			return super.visitMethod(access, name, desc, signature, exceptions);
            }
			
            Type[] args = Type.getArgumentTypes(desc);
            String id = name;
            for (int i = 0; i < args.length; i++) {
                String cn = args[i].getClassName();
                if (cn.endsWith("[]")) {
                    cn = cn.replace('[', '$');
                    cn = cn.substring(0, cn.length() - 1);
                }
                cn = cn.replace('.', '_');
                id += cn;
            }

            String flag = "_M" + id;
            m_methods.add(id);

            FieldVisitor flagField = cv.visitField(Opcodes.ACC_PRIVATE, flag, "Z", null, null);
            if (flagField != null) {
                flagField.visitEnd();
            }

            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            return new MethodCodeAdapter(mv, m_owner, access, name, desc);
        }
    }

    /**
     * Visit an outer class.
     * @param owner : class owner
     * @param name : class name
     * @param desc : class descriptor.
     * @see org.objectweb.asm.ClassAdapter#visitOuterClass(java.lang.String, java.lang.String, java.lang.String)
     */
    public void visitOuterClass(String owner, String name, String desc) {
        super.visitOuterClass(owner, name, desc);
    }

    /**
     * Visit source.
     * @param source : source
     * @param debug : debug
     * @see org.objectweb.asm.ClassAdapter#visitSource(java.lang.String, java.lang.String)
     */
    public void visitSource(String source, String debug) {
        super.visitSource(source, debug);
    }

    /**
     * Add the instance manager field (_cm).
     */
    private void addCMField() {
        // Insert _cm field
        FieldVisitor fv = super.visitField(ACC_PRIVATE, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;", null, null);
        fv.visitEnd();
    }

    /**
     * Add the POJO interface to the visited class.
     * @param version : class version
     * @param access : class access
     * @param name : class name
     * @param signature : class signature
     * @param superName : super class
     * @param interfaces : implemented interfaces.
     */
    private void addPOJOInterface(int version, int access, String name, String signature, String superName, String[] interfaces) {

        // Add the POJO interface to the interface list
        // Check that the POJO interface is not already in the list
        boolean found = false;
        for (int i = 0; i < interfaces.length; i++) {
            if (interfaces[i].equals(m_pojo)) {
                found = true;
            }
        }
        String[] itfs;
        if (!found) {
            itfs = new String[interfaces.length + 1];
            for (int i = 0; i < interfaces.length; i++) {
                itfs[i] = interfaces[i];
            }
            itfs[interfaces.length] = m_pojo;
        } else {
            itfs = interfaces;
        }

        String str = "";
        for (int i = 0; i < itfs.length; i++) {
            str += itfs[i] + " ";
        }

        cv.visit(version, access, name, signature, superName, itfs);
    }

    /**
     * Create the setter method for the _cm field.
     */
    private void createComponentManagerSetter() {
        MethodVisitor mv = cv.visitMethod(ACC_PRIVATE, "_setComponentManager", "(Lorg/apache/felix/ipojo/InstanceManager;)V", null, null);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", "getRegistredFields", "()Ljava/util/Set;");
        mv.visitVarInsn(ASTORE, 2);

        for (int i = 0; i < m_fields.size(); i++) {
            String field = (String) m_fields.get(i);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitLdcInsn(field);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "contains", "(Ljava/lang/Object;)Z");
            Label l3 = new Label();
            mv.visitJumpInsn(IFEQ, l3);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ICONST_1);
            mv.visitFieldInsn(PUTFIELD, m_owner, "_F" + field, "Z");
            mv.visitLabel(l3);
        }

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", "getRegistredMethods", "()Ljava/util/Set;");
        mv.visitVarInsn(ASTORE, 2);

        for (int i = 0; i < m_methods.size(); i++) {
            String methodId = (String) m_methods.get(i);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitLdcInsn(methodId);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "contains", "(Ljava/lang/Object;)Z");
            Label l3 = new Label();
            mv.visitJumpInsn(IFEQ, l3);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ICONST_1);
            mv.visitFieldInsn(PUTFIELD, m_owner, "_M" + methodId, "Z");
            mv.visitLabel(l3);
        }

        mv.visitInsn(RETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Create the getComponentInstance method.
     */
    private void createGetComponentInstanceMethod() {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "getComponentInstance", "()Lorg/apache/felix/ipojo/ComponentInstance;", null, null);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, "_cm", "Lorg/apache/felix/ipojo/InstanceManager;");
        mv.visitInsn(ARETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Add the flag field.
     * @param name : name of the field
     */
    private void addFlagField(String name) {
        // Add the flag field
        FieldVisitor flag = cv.visitField(Opcodes.ACC_PRIVATE, "_F" + name, "Z", null, null);
        if (flag != null) {
            flag.visitEnd();
        }
    }

}
