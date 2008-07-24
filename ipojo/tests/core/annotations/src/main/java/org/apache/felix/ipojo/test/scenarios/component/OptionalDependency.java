package org.apache.felix.ipojo.test.scenarios.component;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.test.scenarios.annotations.service.FooService;

@Component
public class OptionalDependency {

    @Requires(optional=true)
    public FooService fs;
    
    @Requires(optional=false)
    public FooService fs2;
    
    @Unbind(optional=true)
    public void unbindBar() {    }
    @Bind
    public void bindBar() {    }
    
    @Unbind
    public void unbindBaz() {    }
    @Bind(optional=true)
    public void bindBaz() {    }
   
    @Requires(id="inv")
    public FooService fs2inv;
    @Bind(id="inv", optional=true)
    public void bindFS2Inv() {   }
    @Unbind(id="inv")
    public void unbindFS2Inv() {   }
    
    @Unbind(optional=true, id="unbindonly")
    public void unbind() {    }
    
    @Bind(optional=true, id="bindonly")
    public void bind() {    }
}
