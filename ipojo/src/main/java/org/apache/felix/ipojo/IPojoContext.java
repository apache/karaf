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

import java.io.File;
import java.io.InputStream;
import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * The iPOJO Context is a BundleContext implementation allowing the separation between Bundle context and Service (Bundle) Context
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class IPojoContext implements BundleContext {
	
	/**
	 * BundleContext used to access bundle method
	 */
	private BundleContext m_bundleContext;
	
	/**
	 * Service Context used to access service interaction 
	 */
	private ServiceContext m_serviceContext;
	
	/**
	 * Constructor.
	 * Used when the service context = the bundle context
	 * @param bc : bundle context
	 */
	public IPojoContext(BundleContext bc) {
		m_bundleContext = bc;
		m_serviceContext = new ServiceContextImpl(bc);
	}
	
	/**
	 * Constructor.
	 * Used when the service context and the bundle context are different
	 * @param bc : bundle context
	 * @param sc : service context
	 */
	public IPojoContext(BundleContext bc, ServiceContext sc) {
		m_bundleContext = bc;
		m_serviceContext = sc;
	}

	/**
	 * @see org.osgi.framework.BundleContext#addBundleListener(org.osgi.framework.BundleListener)
	 */
	public void addBundleListener(BundleListener listener) { m_bundleContext.addBundleListener(listener); }

	/**
	 * @see org.osgi.framework.BundleContext#addFrameworkListener(org.osgi.framework.FrameworkListener)
	 */
	public void addFrameworkListener(FrameworkListener listener) { m_bundleContext.addFrameworkListener(listener); }

	/**
	 * @see org.osgi.framework.BundleContext#addServiceListener(org.osgi.framework.ServiceListener, java.lang.String)
	 */
	public void addServiceListener(ServiceListener listener, String filter)
			throws InvalidSyntaxException { m_serviceContext.addServiceListener(listener, filter); }

	/**
	 * @see org.osgi.framework.BundleContext#addServiceListener(org.osgi.framework.ServiceListener)
	 */
	public void addServiceListener(ServiceListener listener) { m_serviceContext.addServiceListener(listener); }

	/**
	 * @see org.osgi.framework.BundleContext#createFilter(java.lang.String)
	 */
	public Filter createFilter(String filter) throws InvalidSyntaxException { return m_bundleContext.createFilter(filter); }

	/**
	 * @see org.osgi.framework.BundleContext#getAllServiceReferences(java.lang.String, java.lang.String)
	 */
	public ServiceReference[] getAllServiceReferences(String clazz,
			String filter) throws InvalidSyntaxException { return m_serviceContext.getAllServiceReferences(clazz, filter); }

	/**
	 * @see org.osgi.framework.BundleContext#getBundle()
	 */
	public Bundle getBundle() { return m_bundleContext.getBundle(); }

	/**
	 * @see org.osgi.framework.BundleContext#getBundle(long)
	 */
	public Bundle getBundle(long id) { return m_bundleContext.getBundle(id); }

	/**
	 * @see org.osgi.framework.BundleContext#getBundles()
	 */
	public Bundle[] getBundles() { return m_bundleContext.getBundles();	}

	/**
	 * @see org.osgi.framework.BundleContext#getDataFile(java.lang.String)
	 */
	public File getDataFile(String filename) { return m_bundleContext.getDataFile(filename); }

	/**
	 * @see org.osgi.framework.BundleContext#getProperty(java.lang.String)
	 */
	public String getProperty(String key) { return m_bundleContext.getProperty(key); }

	/**
	 * @see org.osgi.framework.BundleContext#getService(org.osgi.framework.ServiceReference)
	 */
	public Object getService(ServiceReference reference) { return m_serviceContext.getService(reference); }

	/**
	 * @see org.osgi.framework.BundleContext#getServiceReference(java.lang.String)
	 */
	public ServiceReference getServiceReference(String clazz) { return m_serviceContext.getServiceReference(clazz); }

	/**
	 * @see org.osgi.framework.BundleContext#getServiceReferences(java.lang.String, java.lang.String)
	 */
	public ServiceReference[] getServiceReferences(String clazz, String filter)
			throws InvalidSyntaxException { return m_serviceContext.getServiceReferences(clazz, filter); }

	/**
	 * @see org.osgi.framework.BundleContext#installBundle(java.lang.String)
	 */
	public Bundle installBundle(String location) throws BundleException { return m_bundleContext.installBundle(location); }

	/**
	 * @see org.osgi.framework.BundleContext#installBundle(java.lang.String, java.io.InputStream)
	 */
	public Bundle installBundle(String location, InputStream input)
			throws BundleException { return m_bundleContext.installBundle(location, input); }

	/**
	 * @see org.osgi.framework.BundleContext#registerService(java.lang.String[], java.lang.Object, java.util.Dictionary)
	 */
	public ServiceRegistration registerService(String[] clazzes,
			Object service, Dictionary properties) { return m_serviceContext.registerService(clazzes, service, properties); }

	/**
	 * @see org.osgi.framework.BundleContext#registerService(java.lang.String, java.lang.Object, java.util.Dictionary)
	 */
	public ServiceRegistration registerService(String clazz, Object service,
			Dictionary properties) { return m_serviceContext.registerService(clazz, service, properties); }

	/**
	 * @see org.osgi.framework.BundleContext#removeBundleListener(org.osgi.framework.BundleListener)
	 */
	public void removeBundleListener(BundleListener listener) { m_bundleContext.removeBundleListener(listener); }

	/**
	 * @see org.osgi.framework.BundleContext#removeFrameworkListener(org.osgi.framework.FrameworkListener)
	 */
	public void removeFrameworkListener(FrameworkListener listener) { m_bundleContext.removeFrameworkListener(listener); }

	/**
	 * @see org.osgi.framework.BundleContext#removeServiceListener(org.osgi.framework.ServiceListener)
	 */
	public void removeServiceListener(ServiceListener listener) { m_serviceContext.removeServiceListener(listener); }

	/**
	 * @see org.osgi.framework.BundleContext#ungetService(org.osgi.framework.ServiceReference)
	 */
	public boolean ungetService(ServiceReference reference) { return m_serviceContext.ungetService(reference); }
	
	/**
	 * Set the component manager to the service context.
	 * @param cm : the component manager
	 */
	public void setComponentInstance(ComponentManager cm) {m_serviceContext.setComponentInstance(cm); }
	
	/**
	 * Get the component manager from the service context.
	 * @return the component manager of the service context
	 */
	public ComponentManager getComponentInstance() { return m_serviceContext.getComponentInstance(); }

}
