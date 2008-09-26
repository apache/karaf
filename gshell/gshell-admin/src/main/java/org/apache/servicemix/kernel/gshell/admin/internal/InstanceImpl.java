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
package org.apache.servicemix.kernel.gshell.admin.internal;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.net.URI;

import org.apache.servicemix.jpm.Process;
import org.apache.servicemix.jpm.ProcessBuilderFactory;
import org.apache.servicemix.jpm.impl.ScriptUtils;
import org.apache.servicemix.kernel.gshell.admin.Instance;
import org.apache.geronimo.gshell.common.io.PumpStreamHandler;
import org.apache.geronimo.gshell.command.IO;
import org.apache.geronimo.gshell.spring.ProxyIO;
import org.osgi.service.prefs.BackingStoreException;

public class InstanceImpl implements Instance {

    private AdminServiceImpl service;
    private String name;
    private Process process;
    //private PumpStreamHandler handler;

    public InstanceImpl(AdminServiceImpl service, String name) {
        this.service = service;
        this.name = name;
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

    public int getPid() {
        checkProcess();
        return this.process != null ? this.process.getPid() : 0;
    }

    public int getPort() throws Exception {
        InputStream is = null;
        try {
            File f = new File(name, "etc/org.apache.servicemix.shell.cfg");
            is = new FileInputStream(f);
            Properties props = new Properties();
            props.load(is);
            String loc = props.getProperty("remoteShellLocation");
            URI uri = new URI(loc);
            return uri.getPort();
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public void changePort(int port) throws Exception {
        checkProcess();
        if (this.process != null) {
            throw new IllegalStateException("Instance not stopped");
        }
        Properties props = new Properties();
        File f = new File(name, "etc/org.apache.servicemix.shell.cfg");
        InputStream is = new FileInputStream(f);
        try {
            props.load(is);
        } finally {
            is.close();
        }
        String loc = props.getProperty("remoteShellLocation");
        loc = loc.replace(Integer.toString(new URI(loc).getPort()), Integer.toString(port));
        props.setProperty("remoteShellLocation", loc);
        OutputStream os = new FileOutputStream(f);
        try {
            props.store(os, null);
        } finally {
            os.close();
        }
    }

    public synchronized void start() throws Exception {
        checkProcess();
        if (this.process != null) {
            throw new IllegalStateException("Instance already started");
        }
        String command = new File(System.getProperty("java.home"), "bin/java" + (ScriptUtils.isWindows() ? ".exe" : "")).getCanonicalPath()
                + " -server -Xms128M -Xmx512M -Dcom.sun.management.jmxremote"
                + " -Dservicemix.home=\"" + System.getProperty("servicemix.home") + "\""
                + " -Dservicemix.base=\"" + new File(name).getCanonicalPath() + "\""
                + " -Dservicemix.startLocalConsole=false"
                + " -Dservicemix.startRemoteShell=true"
                + " -classpath "
                + new File(System.getProperty("servicemix.home"), "lib/servicemix.jar").getCanonicalPath()
                + System.getProperty("path.separator")
                + new File(System.getProperty("servicemix.home"), "lib/servicemix-jaas-boot.jar").getCanonicalPath()
                + " org.apache.servicemix.kernel.main.Main";
        this.process = ProcessBuilderFactory.newInstance().newBuilder()
                        .directory(new File(name))
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
        deleteFile(new File(name));
        this.service.forget(name);
        this.service.saveState();
    }


    public synchronized boolean isRunning() {
        checkProcess();
        return this.process != null;
    }

    protected void checkProcess() {
        if (this.process != null) {
            /*
            try {
                this.process.exitValue();
                this.process = null;
                if (this.handler != null) {
                    this.handler.stop();
                    this.handler = null;
                }
            } catch (IllegalThreadStateException e) {
            }
            */
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
