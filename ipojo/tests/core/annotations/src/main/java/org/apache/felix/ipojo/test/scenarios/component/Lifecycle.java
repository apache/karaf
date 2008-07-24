package org.apache.felix.ipojo.test.scenarios.component;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Controller;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Validate;

@Component
public class Lifecycle {
    @Controller
    boolean lfc;
    
    @Validate
    public void start() {
        
    }
    
    @Invalidate
    public void stop() {
        
    }
}
