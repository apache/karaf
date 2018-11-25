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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.felix.utils.properties.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.karaf.main.ConfigProperties;
import org.apache.karaf.main.util.BootstrapLogManager;

import javax.sql.DataSource;

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

    public static final String PROPERTY_LOCK_URL                = "karaf.lock.jdbc.url";
    public static final String PROPERTY_LOCK_JDBC_DRIVER        = "karaf.lock.jdbc.driver";
    public static final String PROPERTY_LOCK_JDBC_USER          = "karaf.lock.jdbc.user";
    public static final String PROPERTY_LOCK_JDBC_PASSWORD      = "karaf.lock.jdbc.password";
    public static final String PROPERTY_LOCK_JDBC_TABLE         = "karaf.lock.jdbc.table";
    public static final String PROPERTY_LOCK_JDBC_TABLE_ID      = "karaf.lock.jdbc.table_id";
    public static final String PROPERTY_LOCK_JDBC_CLUSTERNAME   = "karaf.lock.jdbc.clustername";
    public static final String PROPERTY_LOCK_JDBC_CACHE         = "karaf.lock.jdbc.cache";
    public static final String PROPERTY_LOCK_JDBC_VALID_TIMEOUT = "karaf.lock.jdbc.valid_timeout";

    public static final String DEFAULT_PASSWORD = "";
    public static final String DEFAULT_USER = "";
    public static final String DEFAULT_TABLE = "KARAF_LOCK";
    public static final String DEFAULT_TABLE_ID = "KARAF_NODE_ID";
    public static final String DEFAULT_CLUSTERNAME = "karaf";
    public static final String DEFAULT_CACHE = "true";
    public static final String DEFAULT_VALID_TIMEOUT = "0";

    final GenericStatements statements;
    DataSource dataSource;
    String url;
    String driver;
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
        this.table = props.getProperty(PROPERTY_LOCK_JDBC_TABLE, DEFAULT_TABLE);
        this.clusterName = props.getProperty(PROPERTY_LOCK_JDBC_CLUSTERNAME, DEFAULT_CLUSTERNAME);
        this.table_id = props.getProperty(PROPERTY_LOCK_JDBC_TABLE_ID, DEFAULT_TABLE_ID);
        this.lock_delay = Integer.parseInt(props.getProperty(ConfigProperties.PROPERTY_LOCK_DELAY, ConfigProperties.DEFAULT_LOCK_DELAY));

        this.statements = createStatements();

        String url = this.url;
        if (url.toLowerCase().startsWith("jdbc:derby")) {
            url = (url.toLowerCase().contains("create=true")) ? url : url + ";create=true";
        }
        boolean cacheEnabled = Boolean.parseBoolean(props.getProperty(PROPERTY_LOCK_JDBC_CACHE, DEFAULT_CACHE));
        int validTimeout = Integer.parseInt(props.getProperty(PROPERTY_LOCK_JDBC_VALID_TIMEOUT, DEFAULT_VALID_TIMEOUT));

        String user = props.getProperty(PROPERTY_LOCK_JDBC_USER, DEFAULT_USER);
        String password = props.getProperty(PROPERTY_LOCK_JDBC_PASSWORD, DEFAULT_PASSWORD);
        this.dataSource = new GenericDataSource(driver, url, user, password, cacheEnabled, validTimeout);
        init();
    }
    
    /**
     * This method is called to create an instance of the JDBCStatements instance.
     *
     * @return An instance of a JDBCStatement object.
     */
    GenericStatements createStatements() {
        return new GenericStatements(table, table_id, clusterName);
    }
    
    void init() {
        try {
            createDatabase();
            try (Connection connection = getConnection()) {
                createSchema(connection);
                generateUniqueId(connection);
            }
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
    void createSchema(Connection connection) {
        try {
            if (schemaExists(connection)) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                connection.setAutoCommit(false);

                String[] createStatements = this.statements.getLockCreateSchemaStatements(System.currentTimeMillis());
                for (String stmt : createStatements) {
                    LOG.info("Executing statement: " + stmt);
                    statement.execute(stmt);
                }

                connection.commit();
                connection.setAutoCommit(true);
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Could not create schema", e );
        }
    }

    /**
     * This method is called to determine if the required database schemas have already been created or not.
     *
     * @return True, if the schemas are available else false.
     */
    boolean schemaExists(Connection connection) {
        return schemaExist(connection, this.statements.getLockTableName())
                && schemaExist(connection, this.statements.getLockIdTableName());
    }

    /**
     * This method is called to determine if the required table is available or not.
     *
     * @param tableName The name of the table to determine if it exists.
     * @return True, if the table exists else false.
     */
    private boolean schemaExist(Connection connection, String tableName) {
        boolean schemaExists = false;
        try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, new String[] {"TABLE"})) {
            schemaExists = rs.next();
        } catch (Exception ignore) {
            LOG.log(Level.SEVERE, "Error testing for db table", ignore);
        }
        return schemaExists;
    }

    /**
     * This method will generate a unique id for this instance that is part of an active set of instances.
     * This method uses a simple algorithm to insure that the id will be unique for all cases.
     */
    void generateUniqueId(Connection connection) {
        String selectString = this.statements.getLockIdSelectStatement();
        try (PreparedStatement selectStatement = connection.prepareStatement(selectString)) {

            boolean uniqueIdSet = false;
            // This loop can only be performed for so long and the chances that this will be
            // looping for more than a few times is unlikely since there will always be at
            // least one instance that is successful.
            while (!uniqueIdSet) {

                // Get the current ID from the karaf ids table
                try (ResultSet rs = selectStatement.executeQuery()) {

                    // Check if we were able to retrieve the result...
                    if (rs.next()) {
                        // Update the row with the next available id
                        int currentId = this.statements.getIdFromLockIdSelectStatement(rs);
                        
						String updateString = this.statements.getLockIdUpdateIdStatement(currentId + 1, currentId);

						try (PreparedStatement updateStatement = connection.prepareStatement(updateString)) {

                            int count = updateStatement.executeUpdate();

                            // Set the uniqueId if and only if is it greater that zero
                            uniqueId = (uniqueIdSet = count > 0) ? currentId + 1 : 0;

                            if (count > 1) {
                                LOG.severe("OOPS there are more than one row within the table ids...");
                            }
                        }
                    } else {
                        LOG.severe("No rows were found....");
                    }
                } catch (SQLException e) {
                    LOG.log(Level.SEVERE, "Received an SQL exception while processing result set", e);
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Received an SQL exception while generating a prepate statement", e);
        }
        LOG.info("INSTANCE unique id: " + uniqueId);
    }
    
    /**
     * This method will return an active connection for this given jdbc driver.
     *
     * @return The JDBC connection instance
     * @throws Exception If the JDBC connection can't be retrieved.
     */
    protected Connection getConnection() throws Exception {
        return dataSource.getConnection();
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
        try (Connection connection = getConnection()) {
            // Try to acquire/update the lock state
            boolean lockAcquired = acquireLock(connection, statements.getLockUpdateIdStatement(uniqueId, ++state, lock_delay, uniqueId));

            if (!lockAcquired) {

                String lockSelectStatement = statements.getLockSelectStatement();

                try (PreparedStatement statement = connection.prepareStatement(lockSelectStatement)) {
                    // Get the current master id and compare with information that we have locally....
                    try (ResultSet rs = statement.executeQuery()) {

                        if (rs.next()) {
                            int currentId = statements.getIdFromLockSelectStatement(rs); // The current master unique id or 0
                            int currentState = statements.getStateFromLockSelectStatement(rs); // The current master state or whatever

                            if (this.currentId == currentId) {
                                // It is the same instance that locked the table
                                if (this.currentState == currentState) {
                                    // Its state has not been updated....
                                    if ((this.currentStateTime + this.currentLockDelay + this.currentLockDelay) < System.currentTimeMillis()) {
                                        // The state was not been updated for more than twice the lock_delay value of the current master...
                                        // Try to steal the lock....
                                        lockAcquired = acquireLock(connection, statements.getLockUpdateIdStatementToStealLock(uniqueId, state, lock_delay, currentId, currentState));
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
                    }
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Unable to determine if the lock was obtain", e);
                }
            }

            return lockAcquired;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error while trying to obtain the lock", e);
            return false;
        }
    }

    /**
     * This method is called to try and acquire the lock and/or update the state for when this instance
     * is already the master instance.  It will try to update the row given the passed data and will
     * succeed if and only if the generated where clause was valid else it would not update the row.
     *
     * @param lockUpdateIdStatement The sql statement used to execute the update.
     * @return True, if the row was updated else false.
     */
    private boolean acquireLock(Connection connection, String lockUpdateIdStatement) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(lockUpdateIdStatement)) {
            // This will only update the row that contains the ID of 0 or curId
            return preparedStatement.executeUpdate() > 0;
        } catch (Exception e) {
            // Do we want to display this message everytime???
            LOG.log(Level.WARNING, "Failed to acquire database lock", e);
            return false;
        }
    }

    /**
     * This method will release the lock that the current master has by setting the karaf_lock table
     * id to 0.  This tells the others that the master has relinquished the lock and someone else can
     * try to acquire that lock and become a master.
     *
     * @see org.apache.karaf.main.lock.Lock#release()
     */
    public void release() throws Exception {
        try (Connection connection = getConnection()) {
            String lockResetIdStatement = statements.getLockResetIdStatement(uniqueId);
            try (PreparedStatement preparedStatement = connection.prepareStatement(lockResetIdStatement)) {
                // This statement will set the ID to 0 and allow others to steal the lock...
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Exception while releasing lock", e);
        }
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
        return lock();
    }

}
