package org.apache.felix.ipojo.test.scenarios.annotations;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.metadata.Element;

public class Properties extends OSGiTestCase {
    
    private IPOJOHelper helper;
    
    public void setUp() {
        helper = new IPOJOHelper(this);
    }
    
    public void testProperties() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.Properties");
        Element[] provs = meta.getElements("properties");
        assertNotNull("Properties exists ", provs);
        Element prov = provs[0];
        Element[] props = prov.getElements("property");
        assertEquals("Number of properties", props.length, 5);
        //Foo
        Element foo = getPropertyByName(props, "foo");
        assertEquals("Check foo field", "m_foo", foo.getAttribute("field"));
        assertEquals("Check foo name", "foo", foo.getAttribute("name"));
        //Bar
        Element bar = getPropertyByName(props, "bar");
        assertEquals("Check bar field", "bar", bar.getAttribute("field"));
        assertEquals("Check bar value", "4", bar.getAttribute("value"));
        //Boo
        Element boo = getPropertyByName(props, "boo");
        assertEquals("Check boo field", "boo", boo.getAttribute("field"));
        assertEquals("Check boo method", "setboo", boo.getAttribute("method"));
        //Baa
        Element baa = getPropertyByName(props, "baa");
        assertEquals("Check baa field", "m_baa", baa.getAttribute("field"));
        assertEquals("Check baa name", "baa", baa.getAttribute("name"));
        assertEquals("Check baa method", "setbaa", baa.getAttribute("method"));
        assertEquals("Check mandatory", "true", baa.getAttribute("mandatory"));

        
        //Bar
        Element baz = getPropertyByName(props, "baz");
        assertEquals("Check baz method", "setbaz", baz.getAttribute("method"));
        assertEquals("Check baz name", "baz", baz.getAttribute("name"));
    }
    
    public void testAbsentPropagation() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.Properties");
        Element[] provs = meta.getElements("properties");
        assertNotNull("Properties exists ", provs);
        Element prov = provs[0];
        String att = prov.getAttribute("propagation");
        assertNull("Propagation", att);
    }
    
    public void testPropagation() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.Propagation");
        Element[] provs = meta.getElements("properties");
        assertNotNull("Properties exists ", provs);
        Element prov = provs[0];
        String att = prov.getAttribute("propagation");
        assertNotNull("Propagation", att);
        assertEquals("Propagation value", "true", att);
    }
    
    public void testNoPropagation() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.NoPropagation");
        Element[] provs = meta.getElements("properties");
        assertNotNull("Properties exists ", provs);
        Element prov = provs[0];
        String att = prov.getAttribute("propagation");
        assertNotNull("Propagation", att);
        assertEquals("Propagation value", "false", att);
    }
    
    public void testPID() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.ManagedServicePID");
        Element[] provs = meta.getElements("properties");
        assertNotNull("Properties exists ", provs);
        Element prov = provs[0];
        String att = prov.getAttribute("pid");
        assertNotNull("PID", att);
        assertEquals("PID Value", "MyPID", att);
    }
    
    public void testAbsentPID() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.Properties");
        Element[] provs = meta.getElements("properties");
        assertNotNull("Properties exists ", provs);
        Element prov = provs[0];
        String att = prov.getAttribute("pid");
        assertNull("PID", att);
    }
    
    public void testPropagationAndPID() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.PropagationandPID");
        Element[] provs = meta.getElements("properties");
        assertNotNull("Properties exists ", provs);
        Element prov = provs[0];
        String att = prov.getAttribute("pid");
        assertNotNull("PID", att);
        assertEquals("PID Value", "MyPID", att);
        att = prov.getAttribute("propagation");
        assertNotNull("Propagation", att);
        assertEquals("Propagation value", "true", att);
    }
    
    public void testPIDAndPropagation() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.PIDandPropagation");
        Element[] provs = meta.getElements("properties");
        assertNotNull("Properties exists ", provs);
        Element prov = provs[0];
        String att = prov.getAttribute("pid");
        assertNotNull("PID", att);
        assertEquals("PID Value", "MyPID", att);
        att = prov.getAttribute("propagation");
        assertNotNull("Propagation", att);
        assertEquals("Propagation value", "true", att);
    }
    
    public void testUpdatedAndPID() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.UpdatedWithManagedService");
        Element[] provs = meta.getElements("properties");
        assertNotNull("Properties exists ", provs);
        Element prov = provs[0];
        String att = prov.getAttribute("pid");
        assertNotNull("PID", att);
        assertEquals("PID Value", "MyPID", att);
        
        att = prov.getAttribute("updated");
        assertNotNull("att", att);
        assertEquals("Updated Value", "after", att);
    }
    
    public void testUpdatedAndProperties() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.UpdatedWithProperties");
        Element[] provs = meta.getElements("properties");
        assertNotNull("Properties exists ", provs);
        Element prov = provs[0];
        String att = prov.getAttribute("pid");
        assertNull("PID", att);
        
        att = prov.getAttribute("updated");
        assertNotNull("att", att);
        assertEquals("Updated Value", "after", att);
    }
    
    private Element getPropertyByName(Element[] props, String name) {
        for (int i = 0; i < props.length; i++) {
            String na = props[i].getAttribute("name");
            String field = props[i].getAttribute("field");
            if (na != null && na.equalsIgnoreCase(name)) {
                return props[i];
            }
            if (field != null && field.equalsIgnoreCase(name)) {
                return props[i];
            }
        }
        fail("Property  " + name + " not found");
        return null;
    }
    
    

}
