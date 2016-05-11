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
package org.apache.karaf.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.channels.FileLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.util.locks.FileLockUtils;
import org.osgi.framework.launch.Framework;

public class InstanceHelper {

    static void updateInstancePid(final File karafHome, final File karafBase, final boolean isStartingInstance) {
        try {
            final String instanceName = System.getProperty("karaf.name");
            final String pid = isStartingInstance ? getPid() : "0";

            if (instanceName != null) {
                String storage = System.getProperty("karaf.instances");
                if (storage == null) {
                    throw new Exception("System property 'karaf.instances' is not set. \n" +
                            "This property needs to be set to the full path of the instance.properties file.");
                }
                File storageFile = new File(storage);
                final File propertiesFile = new File(storageFile, "instance.properties");
                if (!propertiesFile.getParentFile().exists()) {
                    try {
                        if (!propertiesFile.getParentFile().mkdirs()) {
                            throw new Exception("Unable to create directory " + propertiesFile.getParentFile());
                        }
                    } catch (SecurityException se) {
                        throw new Exception(se.getMessage());
                    }
                }
                // don't instance.properties if we're stopping and can't acquire lock
                if (!isStartingInstance) {
                    RandomAccessFile raf = new RandomAccessFile(propertiesFile, "rw");
                    boolean proceed = true;
                    try {
                        FileLock lock = raf.getChannel().tryLock();
                        if (lock == null) {
                            proceed = false;
                        } else {
                            lock.release();
                        }
                    } finally {
                        // if proceed is true than we got the lock or OverlappingFileLockException
                        // but we may proceed in either case
                        raf.close();
                    }
                    if (!proceed) {
                        // we didn't acquire lock, it may mean that root container is holding the lock when
                        // stopping the child
                        return;
                    }
                }
                FileLockUtils.execute(propertiesFile, new FileLockUtils.RunnableWithProperties() {
                    public void run(Properties props) throws IOException {
                        if (props.isEmpty()) {
                            // it's the first instance running, so we consider as root
                            props.setProperty("count", "1");
                            props.setProperty("item.0.name", instanceName);
                            props.setProperty("item.0.loc", karafBase.getAbsolutePath());
                            props.setProperty("item.0.pid", pid);
                            props.setProperty("item.0.root", "true");
                        } else {
                            int count = Integer.parseInt(props.getProperty("count"));
                            for (int i = 0; i < count; i++) {
                                String name = props.getProperty("item." + i + ".name");
                                if (name.equals(instanceName)) {
                                    props.setProperty("item." + i + ".pid", pid);
                                    return;
                                }
                            }
                            // it's not found, let assume it's the root instance, so 0
                            props.setProperty("item.0.name", instanceName);
                            props.setProperty("item.0.pid", pid);
                        }
                    }
                }, true);
            }
        } catch (Exception e) {
            System.err.println("Unable to update instance pid: " + e.getMessage());
        }
    }

    private static String getPid() {
        String pid = ManagementFactory.getRuntimeMXBean().getName();
        if (pid.indexOf('@') > 0) {
            pid = pid.substring(0, pid.indexOf('@'));
        }
        return pid;
    }

    private static void writePid(String pidFile) {
        try {
            if (pidFile != null) {
                RuntimeMXBean rtb = ManagementFactory.getRuntimeMXBean();
                String processName = rtb.getName();
                Pattern pattern = Pattern.compile("^([0-9]+)@.+$", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(processName);
                if (matcher.matches()) {
                    int pid = Integer.parseInt(matcher.group(1));
                    Writer w = new OutputStreamWriter(new FileOutputStream(pidFile));
                    w.write(Integer.toString(pid));
                    w.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void setupShutdown(ConfigProperties config, Framework framework) {
        writePid(config.pidFile);
        try {
            int port = config.shutdownPort;
            String host = config.shutdownHost;
            String portFile = config.portFile;
            final String shutdown = config.shutdownCommand;
            if (port >= 0) {
                ServerSocket shutdownSocket = new ServerSocket(port, 1, InetAddress.getByName(host));
                if (port == 0) {
                    port = shutdownSocket.getLocalPort();
                }
                if (portFile != null) {
                    Writer w = new OutputStreamWriter(new FileOutputStream(portFile));
                    w.write(Integer.toString(port));
                    w.close();
                }
                Thread thread = new ShutdownSocketThread(shutdown, shutdownSocket, framework);
                thread.setDaemon(true);
                thread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
