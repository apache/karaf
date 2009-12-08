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
import org.osgi.framework.Bundle;

public class BundleAdapterImpl {
	private final int m_stateMask;
	private final String m_filter;
	private final Object m_impl;
	private volatile DependencyManager m_manager;
	private final Class m_iface;

	public BundleAdapterImpl(int stateMask, String filter, Object impl, Class iface) {
		m_stateMask = stateMask;
		m_filter = filter;
		m_impl = impl;
		m_iface = iface;
	}
	
	public void added(Bundle bundle) {
		// TODO decorator be smarter:
		m_manager.add(m_manager.createService()
			.setInterface(m_iface.getName(), null)
			.setImplementation(m_impl)
			.add(m_manager.createBundleDependency()
				.setBundle(bundle)
				.setStateMask(m_stateMask)
				.setRequired(true)
				)
			);
	}

	public void removed(Bundle bundle) {
	}
}
