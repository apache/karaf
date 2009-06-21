package org.apache.felix.ipojo.transaction.test.service;

public interface Foo {
    
    public void doSomethingGood();
    
    public void doSomethingBad() throws NullPointerException;
    
    public void doSomethingBad2() throws UnsupportedOperationException;
    
    public void doSomethingLong();

}
