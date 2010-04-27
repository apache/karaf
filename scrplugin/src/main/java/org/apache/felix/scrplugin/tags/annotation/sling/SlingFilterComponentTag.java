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

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scrplugin.Constants;
import org.apache.felix.scrplugin.tags.JavaClassDescription;
import org.apache.felix.scrplugin.tags.annotation.defaulttag.AbstractTag;

import com.thoughtworks.qdox.model.Annotation;

/**
 * Description of a java tag for components.
 */
public class SlingFilterComponentTag extends AbstractTag {

    private final boolean createMetatype;
    private final String name;
    private final String label;
    private final String description;

    /**
     * @param desc Description
     */
    public SlingFilterComponentTag(final Annotation annotation,
            final JavaClassDescription desc,
            final boolean createMetatype,
            final String name,
            final String label,
            final String description) {
        super(annotation, desc, null);
        this.createMetatype = createMetatype;
        this.name = name;
        this.label = label;
        this.description = description;
    }

    @Override
    public String getName() {
        return Constants.COMPONENT;
    }

    @Override
    public Map<String, String> createNamedParameterMap() {
        final Map<String, String> params = new HashMap<String, String>();
        if ( this.name != null ) {
            params.put(Constants.COMPONENT_NAME, this.name);
        }
        if ( this.label != null ) {
            params.put(Constants.COMPONENT_LABEL, this.label);
        }
        if ( this.description != null ) {
            params.put(Constants.COMPONENT_DESCRIPTION, this.description);
        }
        params.put(Constants.COMPONENT_METATYPE, String.valueOf(this.createMetatype));
        return params;
    }

}
