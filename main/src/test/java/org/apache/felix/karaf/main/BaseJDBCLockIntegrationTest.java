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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public abstract class BaseJDBCLockIntegrationTest {
    
    private final Logger LOG = Logger.getLogger(this.getClass().getName());

    DefaultJDBCLock lock;
    Properties props;
    String user = "root";
    String password = "";
    String driver;
    String url;
    String tableName = "LOCK_TABLE";
    String clustername = "karaf_cluster";
    int timeout = 10;
    String momentDatatype = "BIGINT";
    String nodeDatatype = "VARCHAR(20)";

    abstract DefaultJDBCLock createLock(Properties props);
    
    @BeforeClass
    public static void setUpTestSuite() {
        Properties properties = new Properties();
        properties.put("karaf.bootstrap.log", "target/karaf.log");
        BootstrapLogManager.setProperties(properties);        
    }
    
    @Before
    public void setUp() throws Exception {
        props = new Properties();
        props.put("karaf.lock.jdbc.url", url);
        props.put("karaf.lock.jdbc.driver", driver);
        props.put("karaf.lock.jdbc.user", user);
        props.put("karaf.lock.jdbc.password", password);
        props.put("karaf.lock.jdbc.table", tableName);
        props.put("karaf.lock.jdbc.clustername", clustername);
        props.put("karaf.lock.jdbc.timeout", String.valueOf(timeout));
        
        try {
            executeStatement("DROP TABLE " + tableName);
        } catch (Exception e) {
            // expected if the table dosn't exist
        }
    }
    
    @After
    public void tearDown() throws Exception {
        if (lock != null) {
            lock.release();
        }
    }
    
    @Test
    @Ignore
    public void lockShouldRestoreTheLockAfterADbFailure() throws Exception {
        Lock lock1 = createLock(props);
        assertTrue(lock1.lock());
        assertTrue(lock1.isAlive());
        
        // shut down the database
        
        assertFalse(lock1.isAlive());
        
        // start the database
        
        assertTrue(lock1.lock());
        assertTrue(lock1.isAlive());
    }
    
    @Test
    public void initShouldCreateTheSchemaIfItNotExists() throws Exception {
        long start = System.currentTimeMillis();
        lock = createLock(props);
        long end = System.currentTimeMillis();
        
        long moment = queryDatabaseSingleResult("SELECT MOMENT FROM " + tableName);
    
        assertTrue(moment >= start);
        assertTrue(moment <= end);
    }

    @Test
    public void initShouldNotCreateTheSchemaIfItAlreadyExists() throws Exception {
        executeStatement("CREATE TABLE " + tableName + " (MOMENT " + momentDatatype + ", NODE " + nodeDatatype + ")");
        executeStatement("INSERT INTO " + tableName + " (MOMENT, NODE) VALUES (1, '" + clustername + "')");
        
        lock = createLock(props);
        
        long moment = queryDatabaseSingleResult("SELECT MOMENT FROM " + tableName);
    
        assertEquals(1, moment);
    }

    @Test
    public void lockShouldReturnTrueItTheTableIsNotLocked() throws Exception {
        lock = createLock(props);
        
        assertTrue(lock.lock());
        assertTableIsLocked();
    }

    @Test
    public void lockShouldReturnFalseIfAnotherRowIsLocked() throws Exception {
        Connection connection = null;
        try {
            lock = createLock(props);
            
            executeStatement("INSERT INTO " + tableName + " (MOMENT, NODE) VALUES (1, '" + clustername + "_2')");
            connection = lock(tableName, clustername + "_2");
            
            // we can't lock only one row for the cluster
            assertFalse(lock.lock());
        } finally {
            close(connection);
        }
    }
    
    @Test
    public void lockShouldReturnFalseIfTheRowIsAlreadyLocked() throws Exception {
        Connection connection = null;
        try {
            lock = createLock(props);
            connection = lock(tableName, clustername);
            
            assertFalse(lock.lock());
        } finally {
            close(connection);
        }
    }
    
    @Test
    public void release() throws Exception {
        lock = createLock(props);
        
        assertTrue(lock.lock());
        
        lock.release();
        
        assertNull(lock.lockConnection);
        assertTableIsUnlocked();
    }

    @Test
    public void releaseShouldSucceedForAnAlreadyClosedConnection() throws Exception {
        lock = createLock(props);
        
        assertTrue(lock.lock());
        
        lock.lockConnection.rollback(); // release the lock
        lock.lockConnection.close();
        lock.release();
        
        assertTableIsUnlocked();
    }

    @Test
    public void releaseShouldSucceedForANullConnectionReference() throws Exception {
        lock = createLock(props);
        
        assertTrue(lock.lock());
        
        lock.lockConnection.rollback(); // release the lock
        lock.lockConnection.close();
        lock.lockConnection = null;
        lock.release();
        
        assertTableIsUnlocked();
    }

    @Test
    public void isAliveShouldReturnTrueIfItHoldsTheLock() throws Exception {
        lock = createLock(props);
        
        assertTrue(lock.lock());
        assertTrue(lock.isAlive());
    }

    @Test
    public void isAliveShouldReturnFalseIfTheConnectionIsClosed() throws Exception {
        lock = createLock(props);
        
        assertTrue(lock.lock());
        
        lock.lockConnection.rollback(); // release the lock
        lock.lockConnection.close();
        
        assertFalse(lock.isAlive());
    }

    @Test
    public void isAliveShouldReturnFalseIfTheConnectionIsNull() throws Exception {
        lock = createLock(props);
        
        assertTrue(lock.lock());
        
        lock.lockConnection.rollback(); // release the lock
        lock.lockConnection.close();
        lock.lockConnection = null;
        
        assertFalse(lock.isAlive());
    }

    @Test
    public void isAliveShouldReturnFalseIfItNotHoldsTheLock() throws Exception {
        Connection connection = null;
        try {
            lock = createLock(props);
            
            assertTrue(lock.lock());
            
            lock.lockConnection.rollback(); // release the lock
            connection = lock(tableName, clustername); // another connection locks the table
            
            assertFalse(lock.isAlive());
        } finally {
            close(connection);            
        }
    }

    Connection getConnection(String url, String user, String password) throws ClassNotFoundException, SQLException {
        Class.forName(driver);
        Connection connection = DriverManager.getConnection(url, user, password);
        connection.setAutoCommit(false);
        return connection;
    }

    void executeStatement(String stmt) throws SQLException, ClassNotFoundException {
        Connection connection = null;
        Statement statement = null;
        
        try {
            connection = getConnection(url, user, password);
            statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            statement.execute(stmt);    
            connection.commit();
        } finally {
            close(statement);
            close(connection);
        }
    }

    Long queryDatabaseSingleResult(String query) throws ClassNotFoundException, SQLException {
        Connection connection = null;
        Statement statement = null;
        ResultSet rs = null;
        
        try {
            connection = getConnection(url, user, password);
            statement = connection.createStatement();
            rs = statement.executeQuery(query);
            rs.next();
            return rs.getLong(1);
        } finally {
            close(rs);
            close(statement);
            close(connection);
        }
    }

    void assertTableIsLocked() throws ClassNotFoundException, SQLException {
        try {
            executeStatement("UPDATE " + tableName + " SET MOMENT = " + System.currentTimeMillis());
            fail("SQLException for timeout expected because the table should be already locked");
        } catch (SQLException sqle) {
            // expected
        }
    }

    void assertTableIsUnlocked() throws ClassNotFoundException, SQLException {
        executeStatement("UPDATE " + tableName + " SET MOMENT = " + System.currentTimeMillis());
    }

    Connection lock(String table, String node) throws ClassNotFoundException, SQLException {
        Connection connection = null;
        Statement statement = null;
        
        try {
            connection = getConnection(url, user, password);
            statement = connection.createStatement();
            //statement.execute("SELECT * FROM " + table + " WHERE NODE = '" + node + "' FOR UPDATE");
            //statement.execute("UPDATE " + table + " SET MOMENT = " + System.currentTimeMillis() + " WHERE NODE = '" + node + "'");
            statement.execute("SELECT * FROM " + table + " FOR UPDATE");
            statement.execute("UPDATE " + table + " SET MOMENT = " + System.currentTimeMillis());
        } finally {
            close(statement);
            // connection must not be closed!
        }
        
        return connection;
    }

    void close(ResultSet rs) throws SQLException {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception e) {
            }
        }
    }

    void close(Statement statement) throws SQLException {
        if (statement != null) {
            try {
                statement.close();
            } catch (Exception e) {
                LOG.severe("Can't close the statement: " + e);
            }
        }
    }

    void close(Connection connection) throws SQLException {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (Exception e) {
                LOG.severe("Can't rollback the connection: " + e);
            }
            try {
                connection.close();
            } catch (Exception e) {
                LOG.severe("Can't close the connection: " + e);
            }
        }
    }
}