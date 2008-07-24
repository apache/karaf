package org.apache.felix.ipojo.test;

public class FooProvider implements FooService {
    
    public String foo;

    public void foo() { 
        if (foo.equals("foo")) {
            foo = "bar";
        } else {
            foo = "foo";
        }
    }
    
}
