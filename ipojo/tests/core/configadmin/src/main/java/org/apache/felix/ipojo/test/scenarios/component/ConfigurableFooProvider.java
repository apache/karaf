package org.apache.felix.ipojo.test.scenarios.component;

import java.util.Properties;

import org.apache.felix.ipojo.test.scenarios.configadmin.service.FooService;

public class ConfigurableFooProvider implements FooService {
    
    private String message; // Configurable property
    private int invokeCount = 0;
    
    public void setMessage(String message) {
        System.out.println("Set message to " + message);
        this.message = message;
        invokeCount++;
    }

    public boolean foo() {
        return true;
    }

    public Properties fooProps() {
        Properties props = new Properties();
        if (message == null) {
            props.put("message", "NULL");
        } else {
            props.put("message", message);
        }
        props.put("count", new Integer(invokeCount));
        return props;
    }

    public boolean getBoolean() {
        return false;
    }

    public double getDouble() {
        return invokeCount;
    }

    public int getInt() {
        return invokeCount;
    }

    public long getLong() {
        return invokeCount;
    }

    public Boolean getObject() {
        return null;
    }

}
