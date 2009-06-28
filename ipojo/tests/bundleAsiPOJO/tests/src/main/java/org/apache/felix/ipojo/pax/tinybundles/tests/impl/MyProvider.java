package org.apache.felix.ipojo.pax.tinybundles.tests.impl;

import org.apache.felix.ipojo.tinybundles.tests.service.Hello;

public class MyProvider implements Hello {
    
    public String sayHello() {
        return "Hello";
    }

}
