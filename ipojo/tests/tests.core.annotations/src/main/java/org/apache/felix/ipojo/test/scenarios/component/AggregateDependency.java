package org.apache.felix.ipojo.test.scenarios.component;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Unbind;

@Component
public class AggregateDependency {
    
    @Unbind(aggregate=true)
    public void unbindBar() {    }
    @Bind
    public void bindBar() {    }
    
    @Unbind
    public void unbindBaz() {    }
    @Bind(aggregate=true)
    public void bindBaz() {    }
   
    @Unbind(aggregate=true, id="unbindonly")
    public void unbind() {    }
    
    @Bind(aggregate=true, id="bindonly")
    public void bind() {    }
}
