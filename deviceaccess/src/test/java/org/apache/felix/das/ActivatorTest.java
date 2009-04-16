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
package org.apache.felix.das;

import org.apache.felix.dependencymanager.DependencyManager;
import org.apache.felix.dependencymanager.Service;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;



/**
 * 
 * Tests the Activator.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ActivatorTest {

	
	
	private Activator m_activator;
	
	@Mock
	private BundleContext m_context;
	
	@Mock
	private DependencyManager m_manager;
	
	@Before
	public void before() {

		MockitoAnnotations.initMocks(this);
		m_activator = new Activator();
		
	}

	
	
	
	@Test
	public void VerifyActivatorInit() throws Exception {
		
		m_activator.init(m_context, m_manager);
		
		Mockito.verify(m_manager).add(Mockito.isA(Service.class));
		
	}
	
	/**
	 * Verify we do not actively perform any actions during the destroy.
	 * 
	 * @throws Exception
	 */
	@Test
	public void VerifyActivatorDestroy() throws Exception {
		
		m_activator.destroy(m_context, m_manager);
		
		Mockito.verifyZeroInteractions(m_context);
		Mockito.verifyZeroInteractions(m_manager);
		
	}
	
	
}
