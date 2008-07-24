package org.apache.felix.ipojo.test.scenarios.annotations;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.test.scenarios.util.Utils;

public class LifecycleCallbacks extends OSGiTestCase {
    
    public void testCallbacks() {
        Element meta = Utils.getMetatadata(context, "org.apache.felix.ipojo.test.scenarios.component.Lifecycle");
        Element[] cbs = meta.getElements("callback");
        assertNotNull("Callbacks exists ", cbs);
        assertEquals("Callbacks count ", 2, cbs.length);
        
        Element elem = getCallbackByMethodName(cbs, "start");
        assertEquals("Check start method", "start", elem.getAttribute("method"));
        assertEquals("Check start transition", "validate", elem.getAttribute("transition"));
        
        elem = getCallbackByMethodName(cbs, "stop");
        assertEquals("Check stop method", "stop", elem.getAttribute("method"));
        assertEquals("Check stop transition", "invalidate", elem.getAttribute("transition"));
    }
    
    public void testImmediate() {
        Element meta = Utils.getMetatadata(context, "org.apache.felix.ipojo.test.scenarios.component.Immediate");
        assertNotNull("Immediate attribute", meta.getAttribute("immediate"));
        assertEquals("Immediate attribute value", "true", meta.getAttribute("immediate"));
    }
    
    public void testNoImmediate() {
        Element meta = Utils.getMetatadata(context, "org.apache.felix.ipojo.test.scenarios.component.NoImmediate");
        assertNotNull("Immediate attribute", meta.getAttribute("immediate"));
        assertEquals("Immediate attribute value", "false", meta.getAttribute("immediate"));
    }
    
    private Element getCallbackByMethodName(Element[] cbs, String method) {
        for (int i = 0; i < cbs.length; i++) {
            String met = cbs[i].getAttribute("method");
            if (met != null && met.equalsIgnoreCase(method)) {
                return cbs[i];
            }
        }
        fail("Cannot found the callback with the method " + method);
        return null;
    }
    
    

}
