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
package org.apache.felix.dm.impl.dependencies;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import org.apache.felix.dm.dependencies.ResourceDependency;
import org.apache.felix.dm.impl.Logger;
import org.apache.felix.dm.resources.Resource;
import org.apache.felix.dm.resources.ResourceHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class ResourceDependencyImpl implements ResourceDependency, ResourceHandler, DependencyActivation {
	private volatile BundleContext m_context;
	private volatile ServiceRegistration m_registration;
//	private long m_resourceCounter;

    private Object m_callbackInstance;
    private String m_callbackAdded;
    private String m_callbackChanged;
    private String m_callbackRemoved;
    private boolean m_autoConfig;
    private final Logger m_logger;
    private String m_autoConfigInstance;
    protected List m_services = new ArrayList();
	private boolean m_isRequired;
	private String m_resourceFilter;
	private Resource m_trackedResource;
    private boolean m_isStarted;
    private List m_resources = new ArrayList();
    private Resource m_resourceInstance;
    private boolean m_propagate;
	
    public ResourceDependencyImpl(BundleContext context, Logger logger) {
    	m_context = context;
    	m_logger = logger;
    	m_autoConfig = true;
    }
    
	public synchronized boolean isAvailable() {
		return m_resources.size() > 0;
	}

	public boolean isRequired() {
		return m_isRequired;
	}
	
	public boolean isInstanceBound() {
		return false; // TODO for now we are never bound to the service implementation instance
	}

	public void start(DependencyService service) {
	    boolean needsStarting = false;
	    synchronized (this) {
	        m_services.add(service);
	        if (!m_isStarted) {
	            m_isStarted = true;
	            needsStarting = true;
	        }
	    }
	    if (needsStarting) {
	        Properties props = null;
	        if (m_resourceFilter != null) {
	            props = new Properties();
	            props.setProperty(Resource.FILTER, m_resourceFilter);
	        }
	        m_registration = m_context.registerService(ResourceHandler.class.getName(), this, props);
	    }
	}

	public void stop(DependencyService service) {
	    boolean needsStopping = false;
	    synchronized (this) {
            if (m_services.size() == 1 && m_services.contains(service)) {
                m_isStarted = false;
                needsStopping = true;
                m_services.remove(service);
            }
	    }
	    if (needsStopping) {
	        m_registration.unregister();
	        m_registration = null;
	    }
	}

	public void added(Resource resource) {
	    if (m_trackedResource == null || m_trackedResource.equals(resource)) {
    		long counter;
    		Object[] services;
    		synchronized (this) {
    		    m_resources.add(resource);
    			counter = m_resources.size();
    			services = m_services.toArray();
    		}
            for (int i = 0; i < services.length; i++) {
                DependencyService ds = (DependencyService) services[i];
                if (counter == 1) {
                    ds.dependencyAvailable(this);
                    if (!isRequired()) {
                        invokeAdded(ds, resource);
                    }
                }
                else {
                    ds.dependencyChanged(this);
                    invokeAdded(ds, resource);
                }
            }
	    }
	}

	public void changed(Resource resource) {
        if (m_trackedResource == null || m_trackedResource.equals(resource)) {
            Object[] services;
            synchronized (this) {
                services = m_services.toArray();
            }
            for (int i = 0; i < services.length; i++) {
                DependencyService ds = (DependencyService) services[i];
                invokeChanged(ds, resource);
            }
        }
	}

	public void removed(Resource resource) {
        if (m_trackedResource == null || m_trackedResource.equals(resource)) {
    		long counter;
    		Object[] services;
    		synchronized (this) {
    		    m_resources.remove(resource);
    			counter = m_resources.size();
    			services = m_services.toArray();
    		}
            for (int i = 0; i < services.length; i++) {
                DependencyService ds = (DependencyService) services[i];
                if (counter == 0) {
                    ds.dependencyUnavailable(this);
                    if (!isRequired()) {
                        invokeRemoved(ds, resource);
                    }
                }
                else {
                    ds.dependencyChanged(this);
                    invokeRemoved(ds, resource);
                }
            }
        }
	}
	
    public void invokeAdded(DependencyService ds, Resource serviceInstance) {
        Object[] callbackInstances = getCallbackInstances(ds);
        if ((callbackInstances != null) && (m_callbackAdded != null)) {
            invokeCallbackMethod(callbackInstances, m_callbackAdded, serviceInstance);
        }
    }

    public void invokeChanged(DependencyService ds, Resource serviceInstance) {
        Object[] callbackInstances = getCallbackInstances(ds);
        if ((callbackInstances != null) && (m_callbackChanged != null)) {
            invokeCallbackMethod(callbackInstances, m_callbackChanged, serviceInstance);
        }
    }

    public void invokeRemoved(DependencyService ds, Resource serviceInstance) {
        Object[] callbackInstances = getCallbackInstances(ds);
        if ((callbackInstances != null) && (m_callbackRemoved != null)) {
            invokeCallbackMethod(callbackInstances, m_callbackRemoved, serviceInstance);
        }
    }

    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added or removed. When you specify callbacks, the auto configuration 
     * feature is automatically turned off, because we're assuming you don't need it in this 
     * case.
     * 
     * @param added the method to call when a service was added
     * @param removed the method to call when a service was removed
     * @return this service dependency
     */
    public synchronized ResourceDependency setCallbacks(String added, String removed) {
        return setCallbacks(null, added, null, removed);
    }

    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added, changed or removed. When you specify callbacks, the auto 
     * configuration feature is automatically turned off, because we're assuming you don't 
     * need it in this case.
     * 
     * @param added the method to call when a service was added
     * @param changed the method to call when a service was changed
     * @param removed the method to call when a service was removed
     * @return this service dependency
     */
    public synchronized ResourceDependency setCallbacks(String added, String changed, String removed) {
        return setCallbacks(null, added, changed, removed);
    }

    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added or removed. They are called on the instance you provide. When you
     * specify callbacks, the auto configuration feature is automatically turned off, because
     * we're assuming you don't need it in this case.
     * 
     * @param instance the instance to call the callbacks on
     * @param added the method to call when a service was added
     * @param removed the method to call when a service was removed
     * @return this service dependency
     */
    public synchronized ResourceDependency setCallbacks(Object instance, String added, String removed) {
        return setCallbacks(instance, added, null, removed);
    }
    
    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added, changed or removed. They are called on the instance you provide. When you
     * specify callbacks, the auto configuration feature is automatically turned off, because
     * we're assuming you don't need it in this case.
     * 
     * @param instance the instance to call the callbacks on
     * @param added the method to call when a service was added
     * @param changed the method to call when a service was changed
     * @param removed the method to call when a service was removed
     * @return this service dependency
     */
    public synchronized ResourceDependency setCallbacks(Object instance, String added, String changed, String removed) {
        ensureNotActive();
        // if at least one valid callback is specified, we turn off auto configuration
        if (added != null || removed != null || changed != null) {
            setAutoConfig(false);
        }
        m_callbackInstance = instance;
        m_callbackAdded = added;
        m_callbackChanged = changed;
        m_callbackRemoved = removed;
        return this;
    }
    
    private void ensureNotActive() {
        if (m_registration != null) {
            throw new IllegalStateException("Cannot modify state while active.");
        }
    }
    
    /**
     * Sets auto configuration for this service. Auto configuration allows the
     * dependency to fill in any attributes in the service implementation that
     * are of the same type as this dependency. Default is on.
     * 
     * @param autoConfig the value of auto config
     * @return this service dependency
     */
    public synchronized ResourceDependency setAutoConfig(boolean autoConfig) {
        ensureNotActive();
        m_autoConfig = autoConfig;
        return this;
    }
    
    /**
     * Sets auto configuration for this service. Auto configuration allows the
     * dependency to fill in the attribute in the service implementation that
     * has the same type and instance name.
     * 
     * @param instanceName the name of attribute to auto config
     * @return this service dependency
     */
    public synchronized ResourceDependency setAutoConfig(String instanceName) {
        ensureNotActive();
        m_autoConfig = (instanceName != null);
        m_autoConfigInstance = instanceName;
        return this;
    }
    
    private void invokeCallbackMethod(Object[] instances, String methodName, Object service) {
        for (int i = 0; i < instances.length; i++) {
            try {
                invokeCallbackMethod(instances[i], methodName, service);
            }
            catch (NoSuchMethodException e) {
                m_logger.log(Logger.LOG_DEBUG, "Method '" + methodName + "' does not exist on " + instances[i] + ". Callback skipped.");
            }
        }
    }

    private void invokeCallbackMethod(Object instance, String methodName, Object service) throws NoSuchMethodException {
        Class currentClazz = instance.getClass();
        boolean done = false;
        while (!done && currentClazz != null) {
            done = invokeMethod(instance, currentClazz, methodName,
                new Class[][] {{Resource.class}, {Object.class}, {}},
                new Object[][] {{service}, {service}, {}},
                false);
            if (!done) {
                currentClazz = currentClazz.getSuperclass();
            }
        }
        if (!done && currentClazz == null) {
            throw new NoSuchMethodException(methodName);
        }
    }
    
    private boolean invokeMethod(Object object, Class clazz, String name, Class[][] signatures, Object[][] parameters, boolean isSuper) {
        Method m = null;
        for (int i = 0; i < signatures.length; i++) {
            Class[] signature = signatures[i];
            try {
                m = clazz.getDeclaredMethod(name, signature);
                if (!(isSuper && Modifier.isPrivate(m.getModifiers()))) {
                    m.setAccessible(true);
                    try {
                        m.invoke(object, parameters[i]);
                    }
                    catch (InvocationTargetException e) {
                        m_logger.log(Logger.LOG_ERROR, "Exception while invoking method " + m + ".", e);
                    }
                    // we did find and invoke the method, so we return true
                    return true;
                }
            }
            catch (NoSuchMethodException e) {
                // ignore this and keep looking
            }
            catch (Exception e) {
                // could not even try to invoke the method
                m_logger.log(Logger.LOG_ERROR, "Exception while trying to invoke method " + m + ".", e);
            }
        }
        return false;
    }
    
    private synchronized Object[] getCallbackInstances(DependencyService ds) {
        if (m_callbackInstance == null) {
            return ds.getCompositionInstances();
        }
        else {
            return new Object[] { m_callbackInstance };
        }
    }

	public ResourceDependency setResource(Resource resource) {
		m_trackedResource = resource;
		return this;
	}
	
    public synchronized ResourceDependency setRequired(boolean required) {
        ensureNotActive();
        m_isRequired = required;
        return this;
    }

	public ResourceDependency setFilter(String resourceFilter) {
        ensureNotActive();
		m_resourceFilter = resourceFilter;
		return this;
	}
    public synchronized boolean isAutoConfig() {
        return m_autoConfig;
    }
    
    public Resource getResource() {
    	return lookupResource();
    }

    private Resource lookupResource() {
        try {
            return (Resource) m_resources.get(0);
        }
        catch (IndexOutOfBoundsException e) {
            return null;
        }
    }
    
    public Object getAutoConfigInstance() {
        return lookupResource();
    }

    public String getAutoConfigName() {
        return m_autoConfigInstance;
    }

    public Class getAutoConfigType() {
        return Resource.class;
    }

    public void invokeAdded(DependencyService service) {
        // we remember these for future reference, needed for required callbacks
        m_resourceInstance = lookupResource();
        invokeAdded(service, m_resourceInstance);
    }

    public void invokeRemoved(DependencyService service) {
        invokeRemoved(service, m_resourceInstance);
        m_resourceInstance = null;
    }

    public ResourceDependency setPropagate(boolean propagate) {
        ensureNotActive();
        m_propagate = propagate;
        return this;
    }
    
    public Dictionary getProperties() {
        Resource resource = lookupResource();
        if (resource != null) {
            Properties props = new Properties();
            props.put(Resource.NAME, resource.getName());
            props.put(Resource.PATH, resource.getPath());
            props.put(Resource.REPOSITORY, resource.getRepository());
            return props;
        }
        else {
            throw new IllegalStateException("cannot find resource");
        }
    }

    public boolean isPropagated() {
        return m_propagate;
    }
}
