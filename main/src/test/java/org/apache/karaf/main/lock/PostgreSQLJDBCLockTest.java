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

import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

import org.apache.felix.utils.properties.Properties;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class PostgreSQLJDBCLockTest extends BaseJDBCLockTest {

    @Before
    @Override
    public void setUp() throws Exception {
        password = "secret";
        driver = "org.postgresql.Driver";
        url = "jdbc:postgresql://127.0.0.1:5432/test";

        super.setUp();
    }

    DefaultJDBCLock createLock(Properties props) {
        return new PostgreSQLJDBCLock(props) {
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
    public void createConnectionShouldConcatinateOptionsCorrect() {
        props.put("karaf.lock.jdbc.url", this.url + ";dataEncryption=false");

        lock = new PostgreSQLJDBCLock(props) {
            @Override
            boolean schemaExists() {
                return true;
            }

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
        };
    }

    @Test
    @Override
    public void lockShouldReturnTrueItTheTableIsNotLocked() throws Exception {
        initShouldNotCreateTheSchemaIfItAlreadyExists();
        reset(connection, metaData, statement, preparedStatement, resultSet);

        expect(connection.isClosed()).andReturn(false);
        expect(connection.prepareStatement("SELECT * FROM " + tableName + " FOR UPDATE")).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(0);
        expect(preparedStatement.execute()).andReturn(true);
        preparedStatement.close();
        expect(connection.isClosed()).andReturn(false);
        expect(connection.prepareStatement("UPDATE " + tableName + " SET MOMENT = 1")).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(0);
        expect(preparedStatement.executeUpdate()).andReturn(1);
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
        preparedStatement.setQueryTimeout(0);
        expect(preparedStatement.execute()).andReturn(true);
        preparedStatement.close();
        expect(connection.isClosed()).andReturn(false);
        expect(connection.prepareStatement("UPDATE " + tableName + " SET MOMENT = 1")).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(0);
        expect(preparedStatement.executeUpdate()).andThrow(new SQLException());
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
        preparedStatement.setQueryTimeout(0);
        expect(preparedStatement.execute()).andReturn(true);
        preparedStatement.close();
        expect(connection.isClosed()).andReturn(false);
        expect(connection.prepareStatement("UPDATE " + tableName + " SET MOMENT = 1")).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(0);
        expect(preparedStatement.executeUpdate()).andThrow(new SQLException());
        preparedStatement.close();

        replay(connection, metaData, statement, preparedStatement, resultSet);

        boolean lockAcquired = lock.lock();

        verify(connection, metaData, statement, preparedStatement, resultSet);
        assertFalse(lockAcquired);
    }

    @Test
    @Override
    public void lockShouldReturnFalseIfTableIsEmpty() throws Exception {
        initShouldNotCreateTheSchemaIfItAlreadyExists();
        reset(connection, metaData, statement, preparedStatement, resultSet);

        expect(connection.isClosed()).andReturn(false);
        expect(connection.prepareStatement("SELECT * FROM " + tableName + " FOR UPDATE")).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(0);
        expect(preparedStatement.execute()).andReturn(true);
        preparedStatement.close();
        expect(connection.isClosed()).andReturn(false);
        expect(connection.prepareStatement("UPDATE " + tableName + " SET MOMENT = 1")).andReturn(preparedStatement);
        preparedStatement.setQueryTimeout(0);
        expect(preparedStatement.executeUpdate()).andReturn(0);
        preparedStatement.close();

        replay(connection, metaData, statement, preparedStatement, resultSet);

        boolean lockAcquired = lock.lock();

        verify(connection, metaData, statement, preparedStatement, resultSet);
        assertFalse(lockAcquired);
    }
}
