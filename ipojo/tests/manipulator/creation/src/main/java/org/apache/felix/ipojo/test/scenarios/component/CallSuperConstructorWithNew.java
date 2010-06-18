package org.apache.felix.ipojo.test.scenarios.component;

public class CallSuperConstructorWithNew extends ParentClass {
    
    public CallSuperConstructorWithNew() {
        super(new StringBuffer("test"));
        System.out.println("plop");
    } 

}
