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
package org.apache.felix.ipojo.test.composite.infrastructure;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.composite.CompositeManager;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.composite.util.Utils;
import org.osgi.framework.InvalidSyntaxException;

public class EmptyCompositeTest extends OSGiTestCase {
	
	public void testEmptyCompositeCreation() {
		Factory factory = Utils.getFactoryByName(context, "composite.empty");
		Properties props = new Properties();
		props.put("name", "empty");
		
		ComponentInstance ci = null;
		try {
			ci = factory.createComponentInstance(props);
		} catch (Exception e) {
		    e.printStackTrace();
			fail("Unacceptable configuration : " + e.getMessage());
		}
		
		ComponentTypeDescription cd = ci.getFactory().getComponentDescription();
		assertEquals("Check component type name", cd.getName(), "composite.empty");
//		assertEquals("Check class name (" + cd.getClassName() + ")", cd.getClassName(), "composite");
		assertEquals("Check offered service", cd.getprovidedServiceSpecification().length, 0);
		assertEquals("Check configurable properties", cd.getProperties().length, 0);
		
		InstanceDescription id = ci.getInstanceDescription();
		assertEquals("Check composite instance name", id.getName(), "empty");
		assertEquals("Check composite instance state (" + id.getState() + ")", id.getState(), ComponentInstance.VALID);
		
		assertEquals("Check contained instance", id.getContainedInstances().length, 0);
		
		assertTrue("Check composite manager", ci instanceof CompositeManager);
		CompositeManager cm = (CompositeManager) ci;
		ServiceContext sc = cm.getServiceContext();
		try {
			assertEquals("Check number of factories imported", sc.getServiceReferences(Factory.class.getName(), null).length, context.getServiceReferences(Factory.class.getName(), null).length);
		} catch (InvalidSyntaxException e) {
			fail("Invalid filter : " + e.getMessage());
		}
		ci.dispose();
	}
	
	public void testInstanceCreation1() {
		Factory factory = Utils.getFactoryByName(context, "composite.empty");
		Properties props = new Properties();
		props.put("name", "empty");
		
		ComponentInstance ci = null;
		try {
			ci = factory.createComponentInstance(props);
		} catch(Exception e) {
		    e.printStackTrace();
			fail("Unacceptable configuration : " + e.getMessage());
		}
		
		assertTrue("Check composite manager", ci instanceof CompositeManager);
		CompositeManager cm = (CompositeManager) ci;
		ServiceContext sc = cm.getServiceContext();
		try {
			assertEquals("Check number of factories imported", sc.getServiceReferences(Factory.class.getName(), null).length, context.getServiceReferences(Factory.class.getName(), null).length);
		} catch (InvalidSyntaxException e) {
			fail("Invalid filter : " + e.getMessage());
		}
		
		Properties props2 = new Properties();
		props2.put("name", "empty2");
		ComponentInstance ci2 = null;
		try {
			ci2 = factory.createComponentInstance(props2, sc);
		} catch(Exception e) {
		    e.printStackTrace();
			fail("Unacceptable configuration : " + e.getMessage());
		}
		
		InstanceDescription id = ci.getInstanceDescription();
		assertEquals("Check composite instance name", id.getName(), "empty");
		assertEquals("Check composite instance state", id.getState(), ComponentInstance.VALID);
		assertEquals("Check contained instance", id.getContainedInstances().length, 1);
		InstanceDescription id2 = id.getContainedInstances()[0];
		assertEquals("Check composite instance name", id2.getName(), "empty2");
		assertEquals("Check composite instance state", id2.getState(), ComponentInstance.VALID);
		assertEquals("Check contained instance", id2.getContainedInstances().length, 0);
		
		ci2.dispose();
		id = ci.getInstanceDescription();
		assertEquals("Check composite instance name", id.getName(), "empty");
		assertEquals("Check composite instance state", id.getState(), ComponentInstance.VALID);
		assertEquals("Check contained instance", id.getContainedInstances().length, 0);
		
		ci.dispose();
	}
	
	public void testInstanceCreation2() {
		Factory factory = Utils.getFactoryByName(context, "composite.empty");
		Properties props = new Properties();
		props.put("name", "empty");
		
		ComponentInstance ci = null;
		try {
			ci = factory.createComponentInstance(props);
		} catch(Exception e) {
			fail("Unacceptable configuration : " + e.getMessage());
		}
		
		assertTrue("Check composite manager", ci instanceof CompositeManager);
		CompositeManager cm = (CompositeManager) ci;
		ServiceContext sc = cm.getServiceContext();
		try {
			assertEquals("Check number of factories imported", sc.getServiceReferences(Factory.class.getName(), null).length, context.getServiceReferences(Factory.class.getName(), null).length);
		} catch (InvalidSyntaxException e) {
			fail("Invalid filter : " + e.getMessage());
		}
		
		Factory factory2 = Utils.getFactoryByName(sc, "composite.empty");
		assertNotNull("Check factory2 not null", factory2);
		Properties props2 = new Properties();
		props2.put("name", "empty2");
		ComponentInstance ci2 = null;
		try {
			ci2 = factory2.createComponentInstance(props2);
		} catch(Exception e) {
			fail("Unacceptable configuration : " + e.getMessage());
		}
		
		InstanceDescription id = ci.getInstanceDescription();
		assertEquals("Check composite instance name", id.getName(), "empty");
		assertEquals("Check composite instance state", id.getState(), ComponentInstance.VALID);
		assertEquals("Check contained instance", id.getContainedInstances().length, 1);
		InstanceDescription id2 = id.getContainedInstances()[0];
		assertEquals("Check composite instance name", id2.getName(), "empty2");
		assertEquals("Check composite instance state", id2.getState(), ComponentInstance.VALID);
		assertEquals("Check contained instance", id2.getContainedInstances().length, 0);
		
		ci2.dispose();
		id = ci.getInstanceDescription();
		assertEquals("Check composite instance name", id.getName(), "empty");
		assertEquals("Check composite instance state", id.getState(), ComponentInstance.VALID);
		assertEquals("Check contained instance", id.getContainedInstances().length, 0);
		
		ci.dispose();
	}
	
	public void testInstanceCreation3() {
		Factory factory = Utils.getFactoryByName(context, "composite.empty");
		Properties props = new Properties();
		props.put("name", "empty");
		
		ComponentInstance ci = null;
		try {
			ci = factory.createComponentInstance(props);
		} catch(Exception e) {
			fail("Unacceptable configuration : " + e.getMessage());
		}
		
		assertTrue("Check composite manager", ci instanceof CompositeManager);
		CompositeManager cm = (CompositeManager) ci;
		ServiceContext sc = cm.getServiceContext();
		try {
			assertEquals("Check number of factories imported", sc.getServiceReferences(Factory.class.getName(), null).length, context.getServiceReferences(Factory.class.getName(), null).length);
		} catch (InvalidSyntaxException e) {
			fail("Invalid filter : " + e.getMessage());
		}
		
		Factory factory2 = Utils.getFactoryByName(sc, "composite.empty");
		assertNotNull("Check factory2 not null", factory2);
		Properties props2 = new Properties();
		props2.put("name", "empty2");
		ComponentInstance ci2 = null;
		try {
			ci2 = factory2.createComponentInstance(props2, sc);
		} catch(Exception e) {
			fail("Unacceptable configuration : " + e.getMessage());
		}
		
		InstanceDescription id = ci.getInstanceDescription();
		assertEquals("Check composite instance name", id.getName(), "empty");
		assertEquals("Check composite instance state", id.getState(), ComponentInstance.VALID);
		assertEquals("Check contained instance", id.getContainedInstances().length, 1);
		InstanceDescription id2 = id.getContainedInstances()[0];
		assertEquals("Check composite instance name", id2.getName(), "empty2");
		assertEquals("Check composite instance state", id2.getState(), ComponentInstance.VALID);
		assertEquals("Check contained instance", id2.getContainedInstances().length, 0);
		
		ci2.dispose();
		id = ci.getInstanceDescription();
		assertEquals("Check composite instance name", id.getName(), "empty");
		assertEquals("Check composite instance state", id.getState(), ComponentInstance.VALID);
		assertEquals("Check contained instance", id.getContainedInstances().length, 0);
		
		ci.dispose();
	}


}
