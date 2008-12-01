package org.apache.felix.ipojo.test.scenarios.annotations;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.metadata.Element;

public class PolicyDependency extends OSGiTestCase {
    
    private Element[] deps ;
    private IPOJOHelper helper;
    
    public void setUp() {
        helper = new IPOJOHelper(this);
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.PolicyDependency");
        deps = meta.getElements("requires");
    }
    
    public void testField() {
        Element dep = getDependencyById(deps, "fs");
        String opt = dep.getAttribute("policy");
        assertEquals("Check policy", "static", opt);
    }
    
    public void testFieldDPpolicy() {
        Element dep = getDependencyById(deps, "fs2");
        String opt = dep.getAttribute("policy");
        assertEquals("Check policy", "dynamic-priority", opt);
    }
    
    public void testCallbackBind() {
        Element dep = getDependencyById(deps, "Bar");
        String opt = dep.getAttribute("policy");
        assertEquals("Check policy", "static", opt);
    }
    
    public void testCallbackUnbind() {
        Element dep = getDependencyById(deps, "Baz");
        String opt = dep.getAttribute("policy");
        assertEquals("Check policy", "static", opt);
    }
    
    public void testBoth() {
        Element dep = getDependencyById(deps, "inv");
        String opt = dep.getAttribute("policy");
        assertEquals("Check policy", "static", opt);
    }
    
    public void testBindOnly() {
        Element dep = getDependencyById(deps, "bindonly");
        String opt = dep.getAttribute("policy");
        assertEquals("Check policy", "static", opt);
    }
    
    public void testUnbindOnly() {
        Element dep = getDependencyById(deps, "unbindonly");
        String opt = dep.getAttribute("policy");
        assertEquals("Check policy", "static", opt);
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
