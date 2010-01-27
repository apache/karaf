package org.apache.felix.ipojo.test.scenarios.component;

import java.util.Properties;

import org.apache.felix.ipojo.test.scenarios.ps.service.FooService;

public class NullCheckServiceProvider implements FooService {
    
    private String prop1;
    private String prop2;
    
    public NullCheckServiceProvider() {
      if (prop1 == null) {
          prop2= "0";
      }
    }
    

    public boolean foo() {
        if (prop1 == null  && prop2 != null) {
            prop1 = "0";
            prop2 = null;
            return true;
        }
        if (prop2 == null  && prop1 != null) {
            prop1 = null;
            prop2 = "0";
            return true;
        }
        return false;
    }

    public Properties fooProps() {
        return null;
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

}
