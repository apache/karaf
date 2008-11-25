package org.apache.felix.ipojo.test.scenarios.component.extender;

import org.apache.felix.ipojo.annotations.Component;
import org.osgi.framework.Bundle;


@Component
@org.apache.felix.ipojo.extender.Extender(extension="foo", onArrival="onArrival", onDeparture="onDeparture")
public class Extender {
    
    public void onArrival(Bundle bundle, String foo) {
        // nothing
    }
    
    public void onDeparture(Bundle bundle) {
        // nothing
    }
}
