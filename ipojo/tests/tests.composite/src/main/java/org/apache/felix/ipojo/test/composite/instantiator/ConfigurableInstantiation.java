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
package org.apache.felix.ipojo.test.composite.instantiator;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.composite.util.Utils;
import org.apache.felix.ipojo.test.scenarios.service.FooService;
import org.osgi.framework.ServiceReference;

public class ConfigurableInstantiation extends OSGiTestCase {

	private ComponentFactory acceptF;
	private ComponentFactory refuse1F;
	private ComponentFactory refuse2F;
	
	public void setUp() {
		acceptF = (ComponentFactory) Utils.getFactoryByName(context, "composite.bar.5-accept");
		refuse1F = (ComponentFactory) Utils.getFactoryByName(context, "composite.bar.5-refuse1");
		refuse2F = (ComponentFactory) Utils.getFactoryByName(context, "composite.bar.5-refuse2");
		
	}
	
	public void tearDown() { }
	
	public void testAccept() {
		Properties props = new Properties();
		props.put("name", "under");
		ComponentInstance under = null;
		try {
			under = acceptF.createComponentInstance(props);
		} catch(Exception e) {
			fail("Cannot instantiate under : " + e.getMessage());
		}
		assertTrue("Check instance validity", under.getState() == ComponentInstance.VALID);
		ServiceContext sc = Utils.getServiceContext(under);
		ServiceReference ref = sc.getServiceReference(FooService.class.getName());
		assertNotNull("Check refs not null", ref);
		FooService foo = (FooService) sc.getService(ref);
		Properties p = foo.fooProps();
		boolean b = ((Boolean) p.get("boolProp")).booleanValue();
		String s = (String) p.get("strProp");
		int i = ( (Integer) p.get("intProp")).intValue();
		assertTrue("Test boolean", b);
		assertEquals("Test string", s, "foo");
		
		assertEquals("Test int", i, 5); // The code fix to 5.
		under.dispose();
	}
	
	public void testRefuse1() {
		Properties props = new Properties();
		props.put("name", "under-ref1");
		ComponentInstance under = null;
		try {
			under = refuse1F.createComponentInstance(props);
		} catch(Exception e) {
			fail("Cannot instantiate under : " + e.getMessage());
		}
		
		assertTrue("Check that under is not valid", under.getState() == ComponentInstance.INVALID);
        
        under.dispose();
	}
	
	public void testRefuse2() {
		Properties props = new Properties();
		props.put("name", "under-ref2");
		ComponentInstance under = null;
		try {
			under = refuse2F.createComponentInstance(props);
		} catch(Exception e) {
			fail("Cannot instantiate under : " + e.getMessage());
		}
		
		assertTrue("Check that under is not valid", under.getState() == ComponentInstance.INVALID);
        
        under.dispose();
	}
	
}
