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
package org.apache.felix.karaf.shell.admin.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Properties;

import org.apache.felix.karaf.shell.admin.Instance;
import org.apache.felix.karaf.jpm.Process;
import org.apache.felix.karaf.jpm.ProcessBuilderFactory;
import org.apache.felix.karaf.jpm.impl.ScriptUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstanceImpl implements Instance {

    private static final Logger LOG = LoggerFactory.getLogger(InstanceImpl.class);

    private AdminServiceImpl service;
    private String name;
    private String location;
    private Process process;

    public InstanceImpl(AdminServiceImpl service, String name, String location) {
        this.service = service;
        this.name = name;
        this.location = location;
    }

    public void attach(int pid) throws IOException {
        checkProcess();
        if (this.process != null) {
            throw new IllegalStateException("Instance already started");
        }
        this.process = ProcessBuilderFactory.newInstance().newBuilder().attach(pid);
    }

    public String getName() {
        return this.name;
    }

    public String getLocation() {
        return location;
    }

    public boolean exists() {
        return new File(location).isDirectory();
    }

    public int getPid() {
        checkProcess();
        return this.process != null ? this.process.getPid() : 0;
    }

    public int getPort() {
        InputStream is = null;
        try {
            File f = new File(location, "etc/org.apache.felix.karaf.shell.cfg");
            is = new FileInputStream(f);
            Properties props = new Properties();
            props.load(is);
            String loc = props.getProperty("sshPort");
            return Integer.parseInt(loc);
        } catch (Exception e) {
            return 0;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    public void changePort(int port) throws Exception {
        checkProcess();
        if (this.process != null) {
            throw new IllegalStateException("Instance not stopped");
        }
        Properties props = new Properties();
        File f = new File(location, "etc/org.apache.felix.karaf.shell.cfg");
        InputStream is = new FileInputStream(f);
        try {
            props.load(is);
        } finally {
            is.close();
        }
        props.setProperty("sshPort", Integer.toString(port));
        OutputStream os = new FileOutputStream(f);
        try {
            props.store(os, null);
        } finally {
            os.close();
        }
    }

    public synchronized void start(String javaOpts) throws Exception {
        checkProcess();
        if (this.process != null) {
            throw new IllegalStateException("Instance already started");
        }
        if (javaOpts == null) {
            javaOpts = "-server -Xmx512M -Dcom.sun.management.jmxremote";
        }
        File libDir = new File(System.getProperty("karaf.home"), "lib");
        File[] jars = libDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
        StringBuilder classpath = new StringBuilder();
        for (File jar : jars) {
            if (classpath.length() > 0) {
                classpath.append(System.getProperty("path.separator"));
            }
            classpath.append(jar.getCanonicalPath());
        }
        String command = new File(System.getProperty("java.home"), ScriptUtils.isWindows() ? "bin\\java.exe" : "bin/java").getCanonicalPath()
                + " " + javaOpts
                + " -Djava.util.logging.config.file=\"" + new File(location, "etc/java.util.logging.properties").getCanonicalPath() + "\""
                + " -Dkaraf.home=\"" + System.getProperty("karaf.home") + "\""
                + " -Dkaraf.base=\"" + new File(location).getCanonicalPath() + "\""
                + " -Dkaraf.startLocalConsole=false"
                + " -Dkaraf.startRemoteShell=true"
                + " -classpath " + classpath.toString()
                + " org.apache.felix.karaf.main.Bootstrap";
        LOG.debug("Starting instance " + name + " with command: " + command);
        this.process = ProcessBuilderFactory.newInstance().newBuilder()
                        .directory(new File(location))
                        .command(command)
                        .start();
        this.service.saveState();
    }

    public synchronized void stop() throws Exception {
        checkProcess();
        if (this.process == null) {
            throw new IllegalStateException("Instance not started");
        }
        this.process.destroy();
    }

    public synchronized void destroy() throws Exception {
        checkProcess();
        if (this.process != null) {
            throw new IllegalStateException("Instance not stopped");
        }
        deleteFile(new File(location));
        this.service.forget(name);
        this.service.saveState();
    }


    public synchronized String getState() {
        int port = getPort();
        if (!exists() || port <= 0) {
            return ERROR;
        }
        checkProcess();
        if (this.process == null) {
            return STOPPED;
        } else {
            try {
                Socket s = new Socket("localhost", port);
                s.close();
                return STARTED;
            } catch (Exception e) {
                // ignore
            }
            return STARTING;
        }
    }

    protected void checkProcess() {
        if (this.process != null) {
            try {
                if (!this.process.isRunning()) {
                    this.process = null;
                }
            } catch (IOException e) {
            }
        }
    }

    protected static boolean deleteFile(File fileToDelete) {
        if (fileToDelete == null || !fileToDelete.exists()) {
            return true;
        }
        boolean result = true;
        if (fileToDelete.isDirectory()) {
            File[] files = fileToDelete.listFiles();
            if (files == null) {
                result = false;
            } else {
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    if (file.getName().equals(".") || file.getName().equals("..")) {
                        continue;
                    }
                    if (file.isDirectory()) {
                        result &= deleteFile(file);
                    } else {
                        result &= file.delete();
                    }
                }
            }
        }
        result &= fileToDelete.delete();
        return result;
    }
}
