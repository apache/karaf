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
package org.apache.karaf.jaas.modules.syncope;

import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.BackingEngineFactory;
import org.apache.karaf.jaas.modules.JAASUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SyncopeBackingEngineFactory implements BackingEngineFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncopeBackingEngineFactory.class);

    public BackingEngine build(Map<String, ?> options) {
        SyncopeBackingEngine instance = null;
        String address = JAASUtils.getString(options, SyncopeLoginModule.ADDRESS);
        String adminUser = JAASUtils.getString(options, SyncopeLoginModule.ADMIN_USER);
        String adminPassword = JAASUtils.getString(options, SyncopeLoginModule.ADMIN_PASSWORD);
        String version = JAASUtils.getString(options, SyncopeLoginModule.VERSION);

        try {
            instance = new SyncopeBackingEngine(address, version, adminUser, adminPassword);
        } catch (Exception e) {
            LOGGER.error("Error creating the Syncope backing engine", e);
        }

        return instance;
    }

    /**
     * Returns the login module class, that this factory can build.
     */
    public String getModuleClass() {
        return SyncopeLoginModule.class.getName();
    }

}
