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
package org.apache.felix.ipojo;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedServiceFactory;

/**
 * This class abstracts iPOJO factories.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class IPojoFactory implements Factory, ManagedServiceFactory {
    /*
     * TODO there is potentially an issue when calling FactoryStateListener callbacks with the lock
     * It should be called by a separate thread dispatching events to listeners.
     */

    /**
     * List of the managed instance name. This list is shared by all factories.
     */
    protected static List m_instancesName = new ArrayList();

    /**
     * Component-Type description exposed by the factory service.
     */
    protected ComponentTypeDescription m_componentDesc;

    /**
     * List of the managed instance managers. The key of this map is the name (i.e. instance names) of the created instance
     */
    protected final Map m_componentInstances = new HashMap();

    /**
     * Component Type provided by this factory.
     */
    protected final Element m_componentMetadata;

    /**
     * The bundle context reference.
     */
    protected final BundleContext m_context;

    /**
     * Factory Name. Could be the component class name if the factory name is not set.
     * Immutable once set.
     */
    protected String m_factoryName;

    /**
     * List of required handler.
     */
    protected List m_requiredHandlers = new ArrayList();

    /**
     * List of listeners.
     */
    protected List m_listeners = new ArrayList(1);

    /**
     * Logger for the factory (and all component instance).
     */
    protected final Logger m_logger;

    /**
     * Is the factory public (expose as a service).
     */
    protected final boolean m_isPublic;

    /**
     * Service Registration of this factory (Factory & ManagedServiceFactory).
     */
    protected ServiceRegistration m_sr;

    /**
     * Factory state.
     */
    protected int m_state = Factory.INVALID;

    /**
     * Index used to generate instance name if not set.
     */
    private long m_index = 0;

    /**
     * Flag indicating if this factory has already a computed description or not.
     */
    private boolean m_described;

    /**
     * Constructor.
     * @param context : bundle context of the bundle containing the factory.
     * @param metadata : description of the component type.
     * @throws ConfigurationException occurs when the element describing the factory is malformed.
     */
    public IPojoFactory(BundleContext context, Element metadata) throws ConfigurationException {
        m_context = context;
        m_componentMetadata = metadata;
        m_factoryName = getFactoryName();
        String fac = metadata.getAttribute("factory");
        m_isPublic = fac == null || !fac.equalsIgnoreCase("false");
        m_logger = new Logger(m_context, m_factoryName);
        m_requiredHandlers = getRequiredHandlerList(); // Call sub-class to get the list of required handlers.
    }

    public ComponentTypeDescription getComponentTypeDescription() {
        return new ComponentTypeDescription(this);
    }

    /**
     * Adds a factory listener.
     * @param listener : the factory listener to add.
     * @see org.apache.felix.ipojo.Factory#addFactoryStateListener(org.apache.felix.ipojo.FactoryStateListener)
     */
    public void addFactoryStateListener(FactoryStateListener listener) {
        synchronized (this) {
            m_listeners.add(listener);
        }
    }

    /**
     * Gets the logger used by instances of he current factory.
     * @return the factory logger.
     */
    public Logger getLogger() {
        return m_logger;
    }

    /**
     * Computes the factory name.
     * @return the factory name.
     */
    public abstract String getFactoryName();

    /**
     * Computes the required handler list.
     * @return the required handler list
     */
    public abstract List getRequiredHandlerList();

    /**
     * Creates an instance.
     * This method is called with the lock.
     * @param config : instance configuration
     * @param context : ipojo context to use
     * @param handlers : handler array to use
     * @return the new component instance.
     * @throws ConfigurationException : occurs when the instance creation failed during the configuration process.
     */
    public abstract ComponentInstance createInstance(Dictionary config, IPojoContext context, HandlerManager[] handlers)
            throws ConfigurationException;

    /**
     * Creates an instance. The given configuration needs to contain the 'name' property.
     * @param configuration : configuration of the created instance.
     * @return the created component instance.
     * @throws UnacceptableConfiguration : occurs if the given configuration is not consistent with the component type of this factory.
     * @throws MissingHandlerException : occurs if an handler is unavailable when the instance is created.
     * @throws org.apache.felix.ipojo.ConfigurationException : occurs when the instance or type configuration are not correct.
     * @see org.apache.felix.ipojo.Factory#createComponentInstance(java.util.Dictionary)
     */
    public ComponentInstance createComponentInstance(Dictionary configuration) throws UnacceptableConfiguration, MissingHandlerException,
            ConfigurationException {
        return createComponentInstance(configuration, null);
    }

    /**
     * Creates an instance. The given configuration needs to contain the 'name' property.
     * This method is synchronized to assert the validity of the factory during the creation.
     * Callbacks to sub-class and to create instance need to be aware that they are holding the lock.
     * @param configuration : configuration of the created instance.
     * @param serviceContext : the service context to push for this instance.
     * @return the created component instance.
     * @throws UnacceptableConfiguration : occurs if the given configuration is not consistent with the component type of this factory.
     * @throws MissingHandlerException : occurs when an handler is unavailable when creating the instance.
     * @throws org.apache.felix.ipojo.ConfigurationException : when the instance configuration failed.
     * @see org.apache.felix.ipojo.Factory#createComponentInstance(java.util.Dictionary)
     */
    public synchronized ComponentInstance createComponentInstance(Dictionary configuration, ServiceContext serviceContext) throws UnacceptableConfiguration, // NOPMD
            MissingHandlerException, ConfigurationException {
        if (configuration == null) {
            configuration = new Properties();
        }
        
        IPojoContext context = null;
        if (serviceContext == null) {
            context = new IPojoContext(m_context);
        } else {
            context = new IPojoContext(m_context, serviceContext);
        }

        try {
            checkAcceptability(configuration);
        } catch (UnacceptableConfiguration e) {
            m_logger.log(Logger.ERROR, "The configuration is not acceptable : " + e.getMessage());
            throw new UnacceptableConfiguration("The configuration "
                    + configuration + " is not acceptable for " + m_factoryName
                    + ": " + e.getMessage());
        }

        String name;
        if (configuration.get("name") == null) {
            name = generateName();
            configuration.put("name", name);
        } else {
            name = (String) configuration.get("name");
            if (m_instancesName.contains(name)) {
                m_logger.log(Logger.ERROR, "The configuration is not acceptable : Name already used");
                throw new UnacceptableConfiguration("Name already used : " + name);
            }
        }
        // Here we are sure to be valid until the end of the method.
        HandlerManager[] handlers = new HandlerManager[m_requiredHandlers.size()];
        for (int i = 0; i < handlers.length; i++) {
            RequiredHandler req = (RequiredHandler) m_requiredHandlers.get(i);
            handlers[i] = getHandler(req, serviceContext);
        }

        try {
            ComponentInstance instance = createInstance(configuration, context, handlers); // This method is called with the lock.
            m_instancesName.add(name);
            m_componentInstances.put(name, instance);
            return instance;
        } catch (ConfigurationException e) {
            m_logger.log(Logger.ERROR, e.getMessage());
            throw new ConfigurationException(e.getMessage(), m_factoryName);
        }
    }

    public BundleContext getBundleContext() {
        return m_context;
    }

    /**
     * Gets the factory class name.
     * @return the factory class name.
     * @see org.apache.felix.ipojo.Factory#getClassName()
     */
    public abstract String getClassName();

    /**
     * Gets the component type description.
     * @return the component type description object. Null if not already computed.
     */
    public synchronized ComponentTypeDescription getComponentDescription() {
        return m_componentDesc;
    }

    /**
     * Gets the component type description (Element-Attribute form).
     * @return the component type description.
     * @see org.apache.felix.ipojo.Factory#getDescription()
     */
    public synchronized Element getDescription() {
        // Can be null, if not already computed.
        if (m_componentDesc == null) {
            return new Element("No description available for " + m_factoryName, "");
        }
        return m_componentDesc.getDescription();
    }

    /**
     * Computes the list of missing handlers. This method is called with the lock.
     * @return list of missing handlers.
     * @see org.apache.felix.ipojo.Factory#getMissingHandlers()
     */
    public List getMissingHandlers() {
        List list = new ArrayList();
        for (int i = 0; i < m_requiredHandlers.size(); i++) {
            RequiredHandler req = (RequiredHandler) m_requiredHandlers.get(i);
            if (req.getReference() == null) {
                list.add(req.getFullName());
            }
        }
        return list;
    }

    /**
     * Gets the factory name.
     * This name is immutable once set.
     * @return the factory name.
     * @see org.apache.felix.ipojo.Factory#getName()
     */
    public String getName() {
        return m_factoryName;
    }

    /**
     * Gets the list of required handlers.
     * This method is synchronized to avoid the concurrent modification
     * of the required handlers.
     * @return list of required handlers.
     * @see org.apache.felix.ipojo.Factory#getRequiredHandlers()
     */
    public synchronized List getRequiredHandlers() {
        List list = new ArrayList();
        for (int i = 0; i < m_requiredHandlers.size(); i++) {
            RequiredHandler req = (RequiredHandler) m_requiredHandlers.get(i);
            list.add(req.getFullName());
        }
        return list;
    }

    /**
     * Gets the actual factory state.
     * Must be synchronized as this state is dependent of handler availability.
     * @return the actual factory state.
     * @see org.apache.felix.ipojo.Factory#getState()
     */
    public synchronized int getState() {
        return m_state;
    }

    /**
     * Checks if the configuration is acceptable.
     * @param conf : the configuration to test.
     * @return true if the configuration is acceptable.
     * @see org.apache.felix.ipojo.Factory#isAcceptable(java.util.Dictionary)
     */
    public boolean isAcceptable(Dictionary conf) {
        try {
            checkAcceptability(conf);
        } catch (MissingHandlerException e) {
            return false;
        } catch (UnacceptableConfiguration e) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the configuration is acceptable.
     * @param conf : the configuration to test.
     * @throws UnacceptableConfiguration occurs if the configuration is unacceptable.
     * @throws MissingHandlerException occurs if an handler is missing.
     */
    public void checkAcceptability(Dictionary conf) throws UnacceptableConfiguration, MissingHandlerException {
        PropertyDescription[] props;
        synchronized (this) {
            if (m_state == Factory.INVALID) {
                throw new MissingHandlerException(getMissingHandlers());
            }
            props = m_componentDesc.getProperties(); // Stack confinement.
            // The property list is up to date, as the factory is valid.
        }

        // Check that the configuration does not override immutable properties.
        
        for (int i = 0; i < props.length; i++) {
            // Is the property immutable
            if (props[i].isImmutable() && conf.get(props[i].getName()) != null) {
                throw new UnacceptableConfiguration("The property " + props[i] + " cannot be overide : immutable property"); // The instance configuration tries to override an immutable property.
            }
            // Is the property required ?
            if (props[i].getValue() == null && conf.get(props[i].getName()) == null) {
                throw new UnacceptableConfiguration("The property " + props[i].getName() + " is missing"); // The property must be set.
            }
        }
    }

    /**
     * Reconfigures an existing instance.
     * This method is synchronized to assert the validity of the factory during the reconfiguration.
     * @param properties : the new configuration to push.
     * @throws UnacceptableConfiguration : occurs if the new configuration is not consistent with the component type.
     * @throws MissingHandlerException : occurs if the current factory is not valid.
     * @see org.apache.felix.ipojo.Factory#reconfigure(java.util.Dictionary)
     */
    public synchronized void reconfigure(Dictionary properties) throws UnacceptableConfiguration, MissingHandlerException {
        if (properties == null || properties.get("name") == null) {
            throw new UnacceptableConfiguration("The configuration does not contains the \"name\" property");
        }
        String name = (String) properties.get("name");
        
        ComponentInstance instance = (ComponentInstance) m_componentInstances.get(name);
        if (instance == null) { // The instance does not exists.
            return;
        }
        
        checkAcceptability(properties); // Test if the configuration is acceptable
        instance.reconfigure(properties); // re-configure the instance
    }

    /**
     * Removes a factory listener.
     * @param listener : the factory listener to remove.
     * @see org.apache.felix.ipojo.Factory#removeFactoryStateListener(org.apache.felix.ipojo.FactoryStateListener)
     */
    public void removeFactoryStateListener(FactoryStateListener listener) {
        synchronized (this) {
            m_listeners.remove(listener);
        }
    }

    /**
     * Stopping method. This method is call when the factory is stopping.
     * This method is called when holding the lock on the factory.
     */
    public abstract void stopping();

    /**
     * Stops all the instance managers.
     */
    public synchronized void stop() {
        ComponentInstance[] instances;
        if (m_sr != null) {
            m_sr.unregister();
            m_sr = null;
        }
        stopping(); // Method called when holding the lock.
        m_state = INVALID; // Set here to avoid to create instances during the stops.

        Set col = m_componentInstances.keySet();
        Iterator it = col.iterator();
        instances = new ComponentInstance[col.size()]; // Stack confinement
        int index = 0;
        while (it.hasNext()) {
            instances[index] = (ComponentInstance) (m_componentInstances.get(it.next()));
            index++;
        }

        if (m_state == VALID) {
            for (int i = 0; i < m_listeners.size(); i++) {
                ((FactoryStateListener) m_listeners.get(i)).stateChanged(this, INVALID);
            }
        }

        // Dispose created instances.
        for (int i = 0; i < instances.length; i++) {
            ComponentInstance instance = instances[i];
            if (instance.getState() != ComponentInstance.DISPOSED) {
                instance.dispose();
            }
        }

        // Release each handler
        for (int i = 0; i < m_requiredHandlers.size(); i++) {
            ((RequiredHandler) m_requiredHandlers.get(i)).unRef();
        }
        m_described = false;
        m_componentDesc = null;
        m_componentInstances.clear();
    }

    /**
     * Destroys the factory. The factory cannot be restarted. Only the extender can call this method.
     */
    synchronized void dispose() {
        stop(); // Does not hold the lock.
        m_requiredHandlers = null;
        m_listeners = null;
    }

    /**
     * Starting method. This method is called when the factory is starting. This method is called when holding the lock on the factory.
     */
    public abstract void starting();

    /**
     * Starts the factory.
     */
    public synchronized void start() {
        if (m_described) { // Already started.
            return;
        }

        m_componentDesc = getComponentTypeDescription();

        starting();

        computeFactoryState();

        if (m_isPublic) {
            // Exposition of the factory service
            m_sr =
                    m_context.registerService(m_componentDesc.getFactoryInterfacesToPublish(), this, m_componentDesc
                            .getPropertiesToPublish());
        }
    }

    /**
     * Creates or updates an instance.
     * @param name : name of the instance
     * @param properties : configuration of the instance
     * @throws org.osgi.service.cm.ConfigurationException : if the configuration is not consistent for this component type
     * @see org.osgi.service.cm.ManagedServiceFactory#updated(java.lang.String, java.util.Dictionary)
     */
    public void updated(String name, Dictionary properties) throws org.osgi.service.cm.ConfigurationException {
        InstanceManager instance;
        synchronized (this) {
            instance = (InstanceManager) m_componentInstances.get(name);
        }
        
        if (instance == null) {
            try {
                properties.put("name", name); // Add the name in the configuration
                // If an instance with this name was created before, this creation will failed.
                createComponentInstance(properties);
            } catch (UnacceptableConfiguration e) {
                m_logger.log(Logger.ERROR, "The configuration is not acceptable : " + e.getMessage());
                throw new org.osgi.service.cm.ConfigurationException(properties.toString(), e.getMessage());
            } catch (MissingHandlerException e) {
                m_logger.log(Logger.ERROR, "Handler not available : " + e.getMessage());
                throw new org.osgi.service.cm.ConfigurationException(properties.toString(), e.getMessage());
            } catch (ConfigurationException e) {
                m_logger.log(Logger.ERROR, "The Component Type metadata are not correct : " + e.getMessage());
                throw new org.osgi.service.cm.ConfigurationException(properties.toString(), e.getMessage());
            }
        } else {
            try {
                properties.put("name", name); // Add the name in the configuration
                reconfigure(properties); // re-configure the component
            } catch (UnacceptableConfiguration e) {
                m_logger.log(Logger.ERROR, "The configuration is not acceptable : " + e.getMessage());
                throw new org.osgi.service.cm.ConfigurationException(properties.toString(), e.getMessage());
            } catch (MissingHandlerException e) {
                m_logger.log(Logger.ERROR, "The facotry is not valid, at least one handler is missing : " + e.getMessage());
                throw new org.osgi.service.cm.ConfigurationException(properties.toString(), e.getMessage());
            }
        }
    }

    /**
     * Deletes an instance.
     * @param name : name of the instance to delete
     * @see org.osgi.service.cm.ManagedServiceFactory#deleted(java.lang.String)
     */
    public synchronized void deleted(String name) {
        m_instancesName.remove(name);
        ComponentInstance instance = (ComponentInstance) m_componentInstances.remove(name);
        if (instance != null) {
            instance.dispose();
        }
    }

    /**
     * Callback called by instance when disposed.
     * @param instance : the destroyed instance
     */
    public void disposed(ComponentInstance instance) {
        String name = instance.getInstanceName();
        synchronized (this) {
            m_instancesName.remove(name);
            m_componentInstances.remove(name);
        }
    }

    /**
     * Computes the component type description. The factory must be valid when calling this method.
     * This method is called with the lock.
     */
    protected void computeDescription() {
        for (int i = 0; i < m_requiredHandlers.size(); i++) {
            RequiredHandler req = (RequiredHandler) m_requiredHandlers.get(i);
            Handler handler = getHandler(req, null).getHandler();
            try {
                handler.setFactory(this);
                handler.initializeComponentFactory(m_componentDesc, m_componentMetadata);
                ((Pojo) handler).getComponentInstance().dispose();
            } catch (org.apache.felix.ipojo.ConfigurationException e) {
                ((Pojo) handler).getComponentInstance().dispose();
                m_logger.log(Logger.ERROR, e.getMessage());
                stop();
                throw new IllegalStateException(e.getMessage());
            }
        }
    }

    /**
     * Computes factory state.
     * This method is call when holding the lock on the current factory.
     */
    protected void computeFactoryState() {
        boolean isValid = true;
        for (int i = 0; i < m_requiredHandlers.size(); i++) {
            RequiredHandler req = (RequiredHandler) m_requiredHandlers.get(i);
            if (req.getReference() == null) {
                isValid = false;
                break;
            }

        }

        if (isValid) {
            if (m_state == INVALID) {

                if (!m_described) {
                    computeDescription();
                    m_described = true;
                }

                m_state = VALID;
                if (m_sr != null) {
                    m_sr.setProperties(m_componentDesc.getPropertiesToPublish());
                }
                for (int i = 0; i < m_listeners.size(); i++) {
                    ((FactoryStateListener) m_listeners.get(i)).stateChanged(this, VALID);
                }
                return;
            }
        } else {
            if (m_state == VALID) {
                m_state = INVALID;

                // Notify listeners.
                for (int i = 0; i < m_listeners.size(); i++) {
                    ((FactoryStateListener) m_listeners.get(i)).stateChanged(this, INVALID);
                }

                // Dispose created instances.
                Set col = m_componentInstances.keySet();
                String[] keys = (String[]) col.toArray(new String[col.size()]);
                for (int i = 0; i < keys.length; i++) {
                    ComponentInstance instance = (ComponentInstance) m_componentInstances.get(keys[i]);
                    if (instance.getState() != ComponentInstance.DISPOSED) {
                        instance.dispose();
                    }
                    m_instancesName.remove(instance.getInstanceName());
                }

                m_componentInstances.clear();

                if (m_sr != null) {
                    m_sr.setProperties(m_componentDesc.getPropertiesToPublish());
                }

                return;
            }
        }
    }

    /**
     * Checks if the given handler identifier and the service reference can match.
     * Does not need to be synchronized as the method does not use any fields.
     * @param req : the handler identifier.
     * @param ref : the service reference.
     * @return true if the service reference can fulfill the handler requirement
     */
    protected boolean match(RequiredHandler req, ServiceReference ref) {
        String name = (String) ref.getProperty(Handler.HANDLER_NAME_PROPERTY);
        String namespace = (String) ref.getProperty(Handler.HANDLER_NAMESPACE_PROPERTY);
        if (HandlerFactory.IPOJO_NAMESPACE.equals(namespace)) {
            return name.equals(req.getName()) && req.getNamespace() == null;
        }
        return name.equals(req.getName()) && namespace.equals(req.getNamespace());
    }

    /**
     * Returns the handler object for the given required handler. The handler is instantiated in the given service context.
     * This method is called with the lock.
     * @param req : handler to create.
     * @param context : service context in which create the handler (instance context).
     * @return the Handler object.
     */
    protected HandlerManager getHandler(RequiredHandler req, ServiceContext context) {
        try {
            return (HandlerManager) req.getFactory().createComponentInstance(null, context);
        } catch (MissingHandlerException e) {
            m_logger.log(Logger.ERROR, "The creation of the handler " + req.getFullName() + " has failed: " + e.getMessage());
            stop();
            return null;
        } catch (UnacceptableConfiguration e) {
            m_logger.log(Logger.ERROR, "The creation of the handler "
                    + req.getFullName()
                    + " has failed (UnacceptableConfiguration): "
                    + e.getMessage());
            stop();
            return null;
        } catch (org.apache.felix.ipojo.ConfigurationException e) {
            m_logger.log(Logger.ERROR, "The configuration of the handler "
                    + req.getFullName()
                    + " has failed (ConfigurationException): "
                    + e.getMessage());
            stop();
            return null;
        }
    }

    /**
     * Helping method generating a new unique name.
     * This method is call when holding the lock to assert generated name unicity.
     * @return an non already used name
     */
    protected String generateName() {
        String name = m_factoryName + "-" + m_index;
        while (m_instancesName.contains(name)) {
            m_index = m_index + 1;
            name = m_factoryName + "-" + m_index;
        }
        return name;
    }

    /**
     * Structure storing required handlers.
     * Access to this class must mostly be with the lock on the factory.
     * (except to access final fields)
     */
    protected class RequiredHandler implements Comparable {
        /**
         * Factory to create this handler.
         */
        private HandlerFactory m_factory;

        /**
         * Handler name.
         */
        private final String m_name;

        /**
         * Handler start level.
         */
        private int m_level = Integer.MAX_VALUE;

        /**
         * Handler namespace.
         */
        private final String m_namespace;

        /**
         * Service Reference of the handler factory.
         */
        private ServiceReference m_reference;

        /**
         * Constructor.
         * @param name : handler name.
         * @param namespace : handler namespace.
         */
        public RequiredHandler(String name, String namespace) {
            m_name = name;
            m_namespace = namespace;
        }

        /**
         * Equals method. Two handlers are equals if they have same name and namespace or they share the same service reference.
         * @param object : object to compare to the current object.
         * @return : true if the two compared object are equals
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object object) {
            if (object instanceof RequiredHandler) {
                RequiredHandler req = (RequiredHandler) object;
                if (m_namespace == null) {
                    return req.m_name.equalsIgnoreCase(m_name) && req.m_namespace == null;
                } else {
                    return req.m_name.equalsIgnoreCase(m_name) && m_namespace.equalsIgnoreCase(req.m_namespace);
                }
            } else {
                return false;
            }

        }

        /**
         * Gets the factory object used for this handler. The object is get when used for the first time.
         * This method is called with the lock avoiding concurrent modification and on a valid factory.
         * @return the factory object.
         */
        public HandlerFactory getFactory() {
            if (m_reference == null) {
                return null;
            }
            if (m_factory == null) {
                m_factory = (HandlerFactory) m_context.getService(getReference());
            }
            return m_factory;
        }

        /**
         * Get the handler full name (namespace:name).
         * @return the handler full name
         */
        public String getFullName() {
            if (m_namespace == null) {
                return HandlerFactory.IPOJO_NAMESPACE + ":" + m_name;
            } else {
                return m_namespace + ":" + m_name;
            }
        }

        public String getName() {
            return m_name;
        }

        public String getNamespace() {
            return m_namespace;
        }

        public ServiceReference getReference() {
            return m_reference;
        }

        public int getLevel() {
            return m_level;
        }

        /**
         * Release the reference of the used factory.
         * This method is called with the lock on the current factory.
         */
        public void unRef() {
            if (m_reference != null) {
                m_factory = null;
                m_reference = null;
            }
        }

        /**
         * Set the service reference. If the new service reference is null, it unget the used factory (if already get).
         * This method is called with the lock on the current factory.
         * @param ref : new service reference.
         */
        public void setReference(ServiceReference ref) {
            m_reference = ref;
            Integer level = (Integer) m_reference.getProperty(Handler.HANDLER_LEVEL_PROPERTY);
            if (level != null) {
                m_level = level.intValue();
            }
        }

        /**
         * Start level Comparison. This method is used to sort the handler array.
         * This method is called with the lock.
         * @param object : object on which compare.
         * @return -1, 0, +1 according to the comparison of their start level.
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(Object object) {
            if (object instanceof RequiredHandler) {
                RequiredHandler req = (RequiredHandler) object;
                if (this.m_level == req.m_level) {
                    return 0;
                } else if (this.m_level < req.m_level) {
                    return -1;
                } else {
                    return +1;
                }
            }
            return 0;
        }
    }

}
