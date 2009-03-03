package org.apache.felix.ipojo.test.scenarios.manipulation;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.test.scenarios.manipulation.service.CheckService;
import org.osgi.framework.ServiceReference;

import org.apache.felix.ipojo.ComponentInstance;

public class SeveralConstructorTest extends OSGiTestCase {
    
    private IPOJOHelper helper;
    
    private ComponentInstance ci;
    
    public void setUp() {
        helper = new IPOJOHelper(this);
        ci = helper.createComponentInstance("org.apache.felix.ipojo.test.scenarios.component.SeveralConstructors");
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
    }

}
