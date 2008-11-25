package org.apache.felix.ipojo.test.scenarios.annotations;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.test.scenarios.util.Utils;

public class Extender extends OSGiTestCase {
    
    String type = "org.apache.felix.ipojo.test.scenarios.component.extender.Extender";
    String namespace = "org.apache.felix.ipojo.extender";

    
    public void testMetadata() {
        Element meta = Utils.getMetatadata(context, type);
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
