package org.apache.felix.ipojo.test.scenarios.annotations;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.metadata.Element;

public class Dependency extends OSGiTestCase {
    
    private IPOJOHelper helper;
    
    public void setUp() {
        helper = new IPOJOHelper(this);
    }
    
    public void testDependencyDeclaration() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.Dependency");
        Element[] deps = meta.getElements("requires");
        
        // Check fs
        Element dep = getDependencyById(deps, "fs");
        String field = dep.getAttribute("field");
        String id = dep.getAttribute("id");
        String bind = getBind(dep);
        String unbind = getUnbind(dep);
        assertNotNull("Check fs field", field);
        assertEquals("Check fs field", "fs", field);
        assertNull("Check fs bind", bind);
        assertNull("Check fs unbind", unbind);
        assertNull("Check fs id", id);
        
        // Check bar
        dep = getDependencyById(deps, "Bar");
        field = dep.getAttribute("field");
        id = dep.getAttribute("id");
        bind = getBind(dep);
        unbind = getUnbind(dep);
        assertNull("Check bar field", field);
        assertEquals("Check bar bind", "bindBar", bind);
        assertEquals("Check bar unbind", "unbindBar", unbind);
        assertEquals("Check bar id", "Bar", id);
        
        // Check baz
        dep = getDependencyById(deps, "Baz");
        field = dep.getAttribute("field");
        id = dep.getAttribute("id");
        bind = getBind(dep);
        unbind = getUnbind(dep);
        assertNull("Check baz field", field);
        assertEquals("Check baz bind", "bindBaz", bind);
        assertEquals("Check baz unbind", "unbindBaz", unbind);
        assertEquals("Check baz id", "Baz", id);
        
        // Check fs2
        dep = getDependencyById(deps, "fs2");
        field = dep.getAttribute("field");
        id = dep.getAttribute("id");
        bind = getBind(dep);
        unbind = getUnbind(dep);
        assertNotNull("Check fs2 field", field);
        assertEquals("Check fs2 field", "fs2", field);
        assertEquals("Check fs2 bind", "bindFS2", bind);
        assertEquals("Check fs2 unbind", "unbindFS2", unbind);
        
        // Check fs2inv
        dep = getDependencyById(deps, "fs2inv");
        field = dep.getAttribute("field");
        id = dep.getAttribute("id");
        bind = getBind(dep);
        unbind = getUnbind(dep);
        assertNotNull("Check fs2inv field", field);
        assertEquals("Check fs2 field", "fs2inv", field);
        assertEquals("Check fs2 bind", "bindFS2Inv", bind);
        assertEquals("Check fs2 unbind", "unbindFS2Inv", unbind);
        assertEquals("Check fs2 id", "inv", id);
        
        // Check mod
        dep = getDependencyById(deps, "mod");
        id = dep.getAttribute("id");
        bind = getBind(dep);
        unbind = getUnbind(dep);
        String mod = getModified(dep);
        assertEquals("Check mod bind", "bindMod", bind);
        assertEquals("Check mod unbind", "unbindMod", unbind);
        assertEquals("Check mod modified", "modifiedMod", mod);
        assertEquals("Check mod id", "mod", id);
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
    
    private String getBind(Element dep) {
        Element[] elem = dep.getElements("callback");
        for (int i = 0; elem != null && i < elem.length; i++) {
            if (elem[i].getAttribute("type").equalsIgnoreCase("bind")) {
                return elem[i].getAttribute("method");
            }
        }
        return null;
    }
    
    private String getUnbind(Element dep) {
        Element[] elem = dep.getElements("callback");
        for (int i = 0; elem != null && i < elem.length; i++) {
            if (elem[i].getAttribute("type").equalsIgnoreCase("unbind")) {
                return elem[i].getAttribute("method");
            }
        }
        return null;
    }
    
    private String getModified(Element dep) {
        Element[] elem = dep.getElements("callback");
        for (int i = 0; elem != null && i < elem.length; i++) {
            if (elem[i].getAttribute("type").equalsIgnoreCase("modified")) {
                return elem[i].getAttribute("method");
            }
        }
        return null;
    }

}
