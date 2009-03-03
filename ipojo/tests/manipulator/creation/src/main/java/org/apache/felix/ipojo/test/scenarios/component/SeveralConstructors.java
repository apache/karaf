package org.apache.felix.ipojo.test.scenarios.component;

import java.util.Properties;

import org.apache.felix.ipojo.test.scenarios.manipulation.service.CheckService;

public class SeveralConstructors implements CheckService {
    
   
        private String name;

        public SeveralConstructors(){
            this("hello world");
        }

        public SeveralConstructors(final String n) {
            name = n;
        }

        public boolean check() {
            return name != null;
        }

        public Properties getProps() {
            Properties props = new Properties();
            props.put("name", name);
            return props;
        }


}
