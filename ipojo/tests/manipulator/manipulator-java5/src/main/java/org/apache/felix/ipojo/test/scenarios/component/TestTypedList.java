package org.apache.felix.ipojo.test.scenarios.component;

import java.util.List;
import java.util.Properties;

import org.apache.felix.ipojo.test.scenarios.manipulation.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.manipulation.service.FooService;

public class TestTypedList implements CheckService {
    
    private List<FooService> list;

    public boolean check() {
        return ! list.isEmpty();
    }

    public Properties getProps() {
        Properties props = new Properties();
        if (list != null) {
            props.put("list", list);
        
            int i = 0;
            for (FooService fs : list) {
                props.put(i, fs.foo());
                i++;
            }
        }
        
        return props;
    }

}
