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
package org.apache.felix.scrplugin.tags.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.scrplugin.JavaClassDescriptorManager;
import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.tags.JavaField;
import org.apache.felix.scrplugin.tags.JavaTag;
import org.apache.felix.scrplugin.tags.qdox.QDoxJavaClassDescription;

import com.thoughtworks.qdox.model.JavaClass;

/**
 * Reading class description based on java annotations. This extends
 * {@link QDoxJavaClassDescription} to re-use annotation-independent logic and
 * automatic generation of bind/unbind methods.
 */
public class AnnotationJavaClassDescription extends QDoxJavaClassDescription {

    /**
     * @param clazz Java class
     * @param source QDox source
     * @param manager description manager
     */
    public AnnotationJavaClassDescription(Class<?> clazz, JavaClass javaClass, JavaClassDescriptorManager manager) {
        super(clazz, javaClass, manager);
    }

    /**
     * @see JavaClassDescription#getTagByName(String)
     */
    @Override
    public JavaTag getTagByName(String name) {
        for(com.thoughtworks.qdox.model.Annotation annotation : this.javaClass.getAnnotations()) {
            List<JavaTag> tags = manager.getAnnotationTagProviderManager().getTags(annotation, this);
            for (JavaTag tag : tags) {
                if (tag.getName().equals(name)) {
                    return tag;
                }
            }
        }
        return null;
    }

    /**
     * @see JavaClassDescription#getTagsByName(String, boolean)
     */
    @Override
    public JavaTag[] getTagsByName(String name, boolean inherited) throws SCRDescriptorException {

        List<JavaTag> tags = new ArrayList<JavaTag>();
        for(com.thoughtworks.qdox.model.Annotation annotation : this.javaClass.getAnnotations()) {
            List<JavaTag> annotationTags = manager.getAnnotationTagProviderManager().getTags(annotation, this);
            for (JavaTag tag : annotationTags) {
                if (tag.getName().equals(name)) {
                    tags.add(tag);
                }
            }
        }

        if (inherited && this.getSuperClass() != null) {
            final JavaTag[] superTags = this.getSuperClass().getTagsByName(name, inherited);
            if (superTags.length > 0) {
                tags.addAll(Arrays.asList(superTags));
            }
        }

        return tags.toArray(new JavaTag[tags.size()]);
    }

    /**
     * @see JavaClassDescription#getFields()
     */
    @Override
    public JavaField[] getFields() {
        final com.thoughtworks.qdox.model.JavaField fields[] = this.javaClass.getFields();
        if ( fields == null || fields.length == 0 ) {
            return new JavaField[0];
        }
        final JavaField[] javaFields = new JavaField[fields.length];
        for (int i = 0; i < fields.length; i++) {
            javaFields[i] = new AnnotationJavaField(fields[i], this);
        }
        return javaFields;
    }

    /**
     * @see JavaClassDescription#getFieldByName(String)
     */
    @Override
    public JavaField getFieldByName(String name) throws SCRDescriptorException {
        final com.thoughtworks.qdox.model.JavaField field = this.javaClass.getFieldByName(name);
        if (field != null) {
            return new AnnotationJavaField(field, this);
        }
        if (this.getSuperClass() != null) {
            this.getSuperClass().getFieldByName(name);
        }
        return null;
    }

    protected JavaClassDescriptorManager getManager() {
        return this.manager;
    }

}
