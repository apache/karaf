package org.apache.felix.ipojo.test.scenarios.component;

import org.osgi.framework.BundleContext;

public class ParentClassWithBC {
    
    private BundleContext bc;

    public ParentClassWithBC(String foo, BundleContext bc, String bar) {
        this.bc = bc;
        System.out.println(foo + " : " + this.bc + "(" + bar + ")");
    } 

}
