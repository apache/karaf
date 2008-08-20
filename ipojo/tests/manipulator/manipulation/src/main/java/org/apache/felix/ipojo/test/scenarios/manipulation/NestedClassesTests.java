package org.apache.felix.ipojo.test.scenarios.manipulation;

import java.util.Map;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.manipulation.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

public class NestedClassesTests extends OSGiTestCase {
    
    private ComponentInstance instance;
    private CheckService service; 
    
    public void setUp() {
        Factory factory = Utils.getFactoryByName(context, "inners");
        Properties map = new Properties();
        map.put("publicObject", "publicObject");
        map.put("publicInt", new Integer(0));
        map.put("packageObject", "packageObject");
        map.put("packageInt", new Integer(1));
        map.put("protectedObject", "protectedObject");
        map.put("protectedInt", new Integer(2));
        map.put("privateObject", "privateObject");
        map.put("privateInt", new Integer(3));
        map.put("nonObject", "nonObject");
        map.put("nonInt", new Integer(4));
        try {
            instance = factory.createComponentInstance(map);
        } catch (Exception e) {
           fail(e.getMessage());
        }
        
        ServiceReference ref =Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance.getInstanceName());
        assertNotNull("Check service availability", ref);
        service = (CheckService) context.getService(ref);
    }
    
    public void tearDown() {
        instance.dispose();
        instance = null;
        service = null;
    }
    
    public void testPrivateInnerClass() {
        Map data = (Map) service.getProps().get("privateInner");
        assertNotNull("Check data existency", data);
        
        assertEquals("Check public object", "publicObject", data.get("publicObject"));
        assertEquals("Check public int", new Integer(0), data.get("publicInt"));
        assertEquals("Check protected object", "protectedObject", data.get("protectedObject"));
        assertEquals("Check protected int", new Integer(2), data.get("protectedInt"));
        assertEquals("Check package object", "packageObject", data.get("packageObject"));
        assertEquals("Check package int", new Integer(1), data.get("packageInt"));
        assertEquals("Check private object", "privateObject", data.get("privateObject"));
        assertEquals("Check private int", new Integer(3), data.get("privateInt"));
        assertEquals("Check non-managed object", "not-managed", data.get("nonObject"));
        assertEquals("Check non-managed int", new Integer(5), data.get("nonInt"));
    
    }
    
    public void testProtectedInnerClass() {
        Map data = (Map) service.getProps().get("protectedInner");
        assertNotNull("Check data existency", data);
        
        assertEquals("Check public object", "publicObject", data.get("publicObject"));
        assertEquals("Check public int", new Integer(0), data.get("publicInt"));
        assertEquals("Check protected object", "protectedObject", data.get("protectedObject"));
        assertEquals("Check protected int", new Integer(2), data.get("protectedInt"));
        assertEquals("Check package object", "packageObject", data.get("packageObject"));
        assertEquals("Check package int", new Integer(1), data.get("packageInt"));
        assertEquals("Check private object", "privateObject", data.get("privateObject"));
        assertEquals("Check private int", new Integer(3), data.get("privateInt"));
        assertEquals("Check non-managed object", "not-managed", data.get("nonObject"));
        assertEquals("Check non-managed int", new Integer(5), data.get("nonInt"));
    
    }
    
    public void testPackageInnerClass() {
        Map data = (Map) service.getProps().get("packageInner");
        assertNotNull("Check data existency", data);
        
        assertEquals("Check public object", "publicObject", data.get("publicObject"));
        assertEquals("Check public int", new Integer(0), data.get("publicInt"));
        assertEquals("Check protected object", "protectedObject", data.get("protectedObject"));
        assertEquals("Check protected int", new Integer(2), data.get("protectedInt"));
        assertEquals("Check package object", "packageObject", data.get("packageObject"));
        assertEquals("Check package int", new Integer(1), data.get("packageInt"));
        assertEquals("Check private object", "privateObject", data.get("privateObject"));
        assertEquals("Check private int", new Integer(3), data.get("privateInt"));
        assertEquals("Check non-managed object", "not-managed", data.get("nonObject"));
        assertEquals("Check non-managed int", new Integer(5), data.get("nonInt"));
    
    }
    
    public void testPublicInnerClass() {
        Map data = (Map) service.getProps().get("publicInner");
        assertNotNull("Check data existency", data);
        
        assertEquals("Check public object", "publicObject", data.get("publicObject"));
        assertEquals("Check public int", new Integer(0), data.get("publicInt"));
        assertEquals("Check protected object", "protectedObject", data.get("protectedObject"));
        assertEquals("Check protected int", new Integer(2), data.get("protectedInt"));
        assertEquals("Check package object", "packageObject", data.get("packageObject"));
        assertEquals("Check package int", new Integer(1), data.get("packageInt"));
        assertEquals("Check private object", "privateObject", data.get("privateObject"));
        assertEquals("Check private int", new Integer(3), data.get("privateInt"));
        assertEquals("Check non-managed object", "not-managed", data.get("nonObject"));
        assertEquals("Check non-managed int", new Integer(5), data.get("nonInt"));
    
    }
    
    public void testAnonymousInnerClass() {
        Map data = (Map) service.getProps().get("anonymous");
        assertNotNull("Check data existency", data);
        
        assertEquals("Check public object", "publicObject", data.get("publicObject"));
        assertEquals("Check public int", new Integer(0), data.get("publicInt"));
        assertEquals("Check protected object", "protectedObject", data.get("protectedObject"));
        assertEquals("Check protected int", new Integer(2), data.get("protectedInt"));
        assertEquals("Check package object", "packageObject", data.get("packageObject"));
        assertEquals("Check package int", new Integer(1), data.get("packageInt"));
        assertEquals("Check private object", "privateObject", data.get("privateObject"));
        assertEquals("Check private int", new Integer(3), data.get("privateInt"));
        assertEquals("Check non-managed object", "not-managed", data.get("nonObject"));
        assertEquals("Check non-managed int", new Integer(5), data.get("nonInt"));
    
    }
    
    

}
