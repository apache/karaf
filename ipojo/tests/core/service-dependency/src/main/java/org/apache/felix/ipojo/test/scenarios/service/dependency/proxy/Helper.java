package org.apache.felix.ipojo.test.scenarios.service.dependency.proxy;

import java.util.Properties;

import org.apache.felix.ipojo.test.scenarios.service.dependency.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.service.dependency.service.FooService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

public class Helper implements CheckService {
    
    
    private FooService fs;
    private BundleContext context;
    private ServiceRegistration reg;
    
    public Helper(BundleContext bc, FooService svc) {
        fs = svc;
        context = bc;    
    }
    
    public void publish() {
        Properties props = new Properties();
        props.put(Constants.SERVICE_PID, "Helper");
        reg = context.registerService(CheckService.class.getName(), this, props);
    }
    
    public void unpublish() {
        if (reg != null) {
            reg.unregister();
        }
        reg = null;
    }

    public boolean check() {
        return fs.foo();
    }

    public Properties getProps() {
        Properties props = new Properties();
        fs.getBoolean();
        props.put("helper.fs", fs);
        return props;
    }

}
