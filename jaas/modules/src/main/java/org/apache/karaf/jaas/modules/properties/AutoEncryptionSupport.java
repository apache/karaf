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

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;
import org.apache.karaf.util.StreamUtils;
import org.apache.karaf.util.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoEncryptionSupport implements Runnable, Closeable {

    private final Logger LOGGER = LoggerFactory.getLogger(AutoEncryptionSupport.class);
    private volatile boolean running;
    private EncryptionSupport encryptionSupport;
    private ExecutorService executor;

    private String usersFileName;

    public AutoEncryptionSupport(Map<String, Object> properties) {
        running = true;
        encryptionSupport = new EncryptionSupport(properties);
        Object usersFile = properties.get(PropertiesLoginModule.USER_FILE);
        if (usersFile instanceof File) {
            usersFileName = ((File) usersFile).getAbsolutePath();
        } else if (usersFile != null) {
            usersFileName = usersFile.toString();
        }
        executor = Executors.newSingleThreadExecutor(ThreadUtils.namedThreadFactory("encryption"));
        executor.execute(this);
    }

    public void close() {
        running = false;
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    @Override
    public void run() {
        WatchService watchService = null;
        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path dir = null;
            Path file = null;
            if (usersFileName == null) {
                dir = Paths.get(System.getProperty("karaf.etc"));
                file = dir.resolve("users.properties");
            } else {
                file = new File(usersFileName).toPath();
                dir = file.getParent();
            }
            dir.register(watchService, ENTRY_MODIFY);

            encryptedPassword(new Properties(file.toFile()));

            while (running) {
                try {
                    WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                    if (key == null) {
                        continue;
                    }
                    for (WatchEvent<?> event : key.pollEvents()) {
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> ev = (WatchEvent<Path>)event;

                        // Context for directory entry event is the file name of entry
                        Path name = dir.resolve(ev.context());
                        if (file.equals(name)) {
                            encryptedPassword(new Properties(file.toFile()));
                        }
                    }
                    key.reset();
                } catch (IOException e) {
                    LOGGER.warn(e.getMessage(), e);
                } catch (InterruptedException e) {
                    // Ignore as this happens on shutdown
                }
            }

        } catch (IOException e) {
            LOGGER.warn(e.getMessage(), e);
        } finally {
            StreamUtils.close(watchService);
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
            String encryptedPassword = encryptionSupport.encrypt(storedPassword);
            if (!storedPassword.equals(encryptedPassword)) {
                LOGGER.debug("The password isn't flagged as encrypted, encrypt it.");
                StringBuilder userInfosBuilder = new StringBuilder(encryptedPassword);
                for (int i = 1; i < infos.length; i++) {
                    userInfosBuilder.append(',').append(infos[i]);
                }
                userInfos = userInfosBuilder.toString();

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

}
