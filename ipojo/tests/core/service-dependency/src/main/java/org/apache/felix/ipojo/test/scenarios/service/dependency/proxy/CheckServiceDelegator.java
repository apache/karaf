package org.apache.felix.ipojo.test.scenarios.service.dependency.proxy;

import java.util.Properties;

import org.apache.felix.ipojo.test.scenarios.service.dependency.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.service.dependency.service.FooService;
import org.osgi.framework.BundleContext;

public class CheckServiceDelegator implements CheckService {

    private FooService fs;
    
    private Helper helper;
    
    public CheckServiceDelegator(BundleContext bc) {
        helper = new Helper(bc, fs);
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
