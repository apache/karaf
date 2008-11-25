package org.apache.felix.ipojo.test.scenarios.component.whiteboard;

import org.apache.felix.ipojo.annotations.Component;
import org.osgi.framework.ServiceReference;

@Component
@org.apache.felix.ipojo.whiteboard.Wbp(filter="(foo=true)", 
        onArrival="onArrival", 
        onDeparture="onDeparture",
        onModification="onModification")
public class WhiteBoardWIModification {
    
    public void onArrival(ServiceReference ref) {
        // nothing
    }
    
    public void onDeparture(ServiceReference ref) {
        // nothing
    }
    
    public void onModification(ServiceReference ref) {
        // nothing
    }

}
