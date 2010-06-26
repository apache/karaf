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

import java.util.Properties;

/**
 * Represents an exclusive lock on a database,
 * used to avoid multiple Karaf instances attempting
 * to become master.
 * 
 * @version $Revision: $
 */
public class OracleJDBCLock extends DefaultJDBCLock {
    
    private static final String MOMENT_COLUMN_DATA_TYPE = "NUMBER(20)";

    public OracleJDBCLock(Properties props) {
        super(props);
    }

    @Override
    Statements createStatements() {
        Statements statements = new Statements();
        statements.setTableName(table);
        statements.setNodeName(clusterName);
        statements.setMomentColumnDataType(MOMENT_COLUMN_DATA_TYPE);
        return statements;
    }
    
    /**
     * When we perform an update on a long lived locked table, Oracle will save
     * a copy of the transaction in it's UNDO table space. Eventually this can
     * cause the UNDO table to become full, disrupting all locks in the DB instance.
     * A select query just touches the table, ensuring we can still read the DB but
     * doesn't add to the UNDO. 
     */
    @Override
    public boolean lock() {
        return aquireLock();
    }
    
    /**
     * When we perform an update on a long lived locked table, Oracle will save
     * a copy of the transaction in it's UNDO table space. Eventually this can
     * cause the UNDO table to become full, disrupting all locks in the DB instance.
     * A select query just touches the table, ensuring we can still read the DB but
     * doesn't add to the UNDO. 
     */
    @Override
    boolean updateLock() {
        return aquireLock();
    }
}