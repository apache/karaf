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

import java.util.*;

import org.apache.felix.scr.annotations.*;
import org.apache.felix.scrplugin.Constants;
import org.apache.felix.scrplugin.tags.JavaClassDescription;

import com.thoughtworks.qdox.model.Annotation;

/**
 * Description of a java tag for components.
 */
public class PropertyTag extends AbstractTag {

    protected final Property annotation;

    /**
     * @param annotation Annotation
     * @param desc Description
     */
    public PropertyTag(Property annotation, JavaClassDescription desc) {
        super(desc, null);
        this.annotation = annotation;
    }

    /**
     * @param annotation Annotation
     * @param desc Description
     */
    public PropertyTag(final Annotation annotation, JavaClassDescription desc) {
        super(desc, null);
        this.annotation = new Property() {

            public int cardinality() {
                return Util.getIntValue(annotation, "cardinality", Property.class);
            }

            public String description() {
                return Util.getStringValue(annotation, "description", Property.class);
            }

            public String label() {
                return Util.getStringValue(annotation, "label", Property.class);
            }

            public String name() {
                return Util.getStringValue(annotation, "name", Property.class);
            }

            public PropertyOption[] options() {
                final Object obj = annotation.getNamedParameter("options");
                if ( obj != null ) {
                    return (PropertyOption[])obj;
                }
                try {
                    return (PropertyOption[]) Property.class.getMethod("options").getDefaultValue();
                } catch( NoSuchMethodException mnfe) {
                    // we ignore this
                    return null;
                }
            }

            public boolean propertyPrivate() {
                return Util.getBooleanValue(annotation, "propertyPrivate", Property.class);
            }

            public Class<?> type() {
                return Util.getClassValue(annotation, "type", Property.class);
            }

            public String[] value() {
                // value property can be used as String[] or String property
                Object obj = annotation.getNamedParameter("value");
                if (obj instanceof List) {
                    return Util.getStringValues(annotation, "value", Property.class);
                }
                else {
                    return new String[] { Util.getStringValue(annotation, "value", Property.class) };
                }
            }

            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return null;
            }
        };
    }

    @Override
    public String getName() {
        return Constants.PROPERTY;
    }

    @Override
    public Map<String, String> getNamedParameterMap() {
        final Map<String, String> map = new HashMap<String, String>();

        map.put(Constants.PROPERTY_NAME, emptyToNull(this.annotation.name()));
        map.put(Constants.PROPERTY_LABEL, emptyToNull(this.annotation.label()));
        map.put(Constants.PROPERTY_DESCRIPTION, emptyToNull(this.annotation.description()));

        String[] values = this.annotation.value();
        if (values == null || values.length == 0) {
            map.put(Constants.PROPERTY_VALUE, "");
        } else if (values.length == 1) {
            map.put(Constants.PROPERTY_VALUE, values[0]);
        } else {
            for (int i = 0; i < values.length; i++) {
                map.put(Constants.PROPERTY_MULTIVALUE_PREFIX + '.' + i, values[i]);
            }
        }

        String type = null;
        if (this.annotation.type() != AutoDetect.class) {
            type = this.annotation.type().getSimpleName();
        }
        map.put(Constants.PROPERTY_TYPE, type);

        if (this.annotation.cardinality() != 0) {
            map.put(Constants.PROPERTY_CARDINALITY, String.valueOf(this.annotation.cardinality()));
        }

        map.put(Constants.PROPERTY_PRIVATE, String.valueOf(this.annotation.propertyPrivate()));

        return map;
    }

    @Override
    public String[] getParameters() {
        List<String> parameters = new ArrayList<String>();

        String[] defaultParameters = super.getParameters();
        if (defaultParameters != null) {
            parameters.addAll(Arrays.asList(defaultParameters));
        }

        // if defined: add options as parameters to the end of parameter list
        // (strange parsing due to qdox tag restrictions...)
        if (this.annotation.options().length > 0) {
            parameters.add(Constants.PROPERTY_OPTIONS);
            for (PropertyOption option : this.annotation.options()) {
                parameters.add(option.name());
                parameters.add("=");
                parameters.add(option.value());
            }
        }

        return parameters.toArray(new String[parameters.size()]);
    }

}
