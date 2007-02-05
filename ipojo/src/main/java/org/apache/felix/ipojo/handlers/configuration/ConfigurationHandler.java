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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.felix.ipojo.Handler;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.architecture.ComponentDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandler;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * Handler managing the Configuration Admin.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ConfigurationHandler extends Handler implements ManagedService {

    /**
     * Reference on the instance manager.
     */
    private InstanceManager m_manager;

    /**
     * List of the configurable fields.
     */
    private ConfigurableProperty[] m_configurableProperties = new ConfigurableProperty[0];

    /**
     * ProvidedServiceHandler of the component.
     * It is useful to priopagate properties to service registrations.
     */
    private ProvidedServiceHandler m_providedServiceHandler;

    /**
     * Properties propagated at the last "updated".
     */
    private Dictionary m_propagated = new Properties();

    /**
     * PID of the component.
     */
    private String m_pid;

    /**
     * should the component provided ManagedService ?
     */
    private boolean m_isConfigurable;

    /**
     * Service registration of the ManagedService provided by this handler.
     */
    private ServiceRegistration m_sr;

    /**
     * @return instance manager of this handler.
     */
    protected InstanceManager getInstanceManager() { return m_manager; }

    /**
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.InstanceManager, org.apache.felix.ipojo.metadata.Element)
     */
    public void configure(InstanceManager im, Element metadata, Dictionary configuration) {
        // Store the component manager
        m_manager = im;
        ComponentDescription cd = im.getComponentDescription();
        m_configurableProperties = new ConfigurableProperty[0];

        // Build the hashmap
        Element[] confs = metadata.getElements("Properties", "");
        if (confs.length == 0) { return; }

        // Check if the component is dynamically configurable
        m_isConfigurable = false;
        if (confs[0].containsAttribute("configurable") && confs[0].getAttribute("configurable").equalsIgnoreCase("true")) { m_isConfigurable = true; }

        Element[] configurables = confs[0].getElements("Property");

        for (int i = 0; i < configurables.length; i++) {
            String fieldName = configurables[i].getAttribute("field");
            String name = null;
            if (configurables[i].containsAttribute("name")) { name = configurables[i].getAttribute("name"); }
            else { name = fieldName; }
            String value = null;
            if (configurables[i].containsAttribute("value")) { value = configurables[i].getAttribute("value"); }

            if (name != null && configuration.get(name) != null && configuration.get(name) instanceof String) { value = (String) configuration.get(name); }
            else { if (fieldName != null &&  configuration.get(fieldName) != null && configuration.get(fieldName) instanceof String) { value = (String) configuration.get(fieldName); } }

            // Detect the type of the property 
            Element manipulation = metadata.getElements("Manipulation")[0];
            String type = null;
            for (int kk = 0; kk < manipulation.getElements("Field").length; kk++) {
                if (fieldName.equals(manipulation.getElements("Field")[kk].getAttribute("name"))) {
                    type = manipulation.getElements("Field")[kk].getAttribute("type");
                }
            }
            if (type == null) { m_manager.getFactory().getLogger().log(Logger.ERROR, "[" + m_manager.getClassName() + "] The field " + fieldName + " does not exist in the implementation"); return; }
            
            ConfigurableProperty cp = new ConfigurableProperty(name, fieldName, value, type, this);
            
            if (cp.getValue() != null) { cd.addProperty(new PropertyDescription(name, type, cp.getValue().toString())); }
            else { cd.addProperty(new PropertyDescription(name, type, "")); }

            addProperty(cp);
        }

        // Get the PID in the configuration name :
        if (configuration.get("name") != null) { m_pid = (String) configuration.get("name"); }
        else { m_pid = metadata.getAttribute("className"); }

        // Get the provided service handler :
        m_providedServiceHandler = (ProvidedServiceHandler) m_manager.getHandler(ProvidedServiceHandler.class.getName());

        if (configurables.length > 0) {
            String[] fields = new String[m_configurableProperties.length];
            for (int k = 0; k < m_configurableProperties.length; k++) {
                fields[k] = m_configurableProperties[k].getField();

                // Check if the instance configuration contains value for the current property :
                String name = m_configurableProperties[k].getName();
                String fieldName = m_configurableProperties[k].getField();
                if (name != null && configuration.get(name) != null && !(configuration.get(name) instanceof String)) { m_configurableProperties[k].setValue(configuration.get(name)); }
                else { if (fieldName != null &&  configuration.get(fieldName) != null && !(configuration.get(fieldName) instanceof String)) { m_configurableProperties[k].setValue(configuration.get(fieldName)); } }
            }
            m_manager.register(this, fields);
        }
        else { return; }
    }

    /**
     * @see org.apache.felix.ipojo.Handler#stop()
     */
    public void stop() {
        // Unregister the service
        if (m_isConfigurable && m_sr != null) {
            m_manager.getFactory().getLogger().log(Logger.INFO, "[" + m_manager.getClassName() + "] Unregister Managed Service");
            m_sr.unregister();
            m_sr = null;
        }

    }

    /**
     * @see org.apache.felix.ipojo.Handler#start()
     */
    public void start() {
        // Unregister the service if already registred (it should not happen )
        if (m_isConfigurable && m_sr != null) { m_sr.unregister(); }

        // Register the ManagedService
        if (m_isConfigurable) {
            BundleContext bc = m_manager.getContext();
            Dictionary properties = new Properties();
            properties.put(Constants.SERVICE_PID, m_pid);

            m_manager.getFactory().getLogger().log(Logger.INFO, "[" + m_manager.getClassName() + "] Register Managed Service");
            m_sr = bc.registerService(ManagedService.class.getName(), this, properties);
        }
    }

    /**
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
        //Else do nothing
    }

    /**
     * @see org.apache.felix.ipojo.Handler#getterCallback(java.lang.String, java.lang.Object)
     */
    public Object getterCallback(String fieldName, Object value) {
        // Check if the field is a configurable property
        for (int i = 0; i < m_configurableProperties.length; i++) {
            if (m_configurableProperties[i].getField().equals(fieldName)) {
                return m_configurableProperties[i].getValue();
            }
        }
        return value;
    }

    /**
     * @see org.apache.felix.ipojo.Handler#stateChanged(int)
     */
    public void stateChanged(int state) {
        if (state == InstanceManager.VALID) {
            if (m_sr == null) { start(); }
            return;
        }
        if (state == InstanceManager.INVALID) {
            if (m_sr != null) { stop(); }
            return;
        }
    }

    /**
     * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
     */
    public void updated(Dictionary np) throws ConfigurationException {
        Properties toPropagate = new Properties();
        if (np != null) {
            Enumeration keysEnumeration = np.keys();
            while (keysEnumeration.hasMoreElements()) {
                String name = (String) keysEnumeration.nextElement();
                Object value = np.get(name);
                boolean find = false;
                // Check if the field is a configurable property
                for (int i = 0; !find && i < m_configurableProperties.length; i++) {
                    if (m_configurableProperties[i].getName().equals(name)) {
                        // Check if the value has change
                        if (m_configurableProperties[i].getValue() == null || !m_configurableProperties[i].getValue().equals(value)) {
                            m_manager.setterCallback(m_configurableProperties[i].getField(), value); // says that the value has change
                        }
                        find = true;
                        // Else do nothing
                    }
                }
                if (!find) {
                    //The property is not a configurable property
                    m_manager.getFactory().getLogger().log(Logger.INFO, "[" + m_manager.getClassName() + "] The property " + name + " will be propagated to service registrations");
                    toPropagate.put(name, value);
                }
            }
        }
        else { m_manager.getFactory().getLogger().log(Logger.WARNING, "[" + m_manager.getClassName() + "] The pushed configuration is null for " + m_pid); }

        // Propagation of the properties to service registrations :
        if (m_providedServiceHandler != null && !toPropagate.isEmpty()) {
            m_manager.getFactory().getLogger().log(Logger.INFO, "[" + m_manager.getClassName() + "] Properties will be propagated");
            m_providedServiceHandler.removeProperties(m_propagated);
            m_providedServiceHandler.addProperties(toPropagate);
            m_propagated = toPropagate;
        }

    }

    /**
     * Add the given property metadata to the property metadata list.
     * @param p : property metdata to add
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
        }
        else {
            m_configurableProperties = new ConfigurableProperty[] {p};
        }
    }

    /**
     * Check if the liste contains the property.
     * @param name : name of the property
     * @return true if the property exist in the list
     */
    protected boolean containsProperty(String name) {
        for (int i = 0; (m_configurableProperties != null) && (i < m_configurableProperties.length); i++) {
            if (m_configurableProperties[i].getName().equals(name)) { return true; }
        }
        return false;
    }

}
