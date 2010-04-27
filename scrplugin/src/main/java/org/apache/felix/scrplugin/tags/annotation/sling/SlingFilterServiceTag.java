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

import java.util.Collections;
import java.util.Map;

import javax.servlet.Filter;

import org.apache.felix.scrplugin.Constants;
import org.apache.felix.scrplugin.tags.JavaClassDescription;
import org.apache.felix.scrplugin.tags.annotation.defaulttag.AbstractTag;

import com.thoughtworks.qdox.model.Annotation;

/**
 * Description of a java tag for components.
 */
public class SlingFilterServiceTag extends AbstractTag {

    private static final Map<String, String> INTERFACE_MAP =
        Collections.singletonMap(Constants.SERVICE_INTERFACE, Filter.class.getName());

    /**
     * @param desc Description
     */
    public SlingFilterServiceTag(Annotation annotation, JavaClassDescription desc) {
        super(annotation, desc, null);
    }

    @Override
    public String getName() {
        return Constants.SERVICE;
    }

    @Override
    public Map<String, String> createNamedParameterMap() {
        return INTERFACE_MAP;
    }

}
