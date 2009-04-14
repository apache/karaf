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
package org.apache.felix.scrplugin;

import java.util.*;

import org.apache.felix.scrplugin.om.Component;
import org.apache.felix.scrplugin.om.Property;
import org.apache.felix.scrplugin.om.metatype.AttributeDefinition;
import org.apache.felix.scrplugin.om.metatype.OCD;
import org.apache.felix.scrplugin.tags.JavaField;
import org.apache.felix.scrplugin.tags.JavaTag;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Utility class for handling the properties.
 */
public class PropertyHandler {

    /**
     * This is a map using the property name as the key and
     * {@link PropertyDescription} as values.
     */
    final private Map<String, PropertyDescription> properties = new LinkedHashMap<String, PropertyDescription>();

    /** The component. */
    final private Component component;

    /** The ocd or null. */
    final private OCD ocd;

    public PropertyHandler(final Component c, OCD o) {
        this.component = c;
        this.ocd = o;
    }

    /**
     * Process a property.
     * @param tag       The property tag.
     * @param name      The name of the property.
     * @param javaField The corresponding java field or null.
     */
    protected void processProperty(JavaTag   tag,
                                   String    name,
                                   JavaField javaField,
                                   final List<String> warnings)
    throws MojoExecutionException {
        final Property prop = new Property(tag);
        prop.setName(name);
        prop.setType(tag.getNamedParameter(Constants.PROPERTY_TYPE));
        // let's first check for a value attribute
        final String value = tag.getNamedParameter(Constants.PROPERTY_VALUE);
        if ( value != null ) {
            prop.setValue(value);
        } else {
            // now we check for a value ref attribute
            final String valueRef = tag.getNamedParameter(Constants.PROPERTY_VALUE_REF);
            if ( valueRef != null ) {
                this.setPropertyValueRef(tag, prop, valueRef);
            } else {
                // check for multivalue - these can either be values or value refs
                final List<String> values = new ArrayList<String>();
                final Map<String, String> valueMap = tag.getNamedParameterMap();
                for (Iterator<Map.Entry<String, String>> vi = valueMap.entrySet().iterator(); vi.hasNext();) {
                    final Map.Entry<String, String> entry = vi.next();
                    final String key = entry.getKey();
                    if (key.startsWith(Constants.PROPERTY_MULTIVALUE_PREFIX) ) {
                        values.add(entry.getValue());
                    } else if ( key.startsWith(Constants.PROPERTY_MULTIVALUE_REF_PREFIX)
                        || key.startsWith(Constants.OLD_PROPERTY_MULTIVALUE_REF_PREFIX) ) {
                        if ( key.startsWith(Constants.OLD_PROPERTY_MULTIVALUE_REF_PREFIX) ) {
                            warnings.add("@" + tag.getName() + ": " + "Deprecated attribute '" +
                                    Constants.OLD_PROPERTY_MULTIVALUE_REF_PREFIX + "' used, use '" +
                                    Constants.PROPERTY_MULTIVALUE_REF_PREFIX + "' instead "
                                    + " (" + tag.getSourceLocation() + ")");
                        }
                        final String[] stringValues = this.getPropertyValueRef(tag, prop, entry.getValue());
                        if ( stringValues != null ) {
                            for(int i=0; i<stringValues.length; i++) {
                                values.add(stringValues[i]);
                            }
                        }
                    }
                }
                if ( values.size() > 0 ) {
                    prop.setMultiValue(values.toArray(new String[values.size()]));
                } else {
                    // we have no value, valueRef or values so let's try to
                    // get the value of the field if a name attribute is specified
                    final boolean isNamedParameter = tag.getNamedParameter(Constants.PROPERTY_NAME) != null
                                                  || tag.getNamedParameter(Constants.PROPERTY_NAME_REF) != null;
                    if ( isNamedParameter && javaField != null ) {
                        this.setPropertyValueRef(tag, prop, javaField.getName());
                    }
                }
            }
        }

        // property is private if explicitly marked or a well known
        // service property such as service.pid
        final boolean isPrivate = SCRDescriptorMojo.getBoolean(tag,
            Constants.PROPERTY_PRIVATE, false)
            || name.equals(org.osgi.framework.Constants.SERVICE_PID)
            || name.equals(org.osgi.framework.Constants.SERVICE_DESCRIPTION)
            || name.equals(org.osgi.framework.Constants.SERVICE_ID)
            || name.equals(org.osgi.framework.Constants.SERVICE_RANKING)
            || name.equals(org.osgi.framework.Constants.SERVICE_VENDOR)
            || name.equals(ConfigurationAdmin.SERVICE_BUNDLELOCATION)
            || name.equals(ConfigurationAdmin.SERVICE_FACTORYPID);

        // if this is an abstract component we store the extra info in the property
        if ( component.isAbstract() ) {
            prop.setPrivate(isPrivate);
            prop.setLabel(tag.getNamedParameter(Constants.PROPERTY_LABEL));
            prop.setDescription(tag.getNamedParameter(Constants.PROPERTY_DESCRIPTION));
            prop.setCardinality(tag.getNamedParameter(Constants.PROPERTY_CARDINALITY));
        }

        // if this is a public property and the component is generating metatype info
        // store the information!
        if ( !isPrivate && ocd != null ) {
            final AttributeDefinition ad = new AttributeDefinition();
            ocd.getProperties().add(ad);
            ad.setId(prop.getName());
            ad.setType(prop.getType());

            String adName = tag.getNamedParameter(Constants.PROPERTY_LABEL);
            if ( adName == null ) {
                adName = "%" + prop.getName() + ".name";
            }
            ad.setName(adName);
            String adDesc = tag.getNamedParameter(Constants.PROPERTY_DESCRIPTION);
            if ( adDesc == null ) {
                adDesc = "%" + prop.getName() + ".description";
            }
            ad.setDescription(adDesc);
            // set optional multivalues, cardinality might be overwritten by setValues !!
            final String cValue = tag.getNamedParameter(Constants.PROPERTY_CARDINALITY);
            if (cValue != null) {
                if ("-".equals(cValue)) {
                    // unlimited vector
                    ad.setCardinality(new Integer(Integer.MIN_VALUE));
                } else if ("+".equals(cValue)) {
                   // unlimited array
                    ad.setCardinality(new Integer(Integer.MAX_VALUE));
                } else {
                    try {
                        ad.setCardinality(Integer.valueOf(cValue));
                    } catch (NumberFormatException nfe) {
                        // default to scalar in case of conversion problem
                    }
                }
            }
            ad.setDefaultValue(prop.getValue());
            ad.setDefaultMultiValue(prop.getMultiValue());

            // check options
            String[] parameters = tag.getParameters();
            Map<String, String> options = null;
            for (int j=0; j < parameters.length; j++) {
                if (Constants.PROPERTY_OPTIONS.equals(parameters[j])) {
                    options = new LinkedHashMap<String, String>();
                } else if (options != null) {
                    String optionLabel = parameters[j];
                    String optionValue = (j < parameters.length-2) ? parameters[j+2] : null;
                    if (optionValue != null) {
                        options.put(optionLabel, optionValue);
                    }
                    j += 2;
                }
            }
            ad.setOptions(options);
        }

        component.addProperty(prop);
    }

    /**
     * Return the name of the property.
     * The name of the property is derived by:
     * <ol>
     *   <li>looking at the attribute {@link Constants.PROPERTY_NAME}</li>
     *   <li>looking at the attribute {@link Constants.PROPERTY_NAME_REF}</li>
     *   <li>if the property is specified at a filed and the field is of type string the init value is used.</li>
     * </ol>
     *
     * @param property The property tag.
     * @param field    The corresponding field if the property is a tag of a field.
     * @return The name of the property or the defaultName
     */
    protected String getPropertyName(JavaTag tag, JavaField field)
    throws MojoExecutionException {
        // check name property
        String name = tag.getNamedParameter(Constants.PROPERTY_NAME);

        if (StringUtils.isEmpty(name)) {
            // check name ref propery
            name = tag.getNamedParameter(Constants.PROPERTY_NAME_REF);
            if (!StringUtils.isEmpty(name)) {
                final JavaField refField = this.getReferencedField(tag, name);
                final String[] values = refField.getInitializationExpression();
                if ( values == null || values.length == 0 ) {
                    throw new MojoExecutionException("Referenced field for " + name + " has no values for a property name.");
                }
                if ( values.length > 1 ) {
                    throw new MojoExecutionException("Referenced field " + name + " has more than one value for a property name.");
                }
                name = values[0];
            }

            if (StringUtils.isEmpty(name)) {
                // check field
                name = null;
                if ( field != null && "java.lang.String".equals(field.getType()) ) {
                    final String[] initValues = field.getInitializationExpression();
                    if ( initValues != null && initValues.length == 1 ) {
                        name = initValues[0];
                    }
                }
            }
        }
        // final empty check
        if ( StringUtils.isEmpty(name) ) {
            name = null;
        }
        return name;
    }

    protected void setPropertyValueRef(final JavaTag tag, Property property, String valueRef)
    throws MojoExecutionException {
        final String[] values = this.getPropertyValueRef(tag, property, valueRef);
        if ( values != null && values.length == 1 ) {
            property.setValue(values[0]);
        } else if ( values != null && values.length > 1 ) {
            property.setMultiValue(values);
        }
    }

    protected JavaField getReferencedField(final JavaTag tag, String ref)
    throws MojoExecutionException {
        int classSep = ref.lastIndexOf('.');
        JavaField field = null;
        if ( classSep == -1 ) {
            // local variable
            field = tag.getJavaClassDescription().getFieldByName(ref);
        }
        if ( field == null ) {
            field = tag.getJavaClassDescription().getExternalFieldByName(ref);
        }
        if ( field == null ) {
            throw new MojoExecutionException("Property references unknown field " + ref + " in class " + tag.getJavaClassDescription().getName());
        }
        return field;
    }

    protected String[] getPropertyValueRef(final JavaTag tag, Property prop, String valueRef)
    throws MojoExecutionException {
        final JavaField field = this.getReferencedField(tag, valueRef);

        // determine type (if not set explicitly)
        if ( prop.getType() == null ) {
            final String type = field.getType();
            if ( "java.lang.String".equals(type) ) {
                prop.setType("String");
            } else if ("java.lang.Long".equals(type) || "long".equals(type) ) {
                prop.setType("Long");
            } else if ("java.lang.Double".equals(type) || "double".equals(type) ) {
                prop.setType("Double");
            } else if ("java.lang.Float".equals(type) || "float".equals(type) ) {
                prop.setType("Float");
            } else if ("java.lang.Integer".equals(type) || "int".equals(type) ) {
                prop.setType("Integer");
            } else if ("java.lang.Byte".equals(type) || "byte".equals(type) ) {
                prop.setType("Byte");
            } else if ("java.lang.Character".equals(type) || "char".equals(type) ) {
                prop.setType("Char");
            } else if ("java.lang.Boolean".equals(type) || "boolean".equals(type) ) {
                prop.setType("Boolean");
            } else if ("java.lang.Short".equals(type) || "short".equals(type) ) {
                prop.setType("Short");
            }

        }
        return field.getInitializationExpression();
    }

    /**
     * Test if there is already a property with the same name.
     * @param property The tag.
     * @param field
     * @param isInspectedClass
     * @throws MojoExecutionException
     */
    public void testProperty(JavaTag   property,
                             JavaField field,
                             boolean   isInspectedClass)
    throws MojoExecutionException {
        final String propName = this.getPropertyName(property, field);

        if ( propName != null ) {
            if ( properties.containsKey(propName) ) {
                // if the current class is the class we are currently inspecting, we
                // have found a duplicate definition
                if ( isInspectedClass ) {
                    throw new MojoExecutionException("Duplicate definition for property " + propName + " in class " + property.getJavaClassDescription().getName());
                }
            } else {
                properties.put(propName, new PropertyDescription(property, field));
            }
        } else {
            throw new MojoExecutionException("Property has no name " + property.getSourceLocation());
        }
    }

    public void handleField(JavaField javaField, boolean isInspectedClass)
    throws MojoExecutionException {
        final JavaTag tag = javaField.getTagByName(Constants.PROPERTY);
        if (tag != null) {
            this.testProperty(tag, javaField, isInspectedClass);
        }
    }

    /**
     * Process all found properties for the component.
     * @param globalProperties Global properties are set on all components.
     * @param errors List of occured errors.
     * @param warnings List of occured warnings
     * @throws MojoExecutionException
     */
    public void processProperties(final Map<String, String> globalProperties,
                                  final List<String> errors,
                                  final List<String> warnings)
    throws MojoExecutionException {
        final Iterator<Map.Entry<String, PropertyDescription>> propIter = properties.entrySet().iterator();
        while ( propIter.hasNext() ) {
            final Map.Entry<String, PropertyDescription> entry = propIter.next();
            final String propName = entry.getKey();
            final PropertyDescription desc = entry.getValue();
            this.processProperty(desc.propertyTag, propName, desc.field, warnings);
        }
        // apply pre configured global properties
        if ( globalProperties != null ) {
            final Iterator<Map.Entry<String, String>> globalPropIter = globalProperties.entrySet().iterator();
            while ( globalPropIter.hasNext() ) {
                final Map.Entry<String, String> entry = globalPropIter.next();
                final String name = entry.getKey();

                // check if the service already provides this property
                if ( !properties.containsKey(name) && entry.getValue() != null ) {
                    final String value = entry.getValue();

                    final Property p = new Property();
                    p.setName(name);
                    p.setValue(value);
                    p.setType("String");
                    p.setPrivate(true);
                    component.addProperty(p);

                }
            }
        }
    }

    protected static final class PropertyDescription {
        public final JavaTag propertyTag;
        public final JavaField field;

        public PropertyDescription(final JavaTag p, final JavaField f) {
            this.propertyTag = p;
            this.field = f;
        }
    }
}
