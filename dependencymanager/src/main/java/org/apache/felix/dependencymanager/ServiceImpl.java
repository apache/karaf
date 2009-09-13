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
package org.apache.felix.dependencymanager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Service implementation.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceImpl implements Service, ServiceComponent {
    private static final Class[] VOID = new Class[] {};
	private static final ServiceRegistration NULL_REGISTRATION;
    private static final ServiceStateListener[] SERVICE_STATE_LISTENER_TYPE = new ServiceStateListener[] {};

    private final BundleContext m_context;
    private final DependencyManager m_manager;

    // configuration (static)
    private String m_callbackInit;
    private String m_callbackStart;
    private String m_callbackStop;
    private String m_callbackDestroy;
    private Object m_serviceName;
    private Object m_implementation;

    // configuration (dynamic, but does not affect state)
    private Dictionary m_serviceProperties;

    // configuration (dynamic, and affects state)
    private ArrayList m_dependencies = new ArrayList();

    // runtime state (calculated from dependencies)
    private State m_state;

    // runtime state (changes because of state changes)
    private Object m_serviceInstance;
    private ServiceRegistration m_registration;

    // service state listeners
    private final List m_stateListeners = new ArrayList();

    // work queue
    private final SerialExecutor m_executor = new SerialExecutor();

    // instance factory
	private Object m_instanceFactory;
	private String m_instanceFactoryCreateMethod;

	// composition manager
	private Object m_compositionManager;
	private String m_compositionManagerGetMethod;
	private Object m_compositionManagerInstance;
	
	// internal logging
    private final Logger m_logger;
    private ServiceRegistration m_serviceRegistration;
    private Map m_autoConfig = new HashMap();
    private Map m_autoConfigInstance = new HashMap();

    public ServiceImpl(BundleContext context, DependencyManager manager, Logger logger) {
    	m_logger = logger;
        m_state = new State((List) m_dependencies.clone(), false);
        m_context = context;
        m_manager = manager;
        m_callbackInit = "init";
        m_callbackStart = "start";
        m_callbackStop = "stop";
        m_callbackDestroy = "destroy";
        m_implementation = null;
        m_autoConfig.put(BundleContext.class, Boolean.TRUE);
        m_autoConfig.put(ServiceRegistration.class, Boolean.TRUE);
        m_autoConfig.put(DependencyManager.class, Boolean.TRUE);
    }

    private void calculateStateChanges(final State oldState, final State newState) {
    	if (oldState.isWaitingForRequired() && newState.isTrackingOptional()) {
        	m_executor.enqueue(new Runnable() {
				public void run() {
					activateService(newState);
				}});
    	}
    	if (oldState.isTrackingOptional() && newState.isWaitingForRequired()) {
    		m_executor.enqueue(new Runnable() {
				public void run() {
					deactivateService(oldState);
				}});
    	}
    	if (oldState.isInactive() && (newState.isTrackingOptional())) {
    		m_executor.enqueue(new Runnable() {
				public void run() {
					activateService(newState);
				}});
    	}
    	if (oldState.isInactive() && (newState.isWaitingForRequired())) {
    		m_executor.enqueue(new Runnable() {
				public void run() {
					startTrackingRequired(newState);
				}});
    	}
    	if ((oldState.isWaitingForRequired()) && newState.isInactive()) {
    		m_executor.enqueue(new Runnable() {
				public void run() {
					stopTrackingRequired(oldState);
				}});
    	}
    	if ((oldState.isTrackingOptional()) && newState.isInactive()) {
    		m_executor.enqueue(new Runnable() {
				public void run() {
					deactivateService(oldState);
					stopTrackingRequired(oldState);
				}});
    	}
    	m_executor.execute();
    }

    public Service add(final Dependency dependency) {
    	State oldState, newState;
        synchronized (m_dependencies) {
        	oldState = m_state;
            m_dependencies.add(dependency);
        }
        if (oldState.isTrackingOptional() || (oldState.isWaitingForRequired() && dependency.isRequired())) {
        	dependency.start(this);
        }
        synchronized (m_dependencies) {
            newState = new State((List) m_dependencies.clone(), !oldState.isInactive());
            m_state = newState;
            calculateStateChanges(oldState, newState);
        }
        return this;
    }

    public Service remove(Dependency dependency) {
    	State oldState, newState;
        synchronized (m_dependencies) {
        	oldState = m_state;
            m_dependencies.remove(dependency);
        }
        if (oldState.isTrackingOptional() || (oldState.isWaitingForRequired() && dependency.isRequired())) {
        	dependency.stop(this);
        }
        synchronized (m_dependencies) {
            newState = new State((List) m_dependencies.clone(), !oldState.isInactive());
            m_state = newState;
        }
        calculateStateChanges(oldState, newState);
        return this;
    }

    public List getDependencies() {
        synchronized (m_dependencies) {
            return (List) m_dependencies.clone();
        }
    }

    public ServiceRegistration getServiceRegistration() {
        return m_registration;
    }

    public Object getService() {
        return m_serviceInstance;
    }

    public void dependencyAvailable(final Dependency dependency) {
    	State oldState, newState;
        synchronized (m_dependencies) {
        	oldState = m_state;
            newState = new State((List) m_dependencies.clone(), !oldState.isInactive());
            m_state = newState;
        }
        calculateStateChanges(oldState, newState);
        if (newState.isTrackingOptional()) {
        	m_executor.enqueue(new Runnable() {
        		public void run() {
        			updateInstance(dependency);
        		}
        	});
        	m_executor.execute();
        }
    }

    public void dependencyChanged(final Dependency dependency) {
    	State state;
        synchronized (m_dependencies) {
        	state = m_state;
        }
        if (state.isTrackingOptional()) {
        	m_executor.enqueue(new Runnable() {
        		public void run() {
        			updateInstance(dependency);
        		}
        	});
        	m_executor.execute();
        }
    }

    public void dependencyUnavailable(final Dependency dependency) {
    	State oldState, newState;
        synchronized (m_dependencies) {
        	oldState = m_state;
            newState = new State((List) m_dependencies.clone(), !oldState.isInactive());
            m_state = newState;
        }
        calculateStateChanges(oldState, newState);
        if (newState.isTrackingOptional()) {
        	m_executor.enqueue(new Runnable() {
        		public void run() {
        			updateInstance(dependency);
        		}
        	});
        	m_executor.execute();
        }
    }

    public synchronized void start() {
        m_serviceRegistration = m_context.registerService(ServiceComponent.class.getName(), this, null);
    	State oldState, newState;
        synchronized (m_dependencies) {
        	oldState = m_state;
            newState = new State((List) m_dependencies.clone(), true);
            m_state = newState;
        }
        calculateStateChanges(oldState, newState);
    }

    public synchronized void stop() {
    	State oldState, newState;
        synchronized (m_dependencies) {
        	oldState = m_state;
            newState = new State((List) m_dependencies.clone(), false);
            m_state = newState;
        }
        calculateStateChanges(oldState, newState);
        m_serviceRegistration.unregister();
    }

    public synchronized Service setInterface(String serviceName, Dictionary properties) {
	    ensureNotActive();
	    m_serviceName = serviceName;
	    m_serviceProperties = properties;
	    return this;
	}

	public synchronized Service setInterface(String[] serviceName, Dictionary properties) {
	    ensureNotActive();
	    m_serviceName = serviceName;
	    m_serviceProperties = properties;
	    return this;
	}

	public synchronized Service setCallbacks(String init, String start, String stop, String destroy) {
	    ensureNotActive();
	    m_callbackInit = init;
	    m_callbackStart = start;
	    m_callbackStop = stop;
	    m_callbackDestroy = destroy;
	    return this;
	}

	public synchronized Service setImplementation(Object implementation) {
	    ensureNotActive();
	    m_implementation = implementation;
	    return this;
	}

	public synchronized Service setFactory(Object factory, String createMethod) {
	    ensureNotActive();
		m_instanceFactory = factory;
		m_instanceFactoryCreateMethod = createMethod;
		return this;
	}

	public synchronized Service setFactory(String createMethod) {
		return setFactory(null, createMethod);
	}

	public synchronized Service setComposition(Object instance, String getMethod) {
	    ensureNotActive();
		m_compositionManager = instance;
		m_compositionManagerGetMethod = getMethod;
		return this;
	}

	public synchronized Service setComposition(String getMethod) {
		return setComposition(null, getMethod);
	}

	public String toString() {
	    return "ServiceImpl[" + m_serviceName + " " + m_implementation + "]";
	}

	public synchronized Dictionary getServiceProperties() {
	    if (m_serviceProperties != null) {
	        return (Dictionary) ((Hashtable) m_serviceProperties).clone();
	    }
	    return null;
	}

	public synchronized void setServiceProperties(Dictionary serviceProperties) {
	    m_serviceProperties = serviceProperties;
	    if (isRegistered() && (m_serviceName != null)) {
	        m_registration.setProperties(calculateServiceProperties());
	    }
	}

	// service state listener methods
	public void addStateListener(ServiceStateListener listener) {
    	synchronized (m_stateListeners) {
		    m_stateListeners.add(listener);
    	}
    	// when we register as a listener and the service is already started
    	// make sure we invoke the right callbacks so the listener knows
    	State state;
    	synchronized (m_dependencies) {
    		state = m_state;
    	}
    	if (state.isTrackingOptional()) {
    		listener.starting(this);
    		listener.started(this);
    	}
	}

	public void removeStateListener(ServiceStateListener listener) {
    	synchronized (m_stateListeners) {
    		m_stateListeners.remove(listener);
    	}
	}

	void removeStateListeners() {
    	synchronized (m_stateListeners) {
    		m_stateListeners.clear();
    	}
	}

	private void stateListenersStarting() {
		ServiceStateListener[] list = getListeners();
		for (int i = 0; i < list.length; i++) {
		    try {
		        list[i].starting(this);
		    }
		    catch (Throwable t) {
		        m_logger.log(Logger.LOG_ERROR, "Error invoking listener starting method.", t);
		    }
		}
	}

	private void stateListenersStarted() {
        ServiceStateListener[] list = getListeners();
        for (int i = 0; i < list.length; i++) {
            try {
                list[i].started(this);
            }
            catch (Throwable t) {
                m_logger.log(Logger.LOG_ERROR, "Error invoking listener started method.", t);
            }
        }
    }

    private void stateListenersStopping() {
        ServiceStateListener[] list = getListeners();
        for (int i = 0; i < list.length; i++) {
            try {
                list[i].stopping(this);
            }
            catch (Throwable t) {
                m_logger.log(Logger.LOG_ERROR, "Error invoking listener stopping method.", t);
            }
        }
    }

    private void stateListenersStopped() {
        ServiceStateListener[] list = getListeners();
        for (int i = 0; i < list.length; i++) {
            try {
                list[i].stopped(this);
            }
            catch (Throwable t) {
                m_logger.log(Logger.LOG_ERROR, "Error invoking listener stopped method.", t);
            }
        }
    }

	private ServiceStateListener[] getListeners() {
		synchronized (m_stateListeners) {
			return (ServiceStateListener[]) m_stateListeners.toArray(SERVICE_STATE_LISTENER_TYPE);
		}
	}

	private void activateService(State state) {
		String init, start;
		synchronized (this) {
			init = m_callbackInit;
			start = m_callbackStart;
		}
        // service activation logic, first we initialize the service instance itself
        // meaning it is created if necessary and the bundle context is set
        initService();
        // then we invoke the init callback so the service can further initialize
        // itself
        invoke(init);
        // now is the time to configure the service, meaning all required
        // dependencies will be set and any callbacks called
        configureService(state);
        // inform the state listeners we're starting
        stateListenersStarting();
        // invoke the start callback, since we're now ready to be used
        invoke(start);
        // start tracking optional services
        startTrackingOptional(state);
        // register the service in the framework's service registry
        registerService();
        // inform the state listeners we've started
        stateListenersStarted();
    }

    private void deactivateService(State state) {
    	String stop, destroy;
    	synchronized (this) {
    		stop = m_callbackStop;
    		destroy = m_callbackDestroy;
    	}
        // service deactivation logic, first inform the state listeners
        // we're stopping
        stateListenersStopping();
        // then, unregister the service from the framework
        unregisterService();
        // stop tracking optional services
        stopTrackingOptional(state);
        // invoke the stop callback
        invoke(stop);
        // inform the state listeners we've stopped
        stateListenersStopped();
        // invoke the destroy callback
        invoke(destroy);
        // destroy the service instance
        destroyService(state);
    }

    private void invoke(String name) {
        if (name != null) {
            // invoke method if it exists
            try {
                Class clazz = m_serviceInstance.getClass();
                while (clazz != null) {
                	try {
                	Method method = clazz.getDeclaredMethod(name, null);
	                	if (method != null) {
	                		method.setAccessible(true);
	                		try {
    	                		method.invoke(m_serviceInstance, null);
	                		}
	                		catch (InvocationTargetException e) {
	                		    m_logger.log(Logger.LOG_ERROR, "Exception while invoking method " + method + ".", e);
	                		}
	                		return;
	                	}
                	}
                	catch (NoSuchMethodException e) {
                		// ignore this, we keep searching if the method does not exist
                	}
                	clazz = clazz.getSuperclass();
                }
            }
            catch (Exception e) {
                m_logger.log(Logger.LOG_ERROR, "Error trying to invoke method named " + name + ".", e);
            }
        }
    }

    private void startTrackingOptional(State state) {
        Iterator i = state.getDependencies().iterator();
        while (i.hasNext()) {
            Dependency dependency = (Dependency) i.next();
            if (!dependency.isRequired()) {
                dependency.start(this);
            }
        }
    }
    
    private void stopTrackingOptional(State state) {
        Iterator i = state.getDependencies().iterator();
        while (i.hasNext()) {
            Dependency dependency = (Dependency) i.next();
            if (!dependency.isRequired()) {
                dependency.stop(this);
            }
        }
    }

    private void startTrackingRequired(State state) {
        Iterator i = state.getDependencies().iterator();
        while (i.hasNext()) {
            Dependency dependency = (Dependency) i.next();
            if (dependency.isRequired()) {
                dependency.start(this);
            }
        }
    }

    private void stopTrackingRequired(State state) {
        Iterator i = state.getDependencies().iterator();
        while (i.hasNext()) {
            Dependency dependency = (Dependency) i.next();
            if (dependency.isRequired()) {
                dependency.stop(this);
            }
        }
    }

    private Object createInstance(Class clazz) throws SecurityException, NoSuchMethodException, InstantiationException, IllegalAccessException {
		Constructor constructor = clazz.getConstructor(VOID);
		constructor.setAccessible(true);
        return clazz.newInstance();
    }

    void initService() {
    	if (m_serviceInstance == null) {
	        if (m_implementation instanceof Class) {
	            // instantiate
	            try {
	            	m_serviceInstance = createInstance((Class) m_implementation);
	            }
	            catch (Exception e) {
	                m_logger.log(Logger.LOG_ERROR, "Could not create service instance of class " + m_implementation + ".", e);
				}
	        }
	        else {
	        	if (m_instanceFactoryCreateMethod != null) {
	        		Object factory = null;
		        	if (m_instanceFactory != null) {
		        		if (m_instanceFactory instanceof Class) {
		        			try {
								factory = createInstance((Class) m_instanceFactory);
							}
		                    catch (Exception e) {
		                        m_logger.log(Logger.LOG_ERROR, "Could not create factory instance of class " + m_instanceFactory + ".", e);
		                    }
		        		}
		        		else {
		        			factory = m_instanceFactory;
		        		}
		        	}
		        	else {
		        		// TODO review if we want to try to default to something if not specified
		        	    // for now the JavaDoc of setFactory(method) reflects the fact that we need
		        	    // to review it
		        	}
		        	if (factory == null) {
                        m_logger.log(Logger.LOG_ERROR, "Factory cannot be null.");
		        	}
		        	else {
    		        	try {
    						Method m = factory.getClass().getDeclaredMethod(m_instanceFactoryCreateMethod, null);
    						m_serviceInstance = m.invoke(factory, null);
    					}
    		        	catch (Exception e) {
    	                    m_logger.log(Logger.LOG_ERROR, "Could not create service instance using factory " + factory + " method " + m_instanceFactoryCreateMethod + ".", e);
    					}
		        	}
	        	}
	        	if (m_implementation == null) {
                    m_logger.log(Logger.LOG_ERROR, "Implementation cannot be null.");
	        	}
	        	if (m_serviceInstance == null) {
	        	    m_serviceInstance = m_implementation;
	        	}
	        }
	        // configure the bundle context
	        if (((Boolean) m_autoConfig.get(BundleContext.class)).booleanValue()) {
	            configureImplementation(BundleContext.class, m_context, (String) m_autoConfigInstance.get(BundleContext.class));
	        }
            if (((Boolean) m_autoConfig.get(ServiceRegistration.class)).booleanValue()) {
                configureImplementation(ServiceRegistration.class, NULL_REGISTRATION, (String) m_autoConfigInstance.get(ServiceRegistration.class));
            }
            if (((Boolean) m_autoConfig.get(DependencyManager.class)).booleanValue()) {
                configureImplementation(DependencyManager.class, m_manager, (String) m_autoConfigInstance.get(DependencyManager.class));
            }
    	}
    }

    public void setAutoConfig(Class clazz, boolean autoConfig) {
        m_autoConfig.put(clazz, Boolean.valueOf(autoConfig));
    }
    
    public void setAutoConfig(Class clazz, String instanceName) {
        m_autoConfig.put(clazz, Boolean.valueOf(instanceName != null));
        m_autoConfigInstance.put(clazz, instanceName);
    }
    
    private void configureService(State state) {
        // configure all services (the optional dependencies might be configured
        // as null objects but that's what we want at this point)
        configureServices(state);
    }

    private void destroyService(State state) {
        unconfigureServices(state);
        m_serviceInstance = null;
    }

    private void registerService() {
        if (m_serviceName != null) {
            ServiceRegistrationImpl wrapper = new ServiceRegistrationImpl();
            m_registration = wrapper;
            if (((Boolean) m_autoConfig.get(ServiceRegistration.class)).booleanValue()) {
                configureImplementation(ServiceRegistration.class, m_registration, (String) m_autoConfigInstance.get(ServiceRegistration.class));
            }
            
            // service name can either be a string or an array of strings
            ServiceRegistration registration;

            // determine service properties
            Dictionary properties = calculateServiceProperties();

            // register the service
            try {
                if (m_serviceName instanceof String) {
                    registration = m_context.registerService((String) m_serviceName, m_serviceInstance, properties);
                }
                else {
                    registration = m_context.registerService((String[]) m_serviceName, m_serviceInstance, properties);
                }
                wrapper.setServiceRegistration(registration);
            }
            catch (IllegalArgumentException iae) {
                m_logger.log(Logger.LOG_ERROR, "Could not register service " + m_serviceInstance, iae);
                // set the registration to an illegal state object, which will make all invocations on this
                // wrapper fail with an ISE (which also occurs when the SR becomes invalid)
                wrapper.setIllegalState();
            }
        }
    }

	private Dictionary calculateServiceProperties() {
		Dictionary properties = new Properties();
		addTo(properties, m_serviceProperties);
		for (int i = 0; i < m_dependencies.size(); i++) {
			Dependency d = (Dependency) m_dependencies.get(i);
			if (d instanceof ConfigurationDependency) {
				ConfigurationDependency cd = (ConfigurationDependency) d;
				if (cd.isPropagated()) {
					Dictionary dict = cd.getConfiguration();
					addTo(properties, dict);
				}
			}
		}
		if (properties.size() == 0) {
			properties = null;
		}
		return properties;
	}

	private void addTo(Dictionary properties, Dictionary additional) {
		if (properties == null) {
			throw new IllegalArgumentException("Dictionary to add to cannot be null.");
		}
		if (additional != null) {
			Enumeration e = additional.keys();
			while (e.hasMoreElements()) {
				Object key = e.nextElement();
				properties.put(key, additional.get(key));
			}
		}
	}

    private void unregisterService() {
        if (m_serviceName != null) {
            m_registration.unregister();
            configureImplementation(ServiceRegistration.class, NULL_REGISTRATION);
        }
    }

    private void updateInstance(Dependency dependency) {
        if (dependency instanceof ServiceDependency) {
            ServiceDependency sd = (ServiceDependency) dependency;
            // update the dependency in the service instance (it will use
            // a null object if necessary)
            if (sd.isAutoConfig()) {
                configureImplementation(sd.getInterface(), sd.getService(), sd.getAutoConfigName());
            }
        }
        else if (dependency instanceof ConfigurationDependency) {
        	ConfigurationDependency cd = (ConfigurationDependency) dependency;
        	if (cd.isPropagated()) {
        		// change service properties accordingly
        		Dictionary props = calculateServiceProperties();
        		m_registration.setProperties(props);
        	}
        }
    }

    /**
     * Configure a field in the service implementation. The service implementation
     * is searched for fields that have the same type as the class that was specified
     * and for each of these fields, the specified instance is filled in.
     *
     * @param clazz the class to search for
     * @param instance the instance to fill in
     * @param instanceName the name of the instance to fill in, or <code>null</code> if not used
     */
    private void configureImplementation(Class clazz, Object instance, String instanceName) {
    	Object[] instances = getCompositionInstances();
    	if (instances != null) {
	    	for (int i = 0; i < instances.length; i++) {
	    		Object serviceInstance = instances[i];
		        Class serviceClazz = serviceInstance.getClass();
		        while (serviceClazz != null) {
		            Field[] fields = serviceClazz.getDeclaredFields();
		            for (int j = 0; j < fields.length; j++) {
		                if (fields[j].getType().equals(clazz) && (instanceName == null || fields[j].getName().equals(instanceName))) {
		                    try {
		                    	fields[j].setAccessible(true);
		                        // synchronized makes sure the field is actually written to immediately
		                        synchronized (new Object()) {
		                            fields[j].set(serviceInstance, instance);
		                        }
		                    }
		                    catch (Exception e) {
		                        m_logger.log(Logger.LOG_ERROR, "Could not set field " + fields[j], e);
		                        return;
		                    }
		                }
		            }
		            serviceClazz = serviceClazz.getSuperclass();
		        }
	    	}
    	}
    }
    
    public Object[] getCompositionInstances() {
      Object[] instances = null;
      if (m_compositionManagerGetMethod != null) {
	if (m_compositionManager != null) {
	  m_compositionManagerInstance = m_compositionManager;
	}
	else {
	  m_compositionManagerInstance = m_serviceInstance;
	}
	if (m_compositionManagerInstance != null) {
	  try {
	    Method m = m_compositionManagerInstance.getClass().getDeclaredMethod(m_compositionManagerGetMethod, null);
	    m.setAccessible(true);
	    instances = (Object[]) m.invoke(m_compositionManagerInstance, null);
	  }
	  catch (Exception e) {
	    m_logger.log(Logger.LOG_ERROR, "Could not obtain instances from the composition manager.", e);
	    instances = new Object[] { m_serviceInstance };
	  }
	}
      }
      else {
	instances = new Object[] { m_serviceInstance };
      }
      return instances;
    }

    private void configureImplementation(Class clazz, Object instance) {
        configureImplementation(clazz, instance, null);
    }

    private void configureServices(State state) {
        Iterator i = state.getDependencies().iterator();
        while (i.hasNext()) {
            Dependency dependency = (Dependency) i.next();
            if (dependency instanceof ServiceDependency) {
                ServiceDependency sd = (ServiceDependency) dependency;
                if (sd.isAutoConfig()) {
                    if (sd.isRequired()) {
                        configureImplementation(sd.getInterface(), sd.getService(), sd.getAutoConfigName());
                    }
                    else {
                        // for optional services, we do an "ad-hoc" lookup to inject the service if it is
                        // already available even though the tracker has not yet been started
                        configureImplementation(sd.getInterface(), sd.lookupService(), sd.getAutoConfigName());
                    }
                }
                // for required dependencies, we invoke any callbacks here
                if (sd.isRequired()) {
                    sd.invokeAdded();
                }
            }
        }
    }

    private void unconfigureServices(State state) {
        Iterator i = state.getDependencies().iterator();
        while (i.hasNext()) {
            Dependency dependency = (Dependency) i.next();
            if (dependency instanceof ServiceDependency) {
                ServiceDependency sd = (ServiceDependency) dependency;
                // for required dependencies, we invoke any callbacks here
                if (sd.isRequired()) {
                    sd.invokeRemoved();
                }
            }
        }
    }

    private void ensureNotActive() {
    	State state;
    	synchronized (m_dependencies) {
    		state = m_state;
    	}
    	if (!state.isInactive()) {
            throw new IllegalStateException("Cannot modify state while active.");
        }
    }
    boolean isRegistered() {
    	State state;
    	synchronized (m_dependencies) {
    		state = m_state;
    	}
        return (state.isTrackingOptional());
    }

    // ServiceComponent interface
    
    static class SCDImpl implements ServiceComponentDependency {
        private final String m_name;
        private final int m_state;
        private final String m_type;

        public SCDImpl(String name, int state, String type) {
            m_name = name;
            m_state = state;
            m_type = type;
        }

        public String getName() {
            return m_name;
        }

        public int getState() {
            return m_state;
        }

        public String getType() {
            return m_type;
        }
    }
    
    public ServiceComponentDependency[] getComponentDependencies() {
        List deps = getDependencies();
        if (deps != null) {
            ServiceComponentDependency[] result = new ServiceComponentDependency[deps.size()];
            for (int i = 0; i < result.length; i++) {
                Dependency dep = (Dependency) deps.get(i);
                if (dep instanceof ServiceComponentDependency) {
                    result[i] = (ServiceComponentDependency) dep;
                }
                else {
                    result[i] = new SCDImpl(dep.toString(), (dep.isAvailable() ? 1 : 0) + (dep.isRequired() ? 2 : 0), dep.getClass().getName());
                }
            }
            return result;
        }
        return null;
    }

    public String getName() {
        if (m_serviceName instanceof String[]) {
            StringBuffer sb = new StringBuffer();
            String[] names = (String[]) m_serviceName;
            for (int i = 0; i < names.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(names[i]);
            }
            return sb.toString();
        }
        else if (m_serviceName instanceof String) {
            return m_serviceName.toString();
        }
        else {
            return m_implementation.toString();
        }
    }

    public int getState() {
        return (isRegistered() ? 1 : 0);
    }
    
    static {
        NULL_REGISTRATION = (ServiceRegistration) Proxy.newProxyInstance(ServiceImpl.class.getClassLoader(), new Class[] {ServiceRegistration.class}, new DefaultNullObject());
    }
}
