package org.apache.felix.ipojo.transaction.test.component;

import javax.transaction.Transaction;

import org.apache.felix.ipojo.annotations.Component;

@Component
@org.apache.felix.ipojo.transaction.Transaction(field="transaction")
public class ComponentUsingAnnotations {

    Transaction transaction;


    @org.apache.felix.ipojo.transaction.Transactionnal
    public void doSomethingBad() throws NullPointerException {
    }

    @org.apache.felix.ipojo.transaction.Transactionnal(propagation="required")
    public void doSomethingBad2() throws UnsupportedOperationException {

    }

    @org.apache.felix.ipojo.transaction.Transactionnal(propagation="supported", norollbackfor= {"java.lang.Exception"})
    public void doSomethingGood() {

    }

    @org.apache.felix.ipojo.transaction.Transactionnal(timeout=1000, exceptiononrollback=true)
    public void doSomethingLong() {

    }


}
