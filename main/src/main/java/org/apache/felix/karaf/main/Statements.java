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


public class Statements {
    
    protected String tablePrefix = "";
    protected String tableName = "KARAF_LOCK";
    protected String nodeName = "karaf";
    protected String momentColumnDataType = "BIGINT";
    protected String nodeColumnDataType = "VARCHAR(20)";
    
    private String[] lockCreateSchemaStatements;
    private String lockCreateStatement;
    private String lockUpdateStatement;
    
    public String[] getLockCreateSchemaStatements(long moment) {
        if (lockCreateSchemaStatements == null) {
            lockCreateSchemaStatements = new String[] {
                "CREATE TABLE " + getFullLockTableName() + " (MOMENT " + getMomentColumnDataType() + ", NODE " + getNodeColumnDataType() + ")",
                "INSERT INTO " + getFullLockTableName() + " (MOMENT, NODE) VALUES (" + moment + ", '" + getNodeName() + "')", 
            };
        }
        return lockCreateSchemaStatements;
    }
    
    public void setLockCreateSchemaStatements(String[] lockCreateSchemaStatements) {
        this.lockCreateSchemaStatements = lockCreateSchemaStatements;
    }
    
    public String getLockCreateStatement() {
        if (lockCreateStatement == null) {
            lockCreateStatement = "SELECT * FROM " + getFullLockTableName() + " FOR UPDATE";
        }
        return lockCreateStatement;
    }
    
    public void setLockCreateStatement(String lockCreateStatement) {
        this.lockCreateStatement = lockCreateStatement;
    }
    
    public String getLockUpdateStatement(long moment) {
        if (lockUpdateStatement == null) {
            lockUpdateStatement = "UPDATE " + getFullLockTableName() + " SET MOMENT = " + moment;
        }
        return lockUpdateStatement;
    }
    
    public void setLockUpdateStatement(String lockUpdateStatement) {
        this.lockUpdateStatement = lockUpdateStatement;
    }

    long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    public String getFullLockTableName() {
        return getTablePrefix() + getTableName();
    }
    
    public void setMomentColumnDataType(String momentColumnDataType) {
        this.momentColumnDataType = momentColumnDataType;
    }
    
    public String getMomentColumnDataType() {
        return momentColumnDataType;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getNodeColumnDataType() {
        return nodeColumnDataType;
    }

    public void setNodeColumnDataType(String nodeColumnDataType) {
        this.nodeColumnDataType = nodeColumnDataType;
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }
    
    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
}