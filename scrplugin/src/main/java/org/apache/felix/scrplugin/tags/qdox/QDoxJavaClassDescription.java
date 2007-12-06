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
package org.apache.felix.scrplugin.tags.qdox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.scrplugin.tags.JavaClassDescription;
import org.apache.felix.scrplugin.tags.JavaClassDescriptorManager;
import org.apache.felix.scrplugin.tags.JavaField;
import org.apache.felix.scrplugin.tags.JavaMethod;
import org.apache.felix.scrplugin.tags.JavaTag;
import org.apache.felix.scrplugin.tags.ModifiableJavaClassDescription;
import org.apache.maven.plugin.MojoExecutionException;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaParameter;
import com.thoughtworks.qdox.model.JavaSource;
import com.thoughtworks.qdox.model.Type;

/**
 * <code>QDoxJavaClassDescription.java</code>...
 *
 */
public class QDoxJavaClassDescription
    implements JavaClassDescription, ModifiableJavaClassDescription {

    protected final JavaClass javaClass;

    protected final JavaClassDescriptorManager manager;

    protected final JavaSource source;

    public QDoxJavaClassDescription(JavaSource source, JavaClassDescriptorManager m) {
        this.javaClass = source.getClasses()[0];
        this.manager = m;
        this.source = source;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#getSuperClass()
     */
    public JavaClassDescription getSuperClass() throws MojoExecutionException {
        final JavaClass parent = this.javaClass.getSuperJavaClass();
        if ( parent != null ) {
            return this.manager.getJavaClassDescription(parent.getFullyQualifiedName());
        }
        return null;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#getTagByName(java.lang.String)
     */
    public JavaTag getTagByName(String name) {
        final DocletTag tag = this.javaClass.getTagByName(name);
        if ( tag == null ) {
            return null;
        }
        return new QDoxJavaTag(tag, this);
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#getName()
     */
    public String getName() {
        return this.javaClass.getFullyQualifiedName();
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#getTagsByName(java.lang.String, boolean)
     */
    public JavaTag[] getTagsByName(String name, boolean inherited)
    throws MojoExecutionException {
        final DocletTag[] tags = this.javaClass.getTagsByName(name, false);
        JavaTag[] javaTags;
        if ( tags == null || tags.length == 0 ) {
            javaTags = new JavaTag[0];
        } else {
            javaTags = new JavaTag[tags.length];
            for(int i=0; i<tags.length;i++) {
                javaTags[i] = new QDoxJavaTag(tags[i], this);
            }
        }
        if ( inherited && this.getSuperClass() != null ) {
            final JavaTag[] superTags = this.getSuperClass().getTagsByName(name, inherited);
            if ( superTags.length > 0 ) {
                final List list = new ArrayList(Arrays.asList(javaTags));
                list.addAll(Arrays.asList(superTags));
                javaTags = (JavaTag[]) list.toArray(new JavaTag[list.size()]);
            }
        }
        return javaTags;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#getFields()
     */
    public JavaField[] getFields() {
        final com.thoughtworks.qdox.model.JavaField fields[] = this.javaClass.getFields();
        if ( fields == null || fields.length == 0 ) {
            return new JavaField[0];
        }
        final JavaField[] f = new JavaField[fields.length];
        for(int i=0; i<fields.length; i++) {
            f[i] = new QDoxJavaField(fields[i], this);
        }
        return f;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#getImplementedInterfaces()
     */
    public JavaClassDescription[] getImplementedInterfaces()
    throws MojoExecutionException {
        final JavaClass[] interfaces = this.javaClass.getImplementedInterfaces();
        if ( interfaces == null || interfaces.length == 0 ) {
            return new JavaClassDescription[0];
        }
        final JavaClassDescription[] descs = new JavaClassDescription[interfaces.length];
        for(int i=0;i<interfaces.length; i++) {
            descs[i] = this.manager.getJavaClassDescription(interfaces[i].getFullyQualifiedName());
        }
        return descs;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#getMethodBySignature(java.lang.String, java.lang.String[])
     */
    public JavaMethod getMethodBySignature(String name, String[] parameters)
    throws MojoExecutionException {
        Type[] types = null;
        if ( parameters == null || parameters.length == 0 ) {
            types = new Type[0];
        } else {
            types = new Type[parameters.length];
            for(int i=0;i<parameters.length;i++) {
                types[i] = new Type(parameters[i]);
            }
        }
        final com.thoughtworks.qdox.model.JavaMethod m = this.javaClass.getMethodBySignature(name, types);
        if ( m == null ) {
            if ( this.getSuperClass() != null ) {
                return this.getSuperClass().getMethodBySignature(name, parameters);
            }
            return null;
        }
        return new QDoxJavaMethod(m);
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#getMethods()
     */
    public JavaMethod[] getMethods() {
        final com.thoughtworks.qdox.model.JavaMethod[] methods = this.javaClass.getMethods();
        if ( methods == null || methods.length == 0) {
            return new JavaMethod[0];
        }
        final JavaMethod[] m = new JavaMethod[methods.length];
        for(int i=0;i<methods.length;i++) {
            m[i] = new QDoxJavaMethod(methods[i]);
        }
        return m;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#isA(java.lang.String)
     */
    public boolean isA(String type) throws MojoExecutionException {
        final Type qType = new Type(type);
        if ( this.javaClass.isA(type) ) {
            return true;
        }
        final Type[] interfaces = this.javaClass.getImplements();
        if ( interfaces != null ) {
            for(int i=0; i<interfaces.length; i++) {
                if ( interfaces[i].isA(qType) ) {
                    return true;
                }
            }
        }
        if ( this.getSuperClass() != null ) {
            return this.getSuperClass().isA(type);
        }
        return false;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#isAbstract()
     */
    public boolean isAbstract() {
        return this.javaClass.isAbstract();
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#isInterface()
     */
    public boolean isInterface() {
        return this.javaClass.isInterface();
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#isPublic()
     */
    public boolean isPublic() {
        return this.javaClass.isPublic();
    }

    /**
     * @see org.apache.felix.scrplugin.tags.ModifiableJavaClassDescription#addMethods(java.lang.String, java.lang.String, boolean, boolean)
     */
    public void addMethods(final String propertyName,
                           final String className,
                           final boolean createBind,
                           final boolean createUnbind)
    throws MojoExecutionException {
        // now do byte code manipulation
        final String targetDirectory = this.manager.getProject().getBuild().getOutputDirectory();
        final String fileName = targetDirectory + File.separatorChar +  this.getName().replace('.', File.separatorChar) + ".class";
        final ClassNode cn = new ClassNode();
        try {
            final ClassReader reader = new ClassReader(new FileInputStream(fileName));
            reader.accept(cn, 0);

            final ClassWriter writer = new ClassWriter(0);

            // remove existing implementation von previous builds
            final ClassAdapter adapter = new ClassAdapter(writer) {

                protected final String bindMethodName = "bind" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
                protected final String unbindMethodName = "unbind" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
                protected final String description = "(L" + className.replace('.', '/') + ";)V";

                /**
                 * @see org.objectweb.asm.ClassAdapter#visitMethod(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
                 */
                public MethodVisitor visitMethod(int access,
                                                 String name,
                                                 String desc,
                                                 String signature,
                                                 String[] exceptions) {
                    if ( createBind && name.equals(bindMethodName) && description.equals(desc) ) {
                        return null;
                    }
                    if ( createUnbind && name.equals(unbindMethodName)  && description.equals(desc) ) {
                        return null;
                    }
                    return super.visitMethod(access, name, desc, signature, exceptions);
                }

            };

            cn.accept(adapter);
            if ( createBind ) {
                this.createMethod(writer, propertyName, className, true);
            }
            if ( createUnbind ) {
                this.createMethod(writer, propertyName, className, false);
            }

            final FileOutputStream fos = new FileOutputStream(fileName);
            fos.write(writer.toByteArray());
            fos.close();
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to add methods to " + this.getName(), e);
        }
    }

    protected void createMethod(ClassWriter cw, String propertyName, String typeName, boolean bind) {
        final org.objectweb.asm.Type type = org.objectweb.asm.Type.getType("L" + typeName.replace('.', '/') + ";");
        final String methodName = (bind ? "" : "un") + "bind" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PROTECTED, methodName, "(" + type.toString() + ")V", null, null);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        if ( bind ) {
            mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), 1);
            mv.visitFieldInsn(Opcodes.PUTFIELD, this.getName().replace('.', '/'), propertyName, type.toString());
        } else {
            mv.visitFieldInsn(Opcodes.GETFIELD, this.getName().replace('.', '/'), propertyName, type.toString());
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            final Label jmpLabel = new Label();
            mv.visitJumpInsn(Opcodes.IF_ACMPNE, jmpLabel);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitFieldInsn(Opcodes.PUTFIELD, this.getName().replace('.', '/'), propertyName, type.toString());
            mv.visitLabel(jmpLabel);
        }
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 2);
        // add to qdox
        final JavaParameter param = new JavaParameter(new Type(typeName), "param");
        final JavaParameter[] params = new JavaParameter[] {param};
        final com.thoughtworks.qdox.model.JavaMethod meth = new com.thoughtworks.qdox.model.JavaMethod();
        meth.setName(methodName);
        meth.setParameters(params);
        meth.setModifiers(new String[] {"protected"});
        this.javaClass.addMethod(meth);
    }
}
