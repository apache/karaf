package org.apache.felix.ipojo.transaction;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.apache.felix.ipojo.util.Callback;

public class TransactionHandler extends PrimitiveHandler implements Synchronization {

    public static final String NAMESPACE= "org.apache.felix.ipojo.transaction";

    public static final String NAME= "transaction";

    private static final String FIELD_ATTRIBUTE= "field";

    private static final String ONCOMMIT_ATTRIBUTE= "oncommit";

    private static final String ONROLLBACK_ATTRIBUTE= "onrollback";

    private static final String TRANSACTIONNAL_ELEMENT = "transactionnal";

    private static final String METHOD_ATTRIBUTE = "method";

    private static final String TIMEOUT_ATTRIBUTE = "timeout";

    private static final String PROPAGATION_ATTRIBUTE = "propagation";

    private static final String EXCEPTIONONROLLBACK_ATTRIBUTE = "exceptiononrollback";

    private static final String NOROLLBACKFOR_ATTRIBUTE = "norollbackfor";

    public static final int DEFAULT_PROPAGATION = TransactionnalMethod.REQUIRES;


    private TransactionManager m_transactionManager; // Service Dependency

    private List<TransactionnalMethod> m_methods = new ArrayList<TransactionnalMethod>();

    private Callback m_onRollback;

    private Callback m_onCommit;

    private List<Transaction> m_transactions = new ArrayList<Transaction>();


    public void configure(Element arg0, Dictionary arg1)
            throws ConfigurationException {
        Element[] elements = arg0.getElements(NAME, NAMESPACE);
        if (elements.length > 1) {
            throw new ConfigurationException("The handler " + NAMESPACE + ":" + NAME + " cannot be declared several times");
        }

        String field = elements[0].getAttribute(FIELD_ATTRIBUTE);
        if (field != null) {
            FieldMetadata meta = getPojoMetadata().getField(field);
            if (meta == null) {
                throw new ConfigurationException("The transaction field does not exist in the pojo class : " + field);
            }
            if (! meta.getFieldType().equals(Transaction.class.getName())) {
                throw new ConfigurationException("The transaction field type must be " + Transaction.class.getName());
            }
            // Register the interceptor
            getInstanceManager().register(meta, this);

        }

        String oncommit = elements[0].getAttribute(ONCOMMIT_ATTRIBUTE);
        if (oncommit != null) {
            m_onCommit = new Callback(oncommit, new String[] { Transaction.class.getName() }, false, getInstanceManager());
        }

        String onrollback = elements[0].getAttribute(ONROLLBACK_ATTRIBUTE);
        if (onrollback != null) {
            m_onRollback = new Callback(onrollback, new String[] { Transaction.class.getName() }, false, getInstanceManager());
        }


        Element[] sub = elements[0].getElements(TRANSACTIONNAL_ELEMENT);
        if (sub == null  || sub.length == 0) {
            throw new ConfigurationException("The handler " + NAMESPACE + ":" + NAME + " must have " + TRANSACTIONNAL_ELEMENT + " subelement");
        }

        for (int i = 0; i < sub.length; i++) {
            String method = sub[i].getAttribute(METHOD_ATTRIBUTE);
            String to = sub[i].getAttribute(TIMEOUT_ATTRIBUTE);
            String propa = sub[i].getAttribute(PROPAGATION_ATTRIBUTE);
            String nrbf = sub[i].getAttribute(NOROLLBACKFOR_ATTRIBUTE);
            String eorb = sub[i].getAttribute(EXCEPTIONONROLLBACK_ATTRIBUTE);

            if (method == null) {
                throw new ConfigurationException("A transactionnal element must specified the method attribute");
            }
            MethodMetadata meta = this.getPojoMetadata().getMethod(method);
            if (meta == null) {
                throw new ConfigurationException("A transactionnal method is not in the pojo class : " + method);
            }

            int timeout = 0;
            if (to != null) {
                timeout = new Integer(to).intValue();
            }

            int propagation = DEFAULT_PROPAGATION;
            if (propa != null) {
                propagation = parsePropagation(propa);
            }

            List<String> exceptions = new ArrayList<String>();
            if (nrbf != null) {
                exceptions = (List<String>) ParseUtils.parseArraysAsList(nrbf);
            }

            boolean exceptionOnRollback = false;
            if (eorb != null) {
                exceptionOnRollback = new Boolean(eorb).booleanValue();
            }

            TransactionnalMethod tm = new TransactionnalMethod(method, propagation, timeout, exceptions, exceptionOnRollback, this);
            m_methods.add(tm);
            this.getInstanceManager().register(meta, tm);
        }

    }

    private int parsePropagation(String propa) throws ConfigurationException {
       if (propa.equalsIgnoreCase("requires")) {
           return TransactionnalMethod.REQUIRES;

       } else if (propa.equalsIgnoreCase("mandatory")){
           return TransactionnalMethod.MANDATORY;

       } else if (propa.equalsIgnoreCase("notsupported")) {
           return TransactionnalMethod.NOT_SUPPORTED;

       } else if (propa.equalsIgnoreCase("supported")) {
           return TransactionnalMethod.SUPPORTED;

       } else if (propa.equalsIgnoreCase("never")) {
           return TransactionnalMethod.NEVER;

        } else if (propa.equalsIgnoreCase("requiresnew")) {
            return TransactionnalMethod.REQUIRES_NEW;
        }

       throw new ConfigurationException("Unknown propgation policy : " + propa);
    }

    public void start() {
        // Set transaction managers.
        for (TransactionnalMethod method : m_methods) {
            method.setTransactionManager(m_transactionManager);
        }
    }

    public void stop() {
        // Nothing to do.
    }

    public synchronized void bind(TransactionManager tm) {
        for (TransactionnalMethod method : m_methods) {
            method.setTransactionManager(tm);
        }
    }

    public synchronized void unbind(TransactionManager tm) {
        for (TransactionnalMethod method : m_methods) {
            method.setTransactionManager(null);
        }
    }

    public void transactionRolledback(Transaction t) {
       if (m_onRollback != null) {
            try {
                m_onRollback.call(new Object[] { t });
            } catch (NoSuchMethodException e1) {
                error("Cannot invoke the onRollback method, method not found",
                        e1);
            } catch (IllegalAccessException e1) {
                error(
                        "Cannot invoke the onRollback method,cannot access the method",
                        e1);
            } catch (InvocationTargetException e1) {
                error(
                        "Cannot invoke the onRollback method,the method thrown an exception",
                        e1.getTargetException());
            }
        }
    }

    public void transactionCommitted(Transaction t) {
        if (m_onRollback != null) {
            try {
                m_onCommit.call(new Object[] { t });
            } catch (NoSuchMethodException e1) {
                error("Cannot invoke the onCommit callback, method not found",
                        e1);
            } catch (IllegalAccessException e1) {
                error(
                        "Cannot invoke the onCommit callback,cannot access the method",
                        e1);
            } catch (InvocationTargetException e1) {
                error(
                        "Cannot invoke the onCommit callback,the method thrown an exception",
                        e1.getTargetException());
            }
        }

    }

    public void stateChanged(int newState) {
        if (newState == ComponentInstance.INVALID) {
            // rollback all owned transactions.
            for (int i = 0; i < m_methods.size(); i++) {
                m_methods.get(i).rollbackOwnedTransactions();
            }

            for (int i =0; i < m_transactions.size(); i++) {
                try {
                    m_transactions.get(i).setRollbackOnly();
                } catch (Exception e) {
                    error("Cannot set rollback only on a transaction : " + e.getMessage());
                }
            }
        }
    }

    public synchronized Object onGet(Object pojo, String fieldName, Object value) {
        try {
            if (m_transactionManager != null) {
                return m_transactionManager.getTransaction();
            } else {
                return null;
            }
        } catch (SystemException e) {
            error("Cannot get the current transaction, internal error", e);
            return null;
        }
    }

    public void afterCompletion(int arg0) {
        try {
            if (m_transactionManager.getTransaction() != null) {
                m_transactions .remove(m_transactionManager.getTransaction());
                if (arg0 == Status.STATUS_ROLLEDBACK) {
                    transactionRolledback(m_transactionManager.getTransaction());
                } else if (arg0 == Status.STATUS_COMMITTED) {
                    transactionCommitted(m_transactionManager.getTransaction());
                }
            }
        } catch (SystemException e) {
           error("Cannot remove the transaction from the transaction list : " + e.getMessage());
        }
    }

    public void beforeCompletion() {

    }

    public void addTransaction(Transaction transaction) {
        if (m_transactions.contains(transaction)) {
            return;
        }
        try {
            transaction.registerSynchronization(this);
            m_transactions.add(transaction);
        } catch (Exception e) {
           error("Cannot add the transaction to the transaction list : " + e.getMessage());
        }
    }

    public List<Transaction> getTransactions() {
        return m_transactions;
    }


}
