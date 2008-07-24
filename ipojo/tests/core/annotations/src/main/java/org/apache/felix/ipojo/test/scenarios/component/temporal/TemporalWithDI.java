package org.apache.felix.ipojo.test.scenarios.component.temporal;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.handler.temporal.Requires;
import org.apache.felix.ipojo.test.scenarios.annotations.service.FooService;

@Component
public class TemporalWithDI {
    
    @Requires(onTimeout="org.apache.felix.ipojo.test.scenarios.component.ProvidesSimple")
    private FooService fs;
    
}
