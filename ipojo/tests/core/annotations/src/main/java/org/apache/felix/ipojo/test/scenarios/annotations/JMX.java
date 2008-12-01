package org.apache.felix.ipojo.test.scenarios.annotations;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.metadata.Element;

public class JMX extends OSGiTestCase {
    
    private IPOJOHelper helper;
    
    public void setUp() {
        helper = new IPOJOHelper(this);
    }
    
    public void testSimple() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.jmx.JMXSimple");
        /*
         * org.apache.felix.ipojo.handlers.jmx:config domain="my-domain" usesmosgi="false"
        org.apache.felix.ipojo.handlers.jmx:property field="m_foo" name="prop" rights="w" notification="true"
        org.apache.felix.ipojo.handlers.jmx:method description="get the foo prop" method="getFoo"
        org.apache.felix.ipojo.handlers.jmx:method description="set the foo prop" method="setFoo"
         */
        
        Element[] ele = meta.getElements("config", "org.apache.felix.ipojo.handlers.jmx");
        assertNotNull("ele not null", ele);
        assertEquals("Ele size", 1, ele.length);
        String domain = ele[0].getAttribute("domain");
        String mosgi = ele[0].getAttribute("usesmosgi");
        assertEquals("domain", "my-domain", domain);
        assertEquals("mosgi", "false", mosgi);
        
        Element[] props = ele[0].getElements("property", "org.apache.felix.ipojo.handlers.jmx");
        assertNotNull("props not null", props);
        assertEquals("props size", 1, props.length);
        
        Element[] methods = ele[0].getElements("method", "org.apache.felix.ipojo.handlers.jmx");
        assertNotNull("methods not null", methods);
        assertEquals("methods size", 2, methods.length);
        


    }

}
