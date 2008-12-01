package org.apache.felix.ipojo.test.composite.instance;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.composite.CompositeFactory;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.composite.service.CheckService;
import org.apache.felix.ipojo.test.composite.util.Utils;
import org.apache.felix.ipojo.test.instance.service.Service;
import org.osgi.framework.ServiceReference;

public class InstanceScopeTest extends OSGiTestCase {
    
    CompositeFactory factory;
    ComponentInstance instance;
    
    public void setUp() {
        factory = (CompositeFactory) Utils.getFactoryByName(getContext(), "SCOPE-scope");
        assertNotNull("Factory", factory);
        try {
            instance = factory.createComponentInstance(null);
        } catch (Exception e) {
            fail("Fail instantiation : " + e.getMessage());
        }
       

    }
    
    public void tearDown() {
        instance.dispose();
        instance = null;
    }
    
    public void testScope() {
        ServiceReference ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), instance.getInstanceName());
        assertNotNull("Check architecture availability", ref);
        Architecture arch = (Architecture) getContext().getService(ref);
        assertTrue("Validity", arch.getInstanceDescription().getState() == ComponentInstance.VALID);
        
        // Get internal service
        ServiceContext sc = Utils.getServiceContext(instance);
        ServiceReference ref2 = Utils.getServiceReference(sc, CheckService.class.getName(), null);
        assertNotNull("Check CheckService availability", ref2);
        CheckService svc = (CheckService) sc.getService(ref2);
        Properties props = svc.getProps();
        assertEquals("Check props - 1", 1, ((Integer) props.get("1")).intValue());
        assertEquals("Check props - 2", 2, ((Integer) props.get("2")).intValue());
        assertEquals("Check props - 3", 3, ((Integer) props.get("3")).intValue());
        
    }
    
    public void testGlobalUnavailability() {
        ServiceReference ref2 = Utils.getServiceReference(getContext(), Service.class.getName(), null);
        assertNull("Check Service unavailability", ref2);
    }
    
    public void testScopeUnvailability() {
        CompositeFactory factory2 = (CompositeFactory) Utils.getFactoryByName(getContext(), "SCOPE-badscope");
        assertNotNull("Factory", factory2);
        ComponentInstance instance2 = null;
        try {
            instance2 = factory2.createComponentInstance(null);
        } catch (Exception e) {
            fail("Fail instantiation : " + e.getMessage());
        }
        //System.out.println(instance2.getInstanceDescription().getDescription());
        
        assertEquals("Check invalidity", ComponentInstance.INVALID, instance2.getState());
 
    }
    
    

}
