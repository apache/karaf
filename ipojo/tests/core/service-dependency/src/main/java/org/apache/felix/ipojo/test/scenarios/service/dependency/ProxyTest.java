package org.apache.felix.ipojo.test.scenarios.service.dependency;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.handlers.dependency.DependencyHandler;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.service.dependency.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.service.dependency.service.FooService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

public class ProxyTest extends OSGiTestCase {


    public void testDelegation() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        Properties prov = new Properties();
        prov.put("instance.name","FooProvider1-Proxy");
        ComponentInstance fooProvider1 = Utils.getFactoryByName(getContext(), "FooProviderType-1").createComponentInstance(prov);
        
        
        Properties i1 = new Properties();
        i1.put("instance.name","Delegator");
        ComponentInstance instance1 = Utils.getFactoryByName(getContext(), 
                "org.apache.felix.ipojo.test.scenarios.service.dependency.proxy.CheckServiceDelegator").createComponentInstance(i1);
        
        
        ServiceReference ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull(ref);
        CheckService cs = (CheckService) context.getService(ref);
        
        Properties props = cs.getProps();
        FooService helper = (FooService) props.get("helper.fs");
        assertNotNull(helper);
        assertTrue(helper.toString().contains("$$Proxy")); // This is the suffix.
        
        assertTrue(cs.check());
        
        fooProvider1.dispose();
        instance1.dispose();
    }
    
    public void testDelegationOnNullable() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        Properties i1 = new Properties();
        i1.put("instance.name","DelegatorNullable");
        ComponentInstance instance1 = Utils.getFactoryByName(getContext(), 
                "org.apache.felix.ipojo.test.scenarios.service.dependency.proxy.CheckServiceDelegator").createComponentInstance(i1);
        
        
        ServiceReference ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull(ref);
        CheckService cs = (CheckService) context.getService(ref);
        
        Properties props = cs.getProps();
        FooService helper = (FooService) props.get("helper.fs");
        assertNotNull(helper);
        assertTrue(helper.toString().contains("$$Proxy")); // This is the suffix.
        
        assertFalse(cs.check()); // Nullable.
        
        instance1.dispose();
    }
    
    
    public void testGetAndDelegation() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        Properties prov = new Properties();
        prov.put("instance.name","FooProvider1-Proxy");
        ComponentInstance fooProvider1 = Utils.getFactoryByName(getContext(), "FooProviderType-1").createComponentInstance(prov);
        
        
        Properties i1 = new Properties();
        i1.put("instance.name","Delegator");
        ComponentInstance instance1 = Utils.getFactoryByName(getContext(), 
                "org.apache.felix.ipojo.test.scenarios.service.dependency.proxy.CheckServiceGetAndDelegate").createComponentInstance(i1);
        
        
        ServiceReference ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull(ref);
        CheckService cs = (CheckService) context.getService(ref);
        
        Properties props = cs.getProps();
        FooService helper = (FooService) props.get("helper.fs");
        assertNotNull(helper);
        assertTrue(helper.toString().contains("$$Proxy")); // This is the suffix.

        
        assertTrue(cs.check());
        
        fooProvider1.dispose();
        instance1.dispose();
    }
    
    public void testGetAndDelegationOnNullable() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        Properties i1 = new Properties();
        i1.put("instance.name","DelegatorNullable");
        ComponentInstance instance1 = Utils.getFactoryByName(getContext(), 
                "org.apache.felix.ipojo.test.scenarios.service.dependency.proxy.CheckServiceGetAndDelegate").createComponentInstance(i1);
        
        
        ServiceReference ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull(ref);
        CheckService cs = (CheckService) context.getService(ref);
        
        Properties props = cs.getProps();
        FooService helper = (FooService) props.get("helper.fs");
        assertNotNull(helper);
        assertTrue(helper.toString().contains("$$Proxy")); // This is the suffix.

        assertFalse(cs.check()); // Nullable.
        
        
        instance1.dispose();
    }
    
    public void testImmediate() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        Properties prov = new Properties();
        prov.put("instance.name","FooProvider1-Proxy");
        ComponentInstance fooProvider1 = Utils.getFactoryByName(getContext(), "FooProviderType-1").createComponentInstance(prov);
        
        
        Properties i1 = new Properties();
        i1.put("instance.name","Delegator");
        ComponentInstance instance1 = Utils.getFactoryByName(getContext(), 
                "org.apache.felix.ipojo.test.scenarios.service.dependency.proxy.CheckServiceNoDelegate").createComponentInstance(i1);
        
        ServiceReference ref = Utils.getServiceReference(context, CheckService.class.getName(), "(service.pid=Helper)");
        assertNotNull(ref);
        CheckService cs = (CheckService) context.getService(ref);
        
        Properties props = cs.getProps();
        FooService helper = (FooService) props.get("helper.fs");
        assertNotNull(helper);
        assertTrue(helper.toString().contains("$$Proxy")); // This is the suffix.

        assertTrue(cs.check());
        
        fooProvider1.dispose();
        instance1.dispose();
    }
    
    public void testImmediateNoService() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        
        Properties i1 = new Properties();
        i1.put("instance.name","Delegator");
        ComponentInstance instance1 = Utils.getFactoryByName(getContext(), 
                "org.apache.felix.ipojo.test.scenarios.service.dependency.proxy.CheckServiceNoDelegate").createComponentInstance(i1);
        
        ServiceReference ref = Utils.getServiceReference(context, CheckService.class.getName(), "(service.pid=Helper)");
        assertNotNull(ref);
        CheckService cs = (CheckService) context.getService(ref);
        
        try {
            cs.getProps();
            fail("Exception expected");
        } catch(RuntimeException e) {
            //OK
        }
        
        instance1.dispose();
    }
    
    public void testProxyDisabled() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        // Disable proxy
        System.setProperty(DependencyHandler.PROXY_SETTINGS_PROPERTY, DependencyHandler.PROXY_DISABLED);
        Properties prov = new Properties();
        prov.put("instance.name","FooProvider1-Proxy");
        ComponentInstance fooProvider1 = Utils.getFactoryByName(getContext(), "FooProviderType-1").createComponentInstance(prov);
        
        
        Properties i1 = new Properties();
        i1.put("instance.name","Delegator");
        ComponentInstance instance1 = Utils.getFactoryByName(getContext(), 
                "org.apache.felix.ipojo.test.scenarios.service.dependency.proxy.CheckServiceDelegator").createComponentInstance(i1);
        
        
        ServiceReference ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull(ref);
        CheckService cs = (CheckService) context.getService(ref);
        
        Properties props = cs.getProps();
        FooService helper = (FooService) props.get("helper.fs");
        assertNotNull(helper);
        assertFalse(helper.toString().contains("$$Proxy")); // Not a proxy.
        
        assertTrue(cs.check());
        
        fooProvider1.dispose();
        instance1.dispose();
        System.setProperty(DependencyHandler.PROXY_SETTINGS_PROPERTY, DependencyHandler.PROXY_ENABLED);

    }

}
