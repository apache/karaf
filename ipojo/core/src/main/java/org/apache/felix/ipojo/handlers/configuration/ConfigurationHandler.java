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
package org.apache.felix.ipojo.handlers.configuration;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.IPojoConfiguration;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.architecture.ComponentDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandler;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.ManipulationMetadata;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.util.Logger;

/**
 * Handler managing the Configuration Admin.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ConfigurationHandler extends PrimitiveHandler {

    /**
     * List of the configurable fields.
     */
    private ConfigurableProperty[] m_configurableProperties = new ConfigurableProperty[0];

    /**
     * ProvidedServiceHandler of the component. It is useful to propagate
     * properties to service registrations.
     */
    private ProvidedServiceHandler m_providedServiceHandler;

    /**
     * Properties propagated at the last "updated".
     */
    private Dictionary m_propagated = new Properties();

    /**
     * Properties to propagate.
     */
    private Dictionary m_toPropagate = new Properties();

    /**
     * should the component propagate configuration ?
     */
    private boolean m_isConfigurable;

    /**
     * Initialize the component type.
     * @param cd : component type description to populate.
     * @param metadata : component type metadata.
     * @throws ConfigurationException : metadata are incorrect.
     * @see org.apache.felix.ipojo.Handler#initializeComponentFactory(org.apache.felix.ipojo.architecture.ComponentDescription, org.apache.felix.ipojo.metadata.Element)
     */
    public void initializeComponentFactory(ComponentDescription cd, Element metadata) throws ConfigurationException {
        Element[] confs = metadata.getElements("Properties", "");
        if (confs.length == 0) { return; }
        Element[] configurables = confs[0].getElements("Property");
        for (int i = 0; i < configurables.length; i++) {
            String fieldName = null;
            String methodName = null;

            if (configurables[i].containsAttribute("field")) {
                fieldName = configurables[i].getAttribute("field");
            }
            if (configurables[i].containsAttribute("method")) {
                methodName = configurables[i].getAttribute("method");
            }

            if (fieldName != null || methodName != null) {
                String name = null;
                if (configurables[i].containsAttribute("name")) {
                    name = configurables[i].getAttribute("name");
                } else {
                    if (fieldName != null) {
                        name = fieldName;
                    } else {
                        name = methodName;
                    }
                }
                String value = null;
                if (configurables[i].containsAttribute("value")) {
                    value = configurables[i].getAttribute("value");
                }

                // Detect the type of the property
                ManipulationMetadata manipulation = new ManipulationMetadata(metadata);
                String type = null;
                if (fieldName != null) {
                    FieldMetadata fm = manipulation.getField(fieldName);
                    if (fm == null) { throw new ConfigurationException("Malformed property : The field " + fieldName + " does not exist in the implementation", getInstanceManager().getFactory().getName()); }
                    type = fm.getFieldType();
                } else {
                    MethodMetadata[] mm = manipulation.getMethods(methodName);
                    if (mm.length != 0) {
                        if (mm[0].getMethodArguments().length != 1) {
                            log(Logger.ERROR, "[" + getInstanceManager().getClassName() + "] The method " + methodName + " does not have one argument");
                            throw new ConfigurationException("Malformed property :  The method " + methodName + " does not have one argument", getInstanceManager().getFactory().getName());
                        }
                        if (type != null && !type.equals(mm[0].getMethodArguments()[0])) {
                            log(Logger.ERROR, "[" + getInstanceManager().getClassName() + "] The field type (" + type + ") and the method type (" + mm[0].getMethodArguments()[0] + ") are not the same.");
                            throw new ConfigurationException("Malformed property :   The field type (" + type + ") and the method type (" + mm[0].getMethodArguments()[0] + ") are not the same.", getInstanceManager().getFactory().getName());
                        }
                        type = mm[0].getMethodArguments()[0];
                    } else {
                        // Else, the method is in a super class, look for the type attribute to get the type (if not already discovered)
                        if (type == null && configurables[i].containsAttribute("type")) {
                            type = configurables[i].getAttribute("type");
                        } else {
                            log(Logger.ERROR, "The type of the property cannot be discovered, please add a 'type' attribute");
                            return;
                        }
                    }
                }

                if (value != null) {
                    cd.addProperty(new PropertyDescription(name, type, value));
                } else {
                    cd.addProperty(new PropertyDescription(name, type, null));
                }
            } else {
                throw new ConfigurationException("Malformed property : The property need to contain at least a field or a method", getInstanceManager().getFactory().getName());
            }
        }
    }

    /**
     * Configure the handler.
     * 
     * @param metadata : the metadata of the component
     * @param configuration : the instance configuration
     * @throws ConfigurationException : one property metadata is not correct 
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.InstanceManager,
     * org.apache.felix.ipojo.metadata.Element)
     */
    public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {
        // Store the component manager
        m_configurableProperties = new ConfigurableProperty[0];

        // Build the map
        Element[] confs = metadata.getElements("Properties", "");
        if (confs.length == 0) { return; }

        // Check if the component is dynamically configurable
        m_isConfigurable = false;
        // DEPRECATED BLOCK
        if (confs[0].containsAttribute("configurable") && confs[0].getAttribute("configurable").equalsIgnoreCase("true")) {
            log(Logger.WARNING, "The configurable attribute is deprecated, please use the propagation attribute");
            m_isConfigurable = true;
            m_toPropagate = configuration;
        }
        // END
        if (confs[0].containsAttribute("propagation") && confs[0].getAttribute("propagation").equalsIgnoreCase("true")) {
            m_isConfigurable = true;
            m_toPropagate = configuration;
        }

        Element[] configurables = confs[0].getElements("Property");

        List ff = new ArrayList();

        for (int i = 0; i < configurables.length; i++) {
            String fieldName = null;
            String methodName = null;

            if (configurables[i].containsAttribute("field")) {
                fieldName = configurables[i].getAttribute("field");
            }
            if (configurables[i].containsAttribute("method")) {
                methodName = configurables[i].getAttribute("method");
            }

            if (fieldName == null && methodName == null) {
                log(Logger.ERROR, "A configurable property need to have at least a field or a method");
                throw new ConfigurationException("Malformed property : The property need to contain at least a field or a method", getInstanceManager().getFactory().getName());
            }

            String name = null;
            if (configurables[i].containsAttribute("name")) {
                name = configurables[i].getAttribute("name");
            } else {
                if (fieldName != null) {
                    name = fieldName;
                } else {
                    name = methodName;
                }
            }
            String value = null;
            if (configurables[i].containsAttribute("value")) {
                value = configurables[i].getAttribute("value");
            }

            if (name != null && configuration.get(name) != null && configuration.get(name) instanceof String) {
                value = (String) configuration.get(name);
            } else {
                if (fieldName != null && configuration.get(fieldName) != null && configuration.get(fieldName) instanceof String) {
                    value = (String) configuration.get(fieldName);
                }
            }

            // Detect the type of the property
            ManipulationMetadata manipulation = new ManipulationMetadata(metadata);
            String type = null;
            if (fieldName != null) {
                FieldMetadata fm = manipulation.getField(fieldName);
                if (fm == null) {
                    log(Logger.ERROR, "[" + getInstanceManager().getClassName() + "] The field " + fieldName + " does not exist in the implementation");
                    throw new ConfigurationException("Malformed property : The field " + fieldName + " does not exist in the implementation", getInstanceManager().getFactory().getName());
                }
                type = fm.getFieldType();
                ff.add(fm);
            } else {
                MethodMetadata[] mm = manipulation.getMethods(methodName);
                if (mm.length != 0) {
                    if (mm[0].getMethodArguments().length != 1) {
                        log(Logger.ERROR, "[" + getInstanceManager().getClassName() + "] The method " + methodName + " does not have one argument");
                        throw new ConfigurationException("Malformed property :  The method " + methodName + " does not have one argument", getInstanceManager().getFactory().getName());
                    }
                    if (type != null && !type.equals(mm[0].getMethodArguments()[0])) {
                        log(Logger.ERROR, "[" + getInstanceManager().getClassName() + "] The field type (" + type + ") and the method type (" + mm[0].getMethodArguments()[0] + ") are not the same.");
                        throw new ConfigurationException("Malformed property :   The field type (" + type + ") and the method type (" + mm[0].getMethodArguments()[0] + ") are not the same.", getInstanceManager().getFactory().getName());
                    }
                    type = mm[0].getMethodArguments()[0];
                } else {
                    // Else, the method is in a super class, look for the type attribute to get the type (if not already discovered)
                    if (type == null && configurables[i].containsAttribute("type")) {
                        type = configurables[i].getAttribute("type");
                    } else {
                        log(Logger.ERROR, "The type of the property cannot be discovered, please add a 'type' attribute");
                        return;
                    }
                }
            }

            ConfigurableProperty cp = new ConfigurableProperty(name, fieldName, methodName, value, type, this);
            addProperty(cp);
        }

        if (configurables.length > 0) {
            for (int k = 0; k < m_configurableProperties.length; k++) {
                // Check if the instance configuration contains value for the
                // current property :
                String name = m_configurableProperties[k].getName();
                String fieldName = m_configurableProperties[k].getField();
                if (name != null && configuration.get(name) != null && !(configuration.get(name) instanceof String)) {
                    m_configurableProperties[k].setValue(configuration.get(name));
                } else {
                    if (fieldName != null && configuration.get(fieldName) != null && !(configuration.get(fieldName) instanceof String)) {
                        m_configurableProperties[k].setValue(configuration.get(fieldName));
                    }
                }
            }
            getInstanceManager().register(this, (FieldMetadata[]) ff.toArray(new FieldMetadata[0]), null);
        }
    }

    /**
      * Stop method.
      * Do nothing.
      * @see org.apache.felix.ipojo.Handler#stop()
      */
    public void stop() {
    }

    /**
     * Start method.
     * Propagate properties if the propagation is activated.
     * @see org.apache.felix.ipojo.Handler#start()
     */
    public void start() {
        // Get the provided service handler :
        m_providedServiceHandler = (ProvidedServiceHandler) getHandler(IPojoConfiguration.IPOJO_NAMESPACE + ":provides");

        // Propagation
        if (m_isConfigurable) {
            for (int i = 0; i < m_configurableProperties.length; i++) {
                m_toPropagate.put(m_configurableProperties[i].getName(), m_configurableProperties[i].getValue());
            }
            reconfigure(m_toPropagate);
        }
    }

    /**
     * Setter Callback Method.
     * Check if the modified field is a configurable property to update the value.
     * @param fieldName : field name
     * @param value : new value
     * @see org.apache.felix.ipojo.Handler#setterCallback(java.lang.String, java.lang.Object)
     */
    public void setterCallback(String fieldName, Object value) {
        // Verify that the field name correspond to a configurable property
        for (int i = 0; i < m_configurableProperties.length; i++) {
            ConfigurableProperty cp = m_configurableProperties[i];
            if (cp.getField().equals(fieldName)) {
                // Check if the value has changed
                if (cp.getValue() == null || !cp.getValue().equals(value)) {
                    cp.setValue(value); // Change the value
                }
            }
        }
        // Else do nothing
    }

    /**
     * Getter Callback Method.
     * Check if the field is a configurable property to push the stored value.
     * @param fieldName : field name
     * @param value : value pushed by the previous handler
     * @return the stored value or the previous value.
     * @see org.apache.felix.ipojo.Handler#getterCallback(java.lang.String,
     * java.lang.Object)
     */
    public Object getterCallback(String fieldName, Object value) {
        // Check if the field is a configurable property
        for (int i = 0; i < m_configurableProperties.length; i++) {
            if (m_configurableProperties[i].getField().equals(fieldName)) { return m_configurableProperties[i].getValue(); }
        }
        return value;
    }

    /**
     * Handler state changed.
     * @param state : the new instance state.
     * @see org.apache.felix.ipojo.CompositeHandler#stateChanged(int)
     */
    public void stateChanged(int state) {
        if (state == InstanceManager.VALID) {
            start();
            return;
        }
        if (state == InstanceManager.INVALID) {
            stop();
            return;
        }
    }

    /**
     * Add the given property metadata to the property metadata list.
     * 
     * @param p : property metadata to add
     */
    protected void addProperty(ConfigurableProperty p) {
        for (int i = 0; (m_configurableProperties != null) && (i < m_configurableProperties.length); i++) {
            if (m_configurableProperties[i].getName().equals(p.getName())) { return; }
        }

        if (m_configurableProperties.length > 0) {
            ConfigurableProperty[] newProp = new ConfigurableProperty[m_configurableProperties.length + 1];
            System.arraycopy(m_configurableProperties, 0, newProp, 0, m_configurableProperties.length);
            newProp[m_configurableProperties.length] = p;
            m_configurableProperties = newProp;
        } else {
            m_configurableProperties = new ConfigurableProperty[] { p };
        }
    }

    /**
     * Check if the list contains the property.
     * 
     * @param name : name of the property
     * @return true if the property exist in the list
     */
    protected boolean containsProperty(String name) {
        for (int i = 0; (m_configurableProperties != null) && (i < m_configurableProperties.length); i++) {
            if (m_configurableProperties[i].getName().equals(name)) { return true; }
        }
        return false;
    }

    /**
     * Reconfigure the component instance.
     * Check if the new configuration modify the current configuration.
     * @param np : the new configuration
     * @see org.apache.felix.ipojo.Handler#reconfigure(java.util.Dictionary)
     */
    public void reconfigure(Dictionary np) {
        Properties toPropagate = new Properties();
        Enumeration keysEnumeration = np.keys();
        while (keysEnumeration.hasMoreElements()) {
            String name = (String) keysEnumeration.nextElement();
            Object value = np.get(name);
            boolean found = false;
            // Check if the name is a configurable property
            for (int i = 0; !found && i < m_configurableProperties.length; i++) {
                if (m_configurableProperties[i].getName().equals(name)) {
                    // Check if the value has changed
                    if (m_configurableProperties[i].getValue() == null || !m_configurableProperties[i].getValue().equals(value)) {
                        if (m_configurableProperties[i].hasField()) {
                            getInstanceManager().setterCallback(m_configurableProperties[i].getField(), value); // dispatch that the value has changed
                        }
                        if (m_configurableProperties[i].hasMethod()) {
                            m_configurableProperties[i].setValue(value);
                            m_configurableProperties[i].invoke();
                        }
                    }
                    found = true;
                    // Else do nothing
                }
            }
            if (!found) {
                // The property is not a configurable property
                toPropagate.put(name, value);
            }
        }

        // Propagation of the properties to service registrations :
        if (m_providedServiceHandler != null && !toPropagate.isEmpty()) {
            m_providedServiceHandler.removeProperties(m_propagated);

            // Remove the name props
            toPropagate.remove("name");

            m_providedServiceHandler.addProperties(toPropagate);
            m_propagated = toPropagate;
        }
    }

    /**
     * Handler createInstance method.
     * This method is override to allow delayed callback invocation.
     * @param instance : the created object
     * @see org.apache.felix.ipojo.Handler#objectCreated(java.lang.Object)
     */
    public void objectCreated(Object instance) {
        for (int i = 0; i < m_configurableProperties.length; i++) {
            if (m_configurableProperties[i].hasMethod()) {
                m_configurableProperties[i].invoke(instance);
            }
        }
    }

}
