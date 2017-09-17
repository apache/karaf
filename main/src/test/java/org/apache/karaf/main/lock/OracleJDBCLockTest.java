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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

import org.apache.felix.utils.properties.Properties;

import org.junit.Before;
import org.junit.Test;


public class OracleJDBCLockTest extends BaseJDBCLockTest {

    @Before
    @Override
    public void setUp() throws Exception {
        password = "root";
        driver = "oracle.jdbc.driver.OracleDriver";
        url = "jdbc:oracle:thin:@172.16.16.132:1521:XE";
        momentDatatype = "NUMBER(20)";

        super.setUp();
    }

    OracleJDBCLock createLock(Properties props) {
        return new OracleJDBCLock(props) {
            @Override
            Connection doCreateConnection(String driver, String url, String username, String password) {
                assertEquals(this.driver, driver);
                assertEquals(this.url, url);
                assertEquals(this.user, username);
                assertEquals(this.password, password);
                return connection;
            }

            @Override
            long getCurrentTimeMillis() {
                return 1;
            }

            @Override
            public void log(Level level, String msg, Exception e) {
                // Suppress log
            }
        };
    }

    @Test
    @Override
    public void lockShouldReturnTrueItTheTableIsNotLocked() throws Exception {
        initShouldNotCreateTheSchemaIfItAlreadyExists();
        reset(connection, metaData, statement, preparedStatement, resultSet);

        expect(connection.isClosed()).andReturn(false);
        expect(connection.prepareStatement("SELECT * FROM " + tableName + " FOR UPDATE")).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(10);
        expect(preparedStatement.execute()).andReturn(true);
        preparedStatement.close();
        expect(connection.isClosed()).andReturn(false);
        expect(connection.prepareStatement("SELECT COUNT(*) FROM " + tableName)).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(10);
        expect(preparedStatement.executeQuery()).andReturn(resultSet);
        expect(resultSet.next()).andReturn(Boolean.TRUE);
        expect(resultSet.getInt(1)).andReturn(1);
        preparedStatement.close();

        replay(connection, metaData, statement, preparedStatement, resultSet);

        boolean lockAcquired = lock.lock();

        verify(connection, metaData, statement, preparedStatement, resultSet);
        assertTrue(lockAcquired);
    }

    @Test
    @Override
    public void lockShouldReturnFalseIfAnotherRowIsLocked() throws Exception {
        initShouldNotCreateTheSchemaIfItAlreadyExists();
        reset(connection, metaData, statement, preparedStatement, resultSet);

        expect(connection.isClosed()).andReturn(false);
        expect(connection.prepareStatement("SELECT * FROM " + tableName + " FOR UPDATE")).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(10);
        expect(preparedStatement.execute()).andThrow(new SQLException());
        preparedStatement.close();

        replay(connection, metaData, statement, preparedStatement, resultSet);

        boolean lockAcquired = lock.lock();

        verify(connection, metaData, statement, preparedStatement, resultSet);
        assertFalse(lockAcquired);
    }

    @Test
    @Override
    public void lockShouldReturnFalseIfTheRowIsAlreadyLocked() throws Exception {
        initShouldNotCreateTheSchemaIfItAlreadyExists();
        reset(connection, metaData, statement, preparedStatement, resultSet);

        expect(connection.isClosed()).andReturn(false);
        expect(connection.prepareStatement("SELECT * FROM " + tableName + " FOR UPDATE")).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(10);
        expect(preparedStatement.execute()).andThrow(new SQLException());
        preparedStatement.close();

        replay(connection, metaData, statement, preparedStatement, resultSet);

        boolean lockAcquired = lock.lock();

        verify(connection, metaData, statement, preparedStatement, resultSet);
        assertFalse(lockAcquired);
    }

    @Test
    public void isAliveShouldReturnTrueIfItHoldsTheLock() throws Exception {
        initShouldNotCreateTheSchemaIfItAlreadyExists();
        reset(connection, metaData, statement, preparedStatement, resultSet);

        expect(connection.isClosed()).andReturn(false);
        expect(connection.isClosed()).andReturn(false);
        expect(connection.prepareStatement("SELECT * FROM " + tableName + " FOR UPDATE")).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(10);
        expect(preparedStatement.execute()).andReturn(true);
        preparedStatement.close();
        expect(connection.isClosed()).andReturn(false);
        expect(connection.prepareStatement("SELECT COUNT(*) FROM " + tableName)).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(10);
        expect(preparedStatement.executeQuery()).andReturn(resultSet);
        expect(resultSet.next()).andReturn(Boolean.TRUE);
        expect(resultSet.getInt(1)).andReturn(1);
        preparedStatement.close();

        replay(connection, metaData, statement, preparedStatement, resultSet);

        boolean alive = lock.isAlive();

        verify(connection, metaData, statement, preparedStatement, resultSet);
        assertTrue(alive);
    }

    @Test
    public void isAliveShouldReturnFalseIfItNotHoldsTheLock() throws Exception {
        initShouldNotCreateTheSchemaIfItAlreadyExists();
        reset(connection, metaData, statement, preparedStatement, resultSet);

        expect(connection.isClosed()).andReturn(false);
        expect(connection.isClosed()).andReturn(false);
        expect(connection.prepareStatement("SELECT * FROM " + tableName + " FOR UPDATE")).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(10);
        expect(preparedStatement.execute()).andThrow(new SQLException());
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
        expect(connection.prepareStatement("SELECT COUNT(*) FROM " + tableName)).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(10);
        expect(preparedStatement.executeQuery()).andReturn(resultSet);
        expect(resultSet.next()).andReturn(Boolean.TRUE);
        expect(resultSet.getInt(1)).andReturn(0);
        preparedStatement.close();

        replay(connection, metaData, statement, preparedStatement, resultSet);

        boolean lockAcquired = lock.lock();

        verify(connection, metaData, statement, preparedStatement, resultSet);
        assertFalse(lockAcquired);
    }
}
