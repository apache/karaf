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
package org.apache.felix.karaf.main;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Properties;
import java.util.logging.Logger;

public class SimpleFileLock implements Lock {

    private static final Logger LOG = Logger.getLogger(SimpleFileLock.class.getName());
    private static final String PROPERTY_LOCK_DIR = "karaf.lock.dir";
    private static final String PROP_KARAF_BASE = "karaf.base";
    private RandomAccessFile lockFile;
    private FileLock lock;

    public SimpleFileLock(Properties props) {
        try {

            LOG.addHandler( BootstrapLogManager.getDefaultHandler() );
            String lock = props.getProperty(PROPERTY_LOCK_DIR);

            if (lock != null) {
                File karafLock = getKarafLock(new File(lock), props);
                props.setProperty(PROPERTY_LOCK_DIR, karafLock.getPath());
            } else {
                props.setProperty(PROPERTY_LOCK_DIR, System.getProperty(PROP_KARAF_BASE));
            }

            File base = new File(props.getProperty(PROPERTY_LOCK_DIR));
            lockFile = new RandomAccessFile(new File(base, "lock"), "rw");
        } catch (IOException e) {
            throw new RuntimeException("Could not create file lock", e);
        }
    }

    public boolean lock() throws Exception {
        LOG.info("locking");
        if (lock == null) {
            lock = lockFile.getChannel().tryLock();
        }
        return lock != null;
    }

    public void release() throws Exception {
        LOG.info("releasing");
        if (lock != null && lock.isValid()) {
            lock.release();
            lock.channel().close();
        }
        lock = null;
    }
 
    public boolean isAlive() throws Exception {
        return lock != null;
    }

    private static File getKarafLock(File lock,Properties props) {
        File rc = null;

        String path = lock.getPath();
        if (path != null) {
            rc = validateDirectoryExists(path, "Invalid " + PROPERTY_LOCK_DIR + " system property");
        }

        if (rc == null) {
            path = props.getProperty(PROP_KARAF_BASE);
            if (path != null) {
                rc = validateDirectoryExists(path, "Invalid " + PROP_KARAF_BASE + " property");
            }
        }

        if (rc == null) {
            rc = lock;
        }
        return rc;
    }

    private static File validateDirectoryExists(String path, String errPrefix) {
        File rc;
        try {
            rc = new File(path).getCanonicalFile();
        } catch (IOException e) {
            throw new IllegalArgumentException(errPrefix + " '" + path + "' : " + e.getMessage());
        }
        if (!rc.exists()) {
            throw new IllegalArgumentException(errPrefix + " '" + path + "' : does not exist");
        }
        if (!rc.isDirectory()) {
            throw new IllegalArgumentException(errPrefix + " '" + path + "' : is not a directory");
        }
        return rc;
    }

}
