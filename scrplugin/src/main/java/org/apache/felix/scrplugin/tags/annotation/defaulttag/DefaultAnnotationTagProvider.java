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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.*;
import org.apache.felix.scrplugin.tags.JavaField;
import org.apache.felix.scrplugin.tags.JavaTag;
import org.apache.felix.scrplugin.tags.annotation.AnnotationJavaClassDescription;
import org.apache.felix.scrplugin.tags.annotation.AnnotationTagProvider;

/**
 * Provides mapping of default SCR annotations to tag implementations.
 */
public class DefaultAnnotationTagProvider implements AnnotationTagProvider {

    /**
     * @see AnnotationTagProvider#getTags(Annotation,
     *      AnnotationJavaClassDescription, JavaField)
     */
    public List<JavaTag> getTags(Annotation annotation, AnnotationJavaClassDescription description, JavaField field) {
        List<JavaTag> tags = new ArrayList<JavaTag>();

        // check for single annotations
        if (annotation instanceof Component) {
            tags.add(new ComponentTag((Component) annotation, description));
        } else if (annotation instanceof Property) {
            tags.add(new PropertyTag((Property) annotation, description));
        } else if (annotation instanceof Service) {
            tags.add(new ServiceTag((Service) annotation, description));
        } else if (annotation instanceof Reference) {
            tags.add(new ReferenceTag((Reference) annotation, description, field));
        }

        // check for multi-annotations
        else if (annotation instanceof Properties) {
            for (Property property : ((Properties) annotation).value()) {
                tags.add(new PropertyTag(property, description));
            }
        } else if (annotation instanceof Services) {
            for (Service service : ((Services) annotation).value()) {
                tags.add(new ServiceTag(service, description));
            }
        } else if (annotation instanceof References) {
            for (Reference reference : ((References) annotation).value()) {
                tags.add(new ReferenceTag(reference, description, field));
            }
        }

        return tags;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.annotation.AnnotationTagProvider#getTags(java.lang.annotation.Annotation, org.apache.felix.scrplugin.tags.annotation.AnnotationJavaClassDescription, org.apache.felix.scrplugin.tags.JavaField)
     */
    public List<JavaTag> getTags(com.thoughtworks.qdox.model.Annotation annotation,
                                 AnnotationJavaClassDescription description, JavaField field) {
        List<JavaTag> tags = new ArrayList<JavaTag>();

        // check for single annotations
        if (annotation.getType().getJavaClass().getFullyQualifiedName().equals(Component.class.getName())) {
            tags.add(new ComponentTag(annotation, description));
        } else if (annotation.getType().getJavaClass().getFullyQualifiedName().equals(Property.class.getName())) {
            tags.add(new PropertyTag(annotation, description));
        } else if (annotation.getType().getJavaClass().getFullyQualifiedName().equals(Service.class.getName())) {
            tags.add(new ServiceTag(annotation, description));
        } else if (annotation.getType().getJavaClass().getFullyQualifiedName().equals(Reference.class.getName())) {
            tags.add(new ReferenceTag(annotation, description, field));
        }

        // check for multi-annotations
        else if (annotation.getType().getJavaClass().getFullyQualifiedName().equals(Properties.class.getName())) {
            final com.thoughtworks.qdox.model.Annotation[] properties = (com.thoughtworks.qdox.model.Annotation[])annotation.getNamedParameter("value");
            for (com.thoughtworks.qdox.model.Annotation property : properties) {
                tags.add(new PropertyTag(property, description));
            }
        } else if (annotation.getType().getJavaClass().getFullyQualifiedName().equals(Services.class.getName())) {
            final com.thoughtworks.qdox.model.Annotation[] services = (com.thoughtworks.qdox.model.Annotation[])annotation.getNamedParameter("value");
            for (com.thoughtworks.qdox.model.Annotation service : services) {
                tags.add(new ServiceTag(service, description));
            }
        } else if (annotation.getType().getJavaClass().getFullyQualifiedName().equals(References.class.getName())) {
            final com.thoughtworks.qdox.model.Annotation[] references = (com.thoughtworks.qdox.model.Annotation[])annotation.getNamedParameter("value");
            for (com.thoughtworks.qdox.model.Annotation reference : references) {
                tags.add(new ReferenceTag(reference, description, field));
            }
        }

        return tags;
    }

}
