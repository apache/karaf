package org.apache.felix.ipojo.test.scenarios.annotations;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.metadata.Element;

public class Factory extends OSGiTestCase {
    
    private IPOJOHelper helper;
    
    public void setUp() {
        helper = new IPOJOHelper(this);
    }
    
    public void testArch() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.Factory");
        String fact = meta.getAttribute("public");
        String name = meta.getAttribute("name");
        assertNotNull("Factory exists ", fact);
        assertEquals("Factory value", "true", fact);
        assertNotNull("Name exists ", name);
        assertEquals("Name value", "factory", name);
    }
    
    public void testNoArch() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.NoFactory");
        String fact = meta.getAttribute("public");
        String name = meta.getAttribute("name");
        assertNotNull("Factory exists ", fact);
        assertEquals("Factory value", "false", fact);
        assertNotNull("Name exists ", name);
        assertEquals("Name value", "nofactory", name);
    }
    
    public void testFactoryMethod() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.FactoryMethod");
        String method = meta.getAttribute("factory-method");
        assertNotNull("Method exists ", method);
        assertEquals("Method value", "create", method);
    }
    
    

}

