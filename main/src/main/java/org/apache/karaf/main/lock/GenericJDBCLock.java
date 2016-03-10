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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.felix.utils.properties.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.karaf.main.ConfigProperties;
import org.apache.karaf.main.util.BootstrapLogManager;

/**
 * <p>This class is the base class used to provide a master/slave configuration for
 * a given set of active karaf instances using JDBC. </p>
 *
 * <p>This implementation uses two different tables.  A KARAF_NODE_ID, and KARAF_LOCK tables.  The
 * KARAF_NODE_ID table is used to generate a unique id for each instance in the cluster.  While
 * the KARAF_LOCK table is used to determine who is the master of these instances. </p>
 *
 * <p>The tables configurations for the different tables are. </p>
 *
 * <pre>
 *   CREATE TABLE KARAF_NODE_ID ( ID INTEGER DEFAULT 0 )
 *   CREATE TABLE KARAF_LOCK ( ID INTEGER DEFAULT 0, STATE INTEGER DEFAULT 0, LOCK_DELAY INTEGER DEFAULT 0 )
 * </pre>
 *
 * <p>The two tables will include a single row each that is created by a single instance in the cluster. </p>
 *
 * <p>The KARAF_NODE_ID table will be updated once for each active karaf instance with there unique id compared
 * to the other instances within the cluster.  The single row will contain the next available unique id and
 * will not include each clustered instance unique id since these instances can come and go throughout the
 * system lifetime. </p>
 *
 * <p>The KARAF_LOCK table will be used to determine which of the instances will become the master. The master
 * will set the STATE to an initial value and the LOCK_DELAY to a time in milliseconds of when the
 * table will be updated.  It is the responsibility of the master instance to update the STATE field by the
 * allocated lock delay by incrementing the state value.  If the STATE value has not been updated by the 
 * LOCK_DELAY time then a slave has permission to attempt to become the master. </p>
 *
 * <p>While the overview does not describe exactly how this is implemented.  Here is a description of how this
 * is done and what is provides as a fail safe solution. </p>
 *
 * <p>Each instance of this class provides an initialization step, a lock, isAlive and release interface. </p>
 *
 * <p>INITIALIZE:</p>
 *
 * <p>During the initialization step it will determine if the given tables exist within the database.  We only
 * check for a single table since we assume that if one is available then the other must exist. We then
 * add a row to each of the tables.  The added row to the KARAF_NODE_ID table will set the ID to zero since 
 * this is consider a non-existent karaf instance.  The added row to the KARAF_LOCK will set the ID to zero
 * which allows a karaf instances to acquire the lock and become the master instance. </p>
 *
 *
 * <p>LOCK:</p>
 *
 * <p>The current instance will try to acquire the master lock by using the following sql statement. </p>
 *
 * <pre>
 *   UPDATE KARAF_LOCK SET ID = unique_id, STATE = state, LOCK_DELAY = lock_delay 
 *       WHERE ID = 0 OR ID = curId
 * </pre>
 *
 * <p>Now you must be asking why are we using this update statement? The reason is that the statement will
 * guarantee that only one instance will be able to update this row.  The curId is set to this instance
 * unique id or to the prior master unique id if the row was not updated within that master lock_delay. </p>
 *
 * <p>The current update command will set the curId to this instance unique id.  If this fails then it will
 * determine if the current master has not updated the row within its lock_delay.  If it hasn't updated
 * the row within the allocated time then this instance can try to become the master. </p>
 *
 * <p>The current slave instance will then try to steal the lock from the master instance.  Why are we trying
 * to steal the lock from the master?  The reason is that it is possible that the master instance had a 
 * hard failure and there is no mechanisms to determine if that is the case. We then assume that it has 
 * crashed without releasing the lock gracefully.  The slave instance then used the following update statement. </p>
 *
 * <pre>
 *   UPDATE KARAF_LOCK SET ID = unique_id, STATE = state, LOCK_DELAY = lock_delay 
 *       WHERE ( ID = 0 OR ID = curId ) AND STATE = curState
 * </pre>
 *
 * <p>Now why are we using the state value as part of the where clause?  The reason that even though the row was
 * not updated by the allocated delay time.  It is possible that the update statement was performed just after 
 * the current slave check.  This update will insure that the row will be updated if and only if the state was
 * also not updated.  It is possible that the master instance updated the row after the current slave check 
 * and we do not want the slave to update the row and make itself the master.  This will insure that that will
 * not be the case. </p>
 *
 * <p>ISALIVE: </p>
 *
 * <p>This just checks if the connection is active and then just updates the row's STATE by using the lock
 * update call mentioned above. </p>
 *
 * <p>RELEASE: </p>
 *
 * <p>The release process just updates the KARAF_LOCK ID to zero so that other instances will have a chance
 * to become the master. </p>
 *
 * <p>There are two main scenarios that we need to worry about.  Soft and Hard failures.  The soft failure
 * basically allows the master instance to release the master lock and allow other instances to become
 * the master.  As for a hard failure, the current karaf instance crashes and does not release the lock
 * then the other karaf instances will notice that the KARAF_LOCK has not been updated for the current
 * master id and then they can compete for the master lock. </p>
 */
public class GenericJDBCLock implements Lock {

    final Logger LOG = Logger.getLogger(this.getClass().getName());

    public static final String PROPERTY_LOCK_URL               = "karaf.lock.jdbc.url";
    public static final String PROPERTY_LOCK_JDBC_DRIVER       = "karaf.lock.jdbc.driver";
    public static final String PROPERTY_LOCK_JDBC_USER         = "karaf.lock.jdbc.user";
    public static final String PROPERTY_LOCK_JDBC_PASSWORD     = "karaf.lock.jdbc.password";
    public static final String PROPERTY_LOCK_JDBC_TABLE        = "karaf.lock.jdbc.table";
    public static final String PROPERTY_LOCK_JDBC_TABLE_ID     = "karaf.lock.jdbc.table_id";
    public static final String PROPERTY_LOCK_JDBC_CLUSTERNAME  = "karaf.lock.jdbc.clustername";

    public static final String DEFAULT_PASSWORD = "";
    public static final String DEFAULT_USER = "";
    public static final String DEFAULT_TABLE = "KARAF_LOCK";
    public static final String DEFAULT_TABLE_ID = "KARAF_NODE_ID";
    public static final String DEFAULT_CLUSTERNAME = "karaf";

    final GenericStatements statements;
    Connection lockConnection;
    String url;
    String driver;
    String user;
    String password;
    String table;
    String clusterName;
    String table_id;
    int lock_delay;

    // My lock settings
    private int uniqueId = 0;
    private int state = 0;

    // Current master instance lock settings
    private int currentId = 0;
    private int currentState = 0;

    // The last clock time that the master instance state was updated as noticed by this instance.
    private long currentStateTime;
    // The master lock delay time in milliseconds that the master is expected to update the karaf_lock 
    // table state
    private int currentLockDelay;

    public GenericJDBCLock(Properties props) {
        BootstrapLogManager.configureLogger(LOG);
        this.url = props.getProperty(PROPERTY_LOCK_URL);
        this.driver = props.getProperty(PROPERTY_LOCK_JDBC_DRIVER);
        this.user = props.getProperty(PROPERTY_LOCK_JDBC_USER, DEFAULT_USER);
        this.password = props.getProperty(PROPERTY_LOCK_JDBC_PASSWORD, DEFAULT_PASSWORD);
        this.table = props.getProperty(PROPERTY_LOCK_JDBC_TABLE, DEFAULT_TABLE);
        this.clusterName = props.getProperty(PROPERTY_LOCK_JDBC_CLUSTERNAME, DEFAULT_CLUSTERNAME);
        this.table_id = props.getProperty(PROPERTY_LOCK_JDBC_TABLE_ID, DEFAULT_TABLE_ID);
        this.lock_delay = Integer.parseInt(props.getProperty(ConfigProperties.PROPERTY_LOCK_DELAY, ConfigProperties.DEFAULT_LOCK_DELAY));

        this.statements = createStatements();

        init();
    }
    
    /**
     * This method is called to create an instance of the JDBCStatements instance.
     *
     * @return An instance of a JDBCStatement object.
     */
    GenericStatements createStatements() {
        GenericStatements statements = new GenericStatements(table, table_id, clusterName);
        return statements;
    }
    
    void init() {
        try {
            createDatabase();
            createSchema();
            generateUniqueId();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error occured while attempting to obtain connection", e);
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

        String[] createStatments = this.statements.getLockCreateSchemaStatements(System.currentTimeMillis());
        Statement statement = null;
        Connection connection = null;

        try {
            connection = getConnection();
            statement = connection.createStatement();
            connection.setAutoCommit(false);

            for (String stmt : createStatments) {
                LOG.info("Executing statement: " + stmt);
                statement.execute(stmt);
            }

            connection.commit();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Could not create schema", e );
            try {
                // Rollback transaction if and only if there was a failure...
                if (connection != null)
                    connection.rollback();
            } catch (Exception ie) {
                // Do nothing....
            }
        } finally {
            closeSafely(statement);
            try {
                // Reset the auto commit to true
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
                LOG.log(Level.FINE, "Exception while setting the connection auto commit", ignored);
            }
        }
    }

    /**
     * This method is called to determine if the required database schemas have already been created or not.
     *
     * @return True, if the schemas are available else false.
     */
    boolean schemaExists() {
        return schemaExist(this.statements.getLockTableName())
                && schemaExist(this.statements.getLockIdTableName());
    }

    /**
     * This method is called to determine if the required table is available or not.
     *
     * @param tableName The name of the table to determine if it exists.
     * @return True, if the table exists else false.
     */
    private boolean schemaExist(String tableName) {
        ResultSet rs = null;
        boolean schemaExists = false;
        try {
            rs = getConnection().getMetaData().getTables(null, null, tableName, new String[] {"TABLE"});
            schemaExists = rs.next();
        } catch (Exception ignore) {
            LOG.log(Level.SEVERE, "Error testing for db table", ignore);
        } finally {
            closeSafely(rs);
        }
        return schemaExists;
    }

    /**
     * This method will generate a unique id for this instance that is part of an active set of instances.
     * This method uses a simple algorithm to insure that the id will be unique for all cases.
     */
    void generateUniqueId() {
        boolean uniqueIdSet = false;
        String selectString = this.statements.getLockIdSelectStatement();
        PreparedStatement selectStatement = null, updateStatement = null;
        try {
            selectStatement = getConnection().prepareStatement(selectString);

            // This loop can only be performed for so long and the chances that this will be 
            // looping for more than a few times is unlikely since there will always be at
            // least one instance that is successful.
            while (!uniqueIdSet) {

                ResultSet rs = null;

                try {
                    // Get the current ID from the karaf ids table
                    rs = selectStatement.executeQuery();

                    // Check if we were able to retrieve the result...
                    if (rs.next()) {
                        // Update the row with the next available id
                        int currentId = this.statements.getIdFromLockIdSelectStatement(rs);
                        
						String updateString = this.statements.getLockIdUpdateIdStatement(currentId + 1, currentId);

						updateStatement = getConnection().prepareStatement(updateString);
                        
                        int count = updateStatement.executeUpdate();
                        
                        // Set the uniqueId if and only if is it greater that zero
                        uniqueId = ( uniqueIdSet = count > 0 ) ? currentId + 1 : 0;
                        
                        if (count > 1) {
                            LOG.severe("OOPS there are more than one row within the table ids...");
                        }
                    } else {
                        LOG.severe("No rows were found....");
                    }
                } catch (SQLException e) {
                    LOG.log(Level.SEVERE, "Received an SQL exception while processing result set", e);
                } finally {
                    this.closeSafely(rs);
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Received an SQL exception while generating a prepate statement", e);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Received an exception while trying to get a reference to a connection", e);
        } finally {
            closeSafely(selectStatement);
        }
        LOG.info("INSTANCE unique id: " + uniqueId);
    }
    
    /**
     * This method is called to determine if this instance JDBC connection is
     * still connected.
     *
     * @return True, if the connection is still connected else false.
     * @throws SQLException If an SQL error occurs while checking if the lock is connected to the database.
     */
    boolean isConnected() throws SQLException {
        return lockConnection != null && !lockConnection.isClosed();
    }

    /**
     * This method is called to safely close a Statement.
     *
     * @param preparedStatement The statement to be closed.
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
     * @param rs The result set to be closed.
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
     * @return The JDBC connection instance
     * @throws Exception If the JDBC connection can't be retrieved.
     */
    protected Connection getConnection() throws Exception {
        if (!isConnected()) {
            lockConnection = createConnection(driver, url, user, password);
        }

        return lockConnection;
    }

    /**
     * Create a new JDBC connection.
     *
     * @param driver The fully qualified driver class name.
     * @param url The database connection URL.
     * @param username The username for the database.
     * @param password  The password for the database.
     * @return a new JDBC connection.
     * @throws Exception If the JDBC connection can't be created.
     */
    protected Connection createConnection(String driver, String url, String username, String password) throws Exception {
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
     * This method could be used to inject a mock JDBC connection for testing purposes.
     *
     * @param driver The fully qualified driver class name.
     * @param url The database connection URL.
     * @param username The username for the database.
     * @param password The password for the database.
     * @return a new JDBC connection.
     * @throws ClassNotFoundException If the JDBC driver class is not found.
     * @throws SQLException If the JDBC connection can't be created.
     */
    protected Connection doCreateConnection(String driver, String url, String username, String password) throws ClassNotFoundException, SQLException {
        Class.forName(driver);
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * This method is called whenever we want to acquire/steal or update the lock.
     * The different option depend if we are the competing for the master or the lock delay time
     * has been exceeded or that we are the master and are update the state.
     *
     * @return True, if we are the master instance else false.
     *
     * @see org.apache.karaf.main.lock.Lock#lock()
     */
    public boolean lock() throws Exception {
       
        // Try to acquire/update the lock state
        boolean lockAquired = acquireLock(statements.getLockUpdateIdStatement(uniqueId, ++state, lock_delay, uniqueId));
        
        if (!lockAquired) {
            
            String lockSelectStatement = statements.getLockSelectStatement();
            PreparedStatement statement = null;
            ResultSet rs = null;
            
            try {
                statement = getConnection().prepareStatement(lockSelectStatement);
                // Get the current master id and compare with information that we have locally....
                rs = statement.executeQuery();
                
                if (rs.next()) {
                    int currentId = statements.getIdFromLockSelectStatement(rs); // The current master unique id or 0
                    int currentState = statements.getStateFromLockSelectStatement(rs); // The current master state or whatever
                    
                    if (this.currentId == currentId) {
                        // It is the same instance that locked the table
                        if (this.currentState == currentState) {
                            // Its state has not been updated....
                            if ( (this.currentStateTime + this.currentLockDelay + this.currentLockDelay) < System.currentTimeMillis() ) {
                                // The state was not been updated for more than twice the lock_delay value of the current master...
                                // Try to steal the lock....
                                lockAquired = acquireLock(statements.getLockUpdateIdStatementToStealLock(uniqueId, state, lock_delay, currentId, currentState));
                            }
                        } else {
                            // Set the current time to be used to determine if we can 
                            // try to steal the lock later...
                            this.currentStateTime = System.currentTimeMillis();
                            this.currentState = currentState;
                        }
                    } else {
                        // This is a different currentId that is being used...
                        // at this time, it does not matter if the new master id is zero we can try to acquire it
                        // during the next lock call...
                        this.currentId = currentId;
                        this.currentState = currentState;
                        // Update the current state time since this is a new lock service...
                        this.currentStateTime = System.currentTimeMillis();
                        // Get the lock delay value which is specific to the current master...
                        this.currentLockDelay = statements.getLockDelayFromLockSelectStatement(rs);
                    }
                }
            } catch( Exception e ) {
                LOG.log(Level.SEVERE, "Unable to determine if the lock was obtain", e);
            } finally {
                closeSafely(statement);
                closeSafely(rs);
            }
        }
        
        return lockAquired;
    }

    /**
     * This method is called to try and acquire the lock and/or update the state for when this instance
     * is already the master instance.  It will try to update the row given the passed data and will
     * succeed if and only if the generated where clause was valid else it would not update the row.
     *
     * @param lockUpdateIdStatement The sql statement used to execute the update.
     * @return True, if the row was updated else false.
     */
    private boolean acquireLock(String lockUpdateIdStatement) {
        PreparedStatement preparedStatement = null;
        boolean lockAquired = false;
        
        try {
            preparedStatement = getConnection().prepareStatement(lockUpdateIdStatement);
            // This will only update the row that contains the ID of 0 or curId
            lockAquired = preparedStatement.executeUpdate() > 0;
        } catch (Exception e) {
            // Do we want to display this message everytime???
            LOG.log(Level.WARNING, "Failed to acquire database lock", e);
        } finally {
            closeSafely(preparedStatement);
        }
        
        return lockAquired;
    }

    /**
     * This method will release the lock that the current master has by setting the karaf_lock table
     * id to 0.  This tells the others that the master has relinquished the lock and someone else can
     * try to acquire that lock and become a master.
     *
     * @see org.apache.karaf.main.lock.Lock#release()
     */
    public void release() throws Exception {
        if (isConnected()) {
            String lockResetIdStatement = statements.getLockResetIdStatement(uniqueId);
            PreparedStatement preparedStatement = null;
            
            try {
                preparedStatement = getConnection().prepareStatement(lockResetIdStatement);
                // This statement will set the ID to 0 and allow others to steal the lock...
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Exception while rollbacking the connection on release", e);
            } finally {
                closeSafely(preparedStatement);
                try {
                    getConnection().close();
                } catch (SQLException ignored) {
                    LOG.log(Level.FINE, "Exception while closing connection on release", ignored);
                }
            }
        }
        
        lockConnection = null;
    }

    /**
     * This method will check if the jdbc connection is still active and if we were able to 
     * acquire or update the karaf_table information.
     *
     * @return True, if the connection is still active and we still have the lock.
     *
     * @see org.apache.karaf.main.lock.Lock#isAlive()
     *
     */
    public boolean isAlive() throws Exception {
        if (!isConnected()) { 
            LOG.severe("Lost lock!");
            return false; 
        }

        return lock();
    }

}
