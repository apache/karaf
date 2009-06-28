package org.apache.felix.ipojo.pax.tinybundles.tests.impl;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.tinybundles.tests.service.Hello;

@Component
public class Consumer {
    
    @Requires
    private Hello hello;
    
    public Consumer() {
        System.out.println(hello.sayHello());
    }

}
