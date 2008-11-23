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

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.test.scenarios.manipulation.service.PrimitiveManipulationTestService;

/**
 * Check the manipulation of primitive type (boxed and unboxed).
 */
public class PrimitiveTypeTest extends OSGiTestCase {

	PrimitiveManipulationTestService prim;
	
	IPOJOHelper helper;
	
	public void setUp() {
	    helper = new IPOJOHelper(this);
	    ComponentInstance instance = helper.createComponentInstance("ManipulationPrimitives-PrimitiveManipulationTester");
		assertTrue("check instance state", instance.getState() == ComponentInstance.VALID);
		prim = (PrimitiveManipulationTestService) getServiceObject(PrimitiveManipulationTestService.class.getName(), "(instance.name=" + instance.getInstanceName() + ")");
		assertNotNull("Check prim availability", prim);
	}
	
	public void tearDown() {
	    helper.dispose();
		prim = null;
	}
	
	public void testByte() {
		assertEquals("Check - 1", prim.getByte(), 1);
		prim.setByte((byte) 2);
		assertEquals("Check - 2", prim.getByte(), 2);
	}
	
	public void testShort() {
		assertEquals("Check - 1", prim.getShort(), 1);
		prim.setShort((short) 2);
		assertEquals("Check - 2", prim.getShort(), 2);
	}
	
	public void testInt() {
		assertEquals("Check - 1", prim.getInt(), 1);
		prim.setInt((int) 2);
		assertEquals("Check - 2", prim.getInt(), 2);
	}
	
	public void testLong() {
		assertEquals("Check - 1", prim.getLong(), 1);
		prim.setLong((long) 2);
		assertEquals("Check - 2", prim.getLong(), 2);
	}
	
	public void testFloat() {
		assertEquals("Check - 1", prim.getFloat(), 1.1f);
		prim.setFloat(2.2f);
		assertEquals("Check - 2", prim.getFloat(), 2.2f);
	}
	
	public void testDouble() {
		assertEquals("Check - 1", prim.getDouble(), 1.1);
		prim.setDouble(2.2);
		assertEquals("Check - 2", prim.getDouble(), 2.2);
	}
	
	public void testBoolean() {
		assertFalse("Check - 1", prim.getBoolean());
		prim.setBoolean(true);
		assertTrue("Check - 2", prim.getBoolean());
	}
	
	public void testChar() {
		assertEquals("Check - 1", prim.getChar(), 'a');
		prim.setChar('b');
		assertEquals("Check - 2", prim.getChar(), 'b');
	}
	

}
