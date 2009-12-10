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

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.resources.Resource;

public class ResourceAdapterImpl {
	private volatile DependencyManager m_manager;
	private final Object m_impl;
	private final Class m_iface;
    private final boolean m_propagate;

	public ResourceAdapterImpl(Object impl, Class iface, boolean propagate) {
		m_impl = impl;
		m_iface = iface;
		m_propagate = propagate;
	}

	public void added(Resource resource) {
		System.out.println("ADDED " + resource);
		m_manager.add(m_manager.createService()
			.setInterface(m_iface.getName(), null)
			.setImplementation(m_impl)
			.add(m_manager.createResourceDependency()
				.setResource(resource)
				.setRequired(true)
				.setPropagate(m_propagate)
				)
			);
	}

	public void removed(Resource resource) {
	}

}
