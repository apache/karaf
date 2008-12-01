package org.apache.felix.ipojo.test.scenarios.annotations;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.metadata.Element;

public class TemporalDependencies extends OSGiTestCase {
    
    private IPOJOHelper helper;
    
    public void setUp() {
        helper = new IPOJOHelper(this);
    }
    
    public void testSimple() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.temporal.TemporalSimple");
        Element[] provs = meta.getElements("requires", "org.apache.felix.ipojo.handler.temporal");
        assertNotNull("Temporal exists ", provs);
        String field = provs[0].getAttribute("field");
        assertNotNull("Field not null", field);
        assertEquals("Field is fs", "fs", field);
        String to = provs[0].getAttribute("timeout");
        assertNull("No timeout", to);
        String oto = provs[0].getAttribute("onTimeout");
        assertNull("No onTimeout", oto);
    }
    
    public void testDI() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.temporal.TemporalWithDI");
        Element[] provs = meta.getElements("requires", "org.apache.felix.ipojo.handler.temporal");
        assertNotNull("Temporal exists ", provs);
        String field = provs[0].getAttribute("field");
        assertNotNull("Field not null", field);
        assertEquals("Field is fs", "fs", field);
        
        String oto = provs[0].getAttribute("onTimeout");
        assertEquals("onTimeout is the DI", "org.apache.felix.ipojo.test.scenarios.component.ProvidesSimple", oto);
               
    }
    
    public void testEmptyArray() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.temporal.TemporalWithEmptyArray");
        Element[] provs = meta.getElements("requires", "org.apache.felix.ipojo.handler.temporal");
        assertNotNull("Temporal exists ", provs);
        String field = provs[0].getAttribute("field");
        assertNotNull("Field not null", field);
        assertEquals("Field is fs", "fs", field);
        
        String oto = provs[0].getAttribute("onTimeout");
        assertEquals("onTimeout is empty-array", "empty-array", oto);
               
    }
    
    public void testNull() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.temporal.TemporalWithNull");
        Element[] provs = meta.getElements("requires", "org.apache.felix.ipojo.handler.temporal");
        assertNotNull("Temporal exists ", provs);
        String field = provs[0].getAttribute("field");
        assertNotNull("Field not null", field);
        assertEquals("Field is fs", "fs", field);
        
        String oto = provs[0].getAttribute("onTimeout");
        assertEquals("onTimeout is null", "null", oto);
               
    }
    
    public void testNullable() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.temporal.TemporalWithNullable");
        Element[] provs = meta.getElements("requires", "org.apache.felix.ipojo.handler.temporal");
        assertNotNull("Temporal exists ", provs);
        String field = provs[0].getAttribute("field");
        assertNotNull("Field not null", field);
        assertEquals("Field is fs", "fs", field);
        
        String oto = provs[0].getAttribute("onTimeout");
        assertEquals("onTimeout is nullable", "nullable", oto);
               
    }
    
    public void testFilter() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.temporal.TemporalWithFilter");
        Element[] provs = meta.getElements("requires", "org.apache.felix.ipojo.handler.temporal");
        assertNotNull("Temporal exists ", provs);
        String field = provs[0].getAttribute("field");
        assertNotNull("Field not null", field);
        assertEquals("Field is fs", "fs", field);
        
        String filter = provs[0].getAttribute("filter");
        assertEquals("Filter", "(vendor=clement)", filter);
            
    }
    
    public void testTimeout() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.temporal.TemporalWithTimeout");
        Element[] provs = meta.getElements("requires", "org.apache.felix.ipojo.handler.temporal");
        assertNotNull("Temporal exists ", provs);
        String field = provs[0].getAttribute("field");
        assertNotNull("Field not null", field);
        assertEquals("Field is fs", "fs", field);
        
        String to = provs[0].getAttribute("timeout");
        assertEquals("Check timeout", "100", to);
            
    }

}
