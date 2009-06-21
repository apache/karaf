package org.apache.felix.ipojo.transaction.test.component;

import javax.transaction.Transaction;

import org.apache.felix.ipojo.transaction.test.service.CheckService;
import org.apache.felix.ipojo.transaction.test.service.Foo;

public class FooDelegator implements CheckService {
    
    private Foo foo;
    
    private int commit;
    
    private int rollback;
    
    Transaction transaction;
    
    Transaction lastCommitted;
    
    Transaction lastRolledback;
    
    Transaction current;

    
    public void onCommit(String method) {
        commit ++;
    }
    
    public void onRollback(String method, Exception e) {
        rollback ++;
    }

    public void doSomethingBad() throws NullPointerException {
        current = transaction;
        foo.doSomethingBad();
    }

    public void doSomethingBad2() throws UnsupportedOperationException {
       current = transaction;
       foo.doSomethingBad2();

    }

    public void doSomethingGood() {
        current = transaction;
        foo.doSomethingGood();
    }

    public void doSomethingLong() {
       current = transaction;
       foo.doSomethingLong();
    }

    public Transaction getCurrentTransaction() {
       return current;
    }

    public int getNumberOfCommit() {
       return commit;
    }

    public int getNumberOfRollback() {
       return rollback;
    }
    
    public Transaction getLastRolledBack() {
        return lastRolledback;
    }
    
    public Transaction getLastCommitted() {
        return lastCommitted;
    }
    
    public void onRollback(Transaction t) {
        lastRolledback = t;
        rollback ++;
    }
    

    public void onCommit(Transaction t) {
        lastCommitted = t;
        commit ++;
    }

}
