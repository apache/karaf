package org.apache.felix.ipojo.test.scenarios.component;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.test.scenarios.annotations.service.FooService;

@Component
public class DefaultImplementationDependency {

    @Requires(defaultimplementation=ProvidesSimple.class, optional=true)
    public FooService fs;
}
