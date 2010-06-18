package org.apache.felix.ipojo.test.scenarios.component;

public class ParentClass {
    
    private String name;

    public ParentClass(final String n) {
        name = n;
    }
    
    public ParentClass(final StringBuffer n) {
        name = n.toString();
    } 

}
