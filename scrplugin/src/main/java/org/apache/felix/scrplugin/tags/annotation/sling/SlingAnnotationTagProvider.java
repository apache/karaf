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

import org.apache.felix.scr.annotations.sling.*;
import org.apache.felix.scrplugin.tags.JavaField;
import org.apache.felix.scrplugin.tags.JavaTag;
import org.apache.felix.scrplugin.tags.annotation.AnnotationJavaClassDescription;
import org.apache.felix.scrplugin.tags.annotation.AnnotationTagProvider;
import org.apache.felix.scrplugin.tags.annotation.defaulttag.Util;

/**
 * Annotation tag provider for sling-specific SCR annotations.
 */
public class SlingAnnotationTagProvider implements AnnotationTagProvider {

    /**
     * @see org.apache.felix.scrplugin.tags.annotation.AnnotationTagProvider#getTags(com.thoughtworks.qdox.model.Annotation, org.apache.felix.scrplugin.tags.annotation.AnnotationJavaClassDescription, org.apache.felix.scrplugin.tags.JavaField)
     */
    public List<JavaTag> getTags(final com.thoughtworks.qdox.model.Annotation annotation,
            final AnnotationJavaClassDescription description,
            final JavaField field)
   {
        final String annotationName = annotation.getType().getJavaClass().getFullyQualifiedName();
        final List<JavaTag> tags = new ArrayList<JavaTag>();

        // SlingServlet annotation
        if (annotationName.equals(SlingServlet.class.getName()))
        {

            // generate @Component tag if required
            boolean generateComponent = Util.getBooleanValue(annotation, "generateComponent", SlingServlet.class);
            if (generateComponent)
            {
                tags.add(new SlingServletComponentTag(annotation, description));
            }

            // generate @Service tag if required
            boolean generateService = Util.getBooleanValue(annotation, "generateService", SlingServlet.class);
            if (generateService)
            {
                tags.add(new SlingServletServiceTag(annotation, description));
            }

            // generate @Property tags
            // {@see org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_PATHS}
            String[] paths = Util.getStringValues(annotation, description, "paths");
            if (paths != null && paths.length != 0)
            {
                tags.add(new SlingServletPropertyTag(annotation, "sling.servlet.paths", paths, description));
            }

            // {@see org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES}
            String[] resourceTypes = Util.getStringValues(annotation, description, "resourceTypes");
            if (resourceTypes != null && resourceTypes.length != 0)
            {
                tags.add(new SlingServletPropertyTag(annotation, "sling.servlet.resourceTypes", resourceTypes, description));
            }

            // {@see org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_SELECTORS}
            String[] selectors = Util.getStringValues(annotation, description, "selectors");
            if (selectors != null && selectors.length != 0)
            {
                tags.add(new SlingServletPropertyTag(annotation, "sling.servlet.selectors", selectors, description));
            }

            // {@see org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_EXTENSIONS}
            String[] extensions = Util.getStringValues(annotation, description, "extensions");
            if (extensions != null && extensions.length != 0)
            {
                tags.add(new SlingServletPropertyTag(annotation, "sling.servlet.extensions", extensions, description));
            }

            // {@see org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_METHODS}
            String[] methods = Util.getStringValues(annotation, description, "methods");
            if (methods != null && methods.length != 0)
            {
                tags.add(new SlingServletPropertyTag(annotation, "sling.servlet.methods", methods, description));
            }

        }
        // Filter annotation
        else if ( annotationName.equals(SlingFilter.class.getName()) )
        {
            // generate @Component tag if required
            boolean generateComponent = Util.getBooleanValue(annotation, "generateComponent", SlingFilter.class);
            if (generateComponent)
            {
                String name = Util.getStringValue(annotation, description, "name", SlingFilter.class);
                if ( name != null && name.trim().length() == 0 ) {
                    name = null;
                }
                String label = Util.getStringValue(annotation, description, "label", SlingFilter.class);
                if ( label != null && label.trim().length() == 0 ) {
                    label = null;
                }
                String desc = Util.getStringValue(annotation, description, "description", SlingFilter.class);
                if ( desc != null && desc.trim().length() == 0 ) {
                    desc = null;
                }
                final boolean createMetatype = Util.getBooleanValue(annotation, "metatype", SlingFilter.class);
                tags.add(new SlingFilterComponentTag(annotation, description, createMetatype, name, label, desc));
            }

            // generate @Service tag if required
            boolean generateService = Util.getBooleanValue(annotation, "generateService", SlingFilter.class);
            if (generateService)
            {
                tags.add(new SlingFilterServiceTag(annotation, description));
            }

            // property order
            final int order = Util.getIntValue(annotation, "order", SlingFilter.class);
            tags.add(new SlingServletPropertyTag(annotation, "filter.order",String.valueOf(order), description, "Integer", true));

            // property scope
            final SlingFilterScope scope = Util.getEnumValue(annotation, "scope", SlingFilterScope.class, SlingFilter.class);
            tags.add(new SlingServletPropertyTag(annotation, "filter.scope",scope.getScope(), description, null, true));
        }

        return tags;
    }

}
