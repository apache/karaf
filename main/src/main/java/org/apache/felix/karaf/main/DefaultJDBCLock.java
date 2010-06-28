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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Represents an exclusive lock on a database,
 * used to avoid multiple Karaf instances attempting
 * to become master.
 * 
 * @version $Revision: $
 */
public class DefaultJDBCLock implements Lock {

    final Logger LOG = Logger.getLogger(this.getClass().getName());
    
    private static final String PROPERTY_LOCK_URL               = "karaf.lock.jdbc.url";
    private static final String PROPERTY_LOCK_JDBC_DRIVER       = "karaf.lock.jdbc.driver";
    private static final String PROPERTY_LOCK_JDBC_USER         = "karaf.lock.jdbc.user";
    private static final String PROPERTY_LOCK_JDBC_PASSWORD     = "karaf.lock.jdbc.password";
    private static final String PROPERTY_LOCK_JDBC_TABLE        = "karaf.lock.jdbc.table";
    private static final String PROPERTY_LOCK_JDBC_CLUSTERNAME  = "karaf.lock.jdbc.clustername";
    private static final String PROPERTY_LOCK_JDBC_TIMEOUT      = "karaf.lock.jdbc.timeout";
    
    private static final String DEFAULT_PASSWORD = "";
    private static final String DEFAULT_USER = "";
    private static final String DEFAULT_TABLE = "KARAF_LOCK";
    private static final String DEFAULT_CLUSTERNAME = "karaf";
    private static final String DEFAULT_TIMEOUT = "10"; // in seconds

    final Statements statements;
    Connection lockConnection;
    String url;
    String driver;
    String user; 
    String password;
    String table;
    String clusterName;
    int timeout;

    public DefaultJDBCLock(Properties props) {
        LOG.addHandler(BootstrapLogManager.getDefaultHandler());
        
        this.url = props.getProperty(PROPERTY_LOCK_URL);
        this.driver = props.getProperty(PROPERTY_LOCK_JDBC_DRIVER);
        this.user = props.getProperty(PROPERTY_LOCK_JDBC_USER, DEFAULT_USER);
        this.password = props.getProperty(PROPERTY_LOCK_JDBC_PASSWORD, DEFAULT_PASSWORD);
        this.table = props.getProperty(PROPERTY_LOCK_JDBC_TABLE, DEFAULT_TABLE);
        this.clusterName = props.getProperty(PROPERTY_LOCK_JDBC_CLUSTERNAME, DEFAULT_CLUSTERNAME);
        this.timeout = Integer.parseInt(props.getProperty(PROPERTY_LOCK_JDBC_TIMEOUT, DEFAULT_TIMEOUT));
        
        this.statements = createStatements();
        
        init();
    }
    
    Statements createStatements() {
        Statements statements = new Statements();
        statements.setTableName(table);
        statements.setNodeName(clusterName);
        return statements;
    }
    
    void init() {
        try {
            createDatabase();
            createSchema();
        } catch (Exception e) {
            LOG.severe("Error occured while attempting to obtain connection: " + e);
        }
    }
    
    void createDatabase() {
        // do nothing in the default implementation
    }

    void createSchema() {
        if (schemaExists()) {
            return;
        }
        
        String[] createStatments = this.statements.getLockCreateSchemaStatements(getCurrentTimeMillis());
        Statement statement = null;
        
        try {
            statement = getConnection().createStatement();
            
            for (String stmt : createStatments) {
                statement.execute(stmt);
            }
            
            getConnection().commit();
        } catch (Exception e) {
            LOG.severe("Could not create schema: " + e );
        } finally {
            closeSafely(statement);
        }
    }

    boolean schemaExists() {
        ResultSet rs = null;
        boolean schemaExists = false;
        
        try {
            rs = getConnection().getMetaData().getTables(null, null, statements.getFullLockTableName(), new String[] {"TABLE"});
            schemaExists = rs.next();
        } catch (Exception ignore) {
            LOG.severe("Error testing for db table: " + ignore);
        } finally {
            closeSafely(rs);
        }
        
        return schemaExists;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.felix.karaf.main.Lock#lock()
     */
    public boolean lock() {
        boolean result = aquireLock();
        
        if (result) {
            result = updateLock();
        }
        
        return result;
    }
    
    boolean aquireLock() {
        String lockCreateStatement = statements.getLockCreateStatement();
        PreparedStatement preparedStatement = null;
        boolean lockAquired = false;
        
        try {
            preparedStatement = getConnection().prepareStatement(lockCreateStatement);
            preparedStatement.setQueryTimeout(timeout);
            lockAquired = preparedStatement.execute();
        } catch (Exception e) {
            LOG.warning("Failed to acquire database lock: " + e);
        }finally {
            closeSafely(preparedStatement);
        }
        
        return lockAquired;
    }

    boolean updateLock() {
        String lockUpdateStatement = statements.getLockUpdateStatement(getCurrentTimeMillis());
        PreparedStatement preparedStatement = null;
        boolean lockUpdated = false;
        
        try {
            preparedStatement = getConnection().prepareStatement(lockUpdateStatement);
            preparedStatement.setQueryTimeout(timeout);
            int rows = preparedStatement.executeUpdate();
            lockUpdated = (rows == 1);
        } catch (Exception e) {
            LOG.warning("Failed to update database lock: " + e);
        }finally {
            closeSafely(preparedStatement);
        }
        
        return lockUpdated;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.felix.karaf.main.Lock#release()
     */
    public void release() throws Exception {
        if (isConnected()) {
            try {
                getConnection().rollback();
            } catch (SQLException e) {
                LOG.severe("Exception while rollbacking the connection on release: " + e);
            } finally {
                try {
                    getConnection().close();
                } catch (SQLException ignored) {
                    LOG.fine("Exception while closing connection on release: " + ignored);
                }
            }
        }
        
        lockConnection = null;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.felix.karaf.main.Lock#isAlive()
     */
    public boolean isAlive() throws Exception {
        if (!isConnected()) { 
            LOG.severe("Lost lock!");
            return false; 
        }

        return updateLock();
    }
    
    boolean isConnected() throws SQLException {
        return lockConnection != null && !lockConnection.isClosed();
    }
    
    void closeSafely(Statement preparedStatement) {
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                LOG.severe("Failed to close statement: " + e);
            }
        }
    }
    
    void closeSafely(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                LOG.severe("Error occured while releasing ResultSet: " + e);
            }
        }
    }
    
    Connection getConnection() throws Exception {
        if (!isConnected()) {
            lockConnection = createConnection(driver, url, user, password);
            lockConnection.setAutoCommit(false);
        }
        
        return lockConnection;
    }

    /**
     * Create a new jdbc connection.
     * 
     * @param driver
     * @param url
     * @param username
     * @param password
     * @return a new jdbc connection
     * @throws Exception 
     */
    Connection createConnection(String driver, String url, String username, String password) throws Exception {
        if (url.toLowerCase().startsWith("jdbc:derby")) {
            url = (url.toLowerCase().contains("create=true")) ? url : url + ";create=true";
        }
        
        try {
            return doCreateConnection(driver, url, username, password);
        } catch (Exception e) {
            LOG.severe("Error occured while setting up JDBC connection: " + e);
            throw e; 
        }
    }

    /**
     * This method could be used to inject a mock jdbc connection for testing purposes.
     * 
     * @param driver
     * @param url
     * @param username
     * @param password
     * @return
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    Connection doCreateConnection(String driver, String url, String username, String password) throws ClassNotFoundException, SQLException {
        Class.forName(driver);
        // results in a closed connection in Derby if the update lock table request timed out
        // DriverManager.setLoginTimeout(timeout);
        return DriverManager.getConnection(url, username, password);
    }
    
    long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }
}