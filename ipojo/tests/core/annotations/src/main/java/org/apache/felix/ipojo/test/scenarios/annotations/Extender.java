package org.apache.felix.ipojo.test.scenarios.annotations;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.metadata.Element;

public class Extender extends OSGiTestCase {
    
    String type = "org.apache.felix.ipojo.test.scenarios.component.extender.Extender";
    String namespace = "org.apache.felix.ipojo.extender";
    
    private IPOJOHelper helper;
    
    public void setUp() {
        helper = new IPOJOHelper(this);
    }
    
    public void testMetadata() {
        Element meta = helper.getMetadata(type);
        assertNotNull("Check meta", meta);
        Element[] ext = meta.getElements("extender", namespace);
        assertEquals("Check size", 1, ext.length);
        String extension = ext[0].getAttribute("extension");
        String onArr = ext[0].getAttribute("onArrival");
        String onDep = ext[0].getAttribute("onDeparture");
        
        assertEquals("Check extension", "foo", extension);
        assertEquals("Check onArrival", "onArrival", onArr);
        assertEquals("Check onDeparture", "onDeparture", onDep);
    }

}
