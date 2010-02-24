package org.apache.felix.ipojo.test.scenarios.component.controller;

import java.util.Properties;

import org.apache.felix.ipojo.test.scenarios.ps.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.ps.service.FooService;

public class ControllerCheckService implements FooService, CheckService {
    
    
    private boolean controller;

    public boolean foo() {
        return controller;
    }

    public Properties fooProps() {
        Properties props = new Properties();
        props.put("controller", new Boolean(controller));
        return props;
    }

    public boolean getBoolean() {
        return false;
    }

    public double getDouble() {
        return 0;
    }

    public int getInt() {
        return 0;
    }

    public long getLong() {
        return 0;
    }

    public Boolean getObject() {
        return null;
    }

    public boolean check() {
        System.out.println("Before : " + controller);
        controller = ! controller; // Change
        System.out.println("After : " + controller);
        return controller;
    }

    public Properties getProps() {
        Properties props = new Properties();
        props.put("controller", new Boolean(controller));
        return props;
    }

}
