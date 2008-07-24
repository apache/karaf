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

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ParseException;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.apache.felix.ipojo.test.scenarios.manipulation.service.BarService;
import org.apache.felix.ipojo.test.scenarios.manipulation.service.FooService;

/**
 * Check manipulation metadata written in the manifest.
 */
public class ManipulationMetadata extends OSGiTestCase {

	public void testGetMetadata() {
		String header = (String) context.getBundle().getHeaders().get("iPOJO-Components");
		Element elem = null;
		try {
			elem = ManifestMetadataParser.parseHeaderMetadata(header);
		} catch (ParseException e) {
			fail("Parse Exception when parsing iPOJO-Component");
		}
		
		assertNotNull("Check elem not null", elem);
		
		Element manip = getManipulationForComponent(elem, "Manipulation-FooProviderType-1");
		assertNotNull("Check manipulation metadata not null for " + "FooProviderType-1", manip);
	}
	
	public void testInterface() {
		String comp_name = "Manipulation-FooProviderType-1";
		Element manip = getManipulationForComponent(comp_name);
		Element[] itf = manip.getElements("Interface");
		assertEquals("Check interfaces number", itf.length, 1);
		assertEquals("Check itf name", itf[0].getAttribute("name"), FooService.class.getName());
	}
	
	public void testInterfaces() {
		String comp_name = "Manipulation-FooBarProviderType-1";
		Element manip = getManipulationForComponent(comp_name);
		Element[] itf = manip.getElements("Interface");
		assertEquals("Check interfaces number", itf.length, 2);
		assertEquals("Check itf name", itf[0].getAttribute("name"), FooService.class.getName());
		assertEquals("Check itf name", itf[1].getAttribute("name"), BarService.class.getName());
	}
	
	public void testFields() {
		String comp_name = "Manipulation-FooProviderType-Dyn";
		Element manip = getManipulationForComponent(comp_name);
		Element[] fields = manip.getElements("field");
		assertEquals("Check field count " + fields.length, fields.length, 5);
		/*
		private int intProp;	
		private String strProp;
		private String[] strAProp;
		private int[] intAProp;
		private boolean boolProp;
		 */
		
		Element field;
		
		field = getFieldFromName(manip, "intProp");		
		assertEquals("Check field name : " + field.getAttribute("name"), field.getAttribute("name"), "intProp");
		assertEquals("Check field type : " + field.getAttribute("name"), field.getAttribute("type"), "int");
		
		field = getFieldFromName(manip, "strProp");
		assertEquals("Check field name : " + field.getAttribute("name"), field.getAttribute("name"), "strProp");
		assertEquals("Check field type : " + field.getAttribute("name"), field.getAttribute("type"), "java.lang.String");
		
		field = getFieldFromName(manip, "strAProp");
		assertEquals("Check field name : " + field.getAttribute("name"), field.getAttribute("name"), "strAProp");
		assertEquals("Check field type : " + field.getAttribute("name"), field.getAttribute("type"), "java.lang.String[]");
		
		field = getFieldFromName(manip, "intAProp");
		assertEquals("Check field name : " + field.getAttribute("name"), field.getAttribute("name"), "intAProp");
		assertEquals("Check field type : " + field.getAttribute("name"), field.getAttribute("type"), "int[]");
		
		field = getFieldFromName(manip, "boolProp");
		assertEquals("Check field name : " + field.getAttribute("name"), field.getAttribute("name"), "boolProp");
		assertEquals("Check field type : " + field.getAttribute("name"), field.getAttribute("type"), "boolean");
	}
	
	public void testPrimitivesFields() {
		String comp_name = "Manipulation-PrimitiveManipulationTester";
		Element manip = getManipulationForComponent(comp_name);
		Element[] fields = manip.getElements("Field");
		assertEquals("Check field count", fields.length, 16);
		/*
		byte b = 1;
		short s = 1;
		int i = 1;
		long l = 1;
		double d = 1.1;
		float f = 1.1f;
		char c = 'a';
		boolean bool = false;
		byte[] bs = new byte[] {0,1,2};
		short[] ss = new short[] {0,1,2};
		int[] is = new int[] {0,1,2};
		long[] ls = new long[] {0,1,2};
		double[] ds = new double[] {0.0, 1.1, 2.2};
		float[] fs = new float[] {0.0f, 1.1f, 2.2f};
		char[] cs = new char[] {'a', 'b', 'c'};
		boolean[] bools = new boolean[] {false, true, false};
		 */
		Element field;

		field = getFieldFromName(manip, "b");		
		assertEquals("Check field type : " + field.getAttribute("name"), field.getAttribute("type"), "byte");
		field = getFieldFromName(manip, "s");		
		assertEquals("Check field type : " + field.getAttribute("name"), field.getAttribute("type"), "short");
		field = getFieldFromName(manip, "i");		
		assertEquals("Check field type : " + field.getAttribute("name"), field.getAttribute("type"), "int");
		field = getFieldFromName(manip, "l");		
		assertEquals("Check field type : " + field.getAttribute("name"), field.getAttribute("type"), "long");
		field = getFieldFromName(manip, "d");		
		assertEquals("Check field type : " + field.getAttribute("name"), field.getAttribute("type"), "double");
		field = getFieldFromName(manip, "f");		
		assertEquals("Check field type : " + field.getAttribute("name"), field.getAttribute("type"), "float");
		field = getFieldFromName(manip, "c");		
		assertEquals("Check field type : " + field.getAttribute("name"), field.getAttribute("type"), "char");
		field = getFieldFromName(manip, "bool");		
		assertEquals("Check field type : " + field.getAttribute("name"), field.getAttribute("type"), "boolean");
		
		field = getFieldFromName(manip, "bs");		
		assertEquals("Check field type : " + field.getAttribute("name"), field.getAttribute("type"), "byte[]");
		field = getFieldFromName(manip, "ss");		
		assertEquals("Check field type : " + field.getAttribute("name"), field.getAttribute("type"), "short[]");
		field = getFieldFromName(manip, "is");		
		assertEquals("Check field type : " + field.getAttribute("name"), field.getAttribute("type"), "int[]");
		field = getFieldFromName(manip, "ls");		
		assertEquals("Check field type : " + field.getAttribute("name"), field.getAttribute("type"), "long[]");
		field = getFieldFromName(manip, "ds");		
		assertEquals("Check field type : " + field.getAttribute("name"), field.getAttribute("type"), "double[]");
		field = getFieldFromName(manip, "fs");		
		assertEquals("Check field type : " + field.getAttribute("name"), field.getAttribute("type"), "float[]");
		field = getFieldFromName(manip, "cs");		
		assertEquals("Check field type : " + field.getAttribute("name"), field.getAttribute("type"), "char[]");
		field = getFieldFromName(manip, "bools");		
		assertEquals("Check field type : " + field.getAttribute("name"), field.getAttribute("type"), "boolean[]");		
	}
	
	public void testNoArgMethod() {
		String comp_name = "Manipulation-SimpleMultipleCheckServiceProvider";
		Element manip = getManipulationForComponent(comp_name);
		Element method = getMethodFromName(manip, "check");
		assertFalse("Check no args", method.containsAttribute("arguments"));
		assertEquals("Check return", method.getAttribute("return"), "boolean");
	}
	
	public void testOneArgsMethod() {
		String comp_name = "Manipulation-SimpleMultipleCheckServiceProvider";
		Element manip = getManipulationForComponent(comp_name);
		Element method = getMethodFromName(manip, "refBind");
		assertEquals("Check args", method.getAttribute("arguments"), "{org.osgi.framework.ServiceReference}");
		assertEquals("Check args count", 1, ParseUtils.parseArrays("{org.osgi.framework.ServiceReference}").length);
		assertFalse("Check return", method.containsAttribute("return"));
	}
	
	public void testTwoArgsMethod() {
		String comp_name = "Manipulation-SimpleMultipleCheckServiceProvider";
		Element manip = getManipulationForComponent(comp_name);
		Element method = getMethodFromName(manip, "doNothing");
		assertEquals("Check args", method.getAttribute("arguments"), "{java.lang.Object,java.lang.String}");
		assertEquals("Check args count", 2, ParseUtils.parseArrays("{java.lang.Object,java.lang.String}").length);
		assertEquals("Check return", method.getAttribute("return"), "java.lang.Object");
	}
	
	private Element getManipulationForComponent(Element metadata, String comp_name) {
		Element[] comps = metadata.getElements("component");
		for(int i = 0; i < comps.length; i++) {
			if(comps[i].containsAttribute("factory") && comps[i].getAttribute("factory").equals(comp_name)) {
				return comps[i].getElements("manipulation")[0];
			}
            if(comps[i].containsAttribute("name") && comps[i].getAttribute("name").equals(comp_name)) {
                return comps[i].getElements("manipulation")[0];
            }
		}
		return null;
	}
	
	private Element getManipulationForComponent(String comp_name) {
		String header = (String) context.getBundle().getHeaders().get("iPOJO-Components");
		Element elem = null;
		try {
			elem = ManifestMetadataParser.parseHeaderMetadata(header);
		} catch (ParseException e) {
			fail("Parse Exception when parsing iPOJO-Component");
		}
		
		assertNotNull("Check elem not null", elem);		
		Element manip = getManipulationForComponent(elem, comp_name);
		assertNotNull("Check manipulation metadata not null for " + comp_name, manip);
		return manip;
	}
	
	private Element getMethodFromName(Element manip, String name) {
		Element methods[] = manip.getElements("Method");
		for(int i = 0; i < methods.length; i++) {
			if(methods[i].containsAttribute("name") && methods[i].getAttribute("name").equals(name)) {
				return methods[i];
			}
		}
		fail("Method " + name + " not found");
		return null;
	}
	
	private Element getFieldFromName(Element manip, String name) {
		Element fields[] = manip.getElements("Field");
		for(int i = 0; i < fields.length; i++) {
			if(fields[i].containsAttribute("name") && fields[i].getAttribute("name").equals(name)) {
				return fields[i];
			}
		}
		fail("Field " + name + " not found");
		return null;
	}
	

}
