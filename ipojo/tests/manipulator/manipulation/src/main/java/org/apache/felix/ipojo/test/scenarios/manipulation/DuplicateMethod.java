package org.apache.felix.ipojo.test.scenarios.manipulation;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.manipulation.service.Plop;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

public class DuplicateMethod extends OSGiTestCase {
    
    
    public void testDuplicateMethod() {
        ComponentInstance instance = Utils.getComponentInstanceByName(context, "plopimpl", "plop");
        ServiceReference ref = Utils.getServiceReferenceByName(context, Plop.class.getName(), "plop");
        assertNotNull("Check plop", ref);
        Plop plop = (Plop) context.getService(ref);
        Object o = plop.getPlop();
        assertEquals("Check result", "plop", o);
        context.ungetService(ref);
        instance.dispose();
    }
}
