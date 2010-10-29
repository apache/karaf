/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.karaf.jaas.modules.jdbc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author iocanel
 */
public class JDBCBackingEngine implements BackingEngine {

    private static final Log LOG = LogFactory.getLog(JDBCBackingEngine.class);

    private DataSource dataSource;
    private EncryptionSupport encryptionSupport;

    private static final String MSG_CONNECTION_CLOSE_FAILED = "Failed to clearly close connection to the database:";

    private String addUserStatement = "INSERT INTO USERS VALUES(?,?)";
    private String addRoleStatement = "INSERT INTO ROLES VALUES(?,?)";
    private String deleteRoleStatement = "DELETE FROM ROLES WHERE USERNAME=? AND ROLE=?";
    private String deleteAllUserRolesStatement = "DELETE FROM ROLES WHERE USERNAME=?";
    private String deleteUserStatement = "DELETE FROM USERS WHERE USERNAME=?";


    /**
     * Constructor
     *
     * @param dataSource
     */
    public JDBCBackingEngine(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public JDBCBackingEngine(DataSource dataSource, EncryptionSupport encryptionSupport) {
        this.dataSource = dataSource;
        this.encryptionSupport = encryptionSupport;
    }

    /**
     * Adds a new user.
     *
     * @param username
     * @param password
     */
    public void addUser(String username, String password) {
        Connection connection = null;
        PreparedStatement statement = null;

        String newPassword = password;

        //If encryption support is enabled, encrypt password
        if (encryptionSupport != null && encryptionSupport.getEncryption() != null) {
            newPassword = encryptionSupport.getEncryption().encryptPassword(password);
        }

        if (dataSource != null) {

            try {
                connection = dataSource.getConnection();
                statement = connection.prepareStatement(addUserStatement);
                statement.setString(1, username);
                statement.setString(2, newPassword);
                int rows = statement.executeUpdate();

                if (!connection.getAutoCommit()) {
                    connection.commit();
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Executiong [%s], USERNAME=%s, PASSWORD=%s. %i rows affected.", addUserStatement, username, newPassword, rows));
                }
            } catch (SQLException e) {
                LOG.error("Error executiong statement", e);
            } finally {
                try {
                    if (statement != null) {
                        statement.close();
                    }
                    if (connection != null) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    LOG.warn(MSG_CONNECTION_CLOSE_FAILED, e);
                }
            }
        }
    }

    /**
     * Delete user by username.
     *
     * @param username
     */
    public void deleteUser(String username) {
        Connection connection = null;
        PreparedStatement userStatement = null;
        PreparedStatement roleStatement = null;

        if (dataSource != null) {

            try {
                connection = dataSource.getConnection();

                //Remove from roles
                roleStatement = connection.prepareStatement(deleteAllUserRolesStatement);
                roleStatement.setString(1, username);
                roleStatement.executeUpdate();

                //Remove from users
                userStatement = connection.prepareStatement(deleteUserStatement);
                userStatement.setString(1, username);
                int userRows = userStatement.executeUpdate();

                if (!connection.getAutoCommit()) {
                    connection.commit();
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Executiong [%s], USERNAME=%s. %i userRows affected.", deleteUserStatement, username, userRows));
                }
            } catch (SQLException e) {
                LOG.error("Error executiong statement", e);
            } finally {
                try {
                    if (userStatement != null) {
                        userStatement.close();
                    }
                    if (roleStatement != null) {
                        roleStatement.close();
                    }
                    if (connection != null) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    LOG.warn(MSG_CONNECTION_CLOSE_FAILED, e);
                }
            }
        }
    }

    /**
     * Add a role to a user.
     *
     * @param username
     * @param role
     */
    public void addRole(String username, String role) {
        Connection connection = null;
        PreparedStatement statement = null;

        if (dataSource != null) {

            try {
                connection = dataSource.getConnection();
                statement = connection.prepareStatement(addRoleStatement);
                statement.setString(1, username);
                statement.setString(2, role);
                int rows = statement.executeUpdate();

                if (!connection.getAutoCommit()) {
                    connection.commit();
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Executiong [%s], USERNAME=%s, ROLE=%s. %i rows affected.", addRoleStatement, username, role, rows));
                }
            } catch (SQLException e) {
                LOG.error("Error executiong statement", e);
            } finally {
                try {
                    if (statement != null) {
                        statement.close();
                    }
                    if (connection != null) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    LOG.warn(MSG_CONNECTION_CLOSE_FAILED, e);
                }
            }
        }
    }

    /**
     * Remove role from user.
     *
     * @param username
     * @param role
     */
    public void deleteRole(String username, String role) {
        Connection connection = null;
        PreparedStatement statement = null;

        if (dataSource != null) {

            try {
                connection = dataSource.getConnection();
                statement = connection.prepareStatement(deleteRoleStatement);
                statement.setString(1, username);
                statement.setString(2, role);
                int rows = statement.executeUpdate();

                if (!connection.getAutoCommit()) {
                    connection.commit();
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Executiong [%s], USERNAME=%s, ROLE=%s. %i rows affected.", deleteRoleStatement, username, role, rows));
                }
            } catch (SQLException e) {
                LOG.error("Error executing statement", e);
            } finally {
                try {
                    if (statement != null) {
                        statement.close();
                    }
                    if (connection != null) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    LOG.warn(MSG_CONNECTION_CLOSE_FAILED, e);
                }
            }
        }
    }
}
