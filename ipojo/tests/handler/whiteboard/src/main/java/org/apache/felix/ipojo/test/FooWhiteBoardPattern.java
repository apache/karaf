package org.apache.felix.ipojo.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.ServiceReference;

public class FooWhiteBoardPattern implements Observable {
    
    List list = new ArrayList();
    int modifications = 0;
    
    public void onArrival(ServiceReference ref) {
        list.add(ref);
    }
    
    public void onDeparture(ServiceReference ref) {
        list.remove(ref);
    }
    
    public void onModification(ServiceReference ref) {
        modifications = modifications + 1;
    }

    public Map getObservations() {
        Map map = new HashMap();
        map.put("list", list);
        map.put("modifications", new Integer(modifications));
        return map;
    }
    
    

}
