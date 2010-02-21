package org.apache.felix.ipojo.optional;

import org.apache.felix.ipojo.Nullable;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.service.log.LogService;


@Component(immediate=true, name="optional-log-cons")
public class MyComponent {

    @Requires(optional=true, proxy=false)
    private LogService log;
    
    
    public MyComponent() {
        System.out.println("Created ! : " + (log instanceof Nullable) + " - " + log);
        log.log(LogService.LOG_INFO, "Created !");
        
    }
    
    
}
