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
package org.apache.felix.scrplugin.tags.cl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.scrplugin.Constants;
import org.apache.felix.scrplugin.JavaClassDescriptorManager;
import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.om.Component;
import org.apache.felix.scrplugin.tags.JavaClassDescription;
import org.apache.felix.scrplugin.tags.JavaField;
import org.apache.felix.scrplugin.tags.JavaMethod;
import org.apache.felix.scrplugin.tags.JavaTag;

/**
 * <code>ClassLoaderJavaClassDescription.java</code>...
 *
 */
public class ClassLoaderJavaClassDescription implements JavaClassDescription {

    protected static final JavaTag[] EMPTY_TAGS = new JavaTag[0];

    protected final Class<?> clazz;

    protected final JavaClassDescriptorManager manager;

    protected final Component component;

    public ClassLoaderJavaClassDescription(Class<?> c, Component comp, JavaClassDescriptorManager m) {
        this.clazz = c;
        this.manager = m;
        this.component = comp;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#getFields()
     */
    public JavaField[] getFields() {
        final Field[] fields = this.clazz.getFields();
        final JavaField[] javaFields = new JavaField[fields.length];
        for (int i=0; i < fields.length; i++ ) {
            javaFields[i] = new ClassLoaderJavaField(fields[i], this);
        }
        return javaFields;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#getFieldByName(java.lang.String)
     */
    public JavaField getFieldByName(String name) throws SCRDescriptorException {
        Field field = null;
        try {
            field = this.clazz.getField(name);
        } catch (SecurityException e) {
            // ignore
        } catch (NoSuchFieldException e) {
            // ignore
        }
        if ( field != null ) {
            return new ClassLoaderJavaField(field, this);
        }
        if ( this.getSuperClass() != null ) {
            this.getSuperClass().getFieldByName(name);
        }
        return null;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#getExternalFieldByName(java.lang.String)
     */
    public JavaField getExternalFieldByName(String name)
    throws SCRDescriptorException {
        throw new SCRDescriptorException("getExternalFieldByName not supported for this class.", getName(), 0);
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#getReferencedClass(java.lang.String)
     */
    public JavaClassDescription getReferencedClass(String referencedName)
    throws SCRDescriptorException {
        throw new SCRDescriptorException("getReferencedClass not supported for this class.", getName(), 0);
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#getImplementedInterfaces()
     */
    public JavaClassDescription[] getImplementedInterfaces() throws SCRDescriptorException {
        Class<?>[] implemented = clazz.getInterfaces();
        if (implemented.length == 0) {
            return JavaClassDescription.EMPTY_RESULT;
        }

        JavaClassDescription[] jcd = new JavaClassDescription[implemented.length];
        for (int i=0; i < jcd.length; i++) {
            jcd[i] = manager.getJavaClassDescription(implemented[i].getName());
        }
        return jcd;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#getMethodBySignature(java.lang.String, java.lang.String[])
     */
    public JavaMethod getMethodBySignature(String name, String[] parameters)
    throws SCRDescriptorException {
        Class<?>[] classParameters = null;
        if ( parameters != null ) {
            classParameters = new Class[parameters.length];
            for(int i=0; i<parameters.length; i++) {
                try {
                    classParameters[i] = this.manager.getClassLoader().loadClass(parameters[i]);
                } catch (ClassNotFoundException cnfe) {
                    return null;
                }
            }
        }
        Method m = null;
        try {
            m = this.clazz.getDeclaredMethod(name, classParameters);
        } catch (NoClassDefFoundError ncdfe) {
            // if this occurs it usually means that a problem with the maven
            // scopes exists.
            throw new SCRDescriptorException("Class loading error. This error usually occurs if you have a " +
                    "service inheriting from a class coming from another bundle and that class using a " +
                    "third library and all dependencies are specified with scope 'provided'.", getName(), 0, ncdfe);
        } catch (NoSuchMethodException e) {
            // ignore this
        }
        if ( m != null ) {
            return new ClassLoaderJavaMethod(m);
        }
        // try super class
        if ( this.getSuperClass() != null ) {
            return this.getSuperClass().getMethodBySignature(name, parameters);
        }
        return null;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#getMethods()
     */
    public JavaMethod[] getMethods() {
        return JavaMethod.EMPTY_RESULT;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#getName()
     */
    public String getName() {
        return this.clazz.getName();
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#getSuperClass()
     */
    public JavaClassDescription getSuperClass() throws SCRDescriptorException {
        if ( this.clazz.getSuperclass() != null ) {
            return this.manager.getJavaClassDescription(this.clazz.getSuperclass().getName());
        }
        return null;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#getTagByName(java.lang.String)
     */
    public JavaTag getTagByName(String name) {
        // this is only used to retrieve the component tag, so we can ignore it
        // for classes from other bundles and simply return null
        return null;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#getTagsByName(java.lang.String, boolean)
     */
    public JavaTag[] getTagsByName(String name, boolean inherited)
    throws SCRDescriptorException {
        JavaTag[] javaTags = EMPTY_TAGS;
        if ( this.component != null ) {
            if ( Constants.SERVICE.equals(name) ) {
                if ( this.component.getService() != null &&
                     this.component.getService().getInterfaces().size() > 0 ) {
                    javaTags = new JavaTag[this.component.getService().getInterfaces().size()];
                    for(int i=0; i<this.component.getService().getInterfaces().size(); i++) {
                        javaTags[i] = new ClassLoaderJavaTag(this, this.component.getService().getInterfaces().get(i),
                                                             this.component.getService().isServicefactory());
                    }
                }
            } else if ( Constants.PROPERTY.equals(name) ) {
                if ( this.component.getProperties().size() > 0 ) {
                    javaTags = new JavaTag[this.component.getProperties().size()];
                    for(int i=0; i<this.component.getProperties().size(); i++) {
                        javaTags[i] = new ClassLoaderJavaTag(this, this.component.getProperties().get(i));
                    }
                }
            } else if ( Constants.REFERENCE.equals(name) ) {
                if ( this.component.getReferences().size() > 0 ) {
                    javaTags = new JavaTag[this.component.getReferences().size()];
                    for(int i=0; i<this.component.getReferences().size(); i++) {
                        javaTags[i] = new ClassLoaderJavaTag(this, this.component.getReferences().get(i));
                    }
                }
            }
        }
        if ( inherited && this.getSuperClass() != null ) {
            final JavaTag[] superTags = this.getSuperClass().getTagsByName(name, inherited);
            if ( superTags.length > 0 ) {
                final List<JavaTag> list = new ArrayList<JavaTag>(Arrays.asList(javaTags));
                list.addAll(Arrays.asList(superTags));
                javaTags = list.toArray(new JavaTag[list.size()]);
            }
        }
        return javaTags;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#isA(java.lang.String)
     */
    public boolean isA(String type) {
        if ( this.clazz.getName().equals(type) ) {
            return true;
        }
        return this.testClass(this.clazz, type);
    }

    protected boolean testClass(Class<?> c, String type) {
        final Class<?>[] interfaces = c.getInterfaces();
        for(int i=0; i<interfaces.length; i++) {
            if ( interfaces[i].getName().equals(type) ) {
                return true;
            }
            if ( this.testClass(interfaces[i], type) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#isAbstract()
     */
    public boolean isAbstract() {
        return Modifier.isAbstract(this.clazz.getModifiers());
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#isInterface()
     */
    public boolean isInterface() {
        return Modifier.isInterface(this.clazz.getModifiers());
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaClassDescription#isPublic()
     */
    public boolean isPublic() {
        return Modifier.isPublic(this.clazz.getModifiers());
    }

    public String toString() {
        return getName();
    }
}
