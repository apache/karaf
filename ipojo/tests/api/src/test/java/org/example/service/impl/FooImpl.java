package org.example.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.example.service.Foo;

public class FooImpl implements Foo {
    
   // private List<String> m_list = new ArrayList<String>();

    public void doSomething() {
       // Do something...
        System.out.println("Hello World !");
    }
    
    public FooImpl(String s) {
        _setIM(s);
    }
    
    public void _setIM(String s) {
        
    }

}
