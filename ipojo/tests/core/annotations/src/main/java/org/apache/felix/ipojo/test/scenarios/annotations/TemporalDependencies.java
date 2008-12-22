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
    
    public void testSimpleCollection() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.temporal.TemporalCollection");
        Element dep = getElementPerField(meta, "fs1");
        String spec = dep.getAttribute("specification");
        assertNotNull("Specification not null", spec);
        assertEquals("Check specification", "org.apache.felix.ipojo.test.scenarios.annotations.service.FooService", spec);
    }
    
    public void testCollectionWithTimeout() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.temporal.TemporalCollection");
        Element dep = getElementPerField(meta, "fs2");
        String spec = dep.getAttribute("specification");
        assertNotNull("Specification not null", spec);
        assertEquals("Check specification", "org.apache.felix.ipojo.test.scenarios.annotations.service.FooService", spec);
        String to = dep.getAttribute("timeout");
        assertEquals("Check timeout", "300", to);
    }
    
    public void testCollectionWithPolicy() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.temporal.TemporalCollection");
        Element dep = getElementPerField(meta, "fs3");
        String spec = dep.getAttribute("specification");
        assertNotNull("Specification not null", spec);
        assertEquals("Check specification", "org.apache.felix.ipojo.test.scenarios.annotations.service.FooService", spec);
        String to = dep.getAttribute("ontimeout");
        assertEquals("Check policy", "empty", to);
    }
    
    public void testCollectionWithProxy() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.temporal.TemporalCollection");
        Element dep = getElementPerField(meta, "fs4");
        String spec = dep.getAttribute("specification");
        assertNotNull("Specification not null", spec);
        assertEquals("Check specification", "org.apache.felix.ipojo.test.scenarios.annotations.service.FooService", spec);
        String proxy = dep.getAttribute("proxy");
        assertEquals("Check proxy", "true", proxy);
    }
    
    private Element getElementPerField(Element elem, String field) {
        Element[] provs = elem.getElements("requires", "org.apache.felix.ipojo.handler.temporal");
        assertNotNull("Temporal exists ", provs);
        for (int i = 0; i < provs.length; i++) {
            if (provs[i].getAttribute("field").equals(field)) {
                return provs[i];
            }
        }
        return null;
    }

}
