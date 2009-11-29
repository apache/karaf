package org.apache.felix.ipojo.test.scenarios.component;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Modified;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.test.scenarios.annotations.service.FooService;

@Component
public class Dependency {

    @Requires
    public FooService fs;
    
    @Unbind
    public void unbindBar() {
        
    }
    
    @Bind
    public void bindBar() {
        
    }
    
    @Unbind
    public void unbindBaz() {
        
    }
    
    @Bind
    public void bindBaz() {
        
    }
   
    
    @Requires
    public FooService fs2;
    
    @Bind(id="fs2")
    public void bindFS2() {
        
    }
    
    @Unbind(id="fs2")
    public void unbindFS2() {
        
    }
    
    @Requires(id="inv")
    public FooService fs2inv;
    
    @Bind(id="inv")
    public void bindFS2Inv() {
        
    }
    
    @Unbind(id="inv")
    public void unbindFS2Inv() {
        
    }
    
    @Bind(id="mod")
    public void bindMod() {
        
    }
    
    @Unbind(id="mod")
    public void unbindMod() {
        
    }
    
    @Modified(id="mod")
    public void modifiedMod() {
        
    }
    
    
    
}
