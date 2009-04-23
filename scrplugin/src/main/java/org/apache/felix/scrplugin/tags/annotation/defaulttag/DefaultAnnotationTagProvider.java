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
package org.apache.felix.scrplugin.tags.annotation.defaulttag;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.*;
import org.apache.felix.scrplugin.tags.JavaField;
import org.apache.felix.scrplugin.tags.JavaTag;
import org.apache.felix.scrplugin.tags.annotation.AnnotationJavaClassDescription;
import org.apache.felix.scrplugin.tags.annotation.AnnotationTagProvider;

import com.thoughtworks.qdox.model.Annotation;

/**
 * Provides mapping of default SCR annotations to tag implementations.
 */
public class DefaultAnnotationTagProvider implements AnnotationTagProvider {

    /**
     * @see org.apache.felix.scrplugin.tags.annotation.AnnotationTagProvider#getTags(Annotation, org.apache.felix.scrplugin.tags.annotation.AnnotationJavaClassDescription, org.apache.felix.scrplugin.tags.JavaField)
     */
    public List<JavaTag> getTags(Annotation annotation,
                                 AnnotationJavaClassDescription description, JavaField field) {
        List<JavaTag> tags = new ArrayList<JavaTag>();

        // check for single annotations
        if (annotation.getType().getJavaClass().getFullyQualifiedName().equals(Component.class.getName())) {
            tags.add(new ComponentTag(annotation, description));
        } else if (annotation.getType().getJavaClass().getFullyQualifiedName().equals(Property.class.getName())) {
            tags.add(new PropertyTag(annotation, description, field));
        } else if (annotation.getType().getJavaClass().getFullyQualifiedName().equals(Service.class.getName())) {
            tags.add(new ServiceTag(annotation, description));
        } else if (annotation.getType().getJavaClass().getFullyQualifiedName().equals(Reference.class.getName())) {
            tags.add(new ReferenceTag(annotation, description, field));
        }

        // check for multi-annotations
        else if (annotation.getType().getJavaClass().getFullyQualifiedName().equals(Properties.class.getName())) {
            @SuppressWarnings("unchecked")
            final List<Annotation> properties = (List<Annotation>)annotation.getNamedParameter("value");
            for (Annotation property : properties) {
                tags.add(new PropertyTag(property, description, field));
            }
        } else if (annotation.getType().getJavaClass().getFullyQualifiedName().equals(Services.class.getName())) {
            @SuppressWarnings("unchecked")
            final List<Annotation> services = (List<Annotation>)annotation.getNamedParameter("value");
            for (Annotation service : services) {
                tags.add(new ServiceTag(service, description));
            }
        } else if (annotation.getType().getJavaClass().getFullyQualifiedName().equals(References.class.getName())) {
            @SuppressWarnings("unchecked")
            final List<Annotation> references = (List<Annotation>)annotation.getNamedParameter("value");
            for (Annotation reference : references) {
                tags.add(new ReferenceTag(reference, description, field));
            }
        }

        return tags;
    }

}
