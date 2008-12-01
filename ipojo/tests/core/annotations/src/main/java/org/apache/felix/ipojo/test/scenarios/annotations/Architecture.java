package org.apache.felix.ipojo.test.scenarios.annotations;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.metadata.Element;

public class Architecture extends OSGiTestCase {
    
    
    private IPOJOHelper helper;
    
    public void setUp() {
        helper = new IPOJOHelper(this);
    }
    
    
    public void testArch() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.Arch");
        String arch = meta.getAttribute("architecture");
        assertNotNull("Architecture exists ", arch);
        assertEquals("Architecture value", "true", arch);
    }
    
    public void testNoArch() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.NoArch");
        String arch = meta.getAttribute("architecture");
        assertNotNull("Architecture exists ", arch);
        assertEquals("Architecture value", "false", arch);
    }
    
    

}

