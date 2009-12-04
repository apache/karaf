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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.dependencymanager.Dependency;
import org.apache.felix.dependencymanager.DependencyService;
import org.apache.felix.dependencymanager.impl.Logger;
import org.apache.felix.dependencymanager.management.ServiceComponentDependency;
import org.apache.felix.dependencymanager.tracker.BundleTracker;
import org.apache.felix.dependencymanager.tracker.BundleTrackerCustomizer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

public class BundleDependency extends AbstractDependency implements Dependency, BundleTrackerCustomizer, ServiceComponentDependency {
	private final BundleContext m_context;
	private boolean m_isStarted;
	private BundleTracker m_tracker;
	private int m_stateMask = Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE;
	private List m_services = new ArrayList();
	private boolean m_isAvailable;
	
    private Object m_callbackInstance;
    private String m_callbackAdded;
    private String m_callbackChanged;
    private String m_callbackRemoved;
    private boolean m_autoConfig;
	private Bundle m_bundleInstance;
	private Filter m_filter;
	private long m_bundleId = -1;
    
    public BundleDependency(BundleContext context, Logger logger) {
        super(logger);
		m_context = context;
		m_autoConfig = true;
	}

	public boolean isInstanceBound() {
		return false; // TODO for now we are never bound to the service implementation instance
	}

	public synchronized boolean isAvailable() {
        return m_isAvailable;
    }


    public void start(DependencyService service) {
        boolean needsStarting = false;
		synchronized (this) {
		    m_services.add(service);
		    if (!m_isStarted) {
    			m_tracker = new BundleTracker(m_context, m_stateMask, this);
    			m_isStarted = true;
    			needsStarting = true;
		    }
		}
		if (needsStarting) {
		    m_tracker.open();
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
            m_tracker.close();
            m_tracker = null;
            m_services.remove(service);
        }            
	}

	public String getName() {
        StringBuilder sb = new StringBuilder();
        sb.append(m_bundleInstance.getSymbolicName());
        sb.append(' ');
        sb.append(m_bundleInstance.getVersion());
        sb.append(' ');
        sb.append(Integer.toString(m_stateMask, 2));
        if (m_filter != null) {
            sb.append(' ');
            sb.append(m_filter.toString());
        }
        return sb.toString();
	}

	public int getState() {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getType() {
		return "bundle";
	}

	public Object addingBundle(Bundle bundle, BundleEvent event) {
		// if we don't like a bundle, we could reject it here by returning null
		if (m_bundleId >= 0 && m_bundleId != bundle.getBundleId()) {
			return null;
		}
		Filter filter = m_filter;
		if (filter != null) {
			Dictionary headers = bundle.getHeaders();
			if (!m_filter.match(headers)) {
				return null;
			}
		}
        return bundle;
	}
	
	public void addedBundle(Bundle bundle, BundleEvent event, Object object) {
	    boolean makeAvailable = makeAvailable();
	    Object[] services = m_services.toArray();
	    for (int i = 0; i < services.length; i++) {
	        DependencyService ds = (DependencyService) services[i];
	        if (makeAvailable) {
	            ds.dependencyAvailable(this);
	            if (!isRequired()) {
	                invokeAdded(ds, bundle);
	            }
	        }
	        else {
	            ds.dependencyChanged(this);
	            invokeAdded(ds, bundle);
	        }
	    }
	}

	public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
		Object[] services = m_services.toArray();
        for (int i = 0; i < services.length; i++) {
            DependencyService ds = (DependencyService) services[i];
            ds.dependencyChanged(this);
            if (ds.isRegistered()) {
                invokeChanged(ds, bundle);
            }
        }
	}

	public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
		boolean makeUnavailable = makeUnavailable();
        Object[] services = m_services.toArray();
        for (int i = 0; i < services.length; i++) {
            DependencyService ds = (DependencyService) services[i];
            if (makeUnavailable) {
                ds.dependencyUnavailable(this);
                if (!isRequired()) {
                    invokeRemoved(ds, bundle);
                }
            }
            else {
                ds.dependencyChanged(this);
                invokeRemoved(ds, bundle);
            }
        }
	}
	
    private synchronized boolean makeAvailable() {
        if (!isAvailable()) {
            m_isAvailable = true;
            return true;
        }
        return false;
    }
    
    private synchronized boolean makeUnavailable() {
        if ((isAvailable()) && (m_tracker.getTrackingCount() == 0)) {
            m_isAvailable = false;
            return true;
        }
        return false;
    }
    
    public void invokeAdded(DependencyService dependencyService, Bundle service) {
        Object[] callbackInstances = getCallbackInstances(dependencyService);
        if ((callbackInstances != null) && (m_callbackAdded != null)) {
            invokeCallbackMethod(callbackInstances, m_callbackAdded, 
                new Class[][] {{Bundle.class}, {Object.class}, {}},
                new Object[][] {{service}, {service}, {}}
            );
        }
    }

    public void invokeChanged(DependencyService dependencyService, Bundle service) {
        Object[] callbackInstances = getCallbackInstances(dependencyService);
        if ((callbackInstances != null) && (m_callbackChanged != null)) {
            invokeCallbackMethod(callbackInstances, m_callbackChanged, 
                new Class[][] {{Bundle.class}, {Object.class}, {}},
                new Object[][] {{service}, {service}, {}}
            );
        }
    }
    
    public void invokeRemoved(DependencyService dependencyService, Bundle service) {
        Object[] callbackInstances = getCallbackInstances(dependencyService);
        if ((callbackInstances != null) && (m_callbackRemoved != null)) {
            invokeCallbackMethod(callbackInstances, m_callbackRemoved,
              new Class[][] {{Bundle.class}, {Object.class}, {}},
              new Object[][] {{service}, {service}, {}}
            );
        }
    }

    private synchronized Object[] getCallbackInstances(DependencyService dependencyService) {
        if (m_callbackInstance == null) {
            return dependencyService.getCompositionInstances();
        }
        else {
            return new Object[] { m_callbackInstance };
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
    public synchronized BundleDependency setCallbacks(String added, String removed) {
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
    public synchronized BundleDependency setCallbacks(String added, String changed, String removed) {
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
    public synchronized BundleDependency setCallbacks(Object instance, String added, String removed) {
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
    public synchronized BundleDependency setCallbacks(Object instance, String added, String changed, String removed) {
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
        if (m_tracker != null) {
            throw new IllegalStateException("Cannot modify state while active.");
        }
    }
    public synchronized BundleDependency setAutoConfig(boolean autoConfig) {
        ensureNotActive();
        m_autoConfig = autoConfig;
        return this;
    }
    
    public synchronized BundleDependency setRequired(boolean required) {
        ensureNotActive();
        setIsRequired(required);
        return this;
    }

	public BundleDependency setBundle(Bundle bundle) {
		m_bundleId = bundle.getBundleId();
		return this;
	}

	public BundleDependency setFilter(String filter) throws IllegalArgumentException {
		if (filter != null) {
			try {
				m_filter = m_context.createFilter(filter);
			} 
			catch (InvalidSyntaxException e) {
				throw new IllegalArgumentException(e.getMessage());
			}
		}
		return this;
	}
	
	public BundleDependency setStateMask(int mask) {
		m_stateMask = mask;
		return this;
	}
	
    public synchronized boolean isAutoConfig() {
        return m_autoConfig;
    }

    public Bundle getBundle() {
    	Bundle[] bundles = m_tracker.getBundles();
    	if (bundles != null && bundles.length > 0) {
    		return bundles[0];
    	}
    	return null;
    }
}
