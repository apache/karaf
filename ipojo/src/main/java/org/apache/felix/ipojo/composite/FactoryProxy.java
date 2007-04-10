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
package org.apache.felix.ipojo.composite;

import java.util.Dictionary;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.architecture.ComponentDescription;

/**
 * Bridge representing a Factory inside a composition.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class FactoryProxy implements Factory {
	
	private Factory m_delegate;
	private ServiceContext m_context;
	
	/**
	 * Constructor.
	 * @param fact : the targetted factory.
	 * @param s : the service context to target.
	 */
	public FactoryProxy(Factory fact, ServiceContext s) {
		m_delegate = fact;
		m_context = s;
	}

	/**
	 * @see org.apache.felix.ipojo.Factory#createComponentInstance(java.util.Dictionary)
	 */
	public ComponentInstance createComponentInstance(Dictionary configuration) throws UnacceptableConfiguration {
		return m_delegate.createComponentInstance(configuration, m_context);
	}

	/**
	 * @see org.apache.felix.ipojo.Factory#createComponentInstance(java.util.Dictionary, org.apache.felix.ipojo.ServiceContext)
	 */
	public ComponentInstance createComponentInstance(Dictionary configuration, ServiceContext serviceContext) throws UnacceptableConfiguration {
		return m_delegate.createComponentInstance(configuration, serviceContext);
	}

	/**
	 * @see org.apache.felix.ipojo.Factory#getComponentDescription()
	 */
	public ComponentDescription getComponentDescription() { return m_delegate.getComponentDescription(); }

	/**
	 * @see org.apache.felix.ipojo.Factory#getName()
	 */
	public String getName() { return m_delegate.getName(); }

	/**
	 * @see org.apache.felix.ipojo.Factory#isAcceptable(java.util.Dictionary)
	 */
	public boolean isAcceptable(Dictionary conf) { return m_delegate.isAcceptable(conf); }

	/**
	 * @see org.apache.felix.ipojo.Factory#reconfigure(java.util.Dictionary)
	 */
	public void reconfigure(Dictionary conf) throws UnacceptableConfiguration { m_delegate.reconfigure(conf); }

}
