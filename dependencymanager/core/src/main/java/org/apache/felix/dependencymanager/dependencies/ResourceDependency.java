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
package org.apache.felix.dependencymanager.dependencies;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Properties;

import org.apache.felix.dependencymanager.Dependency;
import org.apache.felix.dependencymanager.DependencyService;
import org.apache.felix.dependencymanager.impl.Logger;
import org.apache.felix.dependencymanager.impl.ServiceImpl;
import org.apache.felix.dependencymanager.resources.Resource;
import org.apache.felix.dependencymanager.resources.ResourceHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class ResourceDependency implements Dependency, ResourceHandler {
	private volatile BundleContext m_context;
	private volatile ServiceRegistration m_registration;
	private long m_resourceCounter;

    private Object m_callbackInstance;
    private String m_callbackAdded;
    private String m_callbackChanged;
    private String m_callbackRemoved;
    private boolean m_autoConfig;
    private final Logger m_logger;
    private String m_autoConfigInstance;
    private DependencyService m_service;
	private boolean m_isRequired;
	private String m_resourceFilter;
	private Resource m_resource;
	private Resource m_trackedResource;

	
    public ResourceDependency(BundleContext context, Logger logger) {
    	m_context = context;
    	m_logger = logger;
    	m_autoConfig = true;
    }
    
	public synchronized boolean isAvailable() {
		return m_resourceCounter > 0;
	}

	public boolean isRequired() {
		return m_isRequired;
	}
	
	public boolean isInstanceBound() {
		return false; // TODO for now we are never bound to the service implementation instance
	}

	public void start(DependencyService service) {
		m_service = service;
		Properties props = new Properties();
		// TODO create constant for this key
		props.setProperty("filter", m_resourceFilter);
		m_registration = m_context.registerService(ResourceHandler.class.getName(), this, props);
		
	}

	public void stop(DependencyService service) {
		m_registration.unregister();
		m_registration = null;
	}

	public void added(Resource resource) {
		System.out.println("RD ADDED " + resource);
		long counter;
		synchronized (this) {
			m_resourceCounter++;
			counter = m_resourceCounter;
			m_resource = resource; // TODO this really sucks as a way to track a single resource
		}
        if (counter == 1) {
            m_service.dependencyAvailable(this);
        }
        else {
            m_service.dependencyChanged(this);
        }
        // try to invoke callback, if specified, but only for optional dependencies
        // because callbacks for required dependencies are handled differently
        if (!isRequired()) {
            invokeAdded(resource);
        }
	}

	public void changed(Resource resource) {
		invokeChanged(resource);
	}

	public void removed(Resource resource) {
		long counter;
		synchronized (this) {
			m_resourceCounter--;
			counter = m_resourceCounter;
		}
        if (counter == 0) {
            m_service.dependencyUnavailable(this);
        }
        // try to invoke callback, if specified, but only for optional dependencies
        // because callbacks for required dependencies are handled differently
        if (!isRequired()) {
            invokeRemoved(resource);
        }
	}
	
    public void invokeAdded() {
    	// TODO fixme
        //invokeAdded(m_bundleInstance);
    }

    public void invokeAdded(Resource serviceInstance) {
        Object[] callbackInstances = getCallbackInstances();
        if ((callbackInstances != null) && (m_callbackAdded != null)) {
                invokeCallbackMethod(callbackInstances, m_callbackAdded, serviceInstance);
        }
    }

    public void invokeChanged(Resource serviceInstance) {
        Object[] callbackInstances = getCallbackInstances();
        if ((callbackInstances != null) && (m_callbackChanged != null)) {
//                if (m_reference == null) {
//                    Thread.dumpStack();
//                }
                invokeCallbackMethod(callbackInstances, m_callbackChanged, serviceInstance);
        }
    }

    
    public void invokeRemoved() {
    	// TODO fixme
        //invokeRemoved(m_bundleInstance);
    }
    
    public void invokeRemoved(Resource serviceInstance) {
        Object[] callbackInstances = getCallbackInstances();
        if ((callbackInstances != null) && (m_callbackRemoved != null)) {
//                if (m_reference == null) {
//                    Thread.dumpStack();
//                }
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
    private synchronized Object[] getCallbackInstances() {
        Object[] callbackInstances = ((ServiceImpl) m_service).getCompositionInstances();
        if (m_callbackInstance == null) {
            return callbackInstances;
        }
        Object[] res = new Object[callbackInstances.length + 1];
        res[0] = m_callbackInstance; //this could also be extended to an array...?
        System.arraycopy(callbackInstances, 0, res, 1, callbackInstances.length);
        return res;
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
		m_resourceFilter = resourceFilter;
		return this;
	}
    public synchronized boolean isAutoConfig() {
        return m_autoConfig;
    }

    public Resource getResource() {
    	System.out.println("Fetching resource");
    	return m_resource;
    }
}
