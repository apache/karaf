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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;

import javax.sql.DataSource;

import org.apache.karaf.util.StreamUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class JdbcConnector implements Closeable {
    private BundleContext bundleContext;
    private String datasourceName;
    private Connection connection;
    private Deque<Closeable> resources;
    private ServiceReference<DataSource> reference;

    public JdbcConnector(BundleContext bundleContext, String datasourceName) {
        this.bundleContext = bundleContext;
        this.datasourceName = datasourceName;
        this.resources = new LinkedList<Closeable>();
    }
    
    public Connection connect() throws SQLException {
        reference = lookupDataSource(datasourceName);
        DataSource ds = (DataSource) bundleContext.getService(reference);
        connection = ds.getConnection();
        return connection;
    }
    
    public Statement createStatement() throws SQLException {
        if (connection == null) {
            connect();
        }
        return register(connection.createStatement());
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
    

    private ServiceReference<DataSource> lookupDataSource(String name) {
        Collection<ServiceReference<DataSource>> references;
        try {
            references = bundleContext.getServiceReferences(DataSource.class, "(|(osgi.jndi.service.name=" + name + ")(datasource=" + name + ")(name=" + name + ")(service.id=" + name + "))");
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Error finding datasource with name " + name, e);
        }
        if (references == null || references.size() == 0) {
            throw new IllegalArgumentException("No JDBC datasource found for " + name);
        }
        if (references.size() > 1) {
            throw new IllegalArgumentException("Multiple JDBC datasource found for " + name);
        }
        return references.iterator().next();
    }

    @Override
    public void close() {
        StreamUtils.close(resources.toArray(new Closeable[]{}));
        if (reference != null) {
            bundleContext.ungetService(reference);
        }
    }

}
