package org.apache.felix.ipojo.test.instance;

import org.apache.felix.ipojo.test.instance.service.Service;


public class ServiceProvider implements Service {

    private int i = 0;
    
    public int count() {
        i++;
        return i;
    }

}
