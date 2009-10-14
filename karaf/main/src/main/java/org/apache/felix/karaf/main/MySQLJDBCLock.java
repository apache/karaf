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
package org.apache.felix.karaf.main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Represents an exclusive lock on a database,
 * used to avoid multiple Karaf instances attempting
 * to become master.
 * 
 * @version $Revision: $
 */
public class MySQLJDBCLock implements Lock {

    private static final Logger LOG = Logger.getLogger(MySQLJDBCLock.class.getName());
    private static final String PROPERTY_LOCK_URL               = "karaf.lock.jdbc.url";
    private static final String PROPERTY_LOCK_JDBC_DRIVER       = "karaf.lock.jdbc.driver";
    private static final String PROPERTY_LOCK_JDBC_USER         = "karaf.lock.jdbc.user";
    private static final String PROPERTY_LOCK_JDBC_PASSWORD     = "karaf.lock.jdbc.password";
    private static final String PROPERTY_LOCK_JDBC_TABLE        = "karaf.lock.jdbc.table";
    private static final String PROPERTY_LOCK_JDBC_CLUSTERNAME  = "karaf.lock.jdbc.clustername";
    private static final String PROPERTY_LOCK_JDBC_TIMEOUT      = "karaf.lock.jdbc.timeout";

    private final Statements statements;
    private Connection lockConnection;
    private String url;
    private String database;
    private String driver;
    private String user; 
    private String password;
    private String table;
    private String clusterName;
    private int timeout;

    public MySQLJDBCLock(Properties props) {
        LOG.addHandler(BootstrapLogManager.getDefaultHandler());
        this.url = props.getProperty(PROPERTY_LOCK_URL);
        this.driver = props.getProperty(PROPERTY_LOCK_JDBC_DRIVER);
        this.user = props.getProperty(PROPERTY_LOCK_JDBC_USER);
        this.password = props.getProperty(PROPERTY_LOCK_JDBC_PASSWORD);
        this.table = props.getProperty(PROPERTY_LOCK_JDBC_TABLE);
        this.clusterName = props.getProperty(PROPERTY_LOCK_JDBC_CLUSTERNAME);
        String time = props.getProperty(PROPERTY_LOCK_JDBC_TIMEOUT);
        this.lockConnection = null;
        if (table == null) { table = "KARAF_LOCK"; }
        if ( clusterName == null) { clusterName = "karaf"; }
        if (time != null) { 
            this.timeout = Integer.parseInt(time) * 1000; 
        } else {
            this.timeout = 10000; // 10 seconds
        }
        if (user == null) { user = ""; }
        if (password == null) { password = ""; }

        int db = props.getProperty(PROPERTY_LOCK_URL).lastIndexOf("/");
        this.url = props.getProperty(PROPERTY_LOCK_URL).substring(0, db);
        this.database = props.getProperty(PROPERTY_LOCK_URL).substring(db +1);
        this.statements = new Statements(database, table, clusterName);
        testDB();
    }

    /**
     * testDB - ensure specified database exists.
     *
     */
    private void testDB() {
        try {
            lockConnection = getConnection(driver, url, user, password);
            lockConnection.setAutoCommit(false);
            statements.init(lockConnection, database);
        } catch (Exception e) {
            LOG.severe("Error occured while attempting to obtain connection: " + e + " " + e.getMessage());
        } finally {
            try {
                lockConnection.close();
                lockConnection = null;
            } catch (Exception f) {
                LOG.severe("Error occured while cleaning up connection: " + f + " " + f.getMessage());
            }
        }
    }

    /**
     * setUpdateCursor - Send Update directive to data base server.
     *
     * @throws Exception
     */
    private boolean setUpdateCursor() throws Exception {
        PreparedStatement statement = null;
        boolean result = false;
        try { 
            if ((lockConnection == null) || (lockConnection.isClosed())) { 
                LOG.fine("DefaultJDBCLock#setUpdateCursor:: connection: " + url + "/" + database );
                lockConnection = getConnection(driver, url + "/" + database, user, password);
                lockConnection.setAutoCommit(false);
                statements.init(lockConnection);
            } else {
                LOG.fine("MySQLJDBCLock#setUpdateCursor:: connection already established.");
                return true; 
            }
            String sql = "LOCK TABLES " + database + "." + table + " WRITE";
            statement = lockConnection.prepareStatement(sql);
            result = statement.execute();
        } catch (Exception e) {
            LOG.warning("Could not obtain connection: " + e.getMessage());
        } finally {
            if (null != statement) {
                try {
                    LOG.severe("Cleaning up DB connection.");
                    statement.close();
                } catch (SQLException e1) {
                    LOG.severe("Caught while closing statement: " + e1.getMessage());
                }
                statement = null;
            }
        }
        LOG.info("Connected to data source: " + url + " With RS: " + result);
        return result;
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
            if (!setUpdateCursor()) {
                LOG.severe("Could not set DB update cursor");
                return result;
            }
            LOG.fine("MySQLJDBCLock#lock:: have set Update Cursor, now do update");
            long time = System.currentTimeMillis();
            statement = lockConnection.prepareStatement(statements.getLockUpdateStatement(time));
            int rows = statement.executeUpdate();
            LOG.fine("MySQLJDBCLock#lock:: Number of update rows: " + rows);
            if (rows >= 1) {
                result=true;
            }
        } catch (Exception e) {
            LOG.warning("Failed to acquire database lock: " + e.getMessage());
        }finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    LOG.severe("Failed to close statement" + e);
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
            lockConnection = null;
        }
    }

    /**
     * isAlive - test if lock still exists.
     */
    public boolean isAlive() throws Exception {
        if ((lockConnection == null) || (lockConnection.isClosed())) { 
            LOG.severe("Lost lock!");
            return false; 
        }
        PreparedStatement statement = null;
        boolean result = true;
        try { 
            long time = System.currentTimeMillis();
            statement = lockConnection.prepareStatement(statements.getLockUpdateStatement(time));
            int rows = statement.executeUpdate();
            if (rows < 1) {
                result = false;
            }
        } catch (Exception ex) {
            LOG.severe("Error occured while testing lock: " + ex + " " + ex.getMessage());
            return false;
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (Exception ex1) {
                    LOG.severe("Error occured after testing lock: " + ex1.getMessage());
                }
            }
        }
        return result;
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
            conn = DriverManager.getConnection(url, username, password);
        } catch (Exception e) {
            LOG.severe("Error occured while setting up JDBC connection: " + e);
            throw e; 
        }
        return conn;
    }

}
