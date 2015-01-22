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

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.modules.Encryption;
import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class AutoEncryptionSupport implements Runnable {

    private final Logger LOGGER = LoggerFactory.getLogger(AutoEncryptionSupport.class);

    private WatchService watchService;

    private volatile EncryptionSupport encryptionSupport;

    public AutoEncryptionSupport(Map<String, Object> properties) {
        updated(properties);
    }

    public synchronized void init() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            new Thread(this, "AutoEncryptionSupport").start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void destroy() {
        try {
            if (watchService != null) {
                watchService.close();
                watchService = null;
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    public void updated(Map<String, Object> properties) {
        destroy();
        this.encryptionSupport = new EncryptionSupport(properties);
        init();
    }

    @Override
    public void run() {
        try {
            Path dir = Paths.get(System.getProperty("karaf.etc"));
            if (watchService == null) {
                // just to prevent NPE (KARAF-3460)
                watchService = FileSystems.getDefault().newWatchService();
            }
            dir.register(watchService, ENTRY_MODIFY);

            Path file = dir.resolve("users.properties");
            encryptedPassword(new Properties(file.toFile()));

            while (true) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind kind = event.kind();
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;

                    // Context for directory entry event is the file name of entry
                    Path name = dir.resolve(ev.context());
                    if (file.equals(name)) {
                        encryptedPassword(new Properties(file.toFile()));
                    }
                }
                key.reset();
            }

        } catch (ClosedWatchServiceException | InterruptedException e) {
            // Ignore
        } catch (IOException e) {
            LOGGER.warn("Unable to encrypt user properties file ", e);
        }

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
