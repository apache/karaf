package org.apache.felix.ipojo.test.scenarios.annotations;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.test.scenarios.util.Utils;

public class WhiteBoard extends OSGiTestCase {
    
    String typeWI = "org.apache.felix.ipojo.test.scenarios.component.whiteboard.WhiteBoardWIModification";
    String typeWO = "org.apache.felix.ipojo.test.scenarios.component.whiteboard.WhiteBoardWOModification";
    String namespace = "org.apache.felix.ipojo.whiteboard";
    
    public void testMetadataWithOnModification() {
        Element meta = Utils.getMetatadata(context, typeWI);
        assertNotNull("Check meta", meta);
        Element[] ext = meta.getElements("wbp", namespace);
        assertEquals("Check size", 1, ext.length);
        String filter = ext[0].getAttribute("filter");
        String onArr = ext[0].getAttribute("onArrival");
        String onDep = ext[0].getAttribute("onDeparture");
        String onMod = ext[0].getAttribute("onModification");

        
        assertEquals("Check filter", "(foo=true)", filter);
        assertEquals("Check onArrival", "onArrival", onArr);
        assertEquals("Check onDeparture", "onDeparture", onDep);
        assertEquals("Check onModification", "onModification", onMod);

    }
    
    public void testMetadataWithoutOnModification() {
        Element meta = Utils.getMetatadata(context, typeWO);
        assertNotNull("Check meta", meta);
        Element[] ext = meta.getElements("wbp", namespace);
        assertEquals("Check size", 1, ext.length);
        String filter = ext[0].getAttribute("filter");
        String onArr = ext[0].getAttribute("onArrival");
        String onDep = ext[0].getAttribute("onDeparture");
        String onMod = ext[0].getAttribute("onModification");

        
        assertEquals("Check filter", "(foo=true)", filter);
        assertEquals("Check onArrival", "onArrival", onArr);
        assertEquals("Check onDeparture", "onDeparture", onDep);
        assertNull("Check onModification", onMod);

    }

}
