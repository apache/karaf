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
package org.apache.felix.ipojo.test.scenarios.factories;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.HandlerManagerFactory;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.factories.service.FooService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

/**
 * Check that instances are disposed when the factory is stopped.
 */
public class ObedienceTest extends OSGiTestCase {

	public void testObedience() {
		assertNull("Check no foo service", getContext().getServiceReference(FooService.class.getName()));
		ComponentFactory factory = (ComponentFactory) Utils.getFactoryByName(getContext(), "Factories-FooProviderType-1");
		assertNotNull("Check factory existing", factory);
		
		Properties props1 = new Properties();
		props1.put("instance.name","foo1");
		Properties props2 = new Properties();
		props2.put("instance.name","foo2");
		
		ComponentInstance ci1 = null, ci2 = null;
		try {
			ci1 = factory.createComponentInstance(props1);
			ci2 = factory.createComponentInstance(props2);
		} catch(Exception e) {
			fail("Cannot instantiate foo providers : " + e.getMessage());
		}
		
		assertTrue("Check foo1 validity", ci1.getState() == ComponentInstance.VALID);
		assertTrue("Check foo2 validity", ci2.getState() == ComponentInstance.VALID);
		
		assertNotNull("Check foo service", getContext().getServiceReference(FooService.class.getName()));
		assertEquals("Check the number of Foo", Utils.getServiceReferences(getContext(), FooService.class.getName(), null).length, 2);
		
		factory.stop();
		
		assertTrue("Check foo1 invalidity ("+ci1.getState()+")", ci1.getState() == ComponentInstance.DISPOSED);
		assertTrue("Check foo2 invalidity ("+ci1.getState()+")", ci2.getState() == ComponentInstance.DISPOSED);
		
		assertNull("Check no foo service", getContext().getServiceReference(FooService.class.getName()));
		
		factory.start();
		assertNull("Check no foo service", getContext().getServiceReference(FooService.class.getName()));
	}
	
	public void testDisposeAfterFactoryInvalidation() {
	    ComponentFactory cf = (ComponentFactory) Utils.getFactoryByName(getContext(), "org.apache.felix.ipojo.test.scenarios.component.SimpleType");
	    assertNotNull("Check factory availability -1", cf);
	    assertEquals("Check factory state -1", Factory.VALID, cf.getState());
	    
	    ServiceReference ref_arch = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), "SimpleInstance");
	    assertNotNull("Check Architecture availability -1", ref_arch);
	    
        HandlerManagerFactory hf = (HandlerManagerFactory) Utils.getHandlerFactoryByName(getContext(), "controller");
        assertNotNull("Check handler availability -1", hf);
        assertEquals("Check handler state -1", Factory.VALID, hf.getState());
        
        // Stop the handler
        hf.stop();
        HandlerManagerFactory hf2 = (HandlerManagerFactory) Utils.getHandlerFactoryByName(getContext(), "controller");
        assertNull("Check handler availability -2", hf2);
        
        // Check the factory invalidity
        cf = (ComponentFactory) Utils.getFactoryByName(getContext(), "org.apache.felix.ipojo.test.scenarios.component.SimpleType");
        assertNotNull("Check factory availability -2", cf);
        assertEquals("Check factory state -2", Factory.INVALID, cf.getState());
        
        // Check the instance disparition
        ref_arch = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), "SimpleInstance");
        assertNull("Check Architecture availability -1", ref_arch);
        
        // Restart the handler
        hf.start();
        hf2 = (HandlerManagerFactory) Utils.getHandlerFactoryByName(getContext(), "controller");
        assertNotNull("Check handler availability -3", hf2);
        
        // Check the factory state
        cf = (ComponentFactory) Utils.getFactoryByName(getContext(), "org.apache.felix.ipojo.test.scenarios.component.SimpleType");
        assertNotNull("Check factory availability -3", cf);
        assertEquals("Check factory state -3", Factory.VALID, cf.getState());
        
        
        // Check the instance re-creation
        ref_arch = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), "SimpleInstance");
        assertNotNull("Check Architecture availability -3", ref_arch);
        
	}

}
