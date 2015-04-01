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
package org.apache.karaf.main;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents an exclusive lock on a database,
 * used to avoid multiple Karaf instances attempting
 * to become master.
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
        try {
            LOG.addHandler(BootstrapLogManager.getDefaultHandler());
        } catch (Exception e) {
            e.printStackTrace();
        }

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

    /**
     * This method is called to create an instance of the Statements instance.
     *
     * @return an instance of a Statements object
     */
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
            LOG.log(Level.SEVERE, "Error occurred while attempting to obtain connection", e);
        }
    }

    void createDatabase() {
        // do nothing in the default implementation
    }

    /**
     * This method is called to check and create the required schemas that are used by this instance.
     */
    void createSchema() {
        if (schemaExists()) {
            return;
        }
        
        String[] createStatments = this.statements.getLockCreateSchemaStatements(getCurrentTimeMillis());
        Statement statement = null;
        Connection connection = null;

        try {
            connection = getConnection();
            statement = connection.createStatement();

            for (String stmt : createStatments) {
                LOG.info("Executing statement: " + stmt);
                statement.execute(stmt);
            }
            
            getConnection().commit();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Could not create schema", e);
            try {
                // Rollback transaction if and only if there was a failure...
                if (connection != null)
                    connection.rollback();
            } catch (Exception ie) {
                // Do nothing....
            }
        } finally {
            closeSafely(statement);
        }
    }

    /**
     * This method is called to determine if the required database schemas have already been created or not.
     *
     * @return true, if the schemas are available else false.
     */
    boolean schemaExists() {
        return schemaExist(statements.getFullLockTableName());
    }

    /**
     * This method is called to determine if the required table is available or not.
     *
     * @param tableName  The name of the table to determine if it exists
     *
     * @return true, if the table exists else false
     */
    boolean schemaExist(String tableName) {
        ResultSet rs = null;
        boolean schemaExists = false;
        try {
            DatabaseMetaData metadata = getConnection().getMetaData();
            rs = metadata.getTables(null, null, tableName, new String[]{"TABLE"});
            schemaExists = rs.next();
            if (!schemaExists) {
                rs = metadata.getTables(null, null, tableName.toLowerCase(), new String[] {"TABLE"});
                schemaExists = rs.next();
            }
            if (!schemaExists) {
                rs = metadata.getTables(null, null, tableName.toUpperCase(), new String[] {"TABLE"});
                schemaExists = rs.next();
            }
        } catch (Exception ignore) {
            LOG.log(Level.SEVERE, "Error testing for db table", ignore);
        } finally {
            closeSafely(rs);
        }
        return schemaExists;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.karaf.main.Lock#lock()
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
            // Do we want to display this message everytime???
            LOG.log(Level.WARNING, "Failed to acquire database lock", e);
        } finally {
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
            LOG.log(Level.WARNING, "Failed to update database lock", e);
        } finally {
            closeSafely(preparedStatement);
        }
        
        return lockUpdated;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.karaf.main.Lock#release()
     */
    public void release() throws Exception {
        if (isConnected()) {
            try {
                getConnection().rollback();
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Exception while rollbacking the connection on release", e);
            } finally {
                try {
                    getConnection().close();
                } catch (SQLException ignored) {
                    LOG.log(Level.FINE, "Exception while closing connection on release", ignored);
                }
            }
        }
        
        lockConnection = null;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.karaf.main.Lock#isAlive()
     */
    public boolean isAlive() throws Exception {
        if (!isConnected()) { 
            LOG.severe("Lost lock!");
            return false; 
        }

        return updateLock();
    }

    /**
     * This method is called to determine if this instance jdbc connection is
     * still connected.
     *
     * @return true, if the connection is still connected else false
     *
     * @throws SQLException
     */
    boolean isConnected() throws SQLException {
        return lockConnection != null && !lockConnection.isClosed();
    }

    /**
     * This method is called to safely close a Statement.
     *
     * @param preparedStatement The statement to be closed
     */
    void closeSafely(Statement preparedStatement) {
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Failed to close statement", e);
            }
        }
    }

    /**
     * This method is called to safely close a ResultSet instance.
     *
     * @param rs The result set to be closed
     */
    void closeSafely(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Error occured while releasing ResultSet", e);
            }
        }
    }

    /**
     * This method will return an active connection for this given jdbc driver.
     *
     * @return jdbc Connection instance
     *
     * @throws Exception
     */
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
            LOG.log(Level.SEVERE, "Error occured while setting up JDBC connection", e);
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
