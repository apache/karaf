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

import java.util.Dictionary;
import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * Configuration dependency that can track the availability of a (valid) configuration.
 * To use it, specify a PID for the configuration. The dependency is always required,
 * because if it is not, it does not make sense to use the dependency manager. In that
 * scenario, simply register your service as a <code>ManagedService(Factory></code> and
 * handle everything yourself. Also, only managed services are supported, not factories.
 * There are a couple of things you need to be aware of when implementing the
 * <code>updated(Dictionary)</code> method:
 * <li>
 * <ul>Make sure it throws a <code>ConfigurationException</code> when you get a
 * configuration that is invalid. In this case, the dependency will not change:
 * if it was not available, it will still not be. If it was available, it will
 * remain available and implicitly assume you keep working with your old
 * configuration.</ul>
 * <ul>This method will be called before all required dependencies are available.
 * Make sure you do not depend on these to parse your settings.</ul>
 * </li>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ConfigurationDependency implements Dependency, ManagedService {
	private BundleContext m_context;
	private String m_pid;
	private ServiceRegistration m_registration;
	private volatile Service m_service;
	private Dictionary m_settings;
	
	public ConfigurationDependency(BundleContext context) {
		m_context = context;
	}
	
	public synchronized boolean isAvailable() {
		return m_settings != null;
	}

	public boolean isRequired() {
		return true;
	}
	
	public void start(Service service) {
		m_service = service;
		Properties props = new Properties();
		props.put(Constants.SERVICE_PID, m_pid);
		m_registration = m_context.registerService(ManagedService.class.getName(), this, props);
	}

	public void stop(Service service) {
		m_registration.unregister();
		m_service = null;
	}

	public void updated(Dictionary settings) throws ConfigurationException {
		// if non-null settings come in, we have to instantiate the service and
		// apply these settings
		((ServiceImpl) m_service).initService();
		Object service = m_service.getService();
		if (service != null) {
			if (service instanceof ManagedService) {
				ManagedService ms = (ManagedService) service;
				ms.updated(settings);
				
				// if exception is thrown here, what does that mean for the
				// state of this dependency? how smart do we want to be??
				// it's okay like this, if the new settings contain errors, we
				// remain in the state we were, assuming that any error causes
				// the "old" configuration to stay in effect
			}
			else {
				throw new IllegalStateException("Could not invoke updated on implementation");
			}
		}
		else {
			throw new IllegalStateException("Could not instantiate implementation");
		}
		// if these settings did not cause a configuration exception, we determine
		// if they have caused the dependency state to change
		Dictionary oldSettings = null; 
		synchronized (this) {
			oldSettings = m_settings;
			m_settings = settings;
		}
		if ((oldSettings == null) && (settings != null)) {
			m_service.dependencyAvailable(this);
		}
		if ((oldSettings == null) && (settings == null)) {
			m_service.dependencyUnavailable(this);
		}
		if ((oldSettings != null) && (settings != null)) {
			m_service.dependencyChanged(this);
		}
	}

	public ConfigurationDependency setPid(String pid) {
		ensureNotActive();
		m_pid = pid;
		return this;
	}
	
    private void ensureNotActive() {
        if (m_service != null) {
            throw new IllegalStateException("Cannot modify state while active.");
        }
    }
    
    public String toString() {
    	return "ConfigurationDependency[" + m_pid + "]";
    }
}
