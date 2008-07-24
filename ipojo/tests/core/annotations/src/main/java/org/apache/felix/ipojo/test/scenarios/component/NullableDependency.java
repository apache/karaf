package org.apache.felix.ipojo.test.scenarios.component;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.test.scenarios.annotations.service.FooService;

@Component
public class NullableDependency {

    @Requires(nullable=true)
    public FooService fs;
    
    @Requires(nullable=false)
    public FooService fs2;
    
  
}
