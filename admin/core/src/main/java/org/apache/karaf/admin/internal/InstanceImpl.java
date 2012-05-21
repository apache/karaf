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
package org.apache.karaf.admin.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.karaf.admin.Instance;
import org.apache.karaf.jpm.Process;
import org.apache.karaf.jpm.ProcessBuilderFactory;
import org.apache.karaf.jpm.impl.ScriptUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstanceImpl implements Instance {

    private static final Logger LOG = LoggerFactory.getLogger(InstanceImpl.class);

    private static final String CONFIG_PROPERTIES_FILE_NAME = "config.properties";

    private static final String KARAF_SHUTDOWN_PORT = "karaf.shutdown.port";

    private static final String KARAF_SHUTDOWN_HOST = "karaf.shutdown.host";

    private static final String KARAF_SHUTDOWN_PORT_FILE = "karaf.shutdown.port.file";

    private static final String KARAF_SHUTDOWN_COMMAND = "karaf.shutdown.command";

    private static final String KARAF_SHUTDOWN_PID_FILE = "karaf.shutdown.pid.file";

    private static final String DEFAULT_SHUTDOWN_COMMAND = "SHUTDOWN";

    private AdminServiceImpl service;
    private String name;
    private String location;
    private String javaOpts;
    private Process process;
    private boolean root;

    public InstanceImpl(AdminServiceImpl service, String name, String location, String javaOpts) {
        this(service, name, location, javaOpts, false);
    }
    
    public InstanceImpl(AdminServiceImpl service, String name, String location, String javaOpts, boolean root) {
        this.service = service;
        this.name = name;
        this.location = location;
        this.javaOpts = javaOpts;
        this.root = root;
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

    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isRoot() {
        return root;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean exists() {
        return new File(location).isDirectory();
    }

    public int getPid() {
        checkProcess();
        return this.process != null ? this.process.getPid() : 0;
    }

    public int getSshPort() {
        InputStream is = null;
        try {
            File f = new File(location, "etc/org.apache.karaf.shell.cfg");
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

    public void changeSshPort(int port) throws Exception {
        checkProcess();
        if (this.process != null) {
            throw new IllegalStateException("Instance not stopped");
        }
        Properties props = new Properties();
        File f = new File(location, "etc/org.apache.karaf.shell.cfg");
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

    public int getRmiRegistryPort() {
        InputStream is = null;
        try {
            File f = new File(location, "etc/org.apache.karaf.management.cfg");
            is = new FileInputStream(f);
            Properties props = new Properties();
            props.load(is);
            String loc = props.getProperty("rmiRegistryPort");
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

    public void changeRmiRegistryPort(int port) throws Exception {
        checkProcess();
        if (this.process != null) {
            throw new IllegalStateException("Instance not stopped");
        }
        Properties props = new Properties();
        File f = new File(location, "etc/org.apache.karaf.management.cfg");
        InputStream is = new FileInputStream(f);
        try {
            props.load(is);
        } finally {
            is.close();
        }
        props.setProperty("rmiRegistryPort", Integer.toString(port));
        OutputStream os = new FileOutputStream(f);
        try {
            props.store(os, null);
        } finally {
            os.close();
        }
    }

    public int getRmiServerPort() {
        InputStream is = null;
        try {
            File f = new File(location, "etc/org.apache.karaf.management.cfg");
            is = new FileInputStream(f);
            Properties props = new Properties();
            props.load(is);
            String loc = props.getProperty("rmiServerPort");
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

    public void changeRmiServerPort(int port) throws Exception {
        checkProcess();
        if (this.process != null) {
            throw new IllegalStateException("Instance not stopped");
        }
        Properties props = new Properties();
        File f = new File(location, "etc/org.apache.karaf.management.cfg");
        InputStream is = new FileInputStream(f);
        try {
            props.load(is);
        } finally {
            is.close();
        }
        props.setProperty("rmiServerPort", Integer.toString(port));
        OutputStream os = new FileOutputStream(f);
        try {
            props.store(os, null);
        } finally {
            os.close();
        }
    }

    public String getJavaOpts() {
        return javaOpts;
    }

    public void changeJavaOpts(String javaOpts) throws Exception {
        this.javaOpts = javaOpts;
        this.service.saveState();
    }

    public synchronized void start(String javaOpts) throws Exception {
        checkProcess();
        if (this.process != null) {
            throw new IllegalStateException("Instance already started");
        }
        if (javaOpts == null || javaOpts.length() == 0) {
            javaOpts = this.javaOpts;
        }
        if (javaOpts == null || javaOpts.length() == 0) {
            javaOpts = "-server -Xmx512M -Dcom.sun.management.jmxremote";
        }
        String karafOpts = System.getProperty("karaf.opts", "");  
        
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
        String command = "\""
                + new File(System.getProperty("java.home"), ScriptUtils.isWindows() ? "bin\\java.exe" : "bin/java").getCanonicalPath()
                + "\" " + javaOpts
                + " " + karafOpts
                + " -Djava.util.logging.config.file=\"" + new File(location, "etc/java.util.logging.properties").getCanonicalPath() + "\""
                + " -Djava.endorsed.dirs=\"" + new File(new File(new File(System.getProperty("java.home"), "jre"), "lib"), "endorsed") + System.getProperty("path.separator") + new File(new File(System.getProperty("java.home"), "lib"), "endorsed") + System.getProperty("path.separator") + new File(libDir, "endorsed").getCanonicalPath() + "\""
                + " -Djava.ext.dirs=\"" + new File(new File(new File(System.getProperty("java.home"), "jre"), "lib"), "ext") + System.getProperty("path.separator") + new File(new File(System.getProperty("java.home"), "lib"), "ext") + System.getProperty("path.separator") + new File(libDir, "ext").getCanonicalPath() + "\""
                + " -Dkaraf.home=\"" + System.getProperty("karaf.home") + "\""
                + " -Dkaraf.base=\"" + new File(location).getCanonicalPath() + "\""
                + " -Dkaraf.startLocalConsole=false"
                + " -Dkaraf.startRemoteShell=true"
                + " -classpath " + classpath.toString()
                + " org.apache.karaf.main.Main";
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
        // Try a clean shutdown
        cleanShutdown();
        if (this.process != null) {
            this.process.destroy();
        }
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
        int port = getSshPort();
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

    protected void cleanShutdown() {
        try {
            File file = new File(new File(location, "etc"), CONFIG_PROPERTIES_FILE_NAME);
            URL configPropURL = file.toURI().toURL();
            Properties props = loadPropertiesFile(configPropURL);
            props.put("karaf.base", new File(location).getCanonicalPath());
            props.put("karaf.home", System.getProperty("karaf.home"));
            props.put("karaf.data", new File(new File(location), "data").getCanonicalPath());
            for (Enumeration e = props.propertyNames(); e.hasMoreElements();) {
                String name = (String) e.nextElement();
                props.setProperty(name,
                        substVars(props.getProperty(name), name, null, props));
            }
            int port = Integer.parseInt(props.getProperty(KARAF_SHUTDOWN_PORT, "0"));
            String host = props.getProperty(KARAF_SHUTDOWN_HOST, "localhost");
            String portFile = props.getProperty(KARAF_SHUTDOWN_PORT_FILE);
            String shutdown = props.getProperty(KARAF_SHUTDOWN_COMMAND, DEFAULT_SHUTDOWN_COMMAND);
            if (port == 0 && portFile != null) {
                BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(portFile)));
                String portStr = r.readLine();
                port = Integer.parseInt(portStr);
                r.close();
            }
            // We found the port, try to send the command
            if (port > 0) {
                Socket s = new Socket(host, port);
                s.getOutputStream().write(shutdown.getBytes());
                s.close();
                long t = System.currentTimeMillis() + service.getStopTimeout();
                do {
                    Thread.sleep(100);
                    checkProcess();
                } while (System.currentTimeMillis() < t && process != null);
            }
        } catch (Exception e) {
            LOG.debug("Unable to cleanly shutdown instance", e);
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

    protected static Properties loadPropertiesFile(URL configPropURL) throws Exception {
        // Read the properties file.
        Properties configProps = new Properties();
        InputStream is = null;
        try {
            is = configPropURL.openConnection().getInputStream();
            configProps.load(is);
            is.close();
        }
        catch (Exception ex) {
            System.err.println(
                    "Error loading config properties from " + configPropURL);
            System.err.println("Main: " + ex);
            try {
                if (is != null) is.close();
            }
            catch (IOException ex2) {
                // Nothing we can do.
            }
            return null;
        }
        return configProps;
    }

    private static final String DELIM_START = "${";
    private static final String DELIM_STOP = "}";

    protected static String substVars(String val, String currentKey,
                                      Map<String, String> cycleMap, Properties configProps)
            throws IllegalArgumentException {
        // If there is currently no cycle map, then create
        // one for detecting cycles for this invocation.
        if (cycleMap == null) {
            cycleMap = new HashMap<String, String>();
        }

        // Put the current key in the cycle map.
        cycleMap.put(currentKey, currentKey);

        // Assume we have a value that is something like:
        // "leading ${foo.${bar}} middle ${baz} trailing"

        // Find the first ending '}' variable delimiter, which
        // will correspond to the first deepest nested variable
        // placeholder.
        int stopDelim = val.indexOf(DELIM_STOP);

        // Find the matching starting "${" variable delimiter
        // by looping until we find a start delimiter that is
        // greater than the stop delimiter we have found.
        int startDelim = val.indexOf(DELIM_START);
        while (stopDelim >= 0) {
            int idx = val.indexOf(DELIM_START, startDelim + DELIM_START.length());
            if ((idx < 0) || (idx > stopDelim)) {
                break;
            } else if (idx < stopDelim) {
                startDelim = idx;
            }
        }

        // If we do not have a start or stop delimiter, then just
        // return the existing value.
        if ((startDelim < 0) && (stopDelim < 0)) {
            return val;
        }
        // At this point, we found a stop delimiter without a start,
        // so throw an exception.
        else if (((startDelim < 0) || (startDelim > stopDelim))
                && (stopDelim >= 0)) {
            throw new IllegalArgumentException(
                    "stop delimiter with no start delimiter: "
                            + val);
        }

        // At this point, we have found a variable placeholder so
        // we must perform a variable substitution on it.
        // Using the start and stop delimiter indices, extract
        // the first, deepest nested variable placeholder.
        String variable =
                val.substring(startDelim + DELIM_START.length(), stopDelim);

        // Verify that this is not a recursive variable reference.
        if (cycleMap.get(variable) != null) {
            throw new IllegalArgumentException(
                    "recursive variable reference: " + variable);
        }

        // Get the value of the deepest nested variable placeholder.
        // Try to configuration properties first.
        String substValue = (configProps != null)
                ? configProps.getProperty(variable, null)
                : null;
        if (substValue == null) {
            // Ignore unknown property values.
            substValue = System.getProperty(variable, "");
        }

        // Remove the found variable from the cycle map, since
        // it may appear more than once in the value and we don't
        // want such situations to appear as a recursive reference.
        cycleMap.remove(variable);

        // Append the leading characters, the substituted value of
        // the variable, and the trailing characters to get the new
        // value.
        val = val.substring(0, startDelim)
                + substValue
                + val.substring(stopDelim + DELIM_STOP.length(), val.length());

        // Now perform substitution again, since there could still
        // be substitutions to make.
        val = substVars(val, currentKey, cycleMap, configProps);

        // Return the value.
        return val;
    }

}
