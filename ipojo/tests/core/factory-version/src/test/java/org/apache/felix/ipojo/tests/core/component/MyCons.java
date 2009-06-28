package org.apache.felix.ipojo.tests.core.component;

import org.apache.felix.ipojo.tests.core.service.MyService;

public class MyCons {

    private MyService[] services;

    public MyCons() {
        System.out.println("Bound to " + services.length);
    }

}
