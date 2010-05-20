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

import java.sql.Connection;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;


public class MySQLJDBCLockTest extends BaseJDBCLockTest {
    
    @Before
    @Override
    public void setUp() throws Exception {
        driver = "com.mysql.jdbc.Driver";
        url = "jdbc:mysql://127.0.0.1:3306/test";
        createTableStmtSuffix = " ENGINE=INNODB";
        
        super.setUp();
    }
    
    MySQLJDBCLock createLock(Properties props) {
        return new MySQLJDBCLock(props) {
            @Override
            Connection doCreateConnection(String driver, String url, String username, String password) {
                assertEquals(this.driver, driver);
                assertEquals(this.url + "?createDatabaseIfNotExist=true", url);
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
    public void createConnectionShouldConcatinateOptionsCorrect() {
        props.put("karaf.lock.jdbc.url", this.url + "?connectTimeout=10000");
        
        lock = new MySQLJDBCLock(props) {
            @Override
            Connection doCreateConnection(String driver, String url, String username, String password) {
                assertEquals(this.driver, driver);
                assertEquals(this.url + "&createDatabaseIfNotExist=true", url);
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
}