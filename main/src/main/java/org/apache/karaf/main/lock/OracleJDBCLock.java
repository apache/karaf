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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.apache.felix.utils.properties.Properties;

/**
 * Represents an exclusive lock on a database,
 * used to avoid multiple Karaf instances attempting
 * to become master.
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
        return acquireLock();
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
        return acquireLock();
    }

    /**
     * A SELECT FOR UPDATE does not create a database lock when the SELECT FOR UPDATE is performed
     * on an empty selection. So a succesfull call to {@link DefaultJDBCLock#acquireLock()} is not sufficient to
     * ensure that we are the only one who have acquired the lock.
     */
    @Override
    boolean acquireLock() {
        return super.acquireLock() && lockAcquiredOnNonEmptySelection();
    }

    //Verify that we have a non empty record set.
    private boolean lockAcquiredOnNonEmptySelection() {
        String verifySelectionNotEmpytStatement = statements.getLockVerifySelectionNotEmptyStatement();
        PreparedStatement preparedStatement = null;
        boolean lockAcquired = false;

        try {
            preparedStatement = getConnection().prepareStatement(verifySelectionNotEmpytStatement);
            preparedStatement.setQueryTimeout(timeout);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                lockAcquired = rs.getInt(1) > 0;
            } else {
                LOG.warning("Failed to acquire database lock. Missing database lock record.");
            }
        } catch (Exception e) {
            LOG.warning("Failed to acquire database lock: " + e);
        } finally {
            closeSafely(preparedStatement);
        }
        return lockAcquired;
    }
}
