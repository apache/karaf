package org.apache.felix.ipojo.test.scenarios.service.dependency.proxy;

import java.util.Properties;

import org.apache.felix.ipojo.test.scenarios.service.dependency.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.service.dependency.service.FooService;
import org.osgi.framework.BundleContext;

public class CheckServiceNoDelegate implements CheckService {

    private FooService fs;
    
    private Helper helper;
    
    private BundleContext context;
    
    public CheckServiceNoDelegate(BundleContext bc) {
       context = bc;
       helper = new Helper(context, fs);
    }
    
    public void start() {
        helper.publish();
    }
    
    public void stop() {
        helper.unpublish();
    }
    
    public boolean check() {
        // Don't access the service
        // Just delegate
        return helper.check();
    }

    public Properties getProps() {
        // Don't access the service
        // Just delegate
        return helper.getProps();
    }

}
