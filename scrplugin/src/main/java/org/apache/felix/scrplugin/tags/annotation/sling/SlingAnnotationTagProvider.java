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
package org.apache.felix.scrplugin.tags.annotation.sling;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.felix.scrplugin.tags.JavaField;
import org.apache.felix.scrplugin.tags.JavaTag;
import org.apache.felix.scrplugin.tags.annotation.AnnotationJavaClassDescription;
import org.apache.felix.scrplugin.tags.annotation.AnnotationTagProvider;
import org.apache.felix.scrplugin.tags.annotation.defaulttag.Util;

/**
 * Annotation tag provider for sling-specific SCR annotations.
 */
public class SlingAnnotationTagProvider implements AnnotationTagProvider {

    public List<JavaTag> getTags(com.thoughtworks.qdox.model.Annotation annotation,
            AnnotationJavaClassDescription description, JavaField field) {
        List<JavaTag> tags = new ArrayList<JavaTag>();

        if (annotation.getType().getJavaClass().getFullyQualifiedName().equals(SlingServlet.class.getName())) {

            // generate @Component tag if required
            boolean generateComponent = Util.getBooleanValue(annotation, "generateComponent", SlingServlet.class);
            if (generateComponent) {
                tags.add(new SlingServletComponentTag(annotation, description));
            }

            // generate @Service tag if required
            boolean generateService = Util.getBooleanValue(annotation, "generateService", SlingServlet.class);
            if (generateService) {
                tags.add(new SlingServletServiceTag(annotation, description));
            }

            // generate @Property tags
            // {@see org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_PATHS}
            String[] paths = Util.getStringValues(annotation, description, "paths");
            if (paths != null && paths.length != 0) {
                tags.add(new SlingServletPropertyTag(annotation, "sling.servlet.paths", paths, description));
            }

            // {@see org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES}
            String[] resourceTypes = Util.getStringValues(annotation, description, "resourceTypes");
            if (resourceTypes != null && resourceTypes.length != 0) {
                tags.add(new SlingServletPropertyTag(annotation, "sling.servlet.resourceTypes", resourceTypes, description));
            }

            // {@see org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_SELECTORS}
            String[] selectors = Util.getStringValues(annotation, description, "selectors");
            if (selectors != null && selectors.length != 0) {
                tags.add(new SlingServletPropertyTag(annotation, "sling.servlet.selectors", selectors, description));
            }

            // {@see org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_EXTENSIONS}
            String[] extensions = Util.getStringValues(annotation, description, "extensions");
            if (extensions != null && extensions.length != 0) {
                tags.add(new SlingServletPropertyTag(annotation, "sling.servlet.extensions", extensions, description));
            }

            // {@see org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_METHODS}
            String[] methods = Util.getStringValues(annotation, description, "methods");
            if (methods != null && methods.length != 0) {
                tags.add(new SlingServletPropertyTag(annotation, "sling.servlet.methods", methods, description));
            }

        }

        return tags;
    }

}
