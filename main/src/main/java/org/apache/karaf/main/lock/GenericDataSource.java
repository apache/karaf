/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.main.lock;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

public class GenericDataSource implements DataSource {

    private static final String DRIVER_MANAGER_USER_PROPERTY = "user";
    private static final String DRIVER_MANAGER_PASSWORD_PROPERTY = "password";

    private final String driverClass;
    private final String url;
    private final Properties properties;
    private final int validTimeoutMs;

    private final Queue<Connection> cache;

    private volatile Driver driver;
    private volatile boolean driverClassLoaded;

    public GenericDataSource(String driverClass, String url, String user, String password, boolean cache, int validTimeoutMs) {
        this.driverClass = driverClass;
        this.url = url;
        this.properties = new Properties();
        if (user != null) {
            properties.setProperty(DRIVER_MANAGER_USER_PROPERTY, user);
        }
        if (password != null) {
            properties.setProperty(DRIVER_MANAGER_PASSWORD_PROPERTY, password);
        }
        this.validTimeoutMs = validTimeoutMs;
        this.cache = cache ? new ConcurrentLinkedQueue<>() : null;
    }

    private void ensureDriverLoaded() throws SQLException {
        try {
            if (!driverClassLoaded) {
                synchronized (this) {
                    if (!driverClassLoaded) {
                        if (driverClass != null) {
                            Class.forName(driverClass);
                        }
                        driverClassLoaded = true;
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("Unable to load driver class " + driverClass, e);
        }
    }

    private Driver driver() throws SQLException {
        if (driver == null) {
            synchronized (this) {
                if (driver == null) {
                    driver = DriverManager.getDriver(url);
                }
            }
        }
        return driver;
    }

    public Connection getConnection() throws SQLException {
        ensureDriverLoaded();
        while (true) {
            Connection con = cache != null ? cache.poll() : null;
            if (con == null) {
                con = driver().connect(url, properties);
                if (con == null) {
                    throw new SQLException("Invalid jdbc URL '" + url + "' for driver " + driver());
                }
            } else {
                if (!con.isValid(validTimeoutMs)) {
                    con.close();
                    con = null;
                }
            }
            if (con != null) {
                return wrap(con);
            }
        }
    }

    private Connection wrap(Connection con) {
        return (Connection) Proxy.newProxyInstance(con.getClass().getClassLoader(), new Class[] { Connection.class }, new InvocationHandler() {
            private boolean closed = false;
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getName().equals("close") && method.getParameterCount() == 0) {
                    closed = true;
                    if (!cache.offer(con)) {
                        con.close();
                    }
                    return null;
                } else if (method.getName().equals("isClosed") && method.getParameterCount() == 0) {
                    return closed;
                } else {
                    if (closed) {
                        throw new SQLException("Connection closed");
                    }
                    return method.invoke(con, args);
                }
            }
        });
    }

    public Connection getConnection(String username, String password) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
    }

    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    public void setLoginTimeout(int seconds) throws SQLException {
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

}
