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

import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scrplugin.Constants;
import org.apache.felix.scrplugin.tags.JavaClassDescription;
import org.apache.felix.scrplugin.tags.JavaField;

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
    public PropertyTag(final Annotation annotation, final JavaClassDescription desc, JavaField field) {
        super(annotation, desc, field);
        this.annotation = new Property() {

            public int cardinality() {
                return Util.getIntValue(annotation, "cardinality", Property.class);
            }

            public String description() {
                return Util.getStringValue(annotation, desc, "description", Property.class);
            }

            public String label() {
                return Util.getStringValue(annotation, desc, "label", Property.class);
            }

            public String name() {
                return Util.getStringValue(annotation, desc, "name", Property.class);
            }

            public PropertyOption[] options() {
                final Object obj = annotation.getNamedParameter("options");
                if ( obj != null ) {
                    if ( obj instanceof Annotation ) {
                        final Annotation annotation = (Annotation)obj;
                        return new PropertyOption[] {new PropertyOptionImpl(annotation, desc)};
                    }
                    @SuppressWarnings("unchecked")
                    final List<Annotation> annotations = (List<Annotation>) obj;
                    PropertyOption[] options = new PropertyOption[annotations.size()];
                    for (int index = 0; index < options.length; index++) {
                        final Annotation propAnnotation = annotations.get(index);
                        options[index] = new PropertyOptionImpl(propAnnotation, desc);
                    }
                    return options;
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

            public String[] value() {
                // value property can be used as String[] or String property
                return Util.getStringValues(annotation, desc, "value");
            }

            public boolean[] boolValue() {
                return Util.getBooleanValues(annotation, desc, "boolValue");
            }

            public byte[] byteValue() {
                return Util.getByteValues(annotation, desc, "byteValue");
            }

            public char[] charValue() {
                return Util.getCharValues(annotation, desc, "charValue");
            }

            public double[] doubleValue() {
                return Util.getDoubleValues(annotation, desc, "doubleValue");
            }

            public float[] floatValue() {
                return Util.getFloatValues(annotation, desc, "floatValue");
            }

            public int[] intValue() {
                return Util.getIntValues(annotation, desc, "intValue");
            }

            public long[] longValue() {
                return Util.getLongValues(annotation, desc, "longValue");
            }

            public short[] shortValue() {
                return Util.getShortValues(annotation, desc, "shortValue");
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
    public Map<String, String> createNamedParameterMap() {
        final Map<String, String> map = new LinkedHashMap<String, String>();

        map.put(Constants.PROPERTY_NAME, emptyToNull(this.annotation.name()));
        map.put(Constants.PROPERTY_LABEL, emptyToNull(this.annotation.label()));
        map.put(Constants.PROPERTY_DESCRIPTION, emptyToNull(this.annotation.description()));


        String type = null;
        Object[] values = this.annotation.value();
        // we now check all options
        if (values == null || values.length == 0 ) {
            long[] lValues = this.annotation.longValue();
            if ( lValues == null || lValues.length == 0 ) {
                double[] dValues = this.annotation.doubleValue();
                if ( dValues == null || dValues.length == 0 ) {
                    float[] fValues = this.annotation.floatValue();
                    if ( fValues == null || fValues.length == 0 ) {
                        int[] iValues = this.annotation.intValue();
                        if ( iValues == null || iValues.length == 0 ) {
                            byte[] byteValues = this.annotation.byteValue();
                            if ( byteValues == null || byteValues.length == 0 ) {
                                char[] cValues = this.annotation.charValue();
                                if ( cValues == null || cValues.length == 0 ) {
                                    boolean[] boolValues = this.annotation.boolValue();
                                    if ( boolValues == null || boolValues.length == 0 ) {
                                        short[] sValues  = this.annotation.shortValue();
                                        if ( sValues != null && sValues.length != 0 ) {
                                            values = new Object[sValues.length];
                                            for(int i=0;i<sValues.length;i++) {
                                                values[i] = sValues[i];
                                            }
                                            type = "Short";
                                        }
                                    } else {
                                        values = new Object[boolValues.length];
                                        for(int i=0;i<boolValues.length;i++) {
                                            values[i] = boolValues[i];
                                        }
                                        type = "Boolean";
                                    }
                                } else {
                                    values = new Object[cValues.length];
                                    for(int i=0;i<cValues.length;i++) {
                                        values[i] = cValues[i];
                                    }
                                    type = "Char";
                                }
                            } else {
                                values = new Object[byteValues.length];
                                for(int i=0;i<byteValues.length;i++) {
                                    values[i] = byteValues[i];
                                }
                                type = "Byte";
                            }
                        } else {
                            values = new Object[iValues.length];
                            for(int i=0;i<iValues.length;i++) {
                                values[i] = iValues[i];
                            }
                            type = "Integer";
                        }
                    } else {
                        values = new Object[fValues.length];
                        for(int i=0;i<fValues.length;i++) {
                            values[i] = fValues[i];
                        }
                        type = "Float";
                    }
                } else {
                    values = new Object[dValues.length];
                    for(int i=0;i<dValues.length;i++) {
                        values[i] = dValues[i];
                    }
                    type = "Double";
                }
            } else {
                values = new Object[lValues.length];
                for(int i=0;i<lValues.length;i++) {
                    values[i] = lValues[i];
                }
                type = "Long";
            }
        } else {
            type = "String";
        }

        if ( values != null && values.length > 0 ) {
            map.put(Constants.PROPERTY_TYPE, type);
            if (values.length == 1) {
                map.put(Constants.PROPERTY_VALUE, values[0].toString());
            } else {
                for (int i = 0; i < values.length; i++) {
                    map.put(Constants.PROPERTY_MULTIVALUE_PREFIX + '.' + i, values[i].toString());
                }
            }
        }

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

    protected static class PropertyOptionImpl implements PropertyOption {

        private final Annotation annotation;
        private final JavaClassDescription description;

        public PropertyOptionImpl(final Annotation annotation,
                final JavaClassDescription desc) {
            this.annotation = annotation;
            this.description = desc;
        }

        public String name() {
            final String[] names = Util.getAnnotationValues(annotation, "name", description);
            if ( names != null && names.length > 0 ) {
                return names[0];
            }
            return null;
        }

        public String value() {
            final String[] values = Util.getAnnotationValues(annotation, "value", description);
            if ( values != null && values.length > 0 ) {
                return values[0];
            }
            return null;
        }

        public Class<? extends java.lang.annotation.Annotation> annotationType() {
            return PropertyOption.class;
        }
    }
}
