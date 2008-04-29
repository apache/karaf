package org.apache.felix.ipojo.test.scenarios.annotations;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.test.scenarios.component.ProvidesSimple;
import org.apache.felix.ipojo.test.scenarios.util.Utils;

public class OptionalDependency extends OSGiTestCase {
    
    private Element[] deps ;
    
    public void setUp() {
        Element meta = Utils.getMetatadata(context, "org.apache.felix.ipojo.test.scenarios.component.OptionalDependency");
        deps = meta.getElements("requires");
    }
    
    public void testField() {
        Element dep = getDependencyById(deps, "fs");
        String opt = dep.getAttribute("optional");
        assertEquals("Check optionality", "true", opt);
    }
    
    public void testFieldNoOptional() {
        Element dep = getDependencyById(deps, "fs2");
        String opt = dep.getAttribute("optional");
        assertEquals("Check optionality", "false", opt);
    }
    
    public void testCallbackBind() {
        Element dep = getDependencyById(deps, "Bar");
        String opt = dep.getAttribute("optional");
        assertEquals("Check optionality", "true", opt);
    }
    
    public void testCallbackUnbind() {
        Element dep = getDependencyById(deps, "Baz");
        String opt = dep.getAttribute("optional");
        assertEquals("Check optionality", "true", opt);
    }
    
    public void testBoth() {
        Element dep = getDependencyById(deps, "inv");
        String opt = dep.getAttribute("optional");
        assertEquals("Check optionality", "true", opt);
    }
    
    public void testBindOnly() {
        Element dep = getDependencyById(deps, "bindonly");
        String opt = dep.getAttribute("optional");
        assertEquals("Check optionality", "true", opt);
    }
    
    public void testUnbindOnly() {
        Element dep = getDependencyById(deps, "unbindonly");
        String opt = dep.getAttribute("optional");
        assertEquals("Check optionality", "true", opt);
    }
    
    public void testNullable() {
        Element meta = Utils.getMetatadata(context, "org.apache.felix.ipojo.test.scenarios.component.NullableDependency");
        Element[] deps = meta.getElements("requires");
        Element fs = getDependencyById(deps, "fs");
        String nullable = fs.getAttribute("nullable");
        assertNotNull("Check nullable", nullable);
        assertEquals("Check nullable value", "true", nullable);
    }
    
    public void testNoNullable() {
        Element meta = Utils.getMetatadata(context, "org.apache.felix.ipojo.test.scenarios.component.NullableDependency");
        Element[] deps = meta.getElements("requires");
        Element fs = getDependencyById(deps, "fs2");
        String nullable = fs.getAttribute("nullable");
        assertNotNull("Check nullable", nullable);
        assertEquals("Check nullable value", "false", nullable);
    }
    
    public void testDefaultImplmentation() {
        Element meta = Utils.getMetatadata(context, "org.apache.felix.ipojo.test.scenarios.component.DefaultImplementationDependency");
        Element[] deps = meta.getElements("requires");
        Element fs = getDependencyById(deps, "fs");
        String di = fs.getAttribute("default-implementation");
        assertNotNull("Check DI", di);
        assertEquals("Check DI value", ProvidesSimple.class.getName(), di);
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

}
