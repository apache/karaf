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
package org.apache.karaf.jdbc.internal;

import java.io.Closeable;
import java.io.IOException;
import java.sql.*;
import java.util.Deque;
import java.util.LinkedList;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;

import org.apache.karaf.util.StreamUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class JdbcConnector implements Closeable {
    private BundleContext bundleContext;
    private String datasourceName;
    private Connection connection;
    private Deque<Closeable> resources;
    private ServiceReference<?> reference;

    public JdbcConnector(BundleContext bundleContext, String datasourceName) {
        this.bundleContext = bundleContext;
        this.datasourceName = datasourceName;
        this.resources = new LinkedList<Closeable>();
    }
    
    public Connection connect() throws SQLException {
        reference = lookupDataSource(datasourceName);
        Object datasource = bundleContext.getService(reference);
        if (datasource instanceof DataSource) {
            connection = ((DataSource) datasource).getConnection();
        }
        if (datasource instanceof XADataSource) {
            connection = ((XADataSource) datasource).getXAConnection().getConnection();
        }
        return connection;
    }
    
    public Statement createStatement() throws SQLException {
        if (connection == null) {
            connect();
        }
        if (connection instanceof Connection) {
            return register(((Connection) connection).createStatement());
        }
        if (connection instanceof XAConnection) {
            return register(((XAConnection) connection).getConnection().createStatement());
        }
        return null;
    }

    public Connection register(final Connection connection) {
        resources.addFirst(new Closeable() {
            
            @Override
            public void close() throws IOException {
                try {
                    connection.close();
                } catch (SQLException e) {
                    // Ignore
                }
            }
        });
        return connection;
    }

    public Statement register(final Statement statement) {
        resources.addFirst(new Closeable() {
            
            @Override
            public void close() throws IOException {
                try {
                    statement.close();
                } catch (SQLException e) {
                    // Ignore
                }
            }
        });
        return statement;
    }

    public ResultSet register(final ResultSet resultSet) {
        resources.addFirst(new Closeable() {
            
            @Override
            public void close() throws IOException {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    // Ignore
                }
            }
        });
        return resultSet;
    }
    

    private ServiceReference<?> lookupDataSource(String name) {
        ServiceReference<?>[] references;
        try {
            references = bundleContext.getServiceReferences((String) null,
                    "(&(|(" + Constants.OBJECTCLASS + "=" + DataSource.class.getName() + ")"
                    + "(" + Constants.OBJECTCLASS + "=" + XADataSource.class.getName() + "))"
                    + "(|(osgi.jndi.service.name=" + name + ")(datasource=" + name + ")(name=" + name + ")(service.id=" + name + ")))");
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Error finding datasource with name " + name, e);
        }
        if (references == null || references.length == 0) {
            throw new IllegalArgumentException("No JDBC datasource found for " + name);
        }
        if (references.length > 1) {
            throw new IllegalArgumentException("Multiple JDBC datasource found for " + name);
        }
        return references[0];
    }

    @Override
    public void close() {
        StreamUtils.close(resources.toArray(new Closeable[]{}));
        if (reference != null) {
            bundleContext.ungetService(reference);
        }
    }

}
