package org.apache.felix.ipojo.test.scenarios.component;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.test.scenarios.annotations.service.FooService;

@Component
public class PolicyDependency {

    @Requires(policy="static")
    public FooService fs;
    
    @Requires(policy="dynamic-priority")
    public FooService fs2;
    
    @Unbind(policy="static")
    public void unbindBar() {    }
    @Bind
    public void bindBar() {    }
    
    @Unbind
    public void unbindBaz() {    }
    @Bind(policy="static")
    public void bindBaz() {    }
   
    @Requires(id="inv")
    public FooService fs2inv;
    @Bind(id="inv", policy="static")
    public void bindFS2Inv() {   }
    @Unbind(id="inv")
    public void unbindFS2Inv() {   }
    
    @Unbind(policy="static", id="unbindonly")
    public void unbind() {    }
    
    @Bind(policy="static", id="bindonly")
    public void bind() {    }
}
