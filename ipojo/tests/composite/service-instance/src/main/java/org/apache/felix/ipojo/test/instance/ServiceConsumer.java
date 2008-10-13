package org.apache.felix.ipojo.test.instance;

import java.util.Properties;

import org.apache.felix.ipojo.test.composite.service.CheckService;
import org.apache.felix.ipojo.test.instance.service.Service;

public class ServiceConsumer implements CheckService {
    
    private Service service;
    private Properties props = new Properties();
    
    public ServiceConsumer() {
       props.put("1", new Integer(service.count()));
       props.put("2", new Integer(service.count()));
       props.put("3", new Integer(service.count()));
    }

    public boolean check() {
       return service.count() > 0;
    }

    public Properties getProps() {
        return props;
    }

}
