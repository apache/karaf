package org.apache.felix.ipojo.test.scenarios.component.strategies;

import java.util.Properties;

import org.apache.felix.ipojo.test.scenarios.ps.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.ps.service.FooService;

public class Consumer implements CheckService {
    
    private FooService fs;


    public boolean check() {
        return fs.foo();
    }

    public Properties getProps() {
        Properties props = fs.fooProps();
        props.put("object", fs);
        return props;
    }

}
