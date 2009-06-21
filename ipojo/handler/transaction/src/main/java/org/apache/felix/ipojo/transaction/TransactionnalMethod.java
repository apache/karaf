package org.apache.felix.ipojo.transaction;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.felix.ipojo.MethodInterceptor;

public class TransactionnalMethod implements MethodInterceptor {
    
    public static final int REQUIRES = 0;
    
    public static final int REQUIRES_NEW = 1;
    
    public static final int MANDATORY = 2;
    
    public static final int SUPPORTED = 3;
    
    public static final int NOT_SUPPORTED = 4;

    public static final int NEVER = 5;
  
    private String method;
    
    private int propagation;
    
    private int timeout;
    
    private List<String> exceptions;

    private TransactionManager manager;
    
    private Map <Thread, Transaction> m_owned = new HashMap<Thread, Transaction>();

    private boolean exceptionOnRollback;
    

    private TransactionHandler handler;

    private Transaction suspended;
    
    public TransactionnalMethod(String method, int propagation, int timeout, List<String> exception, boolean exceptionOnRollback, TransactionHandler handler) {
        this.method = method;
        this.propagation = propagation;
        this.timeout = timeout;
        this.exceptions = exception;
        this.exceptionOnRollback = exceptionOnRollback;
        this.handler = handler;
    }
    
    public synchronized void setTransactionManager(TransactionManager tm) {
        System.out.println("Set transaction manager to " + tm);
        manager = tm;
        if (manager == null) {
            // Clear stored transactions.
            m_owned.clear();
            
        }
    }
    
    
    public void onEntry() throws SystemException, NotSupportedException {
        TransactionManager manager = null;
        synchronized (this) {
            if (this.manager != null) {
                manager = this.manager; // Stack confinement
            } else {
                return; // Nothing can be done...
            }
        }
        
        Transaction transaction = manager.getTransaction();
        switch (propagation) {
            case REQUIRES:
                // Are we already in a transaction?
                if (transaction == null) {
                    // No, create one
                    System.out.println("Begin a new transaction !");

                    if (timeout > 0) {
                        manager.setTransactionTimeout(timeout);
                    }
                    manager.begin();
                    m_owned.put(Thread.currentThread(), manager.getTransaction());
                } else {
                    // Add the transaction to the transaction list
                    handler.addTransaction(transaction);
                }
                break;
            case MANDATORY: 
                if (transaction == null) {
                    // Error
                    throw new IllegalStateException("The method " + method + " must be called inside a transaction");
                } else {
                    // Add the transaction to the transaction list
                    handler.addTransaction(transaction);
                }
                break;
            case SUPPORTED:
                // if transaction != null, register the callback, else do nothing
                if (transaction != null) {
                    handler.addTransaction(transaction);
                } // Else do nothing.
                break;
            case NOT_SUPPORTED:
                // Do nothing.
                break;
            case NEVER:
                if (transaction != null) {
                    throw new IllegalStateException("The method " + method + " must never be called inside a transaction");
                }
                break;
            case REQUIRES_NEW:
                if (transaction == null) {
                    // No current transaction, Just creates a new one
                    if (timeout > 0) {
                        manager.setTransactionTimeout(timeout);
                    }
                    manager.begin();
                    System.out.println("== Associate " + Thread.currentThread() + " to " + manager.getTransaction() + " ==");
                    m_owned.put(Thread.currentThread(), manager.getTransaction());
                } else {
                    if (suspended == null) {
                        suspended = manager.suspend();
                        if (timeout > 0) {
                            manager.setTransactionTimeout(timeout);
                        }
                        manager.begin();
                        m_owned.put(Thread.currentThread(), manager.getTransaction());
                    } else {
                        throw new IllegalStateException("The method " + method + " requires to suspend a second times a transaction");
                    }
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown or unsupported propagation policy for " + method + " :" + propagation);
        
        }
    }
    
    public void onExit() throws SecurityException, HeuristicMixedException, HeuristicRollbackException, SystemException, InvalidTransactionException, IllegalStateException {
        switch (propagation) {
            case REQUIRES:
                // Are we the owner of the transaction?
                Transaction transaction = m_owned.get(Thread.currentThread());
                if (transaction != null) { // Owner.
                    try {
                        transaction.commit(); // Commit the transaction
                        m_owned.remove(Thread.currentThread());
                        handler.transactionCommitted(transaction); // Manage potential notification.
                    } catch ( RollbackException e) {
                        m_owned.remove(Thread.currentThread());
                        // The transaction was rolledback
                        if (exceptionOnRollback) {
                            throw new IllegalStateException("The transaction was rolledback : " + e.getMessage());
                        }
                        handler.transactionRolledback(transaction); // Manage potential notification.
                    }
                } // Else wait for commit.
                break;
            case MANDATORY: 
                // We are never the owner, so just exits the transaction.
                break;
            case SUPPORTED:
                // Do nothing.
                break;
            case NOT_SUPPORTED:
                // Do nothing.
                break;
            case NEVER:
                // Do nothing.
                break;
            case REQUIRES_NEW:
                // We're necessary the owner.
                transaction = m_owned.get(Thread.currentThread());
                System.out.println("== Owned " + Thread.currentThread() + " to " + transaction + "==");
                if (transaction == null) {
                    throw new RuntimeException("Cannot apply the REQUIRES NEW propagation, we're not the transaction owner!"); 
                }
                try {
                    transaction.commit(); // Commit the transaction
                    m_owned.remove(Thread.currentThread());
                    handler.transactionCommitted(transaction); // Manage potential notification.
                    if (suspended != null) {
                        manager.suspend(); // suspend the completed transaction.
                        manager.resume(suspended);
                        suspended = null;
                    }
                 } catch ( RollbackException e) { // The transaction was rolledback rather than committed
                    m_owned.remove(Thread.currentThread());
                    if (suspended != null) {
                        manager.suspend(); // suspend the completed transaction.
                        manager.resume(suspended); // The resume transaction is not rolledback, they are independent.
                        suspended = null;
                    }
                    // The transaction was rolledback
                    if (exceptionOnRollback) {
                        throw new IllegalStateException("The transaction was rolledback : " + e.getMessage());
                    }
                    handler.transactionRolledback(transaction); // Manage potential notification.
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown or unsupported propagation policy for " + method + " :" + propagation);
        
        }
    }
    
    public void onError(String exception) throws SystemException {
        TransactionManager manager = null;
        synchronized (this) {
            if (this.manager != null) {
                manager = this.manager; // Stack confinement
            } else {
                return; // Nothing can be done...
            }
        }
        
        // is the error something to exclude, and are we inside the transaction (owner or participant)? 
        if (! exceptions.contains(exception)) {
            Transaction tr = manager.getTransaction();
            if (m_owned.containsValue(tr)  || handler.getTransactions().contains(tr)) {
                // Set the transaction to rollback only
                manager.getTransaction().setRollbackOnly();
            }
        }
    }

    public void onEntry(Object arg0, Method arg1, Object[] arg2) {
        try {
            onEntry();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("An issue occurs during transaction management of " + method + " : " + e.getMessage());
        }
        
    }

    public void onError(Object arg0, Method arg1, Throwable arg2) {
        try {
            System.out.println("Error in  a TM " + arg1.getName() + " " + arg2.getMessage());
            arg2.printStackTrace();

            onError(arg2.getClass().getName());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("An issue occurs during transaction management of " + method + " : " + e.getMessage());
        }
    }

    public void onExit(Object arg0, Method arg1, Object arg2) {
        // Wait for on finally.
    }

    public void onFinally(Object arg0, Method arg1) {
        try {
            System.out.println("Exits a TM " + arg1.getName());

            onExit();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("An issue occurs during transaction management of " + method + " : " + e.getMessage());
        }
    }

    public void rollbackOwnedTransactions() {
        Iterator<Entry<Thread, Transaction>> entries = m_owned.entrySet().iterator();
        while(entries.hasNext()) {
            Entry<Thread, Transaction> entry = entries.next();
            try {
                entry.getValue().rollback();
            } catch (IllegalStateException e) {
                throw new RuntimeException("An issue occurs during transaction management of " + method + " : " + e.getMessage());
            } catch (SystemException e) {
                throw new RuntimeException("An issue occurs during transaction management of " + method + " : " + e.getMessage());
            }
        }
        m_owned.clear();
    }
    
    
}
