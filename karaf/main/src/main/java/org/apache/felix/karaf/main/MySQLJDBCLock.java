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

import java.sql.Connection;
import java.util.Properties;

/**
 * Represents an exclusive lock on a database,
 * used to avoid multiple Karaf instances attempting
 * to become master.
 * 
 * @version $Revision: $
 */
public class MySQLJDBCLock extends DefaultJDBCLock {

    public MySQLJDBCLock(Properties props) {
        super(props);
    }

    Statements createStatements() {
        Statements statements = new Statements();
        statements.setTableName(table);
        statements.setNodeName(clusterName);
        String[] lockCreateSchemaStatements = statements.getLockCreateSchemaStatements(getCurrentTimeMillis());
        for (int index = 0; index < lockCreateSchemaStatements.length; index++) {
            if (lockCreateSchemaStatements[index].toUpperCase().startsWith("CREATE TABLE")) {
                lockCreateSchemaStatements[index] = lockCreateSchemaStatements[index] + " ENGINE=INNODB";
            }
        }
        return statements;
    }
    
    @Override
    Connection createConnection(String driver, String url, String username, String password) throws Exception {
        url = (url.toLowerCase().contains("createDatabaseIfNotExist=true")) ? 
            url : 
            ((url.contains("?")) ? 
                url + "&createDatabaseIfNotExist=true" : 
                url + "?createDatabaseIfNotExist=true");
        
        return super.createConnection(driver, url, username, password);
    }
}