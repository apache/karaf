package org.apache.felix.ipojo.test.scenarios.annotations;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.metadata.Element;

public class LifecycleController extends OSGiTestCase {
    
    private IPOJOHelper helper;
    
    public void setUp() {
        helper = new IPOJOHelper(this);
    }
    
    public void testLFC() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.Lifecycle");
        Element[] ctrls = meta.getElements("controller");
        assertNotNull("Controller exists ", ctrls);
        Element ctrl = ctrls[0];
        assertNotNull("Field", ctrl.getAttribute("field"));
        assertEquals("Field", "lfc", ctrl.getAttribute("field"));
    }
    
    

}

