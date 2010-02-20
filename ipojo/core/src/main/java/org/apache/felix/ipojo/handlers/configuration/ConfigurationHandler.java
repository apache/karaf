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
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandler;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.parser.PojoMetadata;
import org.apache.felix.ipojo.util.Callback;
import org.apache.felix.ipojo.util.Property;
import org.apache.felix.ipojo.util.SecurityHelper;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;

/**
 * Handler managing the Configuration Admin.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ConfigurationHandler extends PrimitiveHandler implements ManagedService {

    /**
     * List of the configurable fields.
     */
    private List/*<Property>*/ m_configurableProperties = new ArrayList(1);

    /**
     * ProvidedServiceHandler of the component. It is useful to propagate
     * properties to service registrations.
     */
    private ProvidedServiceHandler m_providedServiceHandler;

    /**
     * Properties propagated during the last instance "update".
     */
    private Dictionary m_propagatedFromInstance = new Properties();

    /**
     * Properties to propagate.
     */
    private Dictionary m_toPropagate = new Properties();

    /**
     * Properties propagated from the configuration admin.
     */
    private Dictionary m_propagatedFromCA;

    /**
     * Check if the instance was already reconfigured by the configuration admin.
     */
    private boolean m_configurationAlreadyPushed;

    /**
     * should the component propagate configuration ?
     */
    private boolean m_mustPropagate;

    /**
     * Service Registration to publish the service registration.
     */
    private ServiceRegistration m_sr;

    /**
     * Managed Service PID.
     * This PID must be different from the instance name if the instance was created
     * with the Configuration Admin.
     */
    private String m_managedServicePID;

    /**
     * the handler description.
     */
    private ConfigurationHandlerDescription m_description;

    /**
     * Updated method.
     * This method is called when a reconfiguration is completed.
     */
    private Callback m_updated;


    /**
     * Initialize the component type.
     * @param desc : component type description to populate.
     * @param metadata : component type metadata.
     * @throws ConfigurationException : metadata are incorrect.
     * @see org.apache.felix.ipojo.Handler#initializeComponentFactory(org.apache.felix.ipojo.architecture.ComponentTypeDescription, org.apache.felix.ipojo.metadata.Element)
     */
    public void initializeComponentFactory(ComponentTypeDescription desc, Element metadata) throws ConfigurationException {
        Element[] confs = metadata.getElements("Properties", "");
        if (confs == null) { return; }
        Element[] configurables = confs[0].getElements("Property");
        for (int i = 0; configurables != null && i < configurables.length; i++) {
            String fieldName = configurables[i].getAttribute("field");
            String methodName = configurables[i].getAttribute("method");

            if (fieldName == null && methodName == null) {
                throw new ConfigurationException("Malformed property : The property needs to contain at least a field or a method");
            }

            String name = configurables[i].getAttribute("name");
            if (name == null) {
                if (fieldName == null) {
                    name = methodName;
                } else {
                    name = fieldName;
                }
                configurables[i].addAttribute(new Attribute("name", name)); // Add the type to avoid configure checking
            }

            String value = configurables[i].getAttribute("value");

            // Detect the type of the property
            PojoMetadata manipulation = getFactory().getPojoMetadata();
            String type = null;
            if (fieldName == null) {
                MethodMetadata[] method = manipulation.getMethods(methodName);
                if (method.length == 0) {
                    type = configurables[i].getAttribute("type");
                    if (type == null) {
                        throw new ConfigurationException("Malformed property : The type of the property cannot be discovered, add a 'type' attribute");
                    }
                } else {
                    if (method[0].getMethodArguments().length != 1) {
                        throw new ConfigurationException("Malformed property :  The method " + methodName + " does not have one argument");
                    }
                    type = method[0].getMethodArguments()[0];
                    configurables[i].addAttribute(new Attribute("type", type)); // Add the type to avoid configure checking
                }
            } else {
                FieldMetadata field = manipulation.getField(fieldName);
                if (field == null) { throw new ConfigurationException("Malformed property : The field " + fieldName + " does not exist in the implementation class"); }
                type = field.getFieldType();
                configurables[i].addAttribute(new Attribute("type", type)); // Add the type to avoid configure checking
            }

            // Is the property set to immutable
            boolean immutable = false;
            String imm = configurables[i].getAttribute("immutable");
            immutable = imm != null && imm.equalsIgnoreCase("true");

            boolean mandatory = false;
            String man = configurables[i].getAttribute("mandatory");
            mandatory =  man != null && man.equalsIgnoreCase("true");

            PropertyDescription pd = null;
            if (value == null) {
                pd = new PropertyDescription(name, type, null, false); // Cannot be immutable if we have no value.
            } else {
                pd = new PropertyDescription(name, type, value, immutable);
            }

            if (mandatory) {
                pd.setMandatory();
            }

            desc.addProperty(pd);
        }

    }

    /**
     * Configures the handler.
     * Access to field does not require synchronization as this method is executed
     * before any thread access to this object.
     * @param metadata the metadata of the component
     * @param configuration the instance configuration
     * @throws ConfigurationException one property metadata is not correct
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.InstanceManager,
     * org.apache.felix.ipojo.metadata.Element)
     */
    public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {
        // Build the map
        Element[] confs = metadata.getElements("Properties", "");
        Element[] configurables = confs[0].getElements("Property");

        // Check if the component is dynamically configurable
        m_mustPropagate = false;
        String propa = confs[0].getAttribute("propagation");
        if (propa != null && propa.equalsIgnoreCase("true")) {
            m_mustPropagate = true;
            m_toPropagate = configuration; // Instance configuration to propagate.
        }

        // Check if the component support ConfigurationADmin reconfiguration
        m_managedServicePID = confs[0].getAttribute("pid"); // Look inside the component type description
        String instanceMSPID = (String) configuration.get("managed.service.pid"); // Look inside the instance configuration.
        if (instanceMSPID != null) {
            m_managedServicePID = instanceMSPID;
        }

        // updated method
        String upd = confs[0].getAttribute("updated");
        if (upd != null) {
            m_updated = new Callback(upd, new Class[] {Dictionary.class}, false, getInstanceManager());
        }

        for (int i = 0; configurables != null && i < configurables.length; i++) {
            String fieldName = configurables[i].getAttribute("field");
            String methodName = configurables[i].getAttribute("method");

            String name = configurables[i].getAttribute("name"); // The initialize method has fixed the property name.
            String value = configurables[i].getAttribute("value");

            String type = configurables[i].getAttribute("type"); // The initialize method has fixed the property name.

            Property prop = new Property(name, fieldName, methodName, value, type, getInstanceManager(), this);
            addProperty(prop);

            // Check if the instance configuration contains value for the current property :
            if (configuration.get(name) == null) {
                if (fieldName != null && configuration.get(fieldName) != null) {
                    prop.setValue(configuration.get(fieldName));
                }
            } else {
                prop.setValue(configuration.get(name));
            }

            if (fieldName != null) {
                FieldMetadata field = new FieldMetadata(fieldName, type);
                getInstanceManager().register(field, prop);
            }
        }

        m_description = new ConfigurationHandlerDescription(this, m_configurableProperties, m_managedServicePID);

    }

    /**
      * Stop method.
      * This method is synchronized to avoid the configuration admin pushing a configuration during the un-registration.
      * Do nothing.
      * @see org.apache.felix.ipojo.Handler#stop()
      */
    public synchronized void stop() {
        if (m_sr != null) {
            m_sr.unregister();
            m_sr = null;
        }
    }

    /**
     * Start method.
     * This method is synchronized to avoid the config admin pushing a configuration before ending the method.
     * Propagate properties if the propagation is activated.
     * @see org.apache.felix.ipojo.Handler#start()
     */
    public synchronized void start() {
        // Get the provided service handler :
        m_providedServiceHandler = (ProvidedServiceHandler) getHandler(HandlerFactory.IPOJO_NAMESPACE + ":provides");

        // Propagation
        if (m_mustPropagate) {
            for (int i = 0; i < m_configurableProperties.size(); i++) {
                Property prop = (Property) m_configurableProperties.get(i);
                if (prop.getValue() != Property.NO_VALUE && prop.getValue() != null) { // No injected value, or null
                    m_toPropagate.put(prop.getName(), prop.getValue());
                }
            }
            reconfigure(m_toPropagate);
        }

        if (m_managedServicePID != null && m_sr == null) {
            Properties props = new Properties();
            props.put(Constants.SERVICE_PID, m_managedServicePID);
            props.put("instance.name", getInstanceManager().getInstanceName());
            props.put("factory.name", getInstanceManager().getFactory().getFactoryName());
            
            // Security Check
            if (SecurityHelper.hasPermissionToRegisterService(ManagedService.class.getName(), 
                    getInstanceManager().getContext())) {
                m_sr = getInstanceManager().getContext().registerService(ManagedService.class.getName(), this, props);
            } else {
                error("Cannot register the ManagedService - The bundle " 
                        + getInstanceManager().getContext().getBundle().getBundleId() 
                        + " does not have the permission to register the service");
            }
        }
    }

    /**
     * Adds the given property metadata to the property metadata list.
     *
     * @param prop : property metadata to add
     */
    protected void addProperty(Property prop) {
        m_configurableProperties.add(prop);
    }

    /**
     * Checks if the list contains the property.
     *
     * @param name : name of the property
     * @return true if the property exist in the list
     */
    protected boolean containsProperty(String name) {
        for (int i = 0; i < m_configurableProperties.size(); i++) {
            if (((Property) m_configurableProperties.get(i)).getName().equals(name)) { return true; }
        }
        return false;
    }

    /**
     * Reconfigure the component instance.
     * Check if the new configuration modifies the current configuration.
     * Invokes the updated method is needed.
     * @param configuration : the new configuration
     * @see org.apache.felix.ipojo.Handler#reconfigure(java.util.Dictionary)
     */
    public synchronized void reconfigure(Dictionary configuration) {
        info(getInstanceManager().getInstanceName() + " is reconfiguring the properties : " + configuration);
        Properties props = reconfigureProperties(configuration);
        propagate(props, m_propagatedFromInstance);
        m_propagatedFromInstance = props;

        if (getInstanceManager().getPojoObjects() != null) {
            try {
                notifyUpdated(null);
            } catch (Throwable e) {
                error("Cannot call the updated method : " + e.getMessage(), e);
            }
        }
    }

    /**
     * Reconfigured configuration properties and returns non matching properties.
     * When called, it must hold the monitor lock.
     * @param configuration : new configuration
     * @return the properties that does not match with configuration properties
     */
    private Properties reconfigureProperties(Dictionary configuration) {
        Properties toPropagate = new Properties();
        Enumeration keysEnumeration = configuration.keys();
        while (keysEnumeration.hasMoreElements()) {
            String name = (String) keysEnumeration.nextElement();
            Object value = configuration.get(name);
            boolean found = false;
            // Check if the name is a configurable property
            for (int i = 0; i < m_configurableProperties.size(); i++) {
                Property prop = (Property) m_configurableProperties.get(i);
                if (prop.getName().equals(name)) {
                    reconfigureProperty(prop, value);
                    found = true;
                    break; // Exit the search loop
                }
            }
            if (!found) {
                // The property is not a configurable property, aadd it to the toPropagate list.
                toPropagate.put(name, value);
            }
        }

        return toPropagate;

    }

    /**
     * Reconfigures the given property with the given value.
     * This methods handles {@link InstanceManager#onSet(Object, String, Object)}
     * call and the callback invocation.
     * The reconfiguration occurs only if the value changes.
     * @param prop the property object to reconfigure
     * @param value the new value.
     */
    public void reconfigureProperty(Property prop, Object value) {
        if (prop.getValue() == null || ! prop.getValue().equals(value)) {
            prop.setValue(value);
            if (prop.hasField()) {
                getInstanceManager().onSet(null, prop.getField(), prop.getValue()); // Notify other handler of the field value change.
            }
            if (prop.hasMethod()) {
                if (getInstanceManager().getPojoObjects() != null) {
                    prop.invoke(null); // Call on all created pojo objects.
                }
            }
        }
    }

    /**
     * Removes the old properties from the provided services and propagate new properties.
     * @param newProps : new properties to propagate
     * @param oldProps : old properties to remove
     */
    private void propagate(Dictionary newProps, Dictionary oldProps) {
        if (m_mustPropagate && m_providedServiceHandler != null) {
            if (oldProps != null) {
                m_providedServiceHandler.removeProperties(oldProps);
            }

            if (newProps != null) {
                // Remove the name, the pid and the managed service pid props
                newProps.remove("name");
                newProps.remove("managed.service.pid");
                newProps.remove(Constants.SERVICE_PID);
                // Propagation of the properties to service registrations :
                m_providedServiceHandler.addProperties(newProps);
            }
        }
    }

    /**
     * Handler createInstance method.
     * This method is override to allow delayed callback invocation.
     * Invokes the updated method is needed.
     * @param instance : the created object
     * @see org.apache.felix.ipojo.Handler#onCreation(java.lang.Object)
     */
    public void onCreation(Object instance) {
        for (int i = 0; i < m_configurableProperties.size(); i++) {
            Property prop = (Property) m_configurableProperties.get(i);
            if (prop.hasMethod()) {
                prop.invoke(instance);
            }
        }

        try {
            notifyUpdated(instance);
        } catch (Throwable e) {
            error("Cannot call the updated method : " + e.getMessage(), e);
        }
    }

    /**
     * Invokes the updated method.
     * This method build the dictionary containing all valued properties,
     * as well as properties propagated to the provided service handler (
     * only if the propagation is enabled).
     * @param instance the instance on which the callback must be called.
     * If <code>null</code> the callback is called on all the existing
     * object.
     */
    private void notifyUpdated(Object instance) {
        if (m_updated == null) {
            return;
        }
        Properties props = new Properties();
        for (int i = 0; i < m_configurableProperties.size(); i++) {
            String n = ((Property) m_configurableProperties.get(i)).getName();
            Object v = ((Property) m_configurableProperties.get(i)).getValue();
            if (v != Property.NO_VALUE) {
                props.put(n, v);
            }
        }
        // add propagated properties to the list if propagation enable
        if (m_mustPropagate) {
            // Start by properties from the configuration admin,
            if (m_propagatedFromCA != null) {

                Enumeration e = m_propagatedFromCA.keys();
                while (e.hasMoreElements()) {
                    String k = (String) e.nextElement();
                    if (! k.equals("instance.name")) {
                        props.put(k, m_propagatedFromCA.get(k));
                    }
                }
            }
            // Do also the one from the instance configuration
            if (m_propagatedFromInstance != null) {
                Enumeration e = m_propagatedFromInstance.keys();
                while (e.hasMoreElements()) {
                    String k = (String) e.nextElement();
                    if (! k.equals("instance.name")) { // Skip instance.name
                        props.put(k, m_propagatedFromInstance.get(k));
                    }
                }
            }
        }

        try {
            if (instance == null) {
                m_updated.call(new Object[] {props});
            } else {
                m_updated.call(instance, new Object[] {props});
            }
        } catch (Exception e) {
            error("Cannot call the updated method " + m_updated.getMethod() + " : " + e.getMessage());
        }
    }

    /**
     * Managed Service method.
     * This method is called when the instance is reconfigured by the ConfigurationAdmin.
     * When called, it must hold the monitor lock.
     * @param conf : pushed configuration.
     * @throws org.osgi.service.cm.ConfigurationException the reconfiguration failed.
     * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
     */
    public synchronized void updated(Dictionary conf) throws org.osgi.service.cm.ConfigurationException {
        if (conf == null && ! m_configurationAlreadyPushed) {
            return; // First call
        } else if (conf != null) { // Configuration push
            Properties props = reconfigureProperties(conf);
            propagate(props, m_propagatedFromCA);
            m_propagatedFromCA = props;
            m_configurationAlreadyPushed = true;
        } else if (m_configurationAlreadyPushed) { // Configuration deletion
            propagate(null, m_propagatedFromCA);
            m_propagatedFromCA = null;
            m_configurationAlreadyPushed = false;
        }

        if (getInstanceManager().getPojoObjects() != null) {
            try {
                notifyUpdated(null);
            } catch (Throwable e) {
                error("Cannot call the updated method : " + e.getMessage(), e);
            }
        }
    }

    /**
     * Gets the configuration handler description.
     * @return the configuration handler description.
     * @see org.apache.felix.ipojo.Handler#getDescription()
     */
    public HandlerDescription getDescription() {
        return m_description;
    }
    

}
