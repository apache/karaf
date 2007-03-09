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

import java.util.Dictionary;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Default iPOJO Service Context.
 * this service context delegate all calls on the bundle context.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
/**
 * @author Clement
 *
 */
public class DefaultServiceContext implements ServiceContext {
	
	/**
	 * The bundle context on which delegate.
	 */
	private BundleContext m_context;
	
	/**
	 * Instance attached to this service context.
	 */
	private ComponentInstance m_instance;
	
	/**
	 * Constructor. 
	 * @param bc : the bundle context on which delegate.
	 */
	public DefaultServiceContext(BundleContext bc) { m_context = bc; }

	/**
	 * @see org.apache.felix.ipojo.ServiceContext#addServiceListener(org.osgi.framework.ServiceListener, java.lang.String)
	 */
	public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
		m_context.addServiceListener(listener, filter);
	}
	
	/**
	 * @see org.apache.felix.ipojo.ServiceContext#addServiceListener(org.osgi.framework.ServiceListener)
	 */
	public void addServiceListener(ServiceListener listener) {
		m_context.addServiceListener(listener);
	}

	/**
	 * @see org.apache.felix.ipojo.ServiceContext#getAllServiceReferences(java.lang.String, java.lang.String)
	 */
	public ServiceReference[] getAllServiceReferences(String clazz,
			String filter) throws InvalidSyntaxException {
		return m_context.getAllServiceReferences(clazz, filter);
	}

	/**
	 * @see org.apache.felix.ipojo.ServiceContext#getService(org.osgi.framework.ServiceReference)
	 */
	public Object getService(ServiceReference reference) {
		return m_context.getService(reference);
	}

	/**
	 * @see org.apache.felix.ipojo.ServiceContext#getServiceReference(java.lang.String)
	 */
	public ServiceReference getServiceReference(String clazz) {
		return m_context.getServiceReference(clazz);
	}

	/**
	 * @see org.apache.felix.ipojo.ServiceContext#getServiceReferences(java.lang.String, java.lang.String)
	 */
	public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
		return m_context.getServiceReferences(clazz, filter);
	}

	/**
	 * @see org.apache.felix.ipojo.ServiceContext#registerService(java.lang.String[], java.lang.Object, java.util.Dictionary)
	 */
	public ServiceRegistration registerService(String[] clazzes,
			Object service, Dictionary properties) {
		return m_context.registerService(clazzes, service, properties);
	}

	/**
	 * @see org.apache.felix.ipojo.ServiceContext#registerService(java.lang.String, java.lang.Object, java.util.Dictionary)
	 */
	public ServiceRegistration registerService(String clazz, Object service,
			Dictionary properties) {
		return m_context.registerService(clazz, service, properties);
	}

	/**
	 * @see org.apache.felix.ipojo.ServiceContext#removeServiceListener(org.osgi.framework.ServiceListener)
	 */
	public void removeServiceListener(ServiceListener listener) {
		m_context.removeServiceListener(listener);

	}

	/**
	 * @see org.apache.felix.ipojo.ServiceContext#ungetService(org.osgi.framework.ServiceReference)
	 */
	public boolean ungetService(ServiceReference reference) {
		return m_context.ungetService(reference);
	}

	/**
	 * @see org.apache.felix.ipojo.ServiceContext#getComponentInstance()
	 */
	public ComponentInstance getComponentInstance() {
		return m_instance;
	}
	
	/**
	 * @see org.apache.felix.ipojo.ServiceContext#setComponentInstance(org.apache.felix.ipojo.ComponentInstance)
	 */
	public void setComponentInstance(ComponentInstance ci) { m_instance =  ci; }

}
