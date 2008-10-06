package org.apache.felix.ipojo.test.scenarios.configadmin;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.configadmin.service.FooService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ManagedServiceTestForService extends OSGiTestCase {
    
    private String factNameSvc = "CA-ConfigurableProvider";
    private String msp = "foo";

    private ComponentFactory factSvc;
    
    private ConfigurationAdmin admin;
    
    ConfigurationMonitor listener;
    
    
    public void setUp() {
        factSvc = (ComponentFactory) Utils.getFactoryByName(context, factNameSvc);
        admin = (ConfigurationAdmin) Utils.getServiceObject(context, ConfigurationAdmin.class.getName(), null);
        assertNotNull("Check configuration admin availability", admin);
        cleanConfigurationAdmin();
        listener = new ConfigurationMonitor(context);
    }
    
    public void tearDown() {
        listener.stop();
        cleanConfigurationAdmin();
        admin = null;
    }
    
    private void cleanConfigurationAdmin() {
        try {
            Configuration[] configurations = admin.listConfigurations("(service.pid=" + msp + ")");
            for (int i = 0; configurations != null && i < configurations.length; i++) {
                configurations[i].delete();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidSyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void testFactoryCreationAndReconfiguration() {
        Properties props = new Properties();
        props.put("managed.service.pid", msp);
        props.put("message", "message");
        ComponentInstance instance  = null;
        try {
            instance =  factSvc.createComponentInstance(props);
        } catch (Exception e) {
           fail(e.getMessage());
        }
        
        ServiceReference ref = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance.getInstanceName());
        assertEquals("Check no object", 0, instance.getInstanceDescription().getCreatedObjects().length);
        assertNotNull("FS availability", ref);
        
        FooService fs = (FooService) context.getService(ref);
        Properties p = fs.fooProps();
        String mes = p.getProperty("message");
        int count = ((Integer) p.get("count")).intValue();
        assertEquals("Check 1 object", 1, instance.getInstanceDescription().getCreatedObjects().length);
        assertEquals("Check message", "message", mes);
        assertEquals("Check count", 1, count);
        
        //Update
        Configuration configuration;
        try {
            configuration = admin.getConfiguration(msp);
            Dictionary prc = configuration.getProperties();
            if (prc == null) {
                prc = new Properties();
            }
            prc.put("message", "message2");
            configuration.update(prc);
            Thread.sleep(ConfigurationTestSuite.UPDATE_WAIT_TIME);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        ref = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance.getInstanceName());
        assertEquals("Check 1 object", 1, instance.getInstanceDescription().getCreatedObjects().length);
        assertNotNull("FS availability", ref);
        
        fs = (FooService) context.getService(ref);
        p = fs.fooProps();
        mes = p.getProperty("message");
        count = ((Integer) p.get("count")).intValue();
        assertEquals("Check 1 object", 1, instance.getInstanceDescription().getCreatedObjects().length);
        assertEquals("Check message", "message2", mes);
        assertEquals("Check count", 2, count);
        
        instance.dispose();
        
    }
    
    public void testMSFCreationAndReconfiguration() {
        Configuration conf = null;
        try {
            conf = admin.createFactoryConfiguration(factNameSvc);
            Dictionary props = conf.getProperties();
            if (props == null) {
                props = new Properties();
            }
            props.put("managed.service.pid", msp);
            props.put("message", "message");
            conf.update(props);
            Thread.sleep(ConfigurationTestSuite.UPDATE_WAIT_TIME); // Wait for the creation.
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        
        Architecture arch = (Architecture) Utils.getServiceObject(context, org.apache.felix.ipojo.architecture.Architecture.class.getName(), "(architecture.instance=" + conf.getPid() + ")");
        
        ServiceReference ref = Utils.getServiceReferenceByName(context, FooService.class.getName(), conf.getPid());
        assertEquals("Check no object", 0, arch.getInstanceDescription().getCreatedObjects().length);
        assertNotNull("FS availability", ref);
        
        arch = (Architecture) Utils.getServiceObject(context, org.apache.felix.ipojo.architecture.Architecture.class.getName(), "(architecture.instance=" + conf.getPid() + ")");
        FooService fs = (FooService) context.getService(ref);
        Properties p = fs.fooProps();
        String mes = p.getProperty("message");
        int count = ((Integer) p.get("count")).intValue();
        assertEquals("Check 1 object", 1, arch.getInstanceDescription().getCreatedObjects().length);
        assertEquals("Check message", "message", mes);
        assertEquals("Check count", 1, count);
        
        //Update
        Configuration configuration;
        try {
            configuration = admin.getConfiguration(msp);
            Dictionary prc = configuration.getProperties();
            if (prc == null) {
                prc = new Properties();
            }
            prc.put("message", "message2");
            configuration.update(prc);
            Thread.sleep(ConfigurationTestSuite.UPDATE_WAIT_TIME);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        arch = (Architecture) Utils.getServiceObject(context, org.apache.felix.ipojo.architecture.Architecture.class.getName(), "(architecture.instance=" + conf.getPid() + ")");
        ref = Utils.getServiceReferenceByName(context, FooService.class.getName(), conf.getPid());
        assertEquals("Check 1 object", 1, arch.getInstanceDescription().getCreatedObjects().length);
        assertNotNull("FS availability", ref);

        arch = (Architecture) Utils.getServiceObject(context, org.apache.felix.ipojo.architecture.Architecture.class.getName(), "(architecture.instance=" + conf.getPid() + ")");
        fs = (FooService) context.getService(ref);
        p = fs.fooProps();
        mes = p.getProperty("message");
        count = ((Integer) p.get("count")).intValue();
        assertEquals("Check 1 object", 1, arch.getInstanceDescription().getCreatedObjects().length);
        assertEquals("Check message", "message2", mes);
        assertEquals("Check count", 2, count);
        
        try {
            conf.delete();
        } catch (IOException e) {
            fail(e.getMessage());
        }
        
    }
    
    public void testCreationAndReconfiguration2() {
        // The configuration exists before the instance creation.
        
      //Update
        Configuration configuration;
        try {
            configuration = admin.getConfiguration(msp);
            Dictionary prc = configuration.getProperties();
            if (prc == null) {
                prc = new Properties();
            }
            prc.put("message", "message2");
            configuration.update(prc);
            listener.waitForEvent(msp, "1");
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        Properties props = new Properties();
        props.put("managed.service.pid", msp);
        props.put("message", "message");
        ComponentInstance instance  = null;
        try {
            instance =  factSvc.createComponentInstance(props);
            Thread.sleep(ConfigurationTestSuite.UPDATE_WAIT_TIME);
        } catch (Exception e) {
           fail(e.getMessage());
        }
        
        ServiceReference ref = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance.getInstanceName());
        assertEquals("Check no object", 0, instance.getInstanceDescription().getCreatedObjects().length);
        assertNotNull("FS availability", ref);
        
        FooService fs = (FooService) context.getService(ref);
        Properties p = fs.fooProps();
        String mes = p.getProperty("message");
        int count = ((Integer) p.get("count")).intValue();
        assertEquals("Check 1 object", 1, instance.getInstanceDescription().getCreatedObjects().length);
        assertEquals("Check message", "message2", mes); // Already reconfigured.
        assertEquals("Check count", 1, count);
        
        instance.dispose();
        
        //Reconfiguration
        try {
            configuration = admin.getConfiguration(msp);
            Dictionary prc = configuration.getProperties();
            if (prc == null) {
                prc = new Properties();
            }
            prc.put("message", "message3");
            configuration.update(prc);
            listener.waitForEvent(msp, "2");
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        // Recreation of the instance.
        props = new Properties();
        props.put("managed.service.pid", msp);
        props.put("message", "message");
        instance  = null;
        try {
            instance =  factSvc.createComponentInstance(props);
            Thread.sleep(ConfigurationTestSuite.UPDATE_WAIT_TIME);
        } catch (Exception e) {
           fail(e.getMessage());
        }
        
        ref = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance.getInstanceName());
        assertEquals("Check no object", 0, instance.getInstanceDescription().getCreatedObjects().length);
        assertNotNull("FS availability", ref);
        
        fs = (FooService) context.getService(ref);
        p = fs.fooProps();
        mes = p.getProperty("message");
        count = ((Integer) p.get("count")).intValue();
        assertEquals("Check 1 object", 1, instance.getInstanceDescription().getCreatedObjects().length);
        assertEquals("Check message", "message3", mes); // Already reconfigured.
        assertEquals("Check count", 1, count);
        
        instance.dispose();
        
        
    }

    public void testCreationAndReconfiguration3() {
        // The configuration exists before the instance creation.
        
      //Update
        Configuration configuration;
        try {
            configuration = admin.getConfiguration(msp);
            Dictionary prc = configuration.getProperties();
            if (prc == null) {
                prc = new Properties();
            }
            prc.put("message", "message2");
            configuration.update(prc);
            listener.waitForEvent(msp, "1");
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        Properties props = new Properties();
        props.put("managed.service.pid", msp);
        props.put("message", "message");
        ComponentInstance instance  = null;
        try {
            instance =  factSvc.createComponentInstance(props);
            Thread.sleep(ConfigurationTestSuite.UPDATE_WAIT_TIME);
        } catch (Exception e) {
           fail(e.getMessage());
        }
        
        ServiceReference ref = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance.getInstanceName());
        assertEquals("Check no object", 0, instance.getInstanceDescription().getCreatedObjects().length);
        assertNotNull("FS availability", ref);
        
        FooService fs = (FooService) context.getService(ref);
        Properties p = fs.fooProps();
        String mes = p.getProperty("message");
        int count = ((Integer) p.get("count")).intValue();
        assertEquals("Check 1 object", 1, instance.getInstanceDescription().getCreatedObjects().length);
        assertEquals("Check message", "message2", mes); // Already reconfigured.
        assertEquals("Check count", 1, count);
        
        //Reconfiguration
        try {
            configuration = admin.getConfiguration(msp);
            Dictionary prc = configuration.getProperties();
            if (prc == null) {
                prc = new Properties();
            }
            prc.put("message", "message3");
            configuration.update(prc);
            listener.waitForEvent(msp, "2");
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
        instance.dispose();
        
        // Recreation of the instance.
        props = new Properties();
        props.put("managed.service.pid", msp);
        props.put("message", "message");
        instance  = null;
        try {
            instance =  factSvc.createComponentInstance(props);
            Thread.sleep(ConfigurationTestSuite.UPDATE_WAIT_TIME);
        } catch (Exception e) {
           fail(e.getMessage());
        }
        
        ref = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance.getInstanceName());
        assertEquals("Check no object", 0, instance.getInstanceDescription().getCreatedObjects().length);
        assertNotNull("FS availability", ref);
        
        fs = (FooService) context.getService(ref);
        p = fs.fooProps();
        mes = p.getProperty("message");
        count = ((Integer) p.get("count")).intValue();
        assertEquals("Check 1 object", 1, instance.getInstanceDescription().getCreatedObjects().length);
        assertEquals("Check message", "message3", mes); // Already reconfigured.
        assertEquals("Check count", 1, count);
        
        instance.dispose();
        
        
    }
   
    
    

}
