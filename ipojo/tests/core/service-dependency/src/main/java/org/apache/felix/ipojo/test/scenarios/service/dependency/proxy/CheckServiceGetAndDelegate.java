package org.apache.felix.ipojo.test.scenarios.service.dependency.proxy;

import java.util.Properties;

import org.apache.felix.ipojo.test.scenarios.service.dependency.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.service.dependency.service.FooService;
import org.osgi.framework.BundleContext;

public class CheckServiceGetAndDelegate implements CheckService {

    private FooService fs;
    
    private Helper helper;
    
    public CheckServiceGetAndDelegate(BundleContext bc) {
        helper = new Helper(bc, fs);
    }
    
    public boolean check() {
        fs.foo();
        return helper.check();
    }

    public Properties getProps() {
        fs.getBoolean();
        return helper.getProps();
    }

}
