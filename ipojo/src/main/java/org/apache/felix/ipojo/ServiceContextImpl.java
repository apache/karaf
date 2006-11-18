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

public class ServiceContextImpl implements ServiceContext {
	
	private BundleContext m_context;
	
	private ComponentManagerImpl m_instance;
	
	public ServiceContextImpl(BundleContext bc) { m_context = bc; }

	public void addServiceListener(ServiceListener listener, String filter)
			throws InvalidSyntaxException {
		m_context.addServiceListener(listener, filter);
	}
	
	public void addServiceListener(ServiceListener listener) {
		m_context.addServiceListener(listener);
	}

	public ServiceReference[] getAllServiceReferences(String clazz,
			String filter) throws InvalidSyntaxException {
		return m_context.getAllServiceReferences(clazz, filter);
	}

	public Object getService(ServiceReference reference) {
		return m_context.getService(reference);
	}

	public ServiceReference getServiceReference(String clazz) {
		return m_context.getServiceReference(clazz);
	}

	public ServiceReference[] getServiceReferences(String clazz, String filter)
			throws InvalidSyntaxException {
		return m_context.getServiceReferences(clazz, filter);
	}

	public ServiceRegistration registerService(String[] clazzes,
			Object service, Dictionary properties) {
		return m_context.registerService(clazzes, service, properties);
	}

	public ServiceRegistration registerService(String clazz, Object service,
			Dictionary properties) {
		return m_context.registerService(clazz, service, properties);
	}

	public void removeServiceListener(ServiceListener listener) {
		m_context.removeServiceListener(listener);

	}

	public boolean ungetService(ServiceReference reference) {
		return m_context.ungetService(reference);
	}

	public ComponentManager getComponentInstance() {
		return m_instance;
	}
	
	public void setComponentInstance(ComponentManager cm) { m_instance = (ComponentManagerImpl) cm; }

}
