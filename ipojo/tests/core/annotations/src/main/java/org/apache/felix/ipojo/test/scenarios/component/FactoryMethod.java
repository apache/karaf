package org.apache.felix.ipojo.test.scenarios.component;

import org.apache.felix.ipojo.annotations.Component;

@Component(factory_method="create")
public class FactoryMethod {
    
    public static FactoryMethod create() {
        return new FactoryMethod();
    }
}
