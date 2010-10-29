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

package org.apache.karaf.jaas.modules.properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;
import org.apache.karaf.util.Properties;


/**
 * @author iocanel
 */
public class PropertiesBackingEngine implements BackingEngine {

    private static final Log LOG = LogFactory.getLog(PropertiesBackingEngine.class);

    private Properties users;
    private EncryptionSupport encryptionSupport;

    /**
     * Constructor
     *
     * @param users
     */
    public PropertiesBackingEngine(Properties users) {
        this.users = users;
    }

    public PropertiesBackingEngine(Properties users, EncryptionSupport encryptionSupport) {
        this.users = users;
        this.encryptionSupport = encryptionSupport;
    }

    /**
     * Add a user.
     *
     * @param username
     * @param password
     */
    public void addUser(String username, String password) {
        String[] infos = null;
        StringBuffer userInfoBuffer = new StringBuffer();

        String newPassword = password;

        //If encryption support is enabled, encrypt password
        if (encryptionSupport != null && encryptionSupport.getEncryption() != null) {
            newPassword = encryptionSupport.getEncryption().encryptPassword(password);
        }

        String userInfos = users.get(username);

        //If user already exists, update password
        if (userInfos != null && userInfos.length() > 0) {
            infos = userInfos.split(",");
            userInfoBuffer.append(newPassword);

            for (int i = 1; i < infos.length; i++) {
                userInfoBuffer.append(",");
                userInfoBuffer.append(infos[i]);
            }
            String newUserInfo = userInfoBuffer.toString();
            users.put(username, newUserInfo);
        } else {
            users.put(username, newPassword);
        }

        try {
            users.save();
        } catch (Exception ex) {
            LOG.error("Cannot update users file,", ex);
        }
    }

    /**
     * Delete a User.
     *
     * @param username
     */
    public void deleteUser(String username) {
        users.remove(username);

    }

    /**
     * Add a role to a User.
     *
     * @param username
     * @param role
     */
    public void addRole(String username, String role) {
        String userInfos = users.get(username);
        if (userInfos != null) {
            String newUserInfos = userInfos + "," + role;
            users.put(username, newUserInfos);
        }
        try {
            users.save();
        } catch (Exception ex) {
            LOG.error("Cannot update users file,", ex);
        }
    }

    /**
     * Delete a Role form User.
     *
     * @param username
     * @param role
     */
    public void deleteRole(String username, String role) {
        String[] infos = null;
        StringBuffer userInfoBuffer = new StringBuffer();

        String userInfos = users.get(username);

        //If user already exists, remove the role
        if (userInfos != null && userInfos.length() > 0) {
            infos = userInfos.split(",");
            String password = infos[0];
            userInfoBuffer.append(password);

            for (int i = 1; i < infos.length; i++) {
                if (infos[i] != null && !infos[i].equals(role)) {
                    userInfoBuffer.append(",");
                    userInfoBuffer.append(infos[i]);
                }
            }
            String newUserInfo = userInfoBuffer.toString();
            users.put(username, newUserInfo);
        }

        try {
            users.save();
        } catch (Exception ex) {
            LOG.error("Cannot update users file,", ex);
        }
    }
}
