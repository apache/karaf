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
package org.apache.felix.ipojo.test.scenarios.eh;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.HandlerManagerFactory;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.eh.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class HandlerTest extends OSGiTestCase {

	ComponentInstance instance;

	public void setUp() {
		Properties props = new Properties();
		props.put("instance.name","HandlerTest-1");
		props.put("csh.simple", "simple");
		Properties p = new Properties();
		p.put("a", "a");
		p.put("b", "b");
		p.put("c", "c");
		props.put("csh.map", p);
		instance = Utils.getComponentInstance(context, "HANDLER-HandlerTester", props);
	}
	
	public void tearDown() {
		instance.dispose();
		instance = null;
	}
	
	public void testConfiguration1() {
		// Check the availability of CheckService
	    String name = "HandlerTest-1";
		ServiceReference sr = null;
		ServiceReference[] refs = null;
        String filter = "("+"instance.name"+"="+name+")";
        try {
            refs = context.getServiceReferences(CheckService.class.getName(), filter);
        } catch (InvalidSyntaxException e) { System.err.println("Invalid Filter : " + filter);}
        if(refs != null) { sr = refs[0]; }
        
		assertNotNull("Check the check service availability", sr);
		
		CheckService cs = (CheckService) context.getService(sr);
		Properties p = cs.getProps();
		assertEquals("Assert 'simple' equality", p.get("Simple"), "simple");
		assertEquals("Assert 'a' equality", p.get("Map1"), "a");
		assertEquals("Assert 'b' equality", p.get("Map2"), "b");
		assertEquals("Assert 'c' equality", p.get("Map3"), "c");
		
		cs = null;
		context.ungetService(sr);
	}
	
	public void testConfiguration2() {
		// Check the availability of CheckService
	    String name = "HandlerTest-2";
        ServiceReference sr = null;
        ServiceReference[] refs = null;
        String filter = "("+"instance.name"+"="+name+")";
        try {
            refs = context.getServiceReferences(CheckService.class.getName(), filter);
        } catch (InvalidSyntaxException e) { System.err.println("Invalid Filter : " + filter);}
        if(refs != null) { sr = refs[0]; }
		assertNotNull("Check the check service availability", sr);
		
		CheckService cs = (CheckService) context.getService(sr);
		Properties p = cs.getProps();
		assertEquals("Assert 'simple' equality", p.get("Simple"), "Simple");
		assertEquals("Assert 'a' equality", p.get("Map1"), "a");
		assertEquals("Assert 'b' equality", p.get("Map2"), "b");
		assertEquals("Assert 'c' equality", p.get("Map3"), "c");
		
		cs = null;
		context.ungetService(sr);
	}
	

    public void testConfiguration3() {
        // Check the availability of CheckService
        String name = "HandlerTest-2-empty";
        ServiceReference sr = null;
        ServiceReference[] refs = null;
        String filter = "("+"instance.name"+"="+name+")";
        try {
            refs = context.getServiceReferences(CheckService.class.getName(), filter);
        } catch (InvalidSyntaxException e) { System.err.println("Invalid Filter : " + filter);}
        if(refs != null) { sr = refs[0]; }
        assertNotNull("Check the check service availability", sr);
        
        CheckService cs = (CheckService) context.getService(sr);
        Properties p = cs.getProps();
        assertEquals("Assert 'simple' equality", p.get("Simple"), "Simple");
        assertEquals("Size of p", 3, p.size()); // instance name, simple and changes.
        
        cs = null;
        context.ungetService(sr);
    }
	
	public void testLifecycle() {
		// Check the availability of CheckService
	    String name = "HandlerTest-1";
        ServiceReference sr = null;
        ServiceReference[] refs = null;
        String filter = "("+"instance.name"+"="+name+")";
        try {
            refs = context.getServiceReferences(CheckService.class.getName(), filter);
        } catch (InvalidSyntaxException e) { System.err.println("Invalid Filter : " + filter);}
        if(refs != null) { sr = refs[0]; }
		assertNotNull("Check the check service availability", sr);
		
		ServiceReference sr_arch = Utils.getServiceReferenceByName(context, Architecture.class.getName(), "HandlerTest-1");
		Architecture arch = (Architecture) context.getService(sr_arch);
		
		assertEquals("Check instance validity - 0", arch.getInstanceDescription().getState(), ComponentInstance.VALID);
		
		CheckService cs = (CheckService) context.getService(sr);
		Properties p = cs.getProps();
		Integer changes = (Integer) p.get("changes");
		assertNotNull("Check changes no null", changes);
		assertEquals("Changes changes 1 ("+changes+")", changes.intValue(), 1);
		assertEquals("Check instance validity - 1", arch.getInstanceDescription().getState(), ComponentInstance.VALID);
		cs.check();
		p = cs.getProps();
		changes = (Integer) p.get("changes");
		assertEquals("Changes changes 2 ("+changes+")", changes.intValue(), 2);
		assertEquals("Check instance validity - 2", arch.getInstanceDescription().getState(), ComponentInstance.INVALID);
		cs.check();
		p = cs.getProps();
		changes = (Integer) p.get("changes");
		assertEquals("Changes changes 3 ("+changes+")", changes.intValue(), 3);
		assertEquals("Check instance validity - 3", arch.getInstanceDescription().getState(), ComponentInstance.VALID);
		cs.check();
		p = cs.getProps();
		changes = (Integer) p.get("changes");
		assertEquals("Changes changes 4 ("+changes+")", changes.intValue(), 4);
		assertEquals("Check instance validity - 4", arch.getInstanceDescription().getState(), ComponentInstance.INVALID);
		
		cs = null;
		arch = null;
		context.ungetService(sr_arch);
		context.ungetService(sr);
	}
	
	public void testAvailability() {
	    String name = "HandlerTest-1";
        ServiceReference sr = null;
        ServiceReference[] refs = null;
        String filter = "("+"instance.name"+"="+name+")";
        try {
            refs = context.getServiceReferences(CheckService.class.getName(), filter);
        } catch (InvalidSyntaxException e) { System.err.println("Invalid Filter : " + filter);}
        if(refs != null) { sr = refs[0]; }
        assertNotNull("Check the check service availability", sr);
        
        ServiceReference sr_arch = Utils.getServiceReferenceByName(context, Architecture.class.getName(), "HandlerTest-1");
        Architecture arch = (Architecture) context.getService(sr_arch);
        assertEquals("Check validity", arch.getInstanceDescription().getState(), ComponentInstance.VALID);
        
        // Kill the handler factory
        HandlerManagerFactory f = (HandlerManagerFactory) Utils.getHandlerFactoryByName(context, "check");
        f.stop();
        
        sr = Utils.getServiceReferenceByName(context, CheckService.class.getName(), "HandlerTest-1");
        assertNull("Check the check service unavailability", sr);
        
        sr_arch = Utils.getServiceReferenceByName(context, Architecture.class.getName(), "HandlerTest-1");
        assertNull("Check the architecture unavailability", sr_arch);
        
        // The instance is disposed, restart the handler
        f.start();
        
        Properties props = new Properties();
        props.put("instance.name","HandlerTest-1");
        props.put("csh.simple", "simple");
        Properties p = new Properties();
        p.put("a", "a");
        p.put("b", "b");
        p.put("c", "c");
        props.put("csh.map", p);
        instance = Utils.getComponentInstance(context, "HANDLER-HandlerTester", props);
        
        sr = Utils.getServiceReferenceByName(context, CheckService.class.getName(), "HandlerTest-1");
        assertNotNull("Check the check service availability - 2", sr);
        
        sr_arch = Utils.getServiceReferenceByName(context, Architecture.class.getName(), "HandlerTest-1");
        arch = (Architecture) context.getService(sr_arch);
        assertEquals("Check validity - 2", arch.getInstanceDescription().getState(), ComponentInstance.VALID);
	}

}
