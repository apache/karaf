package org.apache.felix.ipojo.test.scenarios.component;

import java.util.Properties;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.test.scenarios.annotations.service.BarService;
import org.apache.felix.ipojo.test.scenarios.annotations.service.FooService;

@Component
@Provides(specifications= {FooService.class, BarService.class})
public class ProvidesProperties implements FooService, BarService {
    
    @ServiceProperty(name = "foo")
    public int m_foo = 0;
    
    @ServiceProperty(value = "4")
    public int bar;
    
    @ServiceProperty
    public void setboo(int boo) {
        
    }
    
    @ServiceProperty(name="baz")
    public void setBaz(int baz) {
        
    }
    
    @ServiceProperty(name="baz")
    int m_baz;
    
    @ServiceProperty
    public int boo;
    
    @ServiceProperty(name="baa")
    public int m_baa;
    
    @ServiceProperty(value="5")
    public void setbaa(int baa) {
        
    }

    public boolean foo() {
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

    public boolean bar() {
        return false;
    }

    public Properties getProps() {
        return null;
    }

}
