package org.apache.felix.ipojo.test.scenarios.component;

public class CallSuperConstructor extends ParentClass {
    
    public CallSuperConstructor() {
        super("test");
        System.out.println("plop");
    } 

}
