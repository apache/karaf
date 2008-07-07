package org.apache.felix.ipojo.test;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

public class WbpTests extends OSGiTestCase {
    
    Factory provFactory;
    Factory factory, factory2;

    public void setUp() {
        provFactory = Utils.getFactoryByName(context, "fooprovider");
        factory = Utils.getFactoryByName(context, "under-providers");
        factory2 = Utils.getFactoryByName(context, "under-properties");
    }
    
    public void tearDown() {
        
    }
    
    public void testServiceProviders() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        ComponentInstance ci = factory.createComponentInstance(new Properties());
        
        ServiceReference ref = Utils.getServiceReferenceByName(context, Observable.class.getName(), ci.getInstanceName());
        assertNotNull("Check Observable availability", ref);
        Observable obs = (Observable) context.getService(ref);
        
        Map map = obs.getObservations();
        assertEquals("Check empty list" , ((List) map.get("list")).size(), 0);
        
        Properties p1 = new Properties();
        p1.put("foo", "foo");
        ComponentInstance prov1 = provFactory.createComponentInstance(p1);
        
        map = obs.getObservations();
        assertEquals("Check list #1" , ((List) map.get("list")).size(), 1);
       
        Properties p2 = new Properties();
        p2.put("foo", "foo");
        ComponentInstance prov2 = provFactory.createComponentInstance(p2);
        
        map = obs.getObservations();
        assertEquals("Check list #2" , ((List) map.get("list")).size(), 2);
        
        prov1.stop();
        
        map = obs.getObservations();
        assertEquals("(1) Check list #1" , ((List) map.get("list")).size(), 1);
        
        prov2.stop();
        
        map = obs.getObservations();
        assertEquals("(2) Check list #0" , ((List) map.get("list")).size(), 0);
        
        prov2.start();
        
        map = obs.getObservations();
        assertEquals("(3) Check list #1" , ((List) map.get("list")).size(), 1);
        
        prov1.start();
        
        map = obs.getObservations();
        assertEquals("(4) Check list #2" , ((List) map.get("list")).size(), 2);
        
        prov1.dispose();
        
        map = obs.getObservations();
        assertEquals("(5) Check list #1" , ((List) map.get("list")).size(), 1);
        
        prov2.dispose();
        
        map = obs.getObservations();
        assertEquals("(6) Check list #0" , ((List) map.get("list")).size(), 0);
        
        context.ungetService(ref);
        ci.dispose();
    }
    
    public void testPropertiesProviders() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        ComponentInstance ci = factory2.createComponentInstance(new Properties());
        
        ServiceReference ref = Utils.getServiceReferenceByName(context, Observable.class.getName(), ci.getInstanceName());
        assertNotNull("Check Observable availability", ref);
        Observable obs = (Observable) context.getService(ref);
        
        Map map = obs.getObservations();
        assertEquals("Check empty list" , ((List) map.get("list")).size(), 0);
        
        Properties p1 = new Properties();
        p1.put("foo", "foo");
        ComponentInstance prov1 = provFactory.createComponentInstance(p1);
        ServiceReference ref1 = Utils.getServiceReferenceByName(context, FooService.class.getName(), prov1.getInstanceName());
        FooService fs1 = (FooService) context.getService(ref1);
        
        map = obs.getObservations();
        assertEquals("Check list #1" , ((List) map.get("list")).size(), 1);
       
        Properties p2 = new Properties();
        p2.put("foo", "foo");
        ComponentInstance prov2 = provFactory.createComponentInstance(p2);
        ServiceReference ref2 = Utils.getServiceReferenceByName(context, FooService.class.getName(), prov2.getInstanceName());
        FooService fs2 = (FooService) context.getService(ref2);
        
        map = obs.getObservations();
        assertEquals("Check list #2" , ((List) map.get("list")).size(), 2);
        
        fs1.foo();
        
        map = obs.getObservations();
        assertEquals("(1) Check list #1" , ((List) map.get("list")).size(), 1);
        
        fs2.foo();
        
        map = obs.getObservations();
        assertEquals("(2) Check list #0" , ((List) map.get("list")).size(), 0);
        
        fs2.foo();
        
        map = obs.getObservations();
        assertEquals("(3) Check list #1" , ((List) map.get("list")).size(), 1);
        
        fs1.foo();
        
        map = obs.getObservations();
        assertEquals("(4) Check list #2" , ((List) map.get("list")).size(), 2);
        
        prov1.dispose();
        
        map = obs.getObservations();
        assertEquals("(5) Check list #1" , ((List) map.get("list")).size(), 1);
        
        prov2.dispose();
        
        map = obs.getObservations();
        assertEquals("(6) Check list #0" , ((List) map.get("list")).size(), 0);
        
        context.ungetService(ref1);
        context.ungetService(ref2);
        context.ungetService(ref);
        ci.dispose();
    }
    
    public void testModifications() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        ComponentInstance ci = factory.createComponentInstance(new Properties());
        
        ServiceReference ref = Utils.getServiceReferenceByName(context, Observable.class.getName(), ci.getInstanceName());
        assertNotNull("Check Observable availability", ref);
        Observable obs = (Observable) context.getService(ref);
        
        Map map = obs.getObservations();
        assertEquals("Check empty list" , ((List) map.get("list")).size(), 0);
        
        Properties p1 = new Properties();
        p1.put("foo", "foo");
        ComponentInstance prov1 = provFactory.createComponentInstance(p1);
        
        map = obs.getObservations();
        assertEquals("Check list #1" , ((List) map.get("list")).size(), 1);
        assertEquals("Check modification #0" , ((Integer) map.get("modifications")).intValue(), 0);
        
        ServiceReference ref2 = Utils.getServiceReference(context, FooService.class.getName(), null);
        assertNotNull("Check FooService availability", ref2);
        
        FooService fs = (FooService) context.getService(ref2);
        fs.foo();
        
        map = obs.getObservations();
        assertEquals("Check list #1" , ((List) map.get("list")).size(), 1);
        assertEquals("Check modification #1 (" + map.get("modifications")+")" , ((Integer) map.get("modifications")).intValue(), 1);
        
        fs.foo();
        
        map = obs.getObservations();
        assertEquals("Check list #1" , ((List) map.get("list")).size(), 1);
        assertEquals("Check modification #2" , ((Integer) map.get("modifications")).intValue(), 2);
        
        prov1.dispose();
        map = obs.getObservations();
        assertEquals("Check list #0" , ((List) map.get("list")).size(), 0);
        assertEquals("Check modification #2" , ((Integer) map.get("modifications")).intValue(), 2);
        
        context.ungetService(ref);
        context.ungetService(ref2);
        ci.dispose();
    }
}
