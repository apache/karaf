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
package org.apache.servicemix.kernel.main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Represents an exclusive lock on a database,
 * used to avoid multiple SMX instances attempting
 * to become master.
 * 
 * @version $Revision: $
 */
public class DefaultJDBCLock implements Lock {

    private static final String PROPERTY_LOCK_URL               = "servicemix.lock.jdbc.url";
    private static final String PROPERTY_LOCK_JDBC_DRIVER       = "servicemix.lock.jdbc.driver";
    private static final String PROPERTY_LOCK_JDBC_USER         = "servicemix.lock.jdbc.user";
    private static final String PROPERTY_LOCK_JDBC_PASSWORD     = "servicemix.lock.jdbc.password";
    private static final String PROPERTY_LOCK_JDBC_TABLE        = "servicemix.lock.jdbc.table";
    private static final String PROPERTY_LOCK_JDBC_CLUSTERNAME  = "servicemix.lock.jdbc.clustername";
    private static final String PROPERTY_LOCK_JDBC_TIMEOUT      = "servicemix.lock.jdbc.timeout";

    private final Statements statements;
    private Connection lockConnection;
    private String url;
    private String driver;
    private String user; 
    private String password;
    private String table;
    private String clusterName;
    private int timeout;

    public DefaultJDBCLock(Properties props) {
        this.url = props.getProperty(PROPERTY_LOCK_URL);
        this.driver = props.getProperty(PROPERTY_LOCK_JDBC_DRIVER);
        this.user = props.getProperty(PROPERTY_LOCK_JDBC_USER);
        this.password = props.getProperty(PROPERTY_LOCK_JDBC_PASSWORD);
        this.table = props.getProperty(PROPERTY_LOCK_JDBC_TABLE);
        this.clusterName = props.getProperty(PROPERTY_LOCK_JDBC_CLUSTERNAME);
        String time = props.getProperty(PROPERTY_LOCK_JDBC_TIMEOUT);
        this.lockConnection = null;
        if (table == null) { table = "SERVICEMIX_LOCK"; }
        if ( clusterName == null) { clusterName = "smx4"; }
        this.statements = new Statements(table, clusterName);
        if (time != null) { 
            this.timeout = Integer.parseInt(time) * 1000; 
        } else {
            this.timeout = 10000; // 10 seconds
        }
        if (user == null) { user = ""; }
        if (password == null) { password = ""; }
        try {
            obtainLock();
        } catch (Exception e) {
            System.err.println("Error occured while attempting to obtain connection: " + e.getMessage());
        }
    }

    /**
     * obtainLock - obtain the lock connection.
     *
     * @throws Exception
     */
    private void obtainLock() throws Exception {
        PreparedStatement statement = null;
        while (true) {
            try {
                lockConnection = getConnection(driver, url, user, password);
                lockConnection.setAutoCommit(false);
                statements.init(lockConnection);
                String sql = statements.testLockTableStatus();
                statement = lockConnection.prepareStatement(sql);
                statement.execute();
                break;
            } catch (Exception e) {
                System.err.println("Could not obtain lock: " + e.getMessage());
                Thread.sleep(this.timeout);
            } finally {
                if (null != statement) {
                    try {
                        statement.close();
                    } catch (SQLException e1) {
                        System.err.println("Caught while closing statement: " + e1.getMessage());
                    }
                    statement = null;
                }
            }
            Thread.sleep(this.timeout);
        }
        System.out.println("Connected to data source: " + url);
    }

    /**
     * lock - a KeepAlive function to maintain lock. 
     *
     * @return true if connection lock retained, false otherwise.
     */
    public boolean lock() {
        PreparedStatement statement = null;
        boolean result = false;
        try {
            if (lockConnection.isClosed()) { obtainLock(); } 
            long time = System.currentTimeMillis();
            statement = lockConnection.prepareStatement(statements.getLockUpdateStatement(time));
            int rows = statement.executeUpdate();
            if (rows == 1) {
                result=true;
            }
        } catch (Exception e) {
            System.err.println("Failed to acquire database lock: " + e.getMessage());
        }finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    System.err.println("Failed to close statement" + e);
                }
            }
        }
        return result;
    }

    /**
     * release - terminate the lock connection safely.
     */
    public void release() throws Exception {
        if (lockConnection != null && !lockConnection.isClosed()) {
            lockConnection.rollback();
            lockConnection.close();
        }
    }

    /**
     * isAlive - test if lock still exists.
     */
    public boolean isAlive() throws Exception {
        if (lockConnection == null) { return false; }
        PreparedStatement statement = null;
        try { 
            lockConnection.setAutoCommit(false);
            statements.init(lockConnection);
            String sql = statements.testLockTableStatus();
            statement = lockConnection.prepareStatement(sql);
            statement.execute();
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    /**
     * getConnection - Obtain connection to database via jdbc driver.
     *
     * @throws Exception
     * @param driver, the JDBC driver class.
     * @param url, url to data source.
     * @param username, user to access data source.
     * @param password, password for specified user.
     * @return connection, null returned if conenction fails.
     */
    private Connection getConnection(String driver, String url, 
                                     String username, String password) throws Exception {
        Connection conn = null;
        try {
            Class.forName(driver);
            conn = DriverManager.getConnection(url + ";create=true", username, password);
        } catch (Exception e) {
            throw e; 
        }
        return conn;
    }

}
