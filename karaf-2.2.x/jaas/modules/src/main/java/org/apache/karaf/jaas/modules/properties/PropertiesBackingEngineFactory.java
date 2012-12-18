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

import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.BackingEngineFactory;
import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;
import org.apache.karaf.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class PropertiesBackingEngineFactory implements BackingEngineFactory {

    private final Logger logger = LoggerFactory.getLogger(PropertiesBackingEngineFactory.class);

    private static final String USER_FILE = "users";

    /**
     * Builds the Backing Engine
     *
     * @param options
     * @return
     */
    public BackingEngine build(Map options) {
        PropertiesBackingEngine engine = null;
        String usersFile = (String) options.get(USER_FILE);

        File f = new File(usersFile);
        Properties users;
        try {
            users = new Properties(f);
            EncryptionSupport encryptionSupport = new EncryptionSupport(options);
            engine = new PropertiesBackingEngine(users, encryptionSupport);
        } catch (IOException ioe) {
            logger.warn("Cannot open users file:" + usersFile);
        } finally {
            return engine;
        }
    }

    /**
     * Returns the login module class, that this factory can build.
     *
     * @return
     */
    public String getModuleClass() {
        return PropertiesLoginModule.class.getName();
    }
}
