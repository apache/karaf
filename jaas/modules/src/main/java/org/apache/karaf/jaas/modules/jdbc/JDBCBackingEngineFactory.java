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

import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.BackingEngineFactory;
import org.apache.karaf.jaas.modules.JAASUtils;
import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Map;

public class JDBCBackingEngineFactory implements BackingEngineFactory {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(JDBCBackingEngineFactory.class);

    /**
     * Build a Backing engine for the JDBCLoginModule.
     */
    public BackingEngine build(Map<String, ?> options) {
        JDBCBackingEngine instance = null;
        String datasourceURL = JAASUtils.getString(options, JDBCUtils.DATASOURCE);
        BundleContext bundleContext = (BundleContext) options.get(BundleContext.class.getName());

        String addUserStatement = JAASUtils.getString(options, JDBCLoginModule.INSERT_USER_STATEMENT);
        String addRoleStatement = JAASUtils.getString(options, JDBCLoginModule.INSERT_ROLE_STATEMENT);
        String deleteRoleStatement = JAASUtils.getString(options, JDBCLoginModule.DELETE_ROLE_STATEMENT);
        String deleteAllUserRolesStatement = JAASUtils.getString(options, JDBCLoginModule.DELETE_ROLES_STATEMENT);
        String deleteUserStatement = JAASUtils.getString(options, JDBCLoginModule.DELETE_USER_STATEMENT);
        String selectUsersQuery = JAASUtils.getString(options, JDBCLoginModule.USER_QUERY);
        String selectRolesQuery = JAASUtils.getString(options, JDBCLoginModule.ROLE_QUERY);

        try {
            DataSource dataSource = JDBCUtils.createDatasource(bundleContext, datasourceURL);
            EncryptionSupport encryptionSupport = new EncryptionSupport(options);
            instance = new JDBCBackingEngine(dataSource, encryptionSupport);
            if(addUserStatement != null) {
                instance.setAddUserStatement(addUserStatement);
            }
            if(addRoleStatement != null) {
                instance.setAddRoleStatement(addRoleStatement);
            }
            if(deleteRoleStatement != null) {
                instance.setDeleteRoleStatement(deleteRoleStatement);
            }
            if(deleteAllUserRolesStatement != null) {
                instance.setDeleteAllUserRolesStatement(deleteAllUserRolesStatement);
            }
            if(deleteUserStatement != null) {
                instance.setDeleteUserStatement(deleteUserStatement);
            }
            if(selectUsersQuery != null) {
                instance.setSelectUsersQuery(selectUsersQuery);
            }
            if(selectRolesQuery != null) {
                instance.setSelectRolesQuery(selectRolesQuery);
            }
        } catch (Exception e) {
            LOGGER.error("Error creating JDBCBackingEngine.", e);
        }
        return instance;
    }

    /**
     * Returns the login module class, that this factory can build.
     */
    public String getModuleClass() {
        return JDBCLoginModule.class.getName();
    }

}
