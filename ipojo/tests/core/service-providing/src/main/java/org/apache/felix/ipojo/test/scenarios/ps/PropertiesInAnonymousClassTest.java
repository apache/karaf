package org.apache.felix.ipojo.test.scenarios.ps;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.test.scenarios.ps.service.FooService;
import org.osgi.framework.ServiceReference;

public class PropertiesInAnonymousClassTest extends OSGiTestCase {

	IPOJOHelper helper;

	public void setUp() {
	    helper = new IPOJOHelper(this);
		String type = "PS-FooProviderTypeAnonymous-Dyn";
		helper.createComponentInstance(type, "FooProviderAno-1");

	}

	public void tearDown() {
		helper.dispose();
	}

	public void testProperties1() {
		ServiceReference sr = helper.getServiceReferenceByName(FooService.class.getName(), "FooProviderAno-1");
		assertNotNull("Check the availability of the FS service", sr);

		// Check service properties
		Integer intProp = (Integer) sr.getProperty("int");
		Boolean boolProp = (Boolean) sr.getProperty("boolean");
		String strProp = (String) sr.getProperty("string");
		String[] strAProp = (String[]) sr.getProperty("strAProp");
		int[] intAProp = (int[]) sr.getProperty("intAProp");

		assertEquals("Check intProp equality (1)", intProp, new Integer(2));
		assertEquals("Check longProp equality (1)", boolProp, new Boolean(false));
		assertEquals("Check strProp equality (1)", strProp, new String("foo"));
		assertNotNull("Check strAProp not nullity (1)", strAProp);
		String[] v = new String[] {"foo", "bar"};
		for (int i = 0; i < strAProp.length; i++) {
			if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality (1)"); }
		}
		assertNotNull("Check intAProp not nullity", intAProp);
		int[] v2 = new int[] {1, 2, 3};
		for (int i = 0; i < intAProp.length; i++) {
			if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality (1)"); }
		}

		// Invoke
		FooService fs = (FooService) getServiceObject(sr);
		assertTrue("invoke fs", fs.foo());

		sr = helper.getServiceReferenceByName(FooService.class.getName(), "FooProviderAno-1");
		// Re-check the property (change)
		intProp = (Integer) sr.getProperty("int");
		boolProp = (Boolean) sr.getProperty("boolean");
		strProp = (String) sr.getProperty("string");
		strAProp = (String[]) sr.getProperty("strAProp");
		intAProp = (int[]) sr.getProperty("intAProp");

		assertEquals("Check intProp equality (2)", intProp, new Integer(3));
		assertEquals("Check longProp equality (2)", boolProp, new Boolean(true));
		assertEquals("Check strProp equality (2)", strProp, new String("bar"));
		assertNotNull("Check strAProp not nullity (2)", strAProp);
		v = new String[] {"foo", "bar", "baz"};
		for (int i = 0; i < strAProp.length; i++) {
			if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality (2)"); }
		}
		assertNotNull("Check intAProp not nullity (2)", intAProp);
		v2 = new int[] {3, 2, 1};
		for (int i = 0; i < intAProp.length; i++) {
			if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality (2)"); }
		}

		fs = null;
	}

}
