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
import org.apache.felix.ipojo.util.SecurityHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedServiceFactory;

/**
 * This class defines common mechanisms of iPOJO component factories
 * (i.e. component type).
 * This class implements both the Factory and ManagedServiceFactory
 * services.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class IPojoFactory implements Factory, ManagedServiceFactory {
    /*
     * TODO there is potentially an issue when calling FactoryStateListener callbacks with the lock
     * It should be called by a separate thread dispatching events to listeners.
     */

    /**
     * The list of the managed instance name.
     * This list is shared by all factories and is
     * used to assert name unicity.
     */
    protected static final List INSTANCE_NAME = new ArrayList();

    /**
     * The component type description exposed by the {@link Factory} service.
     */
    protected ComponentTypeDescription m_componentDesc;

    /**
     * The list of the managed instance managers.
     * The key of this map is the name (i.e. instance names) of the created instance
     */
    protected final Map m_componentInstances = new HashMap();

    /**
     * The component type metadata.
     */
    protected final Element m_componentMetadata;

    /**
     * The bundle context reference.
     */
    protected final BundleContext m_context;

    /**
     * The factory name.
     * Could be the component class name if the factory name is not set.
     * Immutable once set.
     */
    protected String m_factoryName;

    /**
     * The list of required handlers.
     */
    protected List m_requiredHandlers = new ArrayList();

    /**
     * The list of factory state listeners.
     * @see FactoryStateListener
     */
    protected List m_listeners = new ArrayList(1);

    /**
     * The logger for the factory (and all component instances).
     */
    protected final Logger m_logger;

    /**
     * Is the factory public (exposed as services).
     */
    protected final boolean m_isPublic;

    /**
     * The version of the component type.
     */
    protected final String m_version;

    /**
     * The service registration of this factory (Factory & ManagedServiceFactory).
     * @see ManagedServiceFactory
     * @see Factory
     */
    protected ServiceRegistration m_sr;

    /**
     * The factory state.
     * Can be:
     * <li>{@link Factory#INVALID}</li>
     * <li>{@link Factory#VALID}</li>
     * The factory is invalid at the beginning.
     * A factory becomes valid if every required handlers
     * are available (i.e. can be created).
     */
    protected int m_state = Factory.INVALID;

    /**
     * The index used to generate instance name if not set.
     */
    private long m_index = 0;

    /**
     * The flag indicating if this factory has already a
     * computed description or not.
     */
    private boolean m_described;

    /**
     * Creates an iPOJO Factory.
     * At the end of this method, the required set of handler is computed.
     * But the result is computed by a sub-class.
     * @param context the bundle context of the bundle containing the factory.
     * @param metadata the description of the component type.
     * @throws ConfigurationException if the element describing the factory is malformed.
     */
    public IPojoFactory(BundleContext context, Element metadata) throws ConfigurationException {
        m_context = context;
        m_componentMetadata = metadata;
        m_factoryName = getFactoryName();
        String fac = metadata.getAttribute("public");
        m_isPublic = fac == null || !fac.equalsIgnoreCase("false");
        m_logger = new Logger(m_context, m_factoryName);

        // Compute the component type version.
        String version = metadata.getAttribute("version");
        if ("bundle".equalsIgnoreCase(version)) { // Handle the "bundle" constant: use the bundle version.
            m_version = (String) m_context.getBundle().getHeaders().get(Constants.BUNDLE_VERSION);
        } else {
            m_version = version;
        }

        m_requiredHandlers = getRequiredHandlerList(); // Call sub-class to get the list of required handlers.
        
        m_logger.log(Logger.INFO, "New factory created : " + m_factoryName);
    }

    /**
     * Gets the component type description.
     * @return the component type description
     */
    public ComponentTypeDescription getComponentTypeDescription() {
        return new ComponentTypeDescription(this);
    }

    /**
     * Adds a factory listener.
     * @param listener the factory listener to add.
     * @see org.apache.felix.ipojo.Factory#addFactoryStateListener(org.apache.felix.ipojo.FactoryStateListener)
     */
    public void addFactoryStateListener(FactoryStateListener listener) {
        synchronized (this) {
            m_listeners.add(listener);
        }
    }

    /**
     * Gets the logger used by instances created by the current factory.
     * @return the factory logger.
     */
    public Logger getLogger() {
        return m_logger;
    }

    /**
     * Computes the factory name.
     * Each sub-type must override this method.
     * @return the factory name.
     */
    public abstract String getFactoryName();

    /**
     * Computes the required handler list.
     * Each sub-type must override this method.
     * @return the required handler list
     */
    public abstract List getRequiredHandlerList();

    /**
     * Creates an instance.
     * This method is called with the monitor lock.
     * @param config the instance configuration
     * @param context the iPOJO context to use
     * @param handlers the handler array to use
     * @return the new component instance.
     * @throws ConfigurationException if the instance creation failed during the configuration process.
     */
    public abstract ComponentInstance createInstance(Dictionary config, IPojoContext context, HandlerManager[] handlers)
        throws ConfigurationException;

    /**
     * Creates an instance.
     * This method creates the instance in the global context.
     * @param configuration the configuration of the created instance.
     * @return the created component instance.
     * @throws UnacceptableConfiguration if the given configuration is not consistent with the component type of this factory.
     * @throws MissingHandlerException if an handler is unavailable when the instance is created.
     * @throws org.apache.felix.ipojo.ConfigurationException if the instance or type configuration are not correct.
     * @see org.apache.felix.ipojo.Factory#createComponentInstance(java.util.Dictionary)
     */
    public ComponentInstance createComponentInstance(Dictionary configuration) throws UnacceptableConfiguration, MissingHandlerException,
            ConfigurationException {
        return createComponentInstance(configuration, null);
    }

    /**
     * Creates an instance in the specified service context.
     * This method is synchronized to assert the validity of the factory during the creation.
     * Callbacks to sub-class and  created instances need to be aware that they are holding the monitor lock.
     * This method call the override {@link IPojoFactory#createInstance(Dictionary, IPojoContext, HandlerManager[])
     * method.
     * @param configuration the configuration of the created instance.
     * @param serviceContext the service context to push for this instance.
     * @return the created component instance.
     * @throws UnacceptableConfiguration if the given configuration is not consistent with the component type of this factory.
     * @throws MissingHandlerException if an handler is unavailable when creating the instance.
     * @throws org.apache.felix.ipojo.ConfigurationException if the instance configuration failed.
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
        if (configuration.get("instance.name") == null && configuration.get("name") == null) { // Support both instance.name & name
            name = generateName();
            configuration.put("instance.name", name);
        } else {
            name = (String) configuration.get("instance.name");
            if (name == null) {
                name = (String) configuration.get("name");
                getLogger().log(Logger.WARNING, "The 'name' (" + name + ") attribute, used as the instance name, is deprecated, please use the 'instance.name' attribute");
                configuration.put("instance.name", name);
            }
            if (INSTANCE_NAME.contains(name)) {
                m_logger.log(Logger.ERROR, "The configuration is not acceptable : Name already used");
                throw new UnacceptableConfiguration(getFactoryName() + " : Name already used : " + name);
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
            INSTANCE_NAME.add(name);
            m_componentInstances.put(name, instance);
            m_logger.log(Logger.INFO, "Instance " + name + " from factory " + m_factoryName + " created");
            return instance;
        } catch (ConfigurationException e) {
            m_logger.log(Logger.ERROR, e.getMessage());
            throw new ConfigurationException(e.getMessage(), m_factoryName);
        }
        

    }

    /**
     * Gets the bundle context of the factory.
     * @return the bundle context of the factory.
     * @see org.apache.felix.ipojo.Factory#getBundleContext()
     */
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
     * @return the component type description object. <code>Null</code> if not already computed.
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
     * Computes the list of missing handlers. This method is called with the monitor lock.
     * @return the list of missing handlers.
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
     * @return the list of required handlers.
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
     * @param conf the configuration to test.
     * @return <code>true</code> if the configuration is acceptable.
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
     * This method checks the following assertions:
     * <li>All handlers can be creates</li>
     * <li>The configuration does not override immutable properties</li>
     * <li>The configuration contains a value for every unvalued property</li>
     * @param conf the configuration to test.
     * @throws UnacceptableConfiguration if the configuration is unacceptable.
     * @throws MissingHandlerException if an handler is missing.
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
            if (props[i].isMandatory() && props[i].getValue() == null && conf.get(props[i].getName()) == null) {
                throw new UnacceptableConfiguration("The mandatory property " + props[i].getName() + " is missing"); // The property must be set.
            }
        }
    }

    /**
     * Reconfigures an existing instance.
     * The acceptability of the configuration is checked before the reconfiguration. Moreover,
     * the configuration must contain the 'instance.name' property specifying the instance
     * to reconfigure.
     * This method is synchronized to assert the validity of the factory during the reconfiguration.
     * @param properties the new configuration to push.
     * @throws UnacceptableConfiguration if the new configuration is not consistent with the component type.
     * @throws MissingHandlerException if the current factory is not valid.
     * @see org.apache.felix.ipojo.Factory#reconfigure(java.util.Dictionary)
     */
    public synchronized void reconfigure(Dictionary properties) throws UnacceptableConfiguration, MissingHandlerException {
        if (properties == null || (properties.get("instance.name") == null && properties.get("name") == null)) { // Support both instance.name and name
            throw new UnacceptableConfiguration("The configuration does not contains the \"instance.name\" property");
        }

        String name = (String) properties.get("instance.name");
        if (name == null) {
            name = (String) properties.get("name");
        }

        ComponentInstance instance = (ComponentInstance) m_componentInstances.get(name);
        if (instance == null) { // The instance does not exists.
            return;
        }

        checkAcceptability(properties); // Test if the configuration is acceptable
        instance.reconfigure(properties); // re-configure the instance
    }

    /**
     * Removes a factory listener.
     * @param listener the factory listener to remove.
     * @see org.apache.felix.ipojo.Factory#removeFactoryStateListener(org.apache.felix.ipojo.FactoryStateListener)
     */
    public void removeFactoryStateListener(FactoryStateListener listener) {
        synchronized (this) {
            m_listeners.remove(listener);
        }
    }

    /**
     * Stopping method.
     * This method is call when the factory is stopping.
     * This method is called when holding the lock on the factory.
     */
    public abstract void stopping();

    /**
     * Stops all the instance managers.
     * This method calls the {@link IPojoFactory#stopping()} method,
     * notifies listeners, and disposes created instances. Moreover,
     * if the factory is public, services are also unregistered.
     *
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
        
        m_logger.log(Logger.INFO, "Factory " + m_factoryName + " stopped");

    }

    /**
     * Destroys the factory.
     * The factory cannot be restarted. Only the {@link Extender} can call this method.
     */
    synchronized void dispose() {
        stop(); // Does not hold the lock.
        m_requiredHandlers = null;
        m_listeners = null;
    }

    /**
     * Starting method.
     * This method is called when the factory is starting.
     * This method is called when holding the lock on the factory.
     */
    public abstract void starting();

    /**
     * Starts the factory.
     * Tries to compute the component type description,
     * calls the {@link IPojoFactory#starting()} method,
     * and published services if the factory is public.
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
            BundleContext bc = SecurityHelper.selectContextToRegisterServices(m_componentDesc.getFactoryInterfacesToPublish(), 
                    m_context, getIPOJOBundleContext());
            m_sr =
                    bc.registerService(m_componentDesc.getFactoryInterfacesToPublish(), this, m_componentDesc
                            .getPropertiesToPublish());
        }
        
        m_logger.log(Logger.INFO, "Factory " + m_factoryName + " started");

    }
    
    /**
     * Gets the iPOJO Bundle Context.
     * @return the iPOJO Bundle Context
     */
    protected final BundleContext getIPOJOBundleContext() {
        return Extender.getIPOJOBundleContext();
    }

    /**
     * Creates or updates an instance.
     * @param name the name of the instance
     * @param properties the new configuration of the instance
     * @throws org.osgi.service.cm.ConfigurationException if the configuration is not consistent for this component type
     * @see org.osgi.service.cm.ManagedServiceFactory#updated(java.lang.String, java.util.Dictionary)
     */
    public void updated(String name, Dictionary properties) throws org.osgi.service.cm.ConfigurationException {
        InstanceManager instance;
        synchronized (this) {
            instance = (InstanceManager) m_componentInstances.get(name);
        }

        if (instance == null) {
            try {
                properties.put("instance.name", name); // Add the name in the configuration
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
                properties.put("instance.name", name); // Add the name in the configuration
                reconfigure(properties); // re-configure the component
            } catch (UnacceptableConfiguration e) {
                m_logger.log(Logger.ERROR, "The configuration is not acceptable : " + e.getMessage());
                throw new org.osgi.service.cm.ConfigurationException(properties.toString(), e.getMessage());
            } catch (MissingHandlerException e) {
                m_logger.log(Logger.ERROR, "The factory is not valid, at least one handler is missing : " + e.getMessage());
                throw new org.osgi.service.cm.ConfigurationException(properties.toString(), e.getMessage());
            }
        }
    }

    /**
     * Deletes an instance.
     * @param name the name of the instance to delete
     * @see org.osgi.service.cm.ManagedServiceFactory#deleted(java.lang.String)
     */
    public synchronized void deleted(String name) {
        INSTANCE_NAME.remove(name);
        ComponentInstance instance = (ComponentInstance) m_componentInstances.remove(name);
        if (instance != null) {
            instance.dispose();
        }
    }

    /**
     * Callback called by instance when disposed.
     * @param instance the destroyed instance
     */
    public void disposed(ComponentInstance instance) {
        String name = instance.getInstanceName();
        synchronized (this) {
            INSTANCE_NAME.remove(name);
            m_componentInstances.remove(name);
        }
    }

    /**
     * Computes the component type description.
     * To do this, it creates a 'ghost' instance of the handler
     * and calls the {@link Handler#initializeComponentFactory(ComponentTypeDescription, Element)}
     * method. The handler instance is then deleted.
     * The factory must be valid when calling this method.
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
     * The factory is valid if every required handler are available.
     * If the factory becomes valid for the first time, the component
     * type description is computed.
     * This method is called when holding the lock on the current factory.
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
                    INSTANCE_NAME.remove(instance.getInstanceName());
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
     * Checks if the given handler identifier and the service reference match.
     * Does not need to be synchronized as the method does not use any fields.
     * @param req the handler identifier.
     * @param ref the service reference.
     * @return <code>true</code> if the service reference can fulfill the handler requirement
     */
    protected boolean match(RequiredHandler req, ServiceReference ref) {
        String name = (String) ref.getProperty(Handler.HANDLER_NAME_PROPERTY);
        String namespace = (String) ref.getProperty(Handler.HANDLER_NAMESPACE_PROPERTY);
        if (HandlerFactory.IPOJO_NAMESPACE.equals(namespace)) {
            return name.equalsIgnoreCase(req.getName()) && req.getNamespace() == null; 
        }
        return name.equalsIgnoreCase(req.getName()) && namespace.equalsIgnoreCase(req.getNamespace());
    }

    /**
     * Returns the handler object for the given required handler.
     * The handler is instantiated in the given service context.
     * This method is called with the lock.
     * @param req the handler to create.
     * @param context the service context in which the handler is created (same as the instance context).
     * @return the handler object.
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
     * Helper method generating a new unique name.
     * This method is call when holding the lock to assert generated name unicity.
     * @return a non already used name
     */
    protected String generateName() {
        String name = m_factoryName + "-" + m_index;
        while (INSTANCE_NAME.contains(name)) {
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
         * The factory to create this handler.
         */
        private HandlerFactory m_factory;

        /**
         * The handler name.
         */
        private final String m_name;

        /**
         * The handler start level.
         */
        private int m_level = Integer.MAX_VALUE;

        /**
         * The handler namespace.
         */
        private final String m_namespace;

        /**
         * The Service Reference of the handler factory.
         */
        private ServiceReference m_reference;

        /**
         * Crates a Required Handler.
         * @param name the handler name.
         * @param namespace the handler namespace.
         */
        public RequiredHandler(String name, String namespace) {
            m_name = name;
            m_namespace = namespace;
        }

        /**
         * Equals method.
         * Two handlers are equals if they have same name and namespace or they share the same service reference.
         * @param object the object to compare to the current object.
         * @return <code>true</code> if the two compared object are equals
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
         * Hashcode method.
         * This method delegates to the {@link Object#hashCode()}.
         * @return the object hashcode.
         * @see java.lang.Object#hashCode()
         */
        public int hashCode() {
            return super.hashCode();
        }

        /**
         * Gets the factory object used for this handler.
         * The object is get when used for the first time.
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
         * Gets the handler qualified name (<code>namespace:name</code>).
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
         * Releases the reference of the used factory.
         * This method is called with the lock on the current factory.
         */
        public void unRef() {
            if (m_reference != null) {
                m_factory = null;
                m_reference = null;
            }
        }

        /**
         * Sets the service reference. If the new service reference is <code>null</code>, it ungets the used factory (if already get).
         * This method is called with the lock on the current factory.
         * @param ref the new service reference.
         */
        public void setReference(ServiceReference ref) {
            m_reference = ref;
            Integer level = (Integer) m_reference.getProperty(Handler.HANDLER_LEVEL_PROPERTY);
            if (level != null) {
                m_level = level.intValue();
            }
        }

        /**
         * Start level Comparison.
         * This method is used to sort the handler array.
         * This method is called with the lock.
         * @param object the object on which compare.
         * @return <code>-1</code>, <code>0</code>, <code>+1</code> according to the comparison of their start levels.
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
