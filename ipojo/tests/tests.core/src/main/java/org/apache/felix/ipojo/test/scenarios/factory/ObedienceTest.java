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
package org.apache.felix.ipojo.test.scenarios.factory;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.service.FooService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;

public class ObedienceTest extends OSGiTestCase {

	public void testObedience1() {
		assertNull("Check no foo service", context.getServiceReference(FooService.class.getName()));
		ComponentFactory factory = (ComponentFactory) Utils.getFactoryByName(context, "FooProviderType-1");
		assertNotNull("Check factory existing", factory);
		
		Properties props1 = new Properties();
		props1.put("name", "foo1");
		Properties props2 = new Properties();
		props2.put("name", "foo2");
		
		ComponentInstance ci1 = null, ci2 = null;
		try {
			ci1 = factory.createComponentInstance(props1);
			ci2 = factory.createComponentInstance(props2);
		} catch(Exception e) {
			fail("Cannot instantiate foo providers : " + e.getMessage());
		}
		
		assertTrue("Check foo1 validity", ci1.getState() == ComponentInstance.VALID);
		assertTrue("Check foo2 validity", ci2.getState() == ComponentInstance.VALID);
		
		assertNotNull("Check foo service", context.getServiceReference(FooService.class.getName()));
		assertEquals("Check the number of Foo", Utils.getServiceReferences(context, FooService.class.getName(), null).length, 2);
		
		factory.stop();
		
		assertTrue("Check foo1 invalidity ("+ci1.getState()+")", ci1.getState() == ComponentInstance.DISPOSED);
		assertTrue("Check foo2 invalidity ("+ci1.getState()+")", ci2.getState() == ComponentInstance.DISPOSED);
		
		assertNull("Check no foo service", context.getServiceReference(FooService.class.getName()));
		
		factory.start();
		assertNull("Check no foo service", context.getServiceReference(FooService.class.getName()));
	}

}
