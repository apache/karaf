package org.apache.felix.ipojo.test.scenarios.manipulation;

import java.util.List;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.test.scenarios.manipulation.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.manipulation.service.FooService;
import org.osgi.framework.ServiceReference;

public class TypedList extends OSGiTestCase {
    
    ComponentInstance foo1, foo2;
    ComponentInstance checker;
    IPOJOHelper helper; 
    
    public void setUp() {
        helper = new IPOJOHelper(this);
        foo1 = helper.createComponentInstance("org.apache.felix.ipojo.test.scenarios.component.FooServiceImpl", "foo1");
        foo2 = helper.createComponentInstance("org.apache.felix.ipojo.test.scenarios.component.FooServiceImpl", "foo2");
        checker = helper.createComponentInstance("TypedList", "checker");
        foo1.stop();
        foo2.stop();
    }
    
    public void tearDown() {
        helper.dispose();
    }
    
    public void testTypedList() {
        ServiceReference ref = helper.getServiceReferenceByName(CheckService.class.getName(), checker.getInstanceName()); 
        CheckService check = (CheckService) getServiceObject(ref);
        assertNotNull("Checker availability", check);
        // Check without providers
        assertFalse("Empty list", check.check());
        
        // Start the first provider
        foo1.start();
        assertTrue("List with one element", check.check());
        Properties props = check.getProps();
        List<FooService> list = (List<FooService>) props.get("list");
        assertEquals("Check size - 1", 1, list.size());
        
        // Start the second provider 
        foo2.start();
        assertTrue("List with two element", check.check());
        props = check.getProps();
        list = (List<FooService>) props.get("list");
        assertEquals("Check size - 2", 2, list.size());
        
        // Stop the first one
        foo1.stop();
        assertTrue("List with one element (2)", check.check());
        props = check.getProps();
        list = (List<FooService>) props.get("list");
        assertEquals("Check size - 3", 1, list.size());
    }

}
