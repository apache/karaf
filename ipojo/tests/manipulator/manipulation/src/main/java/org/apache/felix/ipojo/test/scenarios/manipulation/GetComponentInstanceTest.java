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
package org.apache.felix.ipojo.test.scenarios.manipulation;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.Pojo;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.manipulation.service.FooService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Check the getComponentInstance method on a POJO
 */
public class GetComponentInstanceTest extends OSGiTestCase {
    
	/**
	 * Check if a pojo can correctly be cast in POJO.
	 * Check the getComponentInstance method. 
	 */
	public void testGetComponentInstance() {
		String factName = "Manipulation-FooProviderType-1";
		String compName = "FooProvider-1";
		ServiceReference[] refs = null;
		
		// Get the factory to create a component instance
		Factory fact = Utils.getFactoryByName(context, factName);
		assertNotNull("Cannot find the factory FooProvider-1", fact);
		
		Properties props = new Properties();
		props.put("instance.name",compName);
		ComponentInstance ci = null;
		try {
			ci = fact.createComponentInstance(props);
		} catch (Exception e1) { fail(e1.getMessage()); }
		
		assertEquals("Check instance name", compName, ci.getInstanceName());
		
		// Get a FooService provider
		try {
			refs = context.getServiceReferences(FooService.class.getName(), "(instance.name=" + compName + ")");
		} catch (InvalidSyntaxException e) { fail("Service query failed (2) " + e); }
		
		assertNotNull("FS not available", refs);
		
		// Get foo object
		FooService fs = (FooService) context.getService(refs[0]);
		
		// Cast to POJO
		Pojo pojo = (Pojo) fs;
		
		// GetComponentInstance
		ComponentInstance instance = pojo.getComponentInstance();
		assertEquals("Check component instance name", instance.getInstanceName(), compName);
		assertEquals("Check component factory name", instance.getFactory().getName(), factName);
		assertNotNull("Instance description not null", instance.getInstanceDescription());
		InstanceDescription id = instance.getInstanceDescription();
		assertTrue("Check instance state", id.getState() == ComponentInstance.VALID);
		assertEquals("Check created pojo count", id.getCreatedObjects().length, 1);
		assertEquals("Check instance description name", id.getName(), compName); 
		
		// Unget the service
		context.ungetService(refs[0]);
		
		ci.dispose();
		
		// Check that there is no more FooService
		try {
			refs = context.getServiceReferences(FooService.class.getName(), null);
		} catch (InvalidSyntaxException e) { fail("Service query failed (3) : " + e.getMessage()); }
		
		assertNull("FS available, but component instance stopped", refs);
	}

}
