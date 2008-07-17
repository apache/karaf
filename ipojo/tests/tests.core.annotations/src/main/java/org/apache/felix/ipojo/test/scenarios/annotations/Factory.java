package org.apache.felix.ipojo.test.scenarios.annotations;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.test.scenarios.util.Utils;

public class Factory extends OSGiTestCase {
    
    public void testArch() {
        Element meta = Utils.getMetatadata(context, "org.apache.felix.ipojo.test.scenarios.component.Factory");
        String fact = meta.getAttribute("public");
        String name = meta.getAttribute("name");
        assertNotNull("Factory exists ", fact);
        assertEquals("Factory value", "true", fact);
        assertNotNull("Name exists ", name);
        assertEquals("Name value", "factory", name);
    }
    
    public void testNoArch() {
        Element meta = Utils.getMetatadata(context, "org.apache.felix.ipojo.test.scenarios.component.NoFactory");
        String fact = meta.getAttribute("public");
        String name = meta.getAttribute("name");
        assertNotNull("Factory exists ", fact);
        assertEquals("Factory value", "false", fact);
        assertNotNull("Name exists ", name);
        assertEquals("Name value", "nofactory", name);
    }
    
    

}

