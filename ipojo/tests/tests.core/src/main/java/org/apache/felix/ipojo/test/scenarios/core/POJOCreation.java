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
package org.apache.felix.ipojo.test.scenarios.core;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.component.FooProviderType1;
import org.apache.felix.ipojo.test.scenarios.service.FooService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class POJOCreation extends OSGiTestCase {
	
	private ComponentInstance ci_lazzy;
	private ComponentInstance ci_immediate;
	private ComponentInstance ci_immediate_singleton;
	
	private Architecture lazzyArch;
	private Architecture immeArch;
	private Architecture immeArchSing;
	
	private ServiceReference lazzyRef;
	private ServiceReference immRef;
	private ServiceReference immRefSing;
    private ComponentInstance ci_lazzy_sing;
    private ComponentInstance ci_lazzy_sev;
    private ServiceReference lazzyRefSing;
    private ServiceReference lazzyRefSev;
    private Architecture lazzyArchSing;
    private Architecture lazzyArchSev;
    private ComponentInstance ci_lazzy_singM;
    private ComponentInstance ci_lazzy_sevM;
    private ServiceReference lazzyRefSingM;
    private ServiceReference lazzyRefSevM;
    private Architecture lazzyArchSingM;
    private Architecture lazzyArchSevM;
	
	public void setUp() {
		String factName = "FooProviderType-1";
		String compName = "FooProvider-1";
		Properties p = new Properties();
		p.put("name", compName);
		ci_lazzy = Utils.getComponentInstance(context, factName ,p);
		
		String factName2 = "ImmediateFooProviderType";
		String compName2 = "FooProvider-2";
		Properties p2 = new Properties();
		p2.put("name", compName2);
		ci_immediate = Utils.getComponentInstance(context, factName2, p2);
		
		String factName3 = "ImmediateFooProviderTypeSingleton";
        String compName3 = "FooProvider-3";
        Properties p3 = new Properties();
        p3.put("name", compName3);
        ci_immediate_singleton = Utils.getComponentInstance(context, factName3, p3);
        
        String factName4 = "FooProviderType-1-Sing";
        String compName4 = "FooProvider-1-Sing";
        Properties p4 = new Properties();
        p4.put("name", compName4);
        ci_lazzy_sing = Utils.getComponentInstance(context, factName4 ,p4);
        
        String factName5 = "FooProviderType-1-Sev";
        String compName5 = "FooProvider-1-Sev";
        Properties p5 = new Properties();
        p5.put("name", compName5);
        ci_lazzy_sev = Utils.getComponentInstance(context, factName5 ,p5);
        
        String factName6 = "FooProviderType-1-SingM";
        String compName6 = "FooProvider-1-SingM";
        Properties p6 = new Properties();
        p6.put("name", compName6);
        ci_lazzy_singM = Utils.getComponentInstance(context, factName6 ,p6);
        
        String factName7 = "FooProviderType-1-SevM";
        String compName7 = "FooProvider-1-SevM";
        Properties p7 = new Properties();
        p7.put("name", compName7);
        ci_lazzy_sevM = Utils.getComponentInstance(context, factName7 ,p7);
		
		lazzyRef = Utils.getServiceReference(context, Architecture.class.getName(), "(instance.name="+compName+")");
		immRef =   Utils.getServiceReference(context, Architecture.class.getName(), "(instance.name="+compName2+")");
		immRefSing = Utils.getServiceReference(context, Architecture.class.getName(), "(instance.name="+compName3+")");
		lazzyRefSing = Utils.getServiceReference(context, Architecture.class.getName(), "(instance.name="+compName4+")");
		lazzyRefSev = Utils.getServiceReference(context, Architecture.class.getName(), "(instance.name="+compName5+")");
		lazzyRefSingM = Utils.getServiceReference(context, Architecture.class.getName(), "(instance.name="+compName6+")");
		lazzyRefSevM = Utils.getServiceReference(context, Architecture.class.getName(), "(instance.name="+compName7+")");
		
		lazzyArch = (Architecture) context.getService(lazzyRef);
		immeArch = (Architecture) context.getService(immRef);
		immeArchSing = (Architecture) context.getService(immRefSing);
		lazzyArchSing = (Architecture) context.getService(lazzyRefSing);
		lazzyArchSev = (Architecture) context.getService(lazzyRefSev);
	    lazzyArchSingM = (Architecture) context.getService(lazzyRefSingM);
	    lazzyArchSevM = (Architecture) context.getService(lazzyRefSevM);
	}
	
	public void tearDown() {
		context.ungetService(lazzyRef);
		context.ungetService(immRef);
		context.ungetService(immRefSing);
		context.ungetService(lazzyRefSing);
		context.ungetService(lazzyRefSev);
		context.ungetService(lazzyRefSingM);
        context.ungetService(lazzyRefSevM);
		lazzyArch = null;
		immeArch = null;
		immeArchSing = null;
		lazzyArchSing = null;
		lazzyArchSev = null;
		lazzyArchSingM = null;
        lazzyArchSevM = null;
		ci_lazzy.dispose();
		ci_immediate.dispose();
		ci_immediate_singleton.dispose();
		ci_lazzy_sing.dispose();
		ci_lazzy_sev.dispose();
		ci_lazzy_singM.dispose();
        ci_lazzy_sevM.dispose();
	}
	
	public void testLazyCreation() {
		assertEquals("Check that no objects are created ", 0, lazzyArch.getInstanceDescription().getCreatedObjects().length);
		ServiceReference[] refs = null;
		try {
			refs = context.getServiceReferences(FooService.class.getName(), "(instance.name="+ci_lazzy.getInstanceName()+")");
		} catch (InvalidSyntaxException e) { e.printStackTrace(); }
		assertNotNull("Check that a FooService from " + ci_lazzy.getInstanceName() + " is available",refs);
		FooService fs = (FooService) context.getService(refs[0]);
		assertTrue("Check the FooService invocation", fs.foo());
		assertEquals("Check the creation of 1 object",1, lazzyArch.getInstanceDescription().getCreatedObjects().length);
		context.ungetService(refs[0]);
	}
	
	public void testLazyCreationSingleton() {
        assertEquals("Check that no objects are created ", 0, lazzyArchSing.getInstanceDescription().getCreatedObjects().length);
        ServiceReference[] refs = null;
        try {
            refs = context.getServiceReferences(FooService.class.getName(), "(instance.name="+ci_lazzy_sing.getInstanceName()+")");
        } catch (InvalidSyntaxException e) { e.printStackTrace(); }
        assertNotNull("Check that a FooService from " + ci_lazzy_sing.getInstanceName() + " is available",refs);
        FooService fs = (FooService) context.getService(refs[0]);
        assertTrue("Check the FooService invocation", fs.foo());
        assertEquals("Check the creation of 1 object",1, lazzyArchSing.getInstanceDescription().getCreatedObjects().length);
        context.ungetService(refs[0]);
    }
	
	public void testLazyCreationSeveral() {
        assertEquals("Check that no objects are created ", 0, lazzyArchSev.getInstanceDescription().getCreatedObjects().length);
        ServiceReference[] refs = null;
        try {
            refs = context.getServiceReferences(FooService.class.getName(), "(instance.name="+ci_lazzy_sev.getInstanceName()+")");
        } catch (InvalidSyntaxException e) { e.printStackTrace(); }
        assertNotNull("Check that a FooService from " + ci_lazzy_sev.getInstanceName() + " is available",refs);
        FooService fs = (FooService) context.getService(refs[0]);
        FooService fs2 = (FooService) context.getService(refs[0]);
        assertTrue("Check the FooService invocation", fs.foo());
        assertTrue("Check the FooService invocation-2", fs2.foo());
        assertEquals("Check the creation of 1 object",1, lazzyArchSev.getInstanceDescription().getCreatedObjects().length);
        context.ungetService(refs[0]);
    }
	
	public void testImmediateCreation() {
		assertEquals("Check that one object is created ", 1, immeArch.getInstanceDescription().getCreatedObjects().length);
		ServiceReference[] refs = null;
		try {
			refs = context.getServiceReferences(FooService.class.getName(), "(instance.name="+ci_immediate.getInstanceName()+")");
		} catch (InvalidSyntaxException e) { e.printStackTrace(); }
		assertNotNull("Check that a FooService from " + ci_immediate.getInstanceName() + " is available",refs);
		FooService fs = (FooService) context.getService(refs[0]);
		assertTrue("Check the FooService invocation", fs.foo());
		assertEquals("Check the creation of 1 object", 1, immeArch.getInstanceDescription().getCreatedObjects().length);
		context.ungetService(refs[0]);
	}
    
    public void testBundleContext() {
        ServiceReference[] refs = null;
        try {
            refs = context.getServiceReferences(FooService.class.getName(), "(instance.name="+ci_lazzy.getInstanceName()+")");
        } catch (InvalidSyntaxException e) { e.printStackTrace(); }
        assertNotNull("Check that a FooService from " + ci_lazzy.getInstanceName() + " is available",refs);
        FooService fs = (FooService) context.getService(refs[0]);
        Properties p = fs.fooProps();
        assertNotNull("Check the bundle context", p.get("context"));
        assertEquals("Check the creation of 1 object",1, lazzyArch.getInstanceDescription().getCreatedObjects().length);
        context.ungetService(refs[0]);
    }

    public void testImmediateSingletonCreation() {
    	assertEquals("Check that one object is created ", 1, immeArchSing.getInstanceDescription().getCreatedObjects().length);
    	ServiceReference[] refs = null;
    	try {
    		refs = context.getServiceReferences(FooService.class.getName(), "(instance.name="+ci_immediate_singleton.getInstanceName()+")");
    	} catch (InvalidSyntaxException e) { e.printStackTrace(); }
    	assertNotNull("Check that a FooService from " + ci_immediate_singleton.getInstanceName() + " is available",refs);
    	FooService fs = (FooService) context.getService(refs[0]);
    	assertTrue("Check the FooService invocation", fs.foo());
    	assertEquals("Check the creation of 1 object", 1, immeArchSing.getInstanceDescription().getCreatedObjects().length);
    	context.ungetService(refs[0]);
    }

    public void testLazyCreationSingletonM() {
        assertEquals("Check that no objects are created ", 0, lazzyArchSingM.getInstanceDescription().getCreatedObjects().length);
        ServiceReference[] refs = null;
        try {
            refs = context.getServiceReferences(FooService.class.getName(), "(instance.name="+ci_lazzy_singM.getInstanceName()+")");
        } catch (InvalidSyntaxException e) { e.printStackTrace(); }
        assertNotNull("Check that a FooService from " + ci_lazzy_singM.getInstanceName() + " is available",refs);
        FooService fs = (FooService) context.getService(refs[0]);
        FooService fs2 = (FooService) context.getService(refs[0]);
        assertTrue("Check the FooService invocation", fs.foo());
        assertTrue("Check the FooService invocation", fs2.foo());
        assertEquals("Check the creation of 1 object",1, lazzyArchSingM.getInstanceDescription().getCreatedObjects().length);
        context.ungetService(refs[0]);
    }

    public void testLazyCreationSeveralM() {
        assertEquals("Check that no objects are created ", 0, lazzyArchSevM.getInstanceDescription().getCreatedObjects().length);
        ServiceReference[] refs = null;
        try {
            refs = context.getServiceReferences(FooService.class.getName(), "(instance.name="+ci_lazzy_sevM.getInstanceName()+")");
        } catch (InvalidSyntaxException e) { e.printStackTrace(); }
        assertNotNull("Check that a FooService from " + ci_lazzy_sevM.getInstanceName() + " is available",refs);
        FooService fs = (FooService) context.getService(refs[0]);
        assertTrue("Check the FooService invocation", fs.foo());
        assertEquals("Check the creation of 1 object",1, lazzyArchSevM.getInstanceDescription().getCreatedObjects().length);
        FooService fs2 = (FooService) context.getService(refs[0]);
        assertTrue("Check the FooService invocation-2", fs2.foo());
        // Only one object as the getService method is called only once (service factory) despite the policy="method".
        assertEquals("Check the creation of 1 object",1, lazzyArchSevM.getInstanceDescription().getCreatedObjects().length);
        context.ungetService(refs[0]);
    }
    
    public void testCustomConstuctor() {
        FooService fs = new FooProviderType1(0, "foo", context);
        Properties props = fs.fooProps();
        assertEquals("Check bar", 0, ((Integer) props.get("bar")).intValue());
        assertEquals("Check foo", "foo", props.get("foo"));
        assertEquals("Check context", context, props.get("context"));
    }
    
    

}