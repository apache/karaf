package org.apache.felix.ipojo.test.scenarios.component.strategies;

import java.util.Properties;

import org.apache.felix.ipojo.test.scenarios.ps.service.BarService;
import org.apache.felix.ipojo.test.scenarios.ps.service.CheckService;

public class BarConsumer implements CheckService {
    
    private BarService bs;


    public boolean check() {
        return bs.bar();
    }

    public Properties getProps() {
        Properties props = bs.getProps();
        props.put("object", bs);
        return props;
    }

}
