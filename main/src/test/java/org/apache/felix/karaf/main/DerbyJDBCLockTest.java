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


public class DerbyJDBCLockTest extends BaseJDBCLockTest {
    
    @Before
    @Override
    public void setUp() throws Exception {
        password = "root";
        driver = "org.apache.derby.jdbc.ClientDriver";
        url = "jdbc:derby://127.0.0.1:1527/test";
        
        super.setUp();
    }
    
    DerbyJDBCLock createLock(Properties props) {
        return new DerbyJDBCLock(props) {
            @Override
            Connection doCreateConnection(String driver, String url, String username, String password) {
                assertEquals(this.driver, driver);
                assertEquals(this.url + ";create=true", url);
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
        props.put("karaf.lock.jdbc.url", this.url + ";dataEncryption=false");
        
        lock = new DerbyJDBCLock(props) {
            @Override
            Connection doCreateConnection(String driver, String url, String username, String password) {
                assertEquals(this.driver, driver);
                assertEquals(this.url + ";create=true", url);
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