package org.apache.felix.ipojo.test.scenarios.annotations;

import java.util.List;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.apache.felix.ipojo.test.scenarios.annotations.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.annotations.service.FooService;

public class ServiceProdiving extends OSGiTestCase {
    
    private IPOJOHelper helper;
    
    public void setUp() {
        helper = new IPOJOHelper(this);
    }
    
    public void testProvidesSimple() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.ProvidesSimple");
        Element[] provs = meta.getElements("provides");
        assertNotNull("Provides exists ", provs);
    }
    
    public void testProvidesDouble() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.ProvidesDouble");
        Element[] provs = meta.getElements("provides");
        assertNotNull("Provides exists ", provs);
    }
    
    public void testProvidesTriple() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.ProvidesTriple");
        Element[] provs = meta.getElements("provides");
        assertNotNull("Provides exists ", provs);
        Element prov = provs[0];
        String itfs = prov.getAttribute("specifications");
        List list = ParseUtils.parseArraysAsList(itfs);
        assertTrue("Provides CS ", list.contains(CheckService.class.getName()));
    }
    
    public void testProvidesQuatro() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.ProvidesQuatro");
        Element[] provs = meta.getElements("provides");
        assertNotNull("Provides exists ", provs);
        Element prov = provs[0];
        String itfs = prov.getAttribute("specifications");
        List list = ParseUtils.parseArraysAsList(itfs);
        assertTrue("Provides CS ", list.contains(CheckService.class.getName()));
        assertTrue("Provides Foo ", list.contains(FooService.class.getName()));
    }
    
    public void testProperties() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.ProvidesProperties");
        Element[] provs = meta.getElements("provides");
        assertNotNull("Provides exists ", provs);
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
        assertEquals("Check mandatory value", "true", bar.getAttribute("mandatory"));
        //Boo
        Element boo = getPropertyByName(props, "boo");
        assertEquals("Check boo field", "boo", boo.getAttribute("field"));
        //Baa
        Element baa = getPropertyByName(props, "baa");
        assertEquals("Check baa field", "m_baa", baa.getAttribute("field"));
        assertEquals("Check baa name", "baa", baa.getAttribute("name"));
        
        //Bar
        Element baz = getPropertyByName(props, "baz");
        assertEquals("Check baz field", "m_baz", baz.getAttribute("field"));
        assertEquals("Check baz name", "baz", baz.getAttribute("name"));        
        
        
    }
    
    public void testServiceController() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.PSServiceController");
        Element[] provs = meta.getElements("provides");
        assertNotNull("Provides exists ", provs);
        System.out.println(provs[0].toString());
        assertNotNull(provs[0].getElements("controller"));
        assertEquals(1, provs[0].getElements("controller").length);
        assertEquals("false", provs[0].getElements("controller")[0].getAttribute("value"));
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
