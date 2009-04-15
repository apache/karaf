package org.example.service.impl;

import org.example.service.Foo;

public class MyComponentImpl {
    
    private Foo myFoo;
    
    private int anInt;
    
    public MyComponentImpl() {
        anInt = 2;
    }
    
    public MyComponentImpl(int i) {
        anInt = i;
    }

    public void start() {
       myFoo.doSomething();
       if (anInt > 0) {
           System.out.println("Set int to " + anInt);
       }
    }

}
