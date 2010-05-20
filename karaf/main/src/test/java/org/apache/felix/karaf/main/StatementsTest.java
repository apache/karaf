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

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class StatementsTest {
    
    private static final String DEFAULT_CREATE_TABLE_STMT = "CREATE TABLE KARAF_LOCK (MOMENT BIGINT, NODE VARCHAR(20))";
    private static final String DEFAULT_POPULATE_TABLE_STMT = "INSERT INTO KARAF_LOCK (MOMENT, NODE) VALUES (1, 'karaf')";
    
    private Statements statements;
    
    @Before
    public void setUp() {
        statements = new Statements();
    }

    @Test
    public void getDefaultLockCreateSchemaStatements() {
        assertArrayEquals(new String[] {DEFAULT_CREATE_TABLE_STMT, DEFAULT_POPULATE_TABLE_STMT}, statements.getLockCreateSchemaStatements(1));
    }
    
    @Test
    public void getCustomLockCreateSchemaStatements() {
        customizeStatements();
        String[] expectedCreateSchemaStmts = new String[] {
                "CREATE TABLE test.LOCK_TABLE (MOMENT NUMBER(20), NODE VARCHAR2(30))", 
                "INSERT INTO test.LOCK_TABLE (MOMENT, NODE) VALUES (2, 'node_1')"};
        
        assertArrayEquals(expectedCreateSchemaStmts, statements.getLockCreateSchemaStatements(2));
    }

    @Test
    public void getDefaultLockCreateStatement() {
        assertEquals("SELECT * FROM KARAF_LOCK FOR UPDATE", statements.getLockCreateStatement());
    }
    
    @Test
    public void getCustomLockCreateStatement() {
        customizeStatements();
        
        assertEquals("SELECT * FROM test.LOCK_TABLE FOR UPDATE", statements.getLockCreateStatement());
    }

    @Test
    public void getDefaultLockUpdateStatement() {
        assertEquals("UPDATE KARAF_LOCK SET MOMENT = 1", statements.getLockUpdateStatement(1));
    }
    
    @Test
    public void getCustomLockUpdateStatement() {
        customizeStatements();
        
        assertEquals("UPDATE test.LOCK_TABLE SET MOMENT = 2", statements.getLockUpdateStatement(2));
    }
    
    private void customizeStatements() {
        statements.setTablePrefix("test.");
        statements.setTableName("LOCK_TABLE");
        statements.setNodeName("node_1");
        statements.setMomentColumnDataType("NUMBER(20)");
        statements.setNodeColumnDataType("VARCHAR2(30)");
    }
}