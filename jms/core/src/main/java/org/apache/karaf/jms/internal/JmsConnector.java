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

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class JmsConnector implements Closeable {
    private BundleContext bc;
    private ServiceReference<ConnectionFactory> reference;
    private Connection connection;
    private Session session;
    private String connectionFactoryName;
    private String username;
    private String password;

    public JmsConnector(BundleContext bc, String connectionFactoryName, String username, String password) throws JMSException {
        this.bc = bc;
        this.connectionFactoryName = connectionFactoryName;
        this.username = username;
        this.password = password;
    }
    
    private ServiceReference<ConnectionFactory> lookupConnectionFactory(String name) {
        Collection<ServiceReference<ConnectionFactory>> references;
        try {
            references = bc.getServiceReferences(ConnectionFactory.class, "(|(osgi.jndi.service.name=" + name + ")(name=" + name + ")(service.id=" + name + "))");
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException("Error finding connection factory service " + name, e);
        }
        if (references == null || references.size() == 0) {
            throw new IllegalArgumentException("No JMS connection factory found for " + name);
        }
        if (references.size() > 1) {
            throw new IllegalArgumentException("Multiple JMS connection factories found for " + name);
        }
        return references.iterator().next();
    }

    @Override
    public void close() throws IOException {
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

    public Connection connect() throws JMSException {
        reference = this.lookupConnectionFactory(connectionFactoryName);
        ConnectionFactory cf = (ConnectionFactory) bc.getService(reference);
        connection = cf.createConnection(username, password);
        connection.start();
        return connection;
    }

    public Session createSession() throws JMSException {
        return createSession(Session.AUTO_ACKNOWLEDGE);
    }

    public Session createSession(int acknowledgeMode) throws JMSException {
        if (connection == null) {
            connect();
        }
        if (acknowledgeMode == Session.SESSION_TRANSACTED) {
            session = connection.createSession(true, acknowledgeMode);
        } else {
            session = connection.createSession(false, acknowledgeMode);
        }
        return session;
    }

}
