/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.jms.internal;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class JmsTemplate {
    private BundleContext bc;
    private String connectionFactoryName;
    private String username;
    private String password;

    public JmsTemplate(BundleContext bc, String connectionFactoryName, String username, String password) {
        this.bc = bc;
        this.connectionFactoryName = connectionFactoryName;
        this.username = username;
        this.password = password;
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    public <E> E execute(JmsCallback<E> callback) {
        ServiceReference reference = null;
        Connection connection = null;
        Session session = null;
        try {
            reference = this.lookupConnectionFactory(connectionFactoryName);
            ConnectionFactory cf = (ConnectionFactory) bc.getService(reference);
            connection = cf.createConnection(username, password);
            connection.start();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            E result = callback.doInSession(connection, session);
            session.commit();
            return result;
        } catch (Exception e) {
            try {
                if (session != null) {
                    session.rollback();
                }
            } catch (JMSException e1) {
                // Ignore 
            }
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (JMSException e) {
                    // Ignore
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException e) {
                    // Ignore
                }
            }
            if (reference != null) {
                bc.ungetService(reference);
            }
        }
    }
    
    @SuppressWarnings("rawtypes")
    private ServiceReference lookupConnectionFactory(String name) throws Exception {
        ServiceReference[] references = bc.getServiceReferences(ConnectionFactory.class.getName(), "(|(osgi.jndi.service.name=" + name + ")(name=" + name + ")(service.id=" + name + "))");
        if (references == null || references.length == 0) {
            throw new IllegalArgumentException("No JMS connection factory found for " + name);
        }
        if (references.length > 1) {
            throw new IllegalArgumentException("Multiple JMS connection factories found for " + name);
        }
        return references[0];
    }
}
