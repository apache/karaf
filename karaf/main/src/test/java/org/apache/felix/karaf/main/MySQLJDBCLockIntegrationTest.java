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


import static org.junit.Assert.assertFalse;

import java.sql.Connection;
import java.util.Properties;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


@Ignore
public class MySQLJDBCLockIntegrationTest extends BaseJDBCLockIntegrationTest {

    @Before
    public void setUp() throws Exception {
        driver = "com.mysql.jdbc.Driver";
        url = "jdbc:mysql://127.0.0.1:3306/test";
        
        super.setUp();
    }
    
    @Override
    MySQLJDBCLock createLock(Properties props) {
        return new MySQLJDBCLock(props);
    }
    
    @Test
    public void initShouldCreateTheDatabaseIfItNotExists() throws Exception {
        String database = "test" + System.currentTimeMillis();
        
        try {
            executeStatement("DROP DATABASE " + database);
        } catch (Exception e) {
            // expected if table dosn't exist
        }
        
        url = "jdbc:mysql://127.0.0.1:3306/" + database;
        props.put("karaf.lock.jdbc.url", url);
        lock = createLock(props);
        
        
        // should throw an exeption, if the database doesn't exists
        Connection connection = getConnection("jdbc:mysql://127.0.0.1:3306/" + database, user, password);
        assertFalse(connection.isClosed());
        
        executeStatement("DROP DATABASE " + database);
        close(connection);
    }
}