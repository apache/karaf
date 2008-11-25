package org.apache.felix.ipojo.test.scenarios.component.jmx;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.handlers.jmx.Config;
import org.apache.felix.ipojo.handlers.jmx.Method;
import org.apache.felix.ipojo.handlers.jmx.Property;

@Component
@Config(domain="my-domain", usesMOSGi=false)
public class JMXSimple {

    @Property(name="prop", notification=true, rights="w")
    String m_foo;
    
    @Method(description="set the foo prop")
    public void setFoo(String mes) {
        System.out.println("Set foo to " + mes);
        m_foo = mes;
    }
    
    @Method(description="get the foo prop")
    public String getFoo() {
        return m_foo;
    }
}
