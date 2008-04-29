package org.apache.felix.ipojo.test.scenarios.annotations;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.test.scenarios.util.Utils;

public class LifecycleController extends OSGiTestCase {
    
    public void testLFC() {
        Element meta = Utils.getMetatadata(context, "org.apache.felix.ipojo.test.scenarios.component.Lifecycle");
        Element[] ctrls = meta.getElements("controller");
        assertNotNull("Controller exists ", ctrls);
        Element ctrl = ctrls[0];
        assertNotNull("Field", ctrl.getAttribute("field"));
        assertEquals("Field", "lfc", ctrl.getAttribute("field"));
    }
    
    

}

