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

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.AutoDetect;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scrplugin.Constants;
import org.apache.felix.scrplugin.tags.JavaClassDescription;

import com.thoughtworks.qdox.model.Annotation;

/**
 * Description of a java tag for components.
 */
public class ServiceTag extends AbstractTag {

    protected final Service annotation;

    /**
     * @param annotation Annotation
     * @param desc Description
     */
    public ServiceTag(final Annotation annotation, JavaClassDescription desc) {
        super(desc, null);
        this.annotation = new Service() {

            public boolean serviceFactory() {
                return Util.getBooleanValue(annotation, "serviceFactory", Service.class);
            }

            public Class<?> value() {
                return Util.getClassValue(annotation, "value", Service.class);
            }

            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return null;
            }
        };
    }

    @Override
    public String getName() {
        return Constants.SERVICE;
    }

    @Override
    public Map<String, String> createNamedParameterMap() {
        final Map<String, String> map = new HashMap<String, String>();

        String serviceInterface = null;
        if (this.annotation.value() != AutoDetect.class) {
            serviceInterface = this.annotation.value().getName();
        }
        map.put(Constants.SERVICE_INTERFACE, serviceInterface);

        map.put(Constants.SERVICE_FACTORY, String.valueOf(this.annotation.serviceFactory()));

        return map;
    }

}
