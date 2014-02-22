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
package org.apache.karaf.jaas.modules.properties;

import java.io.File;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.apache.felix.utils.properties.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesInstaller implements ArtifactInstaller {

    private final Logger LOGGER = LoggerFactory.getLogger(PropertiesInstaller.class);

    private String usersFileName;

    private File usersFile;

    PropertiesLoginModule propertiesLoginModule;

    public PropertiesInstaller(PropertiesLoginModule propertiesLoginModule, String usersFile) {
        this.propertiesLoginModule = propertiesLoginModule;
        this.usersFileName = usersFile;
    }

    public boolean canHandle(File artifact) {
        if (usersFile == null) {
            usersFile = new File(usersFileName);
        }
        return artifact.getName().endsWith(usersFile.getName());
    }

    public void install(File artifact) throws Exception {
        if (usersFile == null) {
            usersFile = new File(usersFileName);
        }
        Properties userProperties = new Properties(usersFile);
        this.propertiesLoginModule.encryptedPassword(userProperties);
    }

    public void update(File artifact) throws Exception {
        if (usersFile == null) {
            usersFile = new File(usersFileName);
        }
        Properties userProperties = new Properties(usersFile);
        this.propertiesLoginModule.encryptedPassword(userProperties);
    }

    public void uninstall(File artifact) throws Exception {
        LOGGER.warn("the users.properties was removed");
    }

}
