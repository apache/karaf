package org.apache.felix.ipojo.test.scenarios.configuration;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.configuration.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.configuration.service.FooService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

public class TestPropertyModifier extends OSGiTestCase {
    
    public void testPropertyModifier() {
        ComponentInstance ci = null; 
        Factory factory =  Utils.getFactoryByName(getContext(), "org.apache.felix.ipojo.test.scenarios.component.PropertyModifier");
        Properties props = new Properties();
        props.put("cls", new String[] {FooService.class.getName()});
        try {
            ci = factory.createComponentInstance(props);
        } catch (Exception e) {
            fail(e.getMessage());
        } 
        
        ServiceReference ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), ci.getInstanceName());
        assertNotNull("Check ref", ref);
        
        // Check the service property
        // Not exposed here:
        assertNull("Classes -0", ref.getProperty("classes"));
        
        CheckService check = (CheckService) getContext().getService(ref);
        assertTrue(check.check());
        
        // Property exposed now.
        ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), ci.getInstanceName());
        Class[] str = (Class[]) ref.getProperty("classes");
        assertEquals("Classes size", 1, str.length);
        assertEquals("Classes[0]", FooService.class.getName(), str[0].getName());
        
        Properties p = check.getProps();
        Class[] str2 = (Class[]) p.get("classes");
        assertEquals("Classes size -2", 1, str2.length);
        assertEquals("Classes[0] -2", FooService.class.getName(), str2[0].getName());
        
        Properties props2 = new Properties();
        props2.put("cls", new String[] {FooService.class.getName(), CheckService.class.getName()});
        try {
            ci.reconfigure(props2);
        } catch (Exception e) {
            fail(e.getMessage());
        } 
        
        // Check the service property
        ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), ci.getInstanceName());
        assertNotNull("Check ref", ref);
        str = (Class[]) ref.getProperty("classes");
        assertEquals("Classes size -3", 2, str.length);
        assertEquals("Classes[0] -3", FooService.class.getName(), str[0].getName());
        assertEquals("Classes[1] -3", CheckService.class.getName(), str[1].getName());

        
        check = (CheckService) getContext().getService(ref);
        p = check.getProps();
        str2 = (Class[]) p.get("classes");
        assertEquals("Classes size -4", 2, str2.length);
        assertEquals("Classes[0] -4", FooService.class.getName(), str2[0].getName());
        assertEquals("Classes[1] -4", CheckService.class.getName(), str2[1].getName());
        
        ci.dispose();
        getContext().ungetService(ref);
        
    }

}
