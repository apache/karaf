package org.apache.felix.ipojo.test.scenarios.component;

import java.util.Dictionary;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Updated;
import org.apache.felix.ipojo.test.scenarios.annotations.service.BarService;
import org.apache.felix.ipojo.test.scenarios.annotations.service.FooService;

@Component(managedservice="MyPID")
public class UpdatedWithManagedService implements FooService, BarService {
    
    @Property(name="foo")
    public int m_foo = 0;
    
    @Property(value = "4")
    public int bar;
    
    @Property
    public void setboo(int boo) {
        
    }
    
    @Property
    public void setbaz(int baz) {
        
    }
    
    @Property
    public int boo;
    
    @Property(name="baa")
    public int m_baa;
    
    @Property(value="5")
    public void setbaa(int baa) {
        
    }

    public boolean foo() {
        return false;
    }

    public java.util.Properties fooProps() {
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

    public boolean bar() {
        return false;
    }

    public java.util.Properties getProps() {
        return null;
    }
    
    @Updated
    public void after(Dictionary conf) {
        // ...
    }

}
