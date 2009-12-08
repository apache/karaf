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
package org.apache.felix.dm.impl;

import java.util.List;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.service.Service;
import org.osgi.framework.ServiceReference;

public class AdapterImpl {
	private volatile DependencyManager m_manager;
	private volatile Service m_service;
	private final Class m_iface;
	private final Class m_iface2;
	private final Class m_impl;
	
	// TODO adapts a "dependency" ... making it sort of a decorator factory
	public AdapterImpl(Class iface, Class iface2, Class impl) {
		m_iface = iface;
		m_iface2 = iface2;
		m_impl = impl;
	}
	
	public void added(ServiceReference ref, Object service) {
		// TODO decorator be smarter:
		
		// get any "global" dependencies
		List dependencies = m_service.getDependencies();

		m_manager.add(m_manager.createService()
			.setInterface(m_iface2.getName(), null)
			.setImplementation(m_impl)
			.add(dependencies)
			.add(m_manager.createServiceDependency()
				.setService(m_iface, ref)
				.setRequired(true)
				)
			);
	}

	public void removed(ServiceReference ref, Object service) {
	}
}
