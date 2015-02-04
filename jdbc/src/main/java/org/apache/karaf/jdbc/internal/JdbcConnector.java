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
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Deque;
import java.util.LinkedList;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class JdbcConnector implements Closeable {

    private CommonDataSource datasource;
    private Connection connection;
    private Deque<AutoCloseable> resources;

    public JdbcConnector(final BundleContext bundleContext, final ServiceReference<?> reference) {
        this.datasource = (CommonDataSource)bundleContext.getService(reference);
        this.resources = new LinkedList<>();
        this.resources.addFirst(new AutoCloseable() {
            @Override
            public void close() throws Exception {
                bundleContext.ungetService(reference);
            }
        });
    }
    
    public Connection connect() throws SQLException {
        if (connection == null) {
            if (datasource instanceof DataSource) {
                connection = ((DataSource) datasource).getConnection();
            } else if (datasource instanceof XADataSource) {
                connection = register(((XADataSource) datasource).getXAConnection()).getConnection();
            } else {
                throw new IllegalStateException("Datasource is not an instance of DataSource nor XADataSource");
            }
            register(connection);
        }
        return connection;
    }
    
    public Statement createStatement() throws SQLException {
        return register(connect().createStatement());
    }

    public <T extends AutoCloseable> T register(final T closeable) {
        resources.addFirst(closeable);
        return closeable;
    }

    public XAConnection register(final XAConnection closeable) {
        register(new AutoCloseable() {
            @Override
            public void close() throws Exception {
                closeable.close();
            }
        });
        return closeable;
    }

    @Override
    public void close() {
        for (AutoCloseable closeable : resources) {
            try {
                closeable.close();
            } catch (Throwable t) {
                // Ignore
            }
        }
    }

}
