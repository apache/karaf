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

import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.factories.service.BarService;
import org.apache.felix.ipojo.test.scenarios.factories.service.FooService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

public class FactoryProps extends OSGiTestCase {
	
//	public void testImplementationClass() {
//		ServiceReference ref1 = Utils.getServiceReferenceByName(context, Factory.class.getName(), "FooProviderType-1");
//		assertNotNull("The factory is available", ref1);
//		String clazz = (String) ref1.getProperty("component.class");
//		assertEquals("Check the implementation class", clazz, FooProviderType1.class.getName());
//	}
	
	public void testSimpleExposition() {
		ServiceReference ref1 = Utils.getServiceReferenceByName(context, Factory.class.getName(), "Factories-FooProviderType-1");
		assertNotNull("The factory is available", ref1);
		String[] spec = (String[]) ref1.getProperty("component.providedServiceSpecifications");
		assertEquals("Check array length", spec.length, 1);
		assertEquals("Check spec", spec[0], FooService.class.getName());
	}
	
	public void testDoubleExposition() {
		ServiceReference ref1 = Utils.getServiceReferenceByName(context, Factory.class.getName(), "Factories-FooBarProviderType-1");
		assertNotNull("The factory is available", ref1);
		String[] spec = (String[]) ref1.getProperty("component.providedServiceSpecifications");
		assertEquals("Check array length", spec.length, 2);
		assertContains("Check spec 1", spec, FooService.class.getName());
		assertContains("Check spec 2", spec, BarService.class.getName());
	}
	
	public void testProps() {
		ServiceReference ref1 = Utils.getServiceReferenceByName(context, Factory.class.getName(), "Factories-FooProviderType-Dyn2");
		assertNotNull("The factory is available", ref1);
		PropertyDescription[] pd = (PropertyDescription[]) ref1.getProperty("component.properties");
		assertEquals("Check property list size", pd.length, 5);
		
		//P0
		assertEquals("0) Check name", "int", pd[0].getName());
		assertEquals("0) Check type", "int", pd[0].getType());
		assertEquals("0) Check value", "4", pd[0].getValue());
		
		//P1
		assertEquals("1) Check name", "boolean", pd[1].getName());
		assertEquals("1) Check type", "boolean", pd[1].getType());
		assertNull("1) Check value", pd[1].getValue());
		
		//P2
		assertEquals("2) Check name", "string", pd[2].getName());
		assertEquals("2) Check type",  String.class.getName(), pd[2].getType());
		assertNull("2) Check value", pd[2].getValue());
		
		//P3
		assertEquals("3) Check name", "strAProp", pd[3].getName());
		assertEquals("3) Check type", "java.lang.String[]", pd[3].getType());
		assertNull("3) Check value", pd[3].getValue());
		
		//P4
		assertEquals("4) Check name", "intAProp", pd[4].getName());
		assertEquals("4) Check type", "int[]", pd[4].getType());
	}
	

}
