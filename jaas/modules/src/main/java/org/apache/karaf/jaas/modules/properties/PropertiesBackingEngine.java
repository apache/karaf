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

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.modules.AbstractPropertiesBackingEngine;
import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;

public class PropertiesBackingEngine extends AbstractPropertiesBackingEngine {

    private EncryptionSupport encryptionSupport;

    public PropertiesBackingEngine(Properties users) {
        super(users);
        this.encryptionSupport = EncryptionSupport.noEncryptionSupport();
    }

    public PropertiesBackingEngine(Properties users, EncryptionSupport encryptionSupport) {
        super(users);
        this.encryptionSupport = encryptionSupport;
    }

    @Override
    public void addUser(String username, String password) {
        if (username.startsWith(GROUP_PREFIX))
            throw new IllegalArgumentException("Prefix not permitted: " + GROUP_PREFIX);

        addUserInternal(username, encryptionSupport.encrypt(password));
    }
}