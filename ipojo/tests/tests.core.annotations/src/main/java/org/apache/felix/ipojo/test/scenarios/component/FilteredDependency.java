package org.apache.felix.ipojo.test.scenarios.component;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.test.scenarios.annotations.service.FooService;

@Component
public class FilteredDependency {

    @Requires(filter="(foo=bar)")
    public FooService fs;
    
    @Unbind(filter="(foo=bar)")
    public void unbindBar() {    }
    @Bind
    public void bindBar() {    }
    
    @Unbind
    public void unbindBaz() {    }
    @Bind(filter="(foo=bar)")
    public void bindBaz() {    }
   
    @Requires(id="inv")
    public FooService fs2inv;
    @Bind(id="inv", filter="(foo=bar)")
    public void bindFS2Inv() {   }
    @Unbind(id="inv")
    public void unbindFS2Inv() {   }
    
    @Unbind(filter="(foo=bar)", id="unbindonly")
    public void unbind() {    }
    
    @Bind(filter="(foo=bar)", id="bindonly")
    public void bind() {    }
}
