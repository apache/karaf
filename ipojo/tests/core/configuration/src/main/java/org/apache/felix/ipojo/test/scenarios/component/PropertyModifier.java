package org.apache.felix.ipojo.test.scenarios.component;

import java.util.Properties;

import org.apache.felix.ipojo.test.scenarios.configuration.service.CheckService;
import org.osgi.framework.BundleContext;

public class PropertyModifier implements CheckService {
    
    private Class[] classes;
    private BundleContext context;
    
    PropertyModifier(BundleContext bc) {
        context = bc;
    }

    public boolean check() {
        return classes != null;
    }
    
    public void setClasses(String[] classes) throws ClassNotFoundException {
        Class[] cls = new Class[classes.length];
        for (int i = 0; i < classes.length; i++) {
            try {
                cls[i] = context.getBundle().loadClass(classes[i]);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                throw e;
            }
        }
        
        this.classes = cls;
    }

    public Properties getProps() {
        Properties props = new Properties();
        props.put("classes", classes);
        return props;
    }

}
