package org.apache.felix.ipojo.transaction.test.service;

import javax.transaction.Transaction;

public interface CheckService {
    
    public void doSomethingGood();
    
    public void doSomethingBad() throws NullPointerException;
    
    public void doSomethingBad2() throws UnsupportedOperationException;
    
    public void doSomethingLong();
    
    public int getNumberOfCommit();
    
    public int getNumberOfRollback();
    
    public Transaction getCurrentTransaction();
    
    public Transaction getLastRolledBack();
    
    public Transaction getLastCommitted();

}
