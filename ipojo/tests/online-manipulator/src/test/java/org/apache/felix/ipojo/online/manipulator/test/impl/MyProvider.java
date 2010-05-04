package org.apache.felix.ipojo.online.manipulator.test.impl;

import org.apache.felix.ipojo.online.manipulator.test.service.Hello;

public class MyProvider implements Hello {
    
    public String sayHello() {
        return "Hello";
    }

}
