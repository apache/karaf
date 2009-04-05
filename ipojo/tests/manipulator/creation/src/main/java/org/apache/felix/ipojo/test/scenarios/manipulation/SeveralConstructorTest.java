package org.apache.felix.ipojo.test.scenarios.manipulation;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.test.scenarios.manipulation.service.CheckService;
import org.osgi.framework.ServiceReference;

import org.apache.felix.ipojo.ComponentInstance;

public class SeveralConstructorTest extends OSGiTestCase {
    
    private IPOJOHelper helper;
    
    private ComponentInstance ci, ci2, ci3;
    
    public void setUp() {
        helper = new IPOJOHelper(this);
        ci = helper.createComponentInstance("org.apache.felix.ipojo.test.scenarios.component.SeveralConstructors");
        ci2 = helper.createComponentInstance("org.apache.felix.ipojo.test.scenarios.component.NoEmptyConstructor");
        ci3 = helper.createComponentInstance("org.apache.felix.ipojo.test.scenarios.component.NoEmptyConstructorWithParentClass");

    }
    
    public void tearDown() {
        helper.dispose();
    }

    
    public void testSeveralConstructor() {
        ServiceReference ref = helper.getServiceReferenceByName(CheckService.class.getName(), ci.getInstanceName());
        CheckService cs = (CheckService) getServiceObject(ref);
        assertTrue("Check assignation", cs.check());
        String name = (String) cs.getProps().get("name");
        assertEquals("Check message", "hello world", name);
        //assertNull("Check message", name);
    }
    
    public void testNoEmptyConstructor() {
        ServiceReference ref = helper.getServiceReferenceByName(CheckService.class.getName(), ci2.getInstanceName());
        CheckService cs = (CheckService) getServiceObject(ref);
        assertFalse("Check assignation", cs.check());
        String name = (String) cs.getProps().get("name");
        assertEquals("Check message", "NULL", name);
    }
    
    public void testNoEmptyConstructorWithAParentClass() {
        ServiceReference ref = helper.getServiceReferenceByName(CheckService.class.getName(), ci3.getInstanceName());
        CheckService cs = (CheckService) getServiceObject(ref);
        assertTrue("Check assignation", cs.check()); // super set name to "hello"
        String name = (String) cs.getProps().get("name");
        assertEquals("Check message", "hello", name);
    }

}
