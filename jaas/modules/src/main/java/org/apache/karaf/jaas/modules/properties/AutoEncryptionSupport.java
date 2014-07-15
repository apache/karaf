/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.jaas.modules.properties;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.modules.AbstractKarafLoginModule;
import org.apache.karaf.jaas.modules.Encryption;
import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoEncryptionSupport {

    public static final String PERIOD = "encryption.check.period";

    private final Logger LOGGER = LoggerFactory.getLogger(AutoEncryptionSupport.class);

    private final String usersFileName;

    private final EncryptionSupport encryptionSupport;

    private final File usersFile;

    private final long period;

    private final Timer timer = new  Timer();

    public AutoEncryptionSupport(Map options) {
        this.encryptionSupport = new EncryptionSupport(options);
        this.usersFileName = (String) options.get(PropertiesLoginModule.USER_FILE);
        this.usersFile = new File(usersFileName).getAbsoluteFile();
        Object period = options.get(PERIOD);
        this.period = period != null ? Long.parseLong(period.toString()) : 60 * 1000l;
    }

    public void init() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    Properties userProperties = new Properties(usersFile);
                    encryptedPassword(userProperties);
                } catch (IOException ioe) {
                    LOGGER.warn("Unable to encrypt user properties file ", ioe);
                }
            }
        };
        timer.schedule(task, 0l, period);
    }

    public void destroy() {
        timer.cancel();
    }

    void encryptedPassword(Properties users) throws IOException {
        boolean changed = false;
        for (String userName : users.keySet()) {
            String user = userName;
            String userInfos = users.get(user);

            if (user.startsWith(PropertiesBackingEngine.GROUP_PREFIX)) {
                continue;
            }

            // the password is in the first position
            String[] infos = userInfos.split(",");
            String storedPassword = infos[0];

            // check if the stored password is flagged as encrypted
            String encryptedPassword = getEncryptedPassword(storedPassword);
            if (!storedPassword.equals(encryptedPassword)) {
                LOGGER.debug("The password isn't flagged as encrypted, encrypt it.");
                userInfos = encryptedPassword + ",";
                for (int i = 1; i < infos.length; i++) {
                    if (i == (infos.length - 1)) {
                        userInfos = userInfos + infos[i];
                    } else {
                        userInfos = userInfos + infos[i] + ",";
                    }
                }
                if (user.contains("\\")) {
                    users.remove(user);
                    user = user.replace("\\", "\\\\");
                }
                users.put(user, userInfos);
                changed = true;
            }
        }
        if (changed) {
            users.save();
        }
    }

    String getEncryptedPassword(String password) {
        Encryption encryption = encryptionSupport.getEncryption();
        String encryptionPrefix = encryptionSupport.getEncryptionPrefix();
        String encryptionSuffix = encryptionSupport.getEncryptionSuffix();

        if (encryption == null) {
            return password;
        } else {
            boolean prefix = encryptionPrefix == null || password.startsWith(encryptionPrefix);
            boolean suffix = encryptionSuffix == null || password.endsWith(encryptionSuffix);
            if (prefix && suffix) {
                return password;
            } else {
                String p = encryption.encryptPassword(password);
                if (encryptionPrefix != null) {
                    p = encryptionPrefix + p;
                }
                if (encryptionSuffix != null) {
                    p = p + encryptionSuffix;
                }
                return p;
            }
        }
    }

}
