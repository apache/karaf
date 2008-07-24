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

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.util.Utils;

/**
 * Test unacceptable configuration.
 */
public class UnacceptableConfigurationTest extends OSGiTestCase {

	/**
	 * Configuration without the name property.
	 */
	public void testWithoutName() {
		Factory f = Utils.getFactoryByName(context, "Factories-FooProviderType-2");
		
		Properties  p = new Properties();
		p.put("int", new Integer(3));
		p.put("long", new Long(42));
		p.put("string", "absdir");
		p.put("strAProp", new String[] {"a"});
		p.put("intAProp", new int[] {1,2});
		
		ComponentInstance ci = null;
		try {
			ci = f.createComponentInstance(p);
			ci.dispose();
		} catch(Exception e) { fail("an acceptable configuration is refused : " + e.getMessage()); }
		
	}
	
	/**
	 * Empty configuration.
	 */
	public void testEmptyConfiguration() {
		Factory f = Utils.getFactoryByName(context, "Factories-FooProviderType-2");
		Properties  p = new Properties();
		
		ComponentInstance ci = null;
		try {
			ci = f.createComponentInstance(p);
			ci.dispose();
		} catch(Exception e) { fail("An acceptable configuration is refused"); }
	}
	
	/**
	 * Empty configuration (just the name).
	 */
	public void testEmptyConfiguration2() {
		Factory f = Utils.getFactoryByName(context, "Factories-FooProviderType-Dyn2");
		Properties  p = new Properties();
		p.put("name", "ko");
		ComponentInstance ci = null;
		try {
			ci = f.createComponentInstance(p);
			ci.dispose();
		} catch(Exception e) { return; }
		
		fail("An unacceptable configuration is accepted");
	}
	
	/**
	 * Null configuration (accept).
	 */
	public void testNull() {
		Factory f = Utils.getFactoryByName(context, "Factories-FooProviderType-2");
		
		ComponentInstance ci = null;
		try {
			ci = f.createComponentInstance(null);
			ci.dispose();
		} catch(Exception e) { fail("An acceptable configuration is refused"); }
	}
	
	/**
	 * Null configuration (fail).
	 */
	public void testNull2() {
		Factory f = Utils.getFactoryByName(context, "Factories-FooProviderType-Dyn2");
		
		ComponentInstance ci = null;
		try {
			ci = f.createComponentInstance(null);
			ci.dispose();
		} catch(Exception e) { return; }
		
		fail("An unacceptable configuration is accepted");
	}
	
	/**
	 * Check static properties.
	 */
	public void testStaticOK() {
		Factory f = Utils.getFactoryByName(context, "Factories-FooProviderType-2");
		
		Properties  p = new Properties();
		p.put("name", "ok");
		p.put("int", new Integer(3));
		p.put("long", new Long(42));
		p.put("string", "absdir");
		p.put("strAProp", new String[] {"a"});
		p.put("intAProp", new int[] {1,2});
		
		ComponentInstance ci = null;
		try {
			ci = f.createComponentInstance(p);
			ci.dispose();
		} catch(Exception e) {
			fail("An acceptable configuration is rejected : " + e.getMessage());
		}
	}
	
	/**
	 * Check dynamic properties.
	 */
	public void testDynamicOK() {
		Factory f = Utils.getFactoryByName(context, "Factories-FooProviderType-Dyn");
		
		Properties  p = new Properties();
		p.put("name", "ok");
		p.put("int", new Integer(3));
		p.put("boolean", new Boolean(true));
		p.put("string", "absdir");
		p.put("strAProp", new String[] {"a"});
		p.put("intAProp", new int[] {1,2});
		
		ComponentInstance ci = null;
		try {
			ci = f.createComponentInstance(p);
			ci.dispose();
		} catch(Exception e) {
		    e.printStackTrace();
			fail("An acceptable configuration is rejected : " + e.getMessage());
		}
	}
	
	/**
	 * Check inconsistent types.
	 */
	public void testDynamicBadType() {
		Factory f = Utils.getFactoryByName(context, "Factories-FooProviderType-Dyn");
		
		Properties  p = new Properties();
		p.put("name", "ok");
		p.put("int", new Integer(3));
		p.put("long", new Long(42));
		p.put("string", "absdir");
		p.put("strAProp", new String[] {"a"});
		p.put("intAProp", new int[] {1,2});
		
		ComponentInstance ci = null;
		try {
			ci = f.createComponentInstance(p);
			ci.dispose();
		} catch(Exception e) {
			fail("An acceptable configuration is rejected : " + e.getMessage());
		}
	}
	
	/**
	 * Check good configuration (with overriding).
	 */
	public void testDynamicComplete() {
		Factory f = Utils.getFactoryByName(context, "Factories-FooProviderType-Dyn2");
		
		Properties  p = new Properties();
		p.put("name", "ok");
		p.put("int", new Integer(3));
		p.put("boolean", new Boolean(true));
		p.put("string", "absdir");
		p.put("strAProp", new String[] {"a"});
		p.put("intAProp", new int[] {1,2});
		
		ComponentInstance ci = null;
		try {
			ci = f.createComponentInstance(p);
			ci.dispose();
		} catch(Exception e) {
			fail("An acceptable configuration is rejected : " + e.getMessage());
		}
	}
	
	/**
	 * Check good configuration.
	 */
	public void testDynamicJustEnough() {
		Factory f = Utils.getFactoryByName(context, "Factories-FooProviderType-Dyn2");
		
		Properties  p = new Properties();
		p.put("name", "ok");
		p.put("boolean", new Boolean(true));
		p.put("string", "absdir");
		p.put("strAProp", new String[] {"a"});
		
		ComponentInstance ci = null;
		try {
			ci = f.createComponentInstance(p);
			ci.dispose();
		} catch(Exception e) {
			fail("An acceptable configuration is rejected : " + e.getMessage());
		}
	}
	
	/**
	 * Check good configuration.
	 */
	public void testDynamicMix() {
		Factory f = Utils.getFactoryByName(context, "Factories-FooProviderType-Dyn2");
		
		Properties  p = new Properties();
		p.put("name", "ok");
		p.put("boolean", new Boolean(true));
		p.put("string", "absdir");
		p.put("strAProp", new String[] {"a"});
		p.put("intAProp", new int[] {1,2});
		
		ComponentInstance ci = null;
		try {
			ci = f.createComponentInstance(p);
			ci.dispose();
		} catch(Exception e) {
			fail("An acceptable configuration is rejected : " + e.getMessage());
		}
	}
	
	/**
	 * Check uncomplete configuration.
	 */
	public void testDynamicUncomplete() {
		Factory f = Utils.getFactoryByName(context, "Factories-FooProviderType-Dyn2");
		
		Properties  p = new Properties();
		p.put("name", "ok");
		p.put("string", "absdir");
		p.put("strAProp", new String[] {"a"});
		p.put("intAProp", new int[] {1,2});
		
		ComponentInstance ci = null;
		try {
			ci = f.createComponentInstance(p);
			ci.dispose();
		} catch(Exception e) { return; }
		
		fail("An unacceptable configuration is accepted");
	}
	
	/**
	 * Check good configuration (more properties).
	 */
	public void testDynamicMore() {
		Factory f = Utils.getFactoryByName(context, "Factories-FooProviderType-Dyn2");
		
		Properties  p = new Properties();
		p.put("name", "ok");
		p.put("int", new Integer(3));
		p.put("boolean", new Boolean(true));
		p.put("string", "absdir");
		p.put("strAProp", new String[] {"a"});
		p.put("intAProp", new int[] {1,2});
		p.put("tralala", "foo");
		
		ComponentInstance ci = null;
		try {
			ci = f.createComponentInstance(p);
			ci.dispose();
		} catch(Exception e) {
			fail("An acceptable configuration is rejected : " + e.getMessage());
		}
	}
	
	/**
	 * Check properties affecting services and component.
	 */
	public void testDoubleProps() {
		Factory f = Utils.getFactoryByName(context, "Factories-FooProviderType-Dyn2");
		
		Properties  p = new Properties();
		p.put("name", "ok");
		p.put("int", new Integer(3));
		p.put("boolean", new Boolean(true));
		p.put("string", "absdir");
		p.put("strAProp", new String[] {"a"});
		p.put("intAProp", new int[] {1,2});
		p.put("boolean", new Boolean(false));
		p.put("string", "toto");
		
		ComponentInstance ci = null;
		try {
			ci = f.createComponentInstance(p);
			ci.dispose();
		} catch(Exception e) {
			fail("An acceptable configuration is rejected : " + e.getMessage());
		}
	}
    
    /**
     * Check instance name unicity.
     */
    public void testUnicity1() {
        Factory f = Utils.getFactoryByName(context, "Factories-FooProviderType-2");
        
        ComponentInstance ci1,ci2, ci3 = null;
        try {
            ci1 = f.createComponentInstance(null);
            ci2 = f.createComponentInstance(null);
            ci3 = f.createComponentInstance(null);
            assertNotEquals("Check name ci1, ci2", ci1.getInstanceName(), ci2.getInstanceName());
            assertNotEquals("Check name ci1, ci3", ci1.getInstanceName(), ci3.getInstanceName());
            assertNotEquals("Check name ci3, ci2", ci3.getInstanceName(), ci2.getInstanceName());
            ci1.dispose();
            ci2.dispose();
            ci3.dispose();
        } catch(Exception e) { fail("An acceptable configuration is refused"); }
    }
    
    /**
     * Check instance name unicity.
     */
    public void testUnicity2() {
        Factory f = Utils.getFactoryByName(context, "Factories-FooProviderType-2");
        
        ComponentInstance ci1,ci2, ci3 = null;
        try {
            Properties p1 = new Properties();
            p1.put("name", "name1");
            ci1 = f.createComponentInstance(p1);
            Properties p2 = new Properties();
            p2.put("name", "name2");
            ci2 = f.createComponentInstance(p2);
            Properties p3 = new Properties();
            p3.put("name", "name3");
            ci3 = f.createComponentInstance(p3);
            assertNotEquals("Check name ci1, ci2", ci1.getInstanceName(), ci2.getInstanceName());
            assertNotEquals("Check name ci1, ci3", ci1.getInstanceName(), ci3.getInstanceName());
            assertNotEquals("Check name ci3, ci2", ci3.getInstanceName(), ci2.getInstanceName());
            ci1.dispose();
            ci2.dispose();
            ci3.dispose();
        } catch(Exception e) { fail("An acceptable configuration is refused"); }
    }
    
    /**
     * Check instance name unicity.
     */
    public void testUnicity3() {
        Factory f = Utils.getFactoryByName(context, "Factories-FooProviderType-2");
        
        ComponentInstance ci1 = null,ci2 = null, ci3 = null;
        try {
            Properties p1 = new Properties();
            p1.put("name", "name1");
            ci1 = f.createComponentInstance(p1);
            Properties p2 = new Properties();
            p2.put("name", "name1");
            ci2 = f.createComponentInstance(p2);
            assertNotEquals("Check name ci1, ci2", ci1.getInstanceName(), ci2.getInstanceName());
            assertNotEquals("Check name ci1, ci3", ci1.getInstanceName(), ci3.getInstanceName());
            assertNotEquals("Check name ci3, ci2", ci3.getInstanceName(), ci3.getInstanceName());
            ci1.dispose();
            ci2.dispose();
            ci3.dispose();
        } catch(Exception e) { 
            ci1.dispose();
            return; }
          
          fail("An unacceptable configuration is acceptable");
    }
    
    /**
     * Check instance name unicity.
     */
    public void testUnicity4() {
        Factory f = Utils.getFactoryByName(context, "Factories-FooProviderType-2");
        Factory f2 = Utils.getFactoryByName(context, "Factories-FooProviderType-1");
        
        ComponentInstance ci1 = null,ci2 = null, ci3 = null;
        try {
            Properties p1 = new Properties();
            p1.put("name", "name1");
            ci1 = f.createComponentInstance(p1);
            Properties p2 = new Properties();
            p2.put("name", "name1");
            ci2 = f2.createComponentInstance(p2);
            System.err.println("==== " + ci1.getInstanceName() + " === " + ci2.getInstanceName());
            assertNotEquals("Check name ci1, ci2", ci1.getInstanceName(), ci2.getInstanceName());
            assertNotEquals("Check name ci1, ci3", ci1.getInstanceName(), ci3.getInstanceName());
            assertNotEquals("Check name ci3, ci2", ci3.getInstanceName(), ci3.getInstanceName());
            ci1.dispose();
            ci2.dispose();
            ci3.dispose();
        } catch(Exception e) { 
            ci1.dispose();
            return; }
          
          fail("An unacceptable configuration is acceptable");
    }
	

}
