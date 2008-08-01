package org.apache.felix.ipojo.test.scenarios.annotations;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.test.scenarios.util.Utils;

public class FilteredDependency extends OSGiTestCase {
    
    private Element[] deps ;
    private Element[] froms;
    
    public void setUp() {
        Element meta = Utils.getMetatadata(context, "org.apache.felix.ipojo.test.scenarios.component.FilteredDependency");
        deps = meta.getElements("requires");
        
        Element meta2 = Utils.getMetatadata(context, "org.apache.felix.ipojo.test.scenarios.component.FromDependency");
        froms = meta2.getElements("requires");
    }
    
    public void testField() {
        Element dep = getDependencyById(deps, "fs");
        String opt = dep.getAttribute("filter");
        assertEquals("Check filter", "(foo=bar)", opt);
    }
    
    public void testCallbackBind() {
        Element dep = getDependencyById(deps, "Bar");
        String opt = dep.getAttribute("filter");
        assertEquals("Check filter", "(foo=bar)", opt);
    }
    
    public void testCallbackUnbind() {
        Element dep = getDependencyById(deps, "Baz");
        String opt = dep.getAttribute("filter");
        assertEquals("Check filter", "(foo=bar)", opt);
    }
    
    public void testBoth() {
        Element dep = getDependencyById(deps, "inv");
        String opt = dep.getAttribute("filter");
        assertEquals("Check filter", "(foo=bar)", opt);
    }
    
    public void testBindOnly() {
        Element dep = getDependencyById(deps, "bindonly");
        String opt = dep.getAttribute("filter");
        assertEquals("Check filter", "(foo=bar)", opt);
    }
    
    public void testUnbindOnly() {
        Element dep = getDependencyById(deps, "unbindonly");
        String opt = dep.getAttribute("filter");
        assertEquals("Check filter", "(foo=bar)", opt);
    }
    
    public void testFromField() {
        Element dep = getDependencyById(froms, "fs");
        String from = dep.getAttribute("from");
        assertEquals("Check from", "X", from);
    }
    
    public void testFromBind() {
        Element dep = getDependencyById(froms, "fs2");
        String from = dep.getAttribute("from");
        assertEquals("Check from", "X", from);
    }
    
    public void testFromUnbind() {
        Element dep = getDependencyById(froms, "inv");
        String from = dep.getAttribute("from");
        assertEquals("Check from", "X", from);
    }
    
    public void testNoFrom() {
        Element dep = getDependencyById(froms, "Bar");
        String from = dep.getAttribute("from");
        assertNull("Check from", from);
    }

    
    private Element getDependencyById(Element[] deps, String name) {
        for (int i = 0; i < deps.length; i++) {
            String na = deps[i].getAttribute("id");
            String field = deps[i].getAttribute("field");
            if (na != null && na.equalsIgnoreCase(name)) {
                return deps[i];
            }
            if (field != null && field.equalsIgnoreCase(name)) {
                return deps[i];
            }
        }
        fail("Dependency  " + name + " not found");
        return null;
    }
    
    

}
