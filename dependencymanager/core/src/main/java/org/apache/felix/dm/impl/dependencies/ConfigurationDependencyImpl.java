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
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.felix.dm.dependencies.ConfigurationDependency;
import org.apache.felix.dm.dependencies.PropertyMetaData;
import org.apache.felix.dm.impl.Logger;
import org.apache.felix.dm.impl.metatype.MetaTypeProviderImpl;
import org.apache.felix.dm.management.ServiceComponentDependency;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * Configuration dependency that can track the availability of a (valid) configuration.
 * To use it, specify a PID for the configuration. The dependency is always required,
 * because if it is not, it does not make sense to use the dependency manager. In that
 * scenario, simply register your service as a <code>ManagedService(Factory)</code> and
 * handle everything yourself. Also, only managed services are supported, not factories.
 * There are a couple of things you need to be aware of when implementing the
 * <code>updated(Dictionary)</code> method:
 * <ul>
 * <li>Make sure it throws a <code>ConfigurationException</code> when you get a
 * configuration that is invalid. In this case, the dependency will not change:
 * if it was not available, it will still not be. If it was available, it will
 * remain available and implicitly assume you keep working with your old
 * configuration.</li>
 * <li>This method will be called before all required dependencies are available.
 * Make sure you do not depend on these to parse your settings.</li>
 * </ul>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ConfigurationDependencyImpl implements ConfigurationDependency, ManagedService, ServiceComponentDependency, DependencyActivation {
	private BundleContext m_context;
	private String m_pid;
	private ServiceRegistration m_registration;
    protected List m_services = new ArrayList();
	private Dictionary m_settings;
	private boolean m_propagate;
	private final Logger m_logger;
    private String m_callback;
    private boolean m_isStarted;
	private final Set m_updateInvokedCache = new HashSet();
    private MetaTypeProviderImpl m_metaType;
	
	public ConfigurationDependencyImpl(BundleContext context, Logger logger) {
		m_context = context;
		m_logger = logger;
	}
	
	public synchronized boolean isAvailable() {
		return m_settings != null;
	}

	/**
	 * Will always return <code>true</code> as optional configuration dependencies
	 * do not make sense. You might as well just implement <code>ManagedService</code>
	 * yourself in those cases.
	 */
	public boolean isRequired() {
		return true;
	}
	
	public boolean isInstanceBound() {
	    // for now, configuration dependencies never are
	    return false;
	}
	
	/**
	 * Returns <code>true</code> when configuration properties should be propagated
	 * as service properties.
	 */
	public boolean isPropagated() {
		return m_propagate;
	}
	
	public Dictionary getConfiguration() {
		return m_settings;
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
	        Properties props = new Properties();
	        props.put(Constants.SERVICE_PID, m_pid);
	        ManagedService ms = this;
	        if (m_metaType != null) {
	            ms = m_metaType;
	        }
	        m_registration = m_context.registerService(ManagedService.class.getName(), ms, props);
	    }
	}

	public void stop(DependencyService service) {
        boolean needsStopping = false;
        synchronized (this) {
            if (m_services.size() == 1 && m_services.contains(service)) {
                m_isStarted = false;
                needsStopping = true;
            }
        }
        if (needsStopping) {
            m_registration.unregister();
            m_registration = null;
            m_services.remove(service);
        }
	}

    public ConfigurationDependency setCallback(String callback) {
		m_callback = callback;
		return this;
	}

	public void updated(Dictionary settings) throws ConfigurationException {
	    synchronized (m_updateInvokedCache) {
	        m_updateInvokedCache.clear();
	    }
	    Dictionary oldSettings = null; 
	    synchronized (this) {
	        oldSettings = m_settings;
	    }
	    
	    if (oldSettings == null && settings == null) {
	        // CM has started but our configuration is not still present in the CM database: ignore
	        return;
	    }

	    Object[] services = m_services.toArray();
        for (int i = 0; i < services.length; i++) {
            DependencyService ds = (DependencyService) services[i];
            // if non-null settings come in, we have to instantiate the service and
            // apply these settings
            ds.initService();
            Object service = ds.getService();

            if (service != null) {
                invokeUpdate(ds, service, settings);
            }
            else {
                m_logger.log(Logger.LOG_ERROR, "Service " + ds + " with configuration dependency " + this + " could not be instantiated.");
                return;
            }
        }

		synchronized (this) {
			m_settings = settings;
		}
		
        for (int i = 0; i < services.length; i++) {
            DependencyService ds = (DependencyService) services[i];
            // If these settings did not cause a configuration exception, we determine if they have 
            // caused the dependency state to change
            if ((oldSettings == null) && (settings != null)) {
                ds.dependencyAvailable(this);
            }
            if ((oldSettings != null) && (settings == null)) {
                ds.dependencyUnavailable(this);
            }
            if ((oldSettings != null) && (settings != null)) {
                ds.dependencyChanged(this);
            }
        }
	}

    public void invokeUpdate(DependencyService ds, Object service, Dictionary settings) throws ConfigurationException {
        boolean wasAdded;
        synchronized (m_updateInvokedCache) {
            wasAdded = m_updateInvokedCache.add(ds);
        }
        if (wasAdded) {
            String callback = (m_callback == null) ? "updated" : m_callback;
            Method m;
            try {
                m = service.getClass().getDeclaredMethod(callback, new Class[] { Dictionary.class });
                m.setAccessible(true);
                // if exception is thrown here, what does that mean for the
                // state of this dependency? how smart do we want to be??
                // it's okay like this, if the new settings contain errors, we
                // remain in the state we were, assuming that any error causes
                // the "old" configuration to stay in effect.
                // CM will log any thrown exceptions.
                m.invoke(service, new Object[] { settings });
            } 
            catch (InvocationTargetException e) {
                // The component has thrown an exception during it's callback invocation.
                if (e.getTargetException() instanceof ConfigurationException) {
                    // the callback threw an OSGi ConfigurationException: just re-throw it.
                    throw (ConfigurationException) e.getTargetException();
                }
                else {
                    // wrap the callback exception into a ConfigurationException.
                    throw new ConfigurationException(null, "Service " + ds + " with " + this.toString() + " could not be updated", e.getTargetException());
                }
            }
            catch (Throwable t) {
                // wrap any other exception as a ConfigurationException.
                throw new ConfigurationException(null, "Service " + ds + " with " + this.toString() + " could not be updated", t);
            }
        }
    }

	/**
	 * Sets the <code>service.pid</code> of the configuration you
	 * are depending on.
	 */
	public ConfigurationDependency setPid(String pid) {
		ensureNotActive();
		m_pid = pid;
		return this;
	}

	/**
	 * Sets propagation of the configuration properties to the service
	 * properties. Any additional service properties specified directly
	 * are merged with these.
	 */
	public ConfigurationDependency setPropagate(boolean propagate) {
		ensureNotActive();
		m_propagate = propagate;
		return this;
	}
	
	private void ensureNotActive() {
	  	if (m_services != null && m_services.size() > 0) {
	  	  throw new IllegalStateException("Cannot modify state while active.");
	  	}
    }
    
    public String toString() {
    	return "ConfigurationDependency[" + m_pid + "]";
    }

    public String getName() {
        return m_pid;
    }

    public int getState() {
        return (isAvailable() ? 1 : 0) + (isRequired() ? 2 : 0);
    }

    public String getType() {
        return "configuration";
    }

    public Object getAutoConfigInstance() {
        return getConfiguration();
    }

    public String getAutoConfigName() {
        // TODO Auto-generated method stub
        return null;
    }

    public Class getAutoConfigType() {
        return Dictionary.class;
    }

    public void invokeAdded(DependencyService service) {
        try {
            invokeUpdate(service, service.getService(), getConfiguration());
        }
        catch (ConfigurationException e) {
            // if this happens, it's definitely an inconsistency, since we
            // asked the instance the same question before (if this is a
            // valid configuration) and then it was
            e.printStackTrace();
        }
    }

    public void invokeRemoved(DependencyService service) {
        // TODO Auto-generated method stub
    }

    public boolean isAutoConfig() {
        // TODO Auto-generated method stub
        return false;
    }

    public Dictionary getProperties() {
        return getConfiguration();
    }
    
    public BundleContext getBundleContext() {
        return m_context;
    }
    
    public Logger getLogger() {
        return m_logger;
    }
    
    public ConfigurationDependency add(PropertyMetaData properties)
    {
        createMetaTypeImpl();
        m_metaType.add(properties);
       return this;
    }

    public ConfigurationDependency setDescription(String description)
    {
        createMetaTypeImpl();
        m_metaType.setDescription(description);
       return this;
    }

    public ConfigurationDependency setHeading(String heading)
    {
        createMetaTypeImpl();
        m_metaType.setName(heading);
       return this;
    }
    
    public ConfigurationDependency setLocalization(String path)
    {
        createMetaTypeImpl();
        m_metaType.setLocalization(path);
        return this;
    }
    
    private synchronized void createMetaTypeImpl() {
        if (m_metaType == null) {
            m_metaType = new MetaTypeProviderImpl(this);
        }
    }
}
