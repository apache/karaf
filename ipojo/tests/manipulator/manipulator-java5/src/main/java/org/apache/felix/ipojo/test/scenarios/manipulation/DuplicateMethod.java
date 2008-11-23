package org.apache.felix.ipojo.test.scenarios.manipulation;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.test.scenarios.manipulation.service.Plop;
import org.osgi.framework.ServiceReference;

public class DuplicateMethod extends OSGiTestCase {
    
    
    public void testDuplicateMethod() {
        IPOJOHelper helper = new IPOJOHelper(this);
        helper.createComponentInstance("plopimpl", "plop");
        ServiceReference ref = helper.getServiceReferenceByName(Plop.class.getName(), "plop");
        assertNotNull("Check plop", ref);
        Plop plop = (Plop) getServiceObject(ref);
        Object o = plop.getPlop();
        assertEquals("Check result", "plop", o);
        helper.dispose();
    }
}
