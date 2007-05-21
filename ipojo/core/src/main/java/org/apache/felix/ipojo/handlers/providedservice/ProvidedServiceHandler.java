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
package org.apache.felix.ipojo.handlers.providedservice;

import java.util.Dictionary;
import java.util.Properties;

import org.apache.felix.ipojo.Handler;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.architecture.ComponentDescription;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.Constants;

/**
 * Composite PRovided Service Handler.
 * This handler maange the service providing for a composition.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ProvidedServiceHandler extends Handler {

    /**
     * The list of the provided service.
     */
    private ProvidedService[] m_providedServices = new ProvidedService[0];

    /**
     * The instance manager.
     */
    private InstanceManager m_manager;

    /**
     * Add a provided service to the list .
     * 
     * @param ps : the provided service to add
     */
    private void addProvidedService(ProvidedService ps) {
        // Verify that the provided service is not already in the array.
        for (int i = 0; (m_providedServices != null) && (i < m_providedServices.length); i++) {
            if (m_providedServices[i] == ps) {
                return;
            }
        }

        if (m_providedServices.length > 0) {
            ProvidedService[] newPS = new ProvidedService[m_providedServices.length + 1];
            System.arraycopy(m_providedServices, 0, newPS, 0, m_providedServices.length);
            newPS[m_providedServices.length] = ps;
            m_providedServices = newPS;
        } else {
            m_providedServices = new ProvidedService[] { ps };
        }
    }

    /**
     * Get the instance manager.
     * @return the instance manager.
     */
    public InstanceManager getInstanceManager() {
        return m_manager;
    }

    /**
     * Get the array of provided service.
     * @return the list of the provided service.
     */
    public ProvidedService[] getProvidedService() {
        return m_providedServices;
    }

    /**
     * Configure the handler.
     * @param im : the instance manager
     * @param componentMetadata : the component type metadata
     * @param configuration : the instance configuration
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.InstanceManager, org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(InstanceManager im, Element componentMetadata, Dictionary configuration) {
        // Fix the instance manager & clean the provided service list
        m_manager = im;

        ComponentDescription cd = im.getComponentDescription();

        m_providedServices = new ProvidedService[0];
        // Create the dependency according to the component metadata
        Element[] providedServices = componentMetadata.getElements("Provides");
        Element manipulation = componentMetadata.getElements("Manipulation")[0];
        for (int i = 0; i < providedServices.length; i++) {
            // Create a ProvidedServiceMetadata object

            // First : create the serviceSpecification array
            String[] serviceSpecification = new String[0];
            if (providedServices[i].containsAttribute("interface")) {
                String serviceSpecificationStr = providedServices[i].getAttribute("interface");
                // Get serviceSpecification if exist in the metadata
                serviceSpecification = ParseUtils.parseArrays(serviceSpecificationStr);
            } else {
                serviceSpecification = new String[manipulation.getElements("Interface").length];
                for (int j = 0; j < manipulation.getElements("Interface").length; j++) {
                    serviceSpecification[j] = manipulation.getElements("Interface")[j].getAttribute("name");
                }
            }
            if (serviceSpecification.length == 0) {
                m_manager.getFactory().getLogger().log(Logger.ERROR,
                        "Cannot instantiate a provided service : no specifications found (no interfaces implemented by the pojo)");
                return;
            }

            // Get the factory policy
            int factory = ProvidedService.SINGLETON_FACTORY;
            if (providedServices[i].containsAttribute("factory") && providedServices[i].getAttribute("factory").equals("service")) {
                factory = ProvidedService.SERVICE_FACTORY;
            }

            // Then create the provided service
            ProvidedService ps = new ProvidedService(this, serviceSpecification, factory);

            Element[] props = providedServices[i].getElements("Property");
            Property[] properties = new Property[props.length];
            for (int j = 0; j < props.length; j++) {
                String name = null;
                if (props[j].containsAttribute("name")) {
                    name = props[j].getAttribute("name");
                }
                String value = null;
                if (props[j].containsAttribute("value")) {
                    value = props[j].getAttribute("value");
                }
                String type = null;
                if (props[j].containsAttribute("type")) {
                    type = props[j].getAttribute("type");
                }
                String field = null;
                if (props[j].containsAttribute("field")) {
                    field = props[j].getAttribute("field");
                }

                if (name != null && configuration.get(name) != null && configuration.get(name) instanceof String) {
                    value = (String) configuration.get(name);
                } else {
                    if (field != null && configuration.get(field) != null && configuration.get(field) instanceof String) {
                        value = (String) configuration.get(field);
                    }
                }

                Property prop = new Property(ps, name, field, type, value, manipulation);
                properties[j] = prop;
            }

            // Attached to properties to the provided service
            ps.setProperties(properties);

            if (checkProvidedService(ps, manipulation)) {
                addProvidedService(ps);
                // Change ComponentInfo
                for (int k = 0; k < ps.getServiceSpecification().length; k++) {
                    cd.addProvidedServiceSpecification(ps.getServiceSpecification()[k]);
                }
                for (int k = 0; k < ps.getProperties().length; k++) {
                    if (!ps.getProperties()[k].getName().equals(Constants.SERVICE_PID) && !ps.getProperties()[k].getName().equals("factory.pid")) {
                        cd.addProperty(new PropertyDescription(ps.getProperties()[k].getName(), ps.getProperties()[k].getType(), ps.getProperties()[k]
                                .getInitialValue()));
                    }
                }
            } else {
                String itfs = "";
                for (int j = 0; j < serviceSpecification.length; j++) {
                    itfs = itfs + " " + serviceSpecification[j];
                }
                m_manager.getFactory().getLogger().log(Logger.ERROR,
                        "[" + m_manager.getClassName() + "] The provided service" + itfs + " is not valid, it will be removed");
                ps = null;
            }

        }

        if (providedServices.length > 0) {
            String[] fields = new String[0];
            for (int i = 0; i < m_providedServices.length; i++) {
                ProvidedService ps = m_providedServices[i];
                for (int j = 0; j < ps.getProperties().length; j++) {
                    Property prop = ps.getProperties()[j];

                    // Check if the instance configuration has a value for this
                    // property
                    if (prop.getName() != null && configuration.get(prop.getName()) != null && !(configuration.get(prop.getName()) instanceof String)) {
                        prop.set(configuration.get(prop.getName()));
                    } else {
                        if (prop.getField() != null && configuration.get(prop.getField()) != null && !(configuration.get(prop.getField()) instanceof String)) {
                            prop.set(configuration.get(prop.getField()));
                        }
                    }
                    if (prop.getField() != null) {
                        String[] newFields = new String[fields.length + 1];
                        System.arraycopy(fields, 0, newFields, 0, fields.length);
                        newFields[fields.length] = prop.getField();
                        fields = newFields;
                    }
                }
            }

            m_manager.register(this, fields);
        }
    }

    /**
     * Check the provided service given in argument in the sense that the
     * metadata are consistent.
     * 
     * @param ps : the provided service to check.
     * @param manipulation : componenet-type manipulation metadata.
     * @return true if the provided service is correct
     */
    private boolean checkProvidedService(ProvidedService ps, Element manipulation) {

        for (int i = 0; i < ps.getServiceSpecification().length; i++) {
            boolean b = false;
            for (int ii = 0; ii < manipulation.getElements("Interface").length; ii++) {
                if (manipulation.getElements("Interface")[ii].getAttribute("name").equals(ps.getServiceSpecification()[i])) {
                    b = true;
                }
            }
            if (!b) {
                m_manager.getFactory().getLogger().log(
                        Logger.ERROR,
                        "[" + m_manager.getClassName() + "] The service specification " + ps.getServiceSpecification()[i]
                                + " is not implemented by the component class");
                return false;
            }

        }

        // Fix internal property type
        for (int i = 0; i < ps.getProperties().length; i++) {
            Property prop = ps.getProperties()[i];
            String field = prop.getField();

            if (field == null) {
                return true; // Static dependency -> Nothing to check
            } else {
                String type = null;
                for (int j = 0; j < manipulation.getElements("Field").length; j++) {
                    if (field.equals(manipulation.getElements("Field")[j].getAttribute("name"))) {
                        type = manipulation.getElements("Field")[j].getAttribute("type");
                        break;
                    }
                }
                if (type == null) {
                    m_manager.getFactory().getLogger().log(Logger.ERROR,
                            "[" + m_manager.getClassName() + "] A declared property was not found in the class : " + prop.getField());
                    return false;
                }
                prop.setType(type); // Set the type
            }
        }
        return true;
    }

    /**
     * Stop the provided service handler.
     * 
     * @see org.apache.felix.ipojo.Handler#stop()
     */
    public void stop() {
    }

    /**
     * Start the provided service handler.
     * 
     * @see org.apache.felix.ipojo.Handler#start()
     */
    public void start() {
    }

   /**
    * Setter Callback Method.
    * Check if the modified field is a property to update the value.
    * @param fieldName : field name
    * @param value : new value
     * @see org.apache.felix.ipojo.Handler#setterCallback(java.lang.String,
     * java.lang.Object)
     */
    public void setterCallback(String fieldName, Object value) {
        // Verify that the field name coreespond to a dependency
        for (int i = 0; i < m_providedServices.length; i++) {
            ProvidedService ps = m_providedServices[i];
            boolean update = false;
            for (int j = 0; j < ps.getProperties().length; j++) {
                Property prop = ps.getProperties()[j];
                if (fieldName.equals(prop.getField())) {
                    // it is the associated property
                    prop.set(value);
                    update = true;
                }
            }
            if (update) {
                ps.update();
            }
        }
        // Else do nothing
    }

    /**
     * Getter Callback Method.
     * Check if the field is a property to push the stored value.
     * @param fieldName : field name
     * @param value : value pushed by the previous handler
     * @return the stored value or the previous value.
     * @see org.apache.felix.ipojo.Handler#getterCallback(java.lang.String,
     * java.lang.Object)
     */
    public Object getterCallback(String fieldName, Object value) {
        for (int i = 0; i < m_providedServices.length; i++) {
            ProvidedService ps = m_providedServices[i];
            for (int j = 0; j < ps.getProperties().length; j++) {
                Property prop = ps.getProperties()[j];
                if (fieldName.equals(prop.getField())) {
                    // it is the associated property
                    return prop.get();
                }
            }
        }
        // Else it is not a property
        return value;
    }

    /**
     * Register the services if the new state is VALID. Unregister the services
     * if the new state is UNRESOLVED.
     * 
     * @param state : the new instance state.
     * @see org.apache.felix.ipojo.CompositeHandler#stateChanged(int)
     */
    public void stateChanged(int state) {
        // If the new state is UNRESOLVED => unregister all the services
        if (state == InstanceManager.INVALID) {
            for (int i = 0; i < m_providedServices.length; i++) {
                m_providedServices[i].unregisterService();
            }
            return;
        }

        // If the new state is VALID => regiter all the services
        if (state == InstanceManager.VALID) {
            for (int i = 0; i < m_providedServices.length; i++) {
                m_providedServices[i].registerService();
            }
            return;
        }

    }

    /**
     * Add properties to all provided services.
     * 
     * @param dict : dictionary fo properties to add
     */
    public void addProperties(Dictionary dict) {
        for (int i = 0; i < m_providedServices.length; i++) {
            m_providedServices[i].addProperties(dict);
            m_providedServices[i].update();
        }
    }

    /**
     * Remove properties form all provided services.
     * 
     * @param dict : dictionary of properties to delete.
     */
    public void removeProperties(Dictionary dict) {
        for (int i = 0; i < m_providedServices.length; i++) {
            m_providedServices[i].deleteProperties(dict);
            m_providedServices[i].update();
        }
    }

    /**
     * Build the provided service description.
     * @return the handler description.
     * @see org.apache.felix.ipojo.Handler#getDescription()
     */
    public HandlerDescription getDescription() {
        ProvidedServiceHandlerDescription pshd = new ProvidedServiceHandlerDescription(this.isValid());

        for (int j = 0; j < getProvidedService().length; j++) {
            ProvidedService ps = getProvidedService()[j];
            ProvidedServiceDescription psd = new ProvidedServiceDescription(ps.getServiceSpecification(), ps.getState(), ps.getServiceReference());

            Properties props = new Properties();
            for (int k = 0; k < ps.getProperties().length; k++) {
                Property prop = ps.getProperties()[k];
                if (prop.getValue() != null) {
                    props.put(prop.getName(), prop.getValue().toString());
                }
            }
            psd.setProperty(props);
            pshd.addProvidedService(psd);
        }
        return pshd;
    }

    /**
     * Reconfigure provided service.
     * @param dict : the new instance configuration.
     * @see org.apache.felix.ipojo.Handler#reconfigure(java.util.Dictionary)
     */
    public void reconfigure(Dictionary dict) {
        for (int j = 0; j < getProvidedService().length; j++) {
            ProvidedService ps = getProvidedService()[j];
            Property[] props = ps.getProperties();
            boolean update = false;
            for (int k = 0; k < props.length; k++) {
                if (dict.get(props[k].getName()) != null) {
                    update = true;
                    if (dict.get(props[k].getName()) instanceof String) {
                        props[k].set((String) dict.get(props[k].getName()));
                    } else {
                        props[k].set(dict.get(props[k].getName()));
                    }
                }
            }
            if (update) {
                ps.update();
            }
        }
    }

}
