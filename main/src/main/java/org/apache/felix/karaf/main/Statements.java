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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class Statements {

    private static Logger LOG = Logger.getLogger(Statements.class.getName());
    private String lockTableName = "KARAF_LOCK";
    private String clusterName = "karaf";
    private String dbName = "sample";
    private String time = "TIME";
    private String cluster = "CLUSTER";
    private String lockCreateStatement;
    private String lockDBCreateStatement;
    private String lockPopulateStatement;

    public Statements(String tableName, String clusterName) {
        LOG.addHandler( BootstrapLogManager.getDefaultHandler() );
        
        this.lockTableName = tableName; 
        this.clusterName = clusterName;
        this.lockCreateStatement="create table " + lockTableName + " (TIME bigint, CLUSTER varchar(20))";
        this.lockPopulateStatement="insert into " + lockTableName + " (TIME, CLUSTER) values (1, '" + clusterName + "')";
    }

    public Statements(String dbName, String tableName, String clusterName) {
        LOG.addHandler( BootstrapLogManager.getDefaultHandler() );
        
        this.dbName = dbName;
        this.lockTableName = tableName;
        this.clusterName = clusterName;
        this.lockDBCreateStatement="create database if not exists " + dbName;
        this.lockCreateStatement="create table " + lockTableName + " (TIME bigint, CLUSTER varchar(20)) ENGINE = INNODB";
        this.lockPopulateStatement="insert into " + lockTableName + " (TIME, CLUSTER) values (1, '" + clusterName + "')";
    }

    public void setDBCreateStatement(String createDB) {
        this.lockDBCreateStatement = createDB;
    }

    public void setCreateStatement(String createTable) {
        this.lockCreateStatement = createTable;
    }

    public void setPopulateStatement(String popTable) {
        this.lockPopulateStatement = popTable;
    }
 
    public void setColumnNames(String time, String cluster) {
        this.time = time;
        this.cluster = cluster; 
    } 

    public String setUpdateCursor() {
        String test = "SELECT * FROM " + lockTableName + " FOR UPDATE";
        return test;
    }

    public String getLockUpdateStatement(long timeStamp) {
        String lockUpdateStatement = "";
        lockUpdateStatement = "UPDATE " + lockTableName + 
                              " SET " + this.time + "=" + timeStamp + 
                              " WHERE " + this.cluster + " = '" + clusterName + "'";
        return lockUpdateStatement;
    }

    public void init (Connection lockConnection, String dbName) {
        Statement s = null;
        try {
            s = lockConnection.createStatement();
            s.execute(lockDBCreateStatement);
        } catch (SQLException e) {
            LOG.severe("SQL Exception: " + e +
                      " " + e.getMessage());
        } catch (Exception ignore) {
            LOG.severe("Could not create database: " + ignore +
                      " " + ignore.getMessage());
        } finally {
            try {
                s.close();
            } catch (Throwable e) {
                // ignore
            }
        }
    }

    /**
     * init - initialize db
     */
    public void init (Connection lockConnection) {
        Statement s = null;
        try {
            // Check to see if the table already exists. If it does, then don't
            // log warnings during startup.
            // Need to run the scripts anyways since they may contain ALTER
            // statements that upgrade a previous version
            // of the table
            boolean alreadyExists = false;
            ResultSet rs = null;
            try {
                rs = lockConnection.getMetaData().getTables(null, null, lockTableName, new String[] {"TABLE"});
                alreadyExists = rs.next();
            } catch (Throwable ignore) {
                LOG.severe("Error testing for db table: " + ignore);
            } finally {
                close(rs);
            }
            if (alreadyExists) {
                return;
            }
            s = lockConnection.createStatement();
            String[] createStatments = {lockCreateStatement, lockPopulateStatement};
            for (int i = 0; i < createStatments.length; i++) {
                // This will fail usually since the tables will be
                // created already.
                try {
                    s.execute(createStatments[i]);
                } catch (SQLException e) {
                    LOG.severe("Could not create JDBC tables; they could already exist."
                             + " Failure was: " + createStatments[i] + " Message: " + e.getMessage()
                             + " SQLState: " + e.getSQLState() + " Vendor code: " + e.getErrorCode());
                }
            }
            lockConnection.commit();
        } catch (Exception ignore) {
            LOG.severe("Error occured during initialization: " + ignore);
        } finally {
            try {
                if (s != null) { s.close(); }
            } catch (Throwable e) {
                LOG.severe("Error occured while closing connection: " + e);
            }
        }
    }

    private static void close(ResultSet rs) {
        try {
            rs.close();
        } catch (Throwable e) {
            LOG.severe("Error occured while releasing ResultSet: " + e);
        }
    }

}
