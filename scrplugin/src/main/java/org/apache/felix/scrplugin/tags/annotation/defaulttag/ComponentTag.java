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

import org.apache.felix.scr.annotations.*;
import org.apache.felix.scrplugin.Constants;
import org.apache.felix.scrplugin.tags.JavaClassDescription;

import com.thoughtworks.qdox.model.Annotation;
import com.thoughtworks.qdox.model.JavaMethod;

/**
 * Description of a java tag for components.
 */
public class ComponentTag extends AbstractTag {

    protected final Component annotation;

    /**
     * @param annotation Annotation
     * @param desc Description
     */
    public ComponentTag(final Annotation annotation, final JavaClassDescription desc) {
        super(annotation, desc, null);
        this.annotation = new Component() {

            public boolean componentAbstract() {
                return Util.getBooleanValue(annotation, "componentAbstract", Component.class);
            }

            public boolean createPid() {
                return Util.getBooleanValue(annotation, "createPid", Component.class);
            }

            public String description() {
                return Util.getStringValue(annotation, desc, "description", Component.class);
            }

            public boolean ds() {
                return Util.getBooleanValue(annotation, "ds", Component.class);
            }

            public String specVersion() {
                return Util.getStringValue( annotation, desc, "specVersion", Component.class );
            }

            public boolean enabled() {
                return Util.getBooleanValue(annotation, "enabled", Component.class);
            }

            public String factory() {
                return Util.getStringValue(annotation, desc, "factory", Component.class);
            }

            public boolean immediate() {
                return Util.getBooleanValue(annotation, "immediate", Component.class);
            }

            public boolean inherit() {
                return Util.getBooleanValue(annotation, "inherit", Component.class);
            }

            public String label() {
                return Util.getStringValue(annotation, desc, "label", Component.class);
            }

            public boolean metatype() {
                return Util.getBooleanValue(annotation, "metatype", Component.class);
            }

            public String name() {
                return Util.getStringValue(annotation, desc, "name", Component.class);
            }

            public ConfigurationPolicy policy() {
                return Util.getEnumValue(annotation, "policy", ConfigurationPolicy.class, Component.class, false);
            }

            public boolean getConfigurationFactory() {
                return Util.getBooleanValue(annotation, "getConfigurationFactory", Component.class);
            }

            public boolean configurationFactory() {
                final Object obj = annotation.getNamedParameter("configurationFactory");
                if ( obj != null ) {
                    return Boolean.valueOf(obj.toString());
                }
                return getConfigurationFactory();
            }

            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return null;
            }
        };
    }

    @Override
    public String getName() {
        return Constants.COMPONENT;
    }

    @Override
    public Map<String, String> createNamedParameterMap() {
        final Map<String, String> map = new HashMap<String, String>();

        map.put(Constants.COMPONENT_NAME, emptyToNull(this.annotation.name()));
        map.put(Constants.COMPONENT_LABEL, emptyToNull(this.annotation.label()));
        map.put(Constants.COMPONENT_DESCRIPTION, emptyToNull(this.annotation.description()));
        map.put(Constants.COMPONENT_ENABLED, String.valueOf(this.annotation.enabled()));
        map.put(Constants.COMPONENT_FACTORY, emptyToNull(this.annotation.factory()));
        map.put(Constants.COMPONENT_SET_METATYPE_FACTORY_PID, String.valueOf(this.annotation.configurationFactory()));

        // FELIX-593: immediate attribute does not default to true all the
        // times hence we only set it if declared in the tag
        if ( this.sourceAnnotation.getNamedParameter("immediate") != null) {
            map.put(Constants.COMPONENT_IMMEDIATE, this.sourceAnnotation.getNamedParameter("immediate").toString());
        }
        map.put(Constants.COMPONENT_INHERIT, String.valueOf(this.annotation.inherit()));
        map.put(Constants.COMPONENT_METATYPE, String.valueOf(this.annotation.metatype()));
        map.put(Constants.COMPONENT_ABSTRACT, String.valueOf(this.annotation.componentAbstract()));
        map.put(Constants.COMPONENT_DS, String.valueOf(this.annotation.ds()));
        map.put(Constants.COMPONENT_DS_SPEC_VERSION, String.valueOf(this.annotation.specVersion()));
        map.put(Constants.COMPONENT_CREATE_PID, String.valueOf(this.annotation.createPid()));

        // version 1.1
        if ( this.annotation.policy() != null ) {
            map.put(Constants.COMPONENT_CONFIG_POLICY, this.annotation.policy().getPolicyString());
        }
        final JavaMethod[] jms = this.sourceAnnotation.getContext().getParent().getParentSource().getClasses()[0].getMethods();
        for(final JavaMethod jm : jms) {
            final Annotation[] annotations = jm.getAnnotations();
            for(final Annotation a : annotations) {
                if ( a.getType().getJavaClass().getFullyQualifiedName().equals(Activate.class.getName()) ) {
                    map.put(Constants.COMPONENT_ACTIVATE, jm.getName());
                }
                if ( a.getType().getJavaClass().getFullyQualifiedName().equals(Deactivate.class.getName()) ) {
                    map.put(Constants.COMPONENT_DEACTIVATE, jm.getName());
                }
                if ( a.getType().getJavaClass().getFullyQualifiedName().equals(Modified.class.getName()) ) {
                    map.put(Constants.COMPONENT_MODIFIED, jm.getName());
                }
            }
        }

        return map;
    }

}
