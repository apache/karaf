package org.apache.felix.ipojo.test.scenarios.annotations;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.test.scenarios.util.Utils;

public class Architecture extends OSGiTestCase {
    
    public void testArch() {
        Element meta = Utils.getMetatadata(context, "org.apache.felix.ipojo.test.scenarios.component.Arch");
        String arch = meta.getAttribute("architecture");
        assertNotNull("Architecture exists ", arch);
        assertEquals("Architecture value", "true", arch);
    }
    
    public void testNoArch() {
        Element meta = Utils.getMetatadata(context, "org.apache.felix.ipojo.test.scenarios.component.NoArch");
        String arch = meta.getAttribute("architecture");
        assertNotNull("Architecture exists ", arch);
        assertEquals("Architecture value", "false", arch);
    }
    
    

}

