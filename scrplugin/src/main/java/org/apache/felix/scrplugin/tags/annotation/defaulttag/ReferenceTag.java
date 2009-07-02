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
import org.apache.felix.scrplugin.tags.JavaField;

import com.thoughtworks.qdox.model.Annotation;

/**
 * Description of a java tag for components.
 */
public class ReferenceTag extends AbstractTag {

    protected final Reference annotation;

    /**
     * @param annotation Annotation
     * @param desc Description
     */
    public ReferenceTag(final Annotation annotation, final JavaClassDescription desc, JavaField field) {
        super(desc, field);

        this.annotation = new Reference() {

            public String bind() {
                return Util.getStringValue(annotation, desc, "bind", Reference.class);
            }

            public ReferenceCardinality cardinality() {
                return Util.getEnumValue(annotation, "cardinality", ReferenceCardinality.class, Reference.class);
            }

            public String name() {
                return Util.getStringValue(annotation, desc, "name", Reference.class);
            }

            public ReferencePolicy policy() {
                return Util.getEnumValue(annotation, "policy", ReferencePolicy.class, Reference.class);
            }

            public Class<?> referenceInterface() {
                return Util.getClassValue(annotation, "referenceInterface", Reference.class);
            }

            public ReferenceStrategy strategy() {
                return Util.getEnumValue(annotation, "strategy", ReferenceStrategy.class, Reference.class);
            }

            public String target() {
                return Util.getStringValue(annotation, desc, "target", Reference.class);
            }

            public String unbind() {
                return Util.getStringValue(annotation, desc, "unbind", Reference.class);
            }

            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return null;
            }
        };
    }

    @Override
    public String getName() {
        return Constants.REFERENCE;
    }

    @Override
    public Map<String, String> createNamedParameterMap() {
        final Map<String, String> map = new HashMap<String, String>();

        map.put(Constants.REFERENCE_NAME, emptyToNull(this.annotation.name()));

        if (this.annotation.referenceInterface() != AutoDetect.class) {
            String referenceInterface = this.annotation.referenceInterface().getName();
            map.put(Constants.REFERENCE_INTERFACE, referenceInterface);
        }

        map.put(Constants.REFERENCE_CARDINALITY, this.annotation.cardinality().getCardinalityString());
        map.put(Constants.REFERENCE_POLICY, this.annotation.policy().getPolicyString());
        map.put(Constants.REFERENCE_TARGET, emptyToNull(this.annotation.target()));
        map.put(Constants.REFERENCE_BIND, emptyToNull(this.annotation.bind()));
        map.put(Constants.REFERENCE_UNDBIND, emptyToNull(this.annotation.unbind()));
        map.put(Constants.REFERENCE_STRATEGY, this.annotation.strategy().getStrategyString());

        return map;
    }

}
