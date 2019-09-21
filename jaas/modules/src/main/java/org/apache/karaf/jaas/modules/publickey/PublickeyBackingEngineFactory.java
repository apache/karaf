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
package org.apache.karaf.jaas.modules.publickey;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.BackingEngineFactory;
import org.apache.karaf.jaas.modules.JAASUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class PublickeyBackingEngineFactory implements BackingEngineFactory {

    private final Logger logger = LoggerFactory.getLogger(PublickeyBackingEngineFactory.class);

    private static final String USER_FILE = "users";

    public BackingEngine build(Map<String, ?> options) {
        String usersFile = JAASUtils.getString(options, USER_FILE);

        File f = new File(usersFile);
        try {
            Properties users = new Properties(f);
            return new PublickeyBackingEngine(users);
        } catch (IOException ioe) {
            logger.warn("Cannot open keys file:" + usersFile);
        }
        return null;
    }

    public String getModuleClass() {
        return PublickeyLoginModule.class.getName();
    }

}
