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

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public abstract class BaseJDBCLockTest {
    
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
    String createTableStmtSuffix = "";
    
    Connection connection;
    DatabaseMetaData metaData;
    ResultSet resultSet;
    PreparedStatement preparedStatement;
    Statement statement;
    
    abstract DefaultJDBCLock createLock(Properties props);
    
    @BeforeClass
    public static void setUpTestSuite() {
        Properties properties = new Properties();
        properties.put("karaf.bootstrap.log", "target/karaf.log");
        BootstrapLogManager.setProperties(properties);        
    }
    
    @Before
    public void setUp() throws Exception {
        connection = EasyMock.createMock(Connection.class);
        metaData = EasyMock.createMock(DatabaseMetaData.class);
        resultSet = EasyMock.createMock(ResultSet.class);
        preparedStatement = EasyMock.createMock(PreparedStatement.class);
        statement = EasyMock.createMock(Statement.class);
        
        props = new Properties();
        props.put("karaf.lock.jdbc.url", url);
        props.put("karaf.lock.jdbc.driver", driver);
        props.put("karaf.lock.jdbc.user", user);
        props.put("karaf.lock.jdbc.password", password);
        props.put("karaf.lock.jdbc.table", tableName);
        props.put("karaf.lock.jdbc.clustername", clustername);
        props.put("karaf.lock.jdbc.timeout", timeout);
    }
    
    @Test
    public void initShouldCreateTheSchemaIfItNotExists() throws Exception {
        expect(connection.isClosed()).andReturn(false);
        connection.setAutoCommit(false);
        expect(connection.getMetaData()).andReturn(metaData);
        expect(metaData.getTables((String) isNull(), (String) isNull(), eq("LOCK_TABLE"), aryEq(new String[] {"TABLE"}))).andReturn(resultSet);
        expect(metaData.getTables((String) isNull(), (String) isNull(), eq("LOCK_TABLE"), aryEq(new String[] {"TABLE"}))).andReturn(resultSet);
        expect(metaData.getTables((String) isNull(), (String) isNull(), eq("lock_table"), aryEq(new String[] {"TABLE"}))).andReturn(resultSet);
        expect(resultSet.next()).andReturn(false);
        expect(resultSet.next()).andReturn(false);
        expect(resultSet.next()).andReturn(false);
        resultSet.close();
        expect(connection.isClosed()).andReturn(false);
        expect(connection.createStatement()).andReturn(statement);
        expect(statement.execute("CREATE TABLE " + tableName + " (MOMENT " + momentDatatype + ", NODE " + nodeDatatype + ")" + createTableStmtSuffix)).andReturn(false);
        expect(statement.execute("INSERT INTO " + tableName + " (MOMENT, NODE) VALUES (1, '" + clustername + "')")).andReturn(false);
        statement.close();
        connection.commit();
        
        replay(connection, metaData, statement, preparedStatement, resultSet);
        
        lock = createLock(props);
        
        verify(connection, metaData, statement, preparedStatement, resultSet);
    }
    
    @Test
    public void initShouldNotCreateTheSchemaIfItAlreadyExists() throws Exception {
        connection.setAutoCommit(false);
        expect(connection.getMetaData()).andReturn(metaData);
        expect(metaData.getTables((String) isNull(), (String) isNull(), anyString(), aryEq(new String[] {"TABLE"}))).andReturn(resultSet);
        expect(resultSet.next()).andReturn(true);
        resultSet.close();
        
        replay(connection, metaData, statement, preparedStatement, resultSet);
        
        lock = createLock(props);
        
        verify(connection, metaData, statement, preparedStatement, resultSet);
    }
    
    @Test
    public void lockShouldReturnTrueItTheTableIsNotLocked() throws Exception {
        initShouldNotCreateTheSchemaIfItAlreadyExists();
        reset(connection, metaData, statement, preparedStatement, resultSet);
        
        expect(connection.isClosed()).andReturn(false);
        expect(connection.prepareStatement("SELECT * FROM " + tableName + " FOR UPDATE")).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(10);
        expect(preparedStatement.execute()).andReturn(true);
        preparedStatement.close();
        expect(connection.isClosed()).andReturn(false);
        expect(connection.prepareStatement("UPDATE " + tableName + " SET MOMENT = 1")).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(10);
        expect(preparedStatement.executeUpdate()).andReturn(1);
        preparedStatement.close();
        
        replay(connection, metaData, statement, preparedStatement, resultSet);
        
        boolean lockAquired = lock.lock();
        
        verify(connection, metaData, statement, preparedStatement, resultSet);
        assertTrue(lockAquired);
    }
    
    @Test
    public void lockShouldReturnFalseIfAnotherRowIsLocked() throws Exception {
        initShouldNotCreateTheSchemaIfItAlreadyExists();
        reset(connection, metaData, statement, preparedStatement, resultSet);
        
        expect(connection.isClosed()).andReturn(false);
        expect(connection.prepareStatement("SELECT * FROM " + tableName + " FOR UPDATE")).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(10);
        expect(preparedStatement.execute()).andReturn(true);
        preparedStatement.close();
        expect(connection.isClosed()).andReturn(false);
        expect(connection.prepareStatement("UPDATE " + tableName + " SET MOMENT = 1")).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(10);
        expect(preparedStatement.executeUpdate()).andThrow(new SQLException());
        preparedStatement.close();
        
        replay(connection, metaData, statement, preparedStatement, resultSet);
        
        boolean lockAquired = lock.lock();
        
        verify(connection, metaData, statement, preparedStatement, resultSet);
        assertFalse(lockAquired);
    }
    
    @Test
    public void lockShouldReturnFalseIfTheRowIsAlreadyLocked() throws Exception {
        initShouldNotCreateTheSchemaIfItAlreadyExists();
        reset(connection, metaData, statement, preparedStatement, resultSet);
        
        expect(connection.isClosed()).andReturn(false);
        expect(connection.prepareStatement("SELECT * FROM " + tableName + " FOR UPDATE")).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(10);
        expect(preparedStatement.execute()).andReturn(true);
        preparedStatement.close();
        expect(connection.isClosed()).andReturn(false);
        expect(connection.prepareStatement("UPDATE " + tableName + " SET MOMENT = 1")).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(10);
        expect(preparedStatement.executeUpdate()).andThrow(new SQLException());
        preparedStatement.close();
        
        replay(connection, metaData, statement, preparedStatement, resultSet);
        
        boolean lockAquired = lock.lock();
        
        verify(connection, metaData, statement, preparedStatement, resultSet);
        assertFalse(lockAquired);
    }
    
    @Test
    public void release() throws Exception {
        initShouldNotCreateTheSchemaIfItAlreadyExists();
        reset(connection, metaData, statement, preparedStatement, resultSet);
        
        expect(connection.isClosed()).andReturn(false);
        expect(connection.isClosed()).andReturn(false);
        expect(connection.isClosed()).andReturn(false);
        connection.rollback();
        connection.close();
        
        replay(connection, metaData, statement, preparedStatement, resultSet);
        
        lock.release();
        
        verify(connection, metaData, statement, preparedStatement, resultSet);
    }
    
    @Test
    public void releaseShouldSucceedForAnAlreadyClosedConnection() throws Exception {
        initShouldNotCreateTheSchemaIfItAlreadyExists();
        reset(connection, metaData, statement, preparedStatement, resultSet);
        
        expect(connection.isClosed()).andReturn(true);
        
        replay(connection, metaData, statement, preparedStatement, resultSet);
        
        lock.release();
        
        verify(connection, metaData, statement, preparedStatement, resultSet);
    }
    
    @Test
    public void releaseShouldSucceedForANullConnectionReference() throws Exception {
        initShouldNotCreateTheSchemaIfItAlreadyExists();
        reset(connection, metaData, statement, preparedStatement, resultSet);
        
        lock.lockConnection = null;
        
        replay(connection, metaData, statement, preparedStatement, resultSet);
        
        lock.release();
        
        verify(connection, metaData, statement, preparedStatement, resultSet);
    }
    
    @Test
    public void isAliveShouldReturnTrueIfItHoldsTheLock() throws Exception {
        initShouldNotCreateTheSchemaIfItAlreadyExists();
        reset(connection, metaData, statement, preparedStatement, resultSet);
        
        expect(connection.isClosed()).andReturn(false);
        expect(connection.isClosed()).andReturn(false);
        expect(connection.prepareStatement("UPDATE " + tableName + " SET MOMENT = 1")).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(10);
        expect(preparedStatement.executeUpdate()).andReturn(1);
        preparedStatement.close();
        
        replay(connection, metaData, statement, preparedStatement, resultSet);
        
        boolean alive = lock.isAlive();
        
        verify(connection, metaData, statement, preparedStatement, resultSet);
        assertTrue(alive);
    }
    
    @Test
    public void isAliveShouldReturnFalseIfTheConnectionIsClosed() throws Exception {
        initShouldNotCreateTheSchemaIfItAlreadyExists();
        reset(connection, metaData, statement, preparedStatement, resultSet);
        
        expect(connection.isClosed()).andReturn(true);
        
        replay(connection, metaData, statement, preparedStatement, resultSet);
        
        boolean alive = lock.isAlive();
        
        verify(connection, metaData, statement, preparedStatement, resultSet);
        assertFalse(alive);
    }
    
    @Test
    public void isAliveShouldReturnFalseIfTheConnectionIsNull() throws Exception {
        initShouldNotCreateTheSchemaIfItAlreadyExists();
        reset(connection, metaData, statement, preparedStatement, resultSet);
        
        lock.lockConnection = null;
        
        replay(connection, metaData, statement, preparedStatement, resultSet);
        
        boolean alive = lock.isAlive();
        
        verify(connection, metaData, statement, preparedStatement, resultSet);
        assertFalse(alive);
    }
    
    @Test
    public void isAliveShouldReturnFalseIfItNotHoldsTheLock() throws Exception {
        initShouldNotCreateTheSchemaIfItAlreadyExists();
        reset(connection, metaData, statement, preparedStatement, resultSet);
        
        expect(connection.isClosed()).andReturn(false);
        expect(connection.isClosed()).andReturn(false);
        expect(connection.prepareStatement("UPDATE " + tableName + " SET MOMENT = 1")).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(10);
        expect(preparedStatement.executeUpdate()).andThrow(new SQLException());
        preparedStatement.close();
        
        replay(connection, metaData, statement, preparedStatement, resultSet);
        
        boolean alive = lock.isAlive();
        
        verify(connection, metaData, statement, preparedStatement, resultSet);
        assertFalse(alive);
    }
    
    @Test
    public void lockShouldReturnFalseIfTableIsEmpty() throws Exception {
        initShouldNotCreateTheSchemaIfItAlreadyExists();
        reset(connection, metaData, statement, preparedStatement, resultSet);
        
        expect(connection.isClosed()).andReturn(false);
        expect(connection.prepareStatement("SELECT * FROM " + tableName + " FOR UPDATE")).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(10);
        expect(preparedStatement.execute()).andReturn(true);
        preparedStatement.close();
        expect(connection.isClosed()).andReturn(false);
        expect(connection.prepareStatement("UPDATE " + tableName + " SET MOMENT = 1")).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(10);
        expect(preparedStatement.executeUpdate()).andReturn(0);
        preparedStatement.close();
        
        replay(connection, metaData, statement, preparedStatement, resultSet);
        
        boolean lockAquired = lock.lock();
        
        verify(connection, metaData, statement, preparedStatement, resultSet);
        assertFalse(lockAquired);
    }
}
