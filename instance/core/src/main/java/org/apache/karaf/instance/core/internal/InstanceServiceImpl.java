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
package org.apache.karaf.instance.core.internal;

import org.apache.karaf.instance.core.Instance;
import org.apache.karaf.instance.core.InstanceService;
import org.apache.karaf.instance.core.InstanceSettings;
import org.apache.karaf.jpm.Process;
import org.apache.karaf.jpm.impl.ProcessBuilderFactoryImpl;
import org.apache.karaf.jpm.impl.ScriptUtils;
import org.apache.karaf.util.properties.FileLockUtils;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

public class InstanceServiceImpl implements InstanceService {

    public static final String STORAGE_FILE = "instance.properties";
    public static final String BACKUP_EXTENSION = ".bak";
    private static final String FEATURES_CFG = "etc/org.apache.karaf.features.cfg";

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceServiceImpl.class);

    private static final String CONFIG_PROPERTIES_FILE_NAME = "config.properties";

    private static final String KARAF_SHUTDOWN_PORT = "karaf.shutdown.port";

    private static final String KARAF_SHUTDOWN_HOST = "karaf.shutdown.host";

    private static final String KARAF_SHUTDOWN_PORT_FILE = "karaf.shutdown.port.file";

    private static final String KARAF_SHUTDOWN_COMMAND = "karaf.shutdown.command";

    private static final String KARAF_SHUTDOWN_PID_FILE = "karaf.shutdown.pid.file";

    private static final String DEFAULT_SHUTDOWN_COMMAND = "SHUTDOWN";

    private LinkedHashMap<String, InstanceImpl> proxies = new LinkedHashMap<String, InstanceImpl>();

    private File storageLocation;

    private long stopTimeout = 30000;

    static class InstanceState {
        String name;
        String loc;
        String opts;
        int pid;
        boolean root;
    }

    static class State {
        int defaultSshPortStart = 8101;
        int defaultRmiRegistryPortStart = 1099;
        int defaultRmiServerPortStart = 44444;
        Map<String, InstanceState> instances;
    }

    public File getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(File storage) {
        this.storageLocation = storage;
    }

    public long getStopTimeout() {
        return stopTimeout;
    }

    public void setStopTimeout(long stopTimeout) {
        this.stopTimeout = stopTimeout;
    }

    private State loadData(org.apache.felix.utils.properties.Properties storage) {
        State state = new State();
        int count = getInt(storage, "count", 0);
        state.defaultSshPortStart = getInt(storage, "ssh.port", state.defaultSshPortStart);
        state.defaultRmiRegistryPortStart = getInt(storage, "rmi.registry.port", state.defaultRmiRegistryPortStart);
        state.defaultRmiServerPortStart = getInt(storage, "rmi.server.port", state.defaultRmiServerPortStart);
        state.instances = new LinkedHashMap<String, InstanceState>();

        for (int i = 0; i < count; i++) {
            InstanceState instance = new InstanceState();
            instance.name = getString(storage, "item." + i + ".name", null);
            instance.loc = getString(storage, "item." + i + ".loc", null);
            instance.opts = getString(storage, "item." + i + ".opts", null);
            instance.pid = getInt(storage, "item." + i + ".pid", 0);
            instance.root = getBool(storage, "item." + i + ".root", false);
            state.instances.put(instance.name, instance);
        }
        // Update proxies list
        for (InstanceState instance : state.instances.values()) {
            if (!this.proxies.containsKey(instance.name)) {
                proxies.put(instance.name, new InstanceImpl(this, instance.name));
            }
        }
        List<String> names = new ArrayList<String>(this.proxies.keySet());
        for (String name : names) {
            if (!state.instances.containsKey(name)) {
                this.proxies.remove(name);
            }
        }
        return state;
    }

    private void saveData(State state, org.apache.felix.utils.properties.Properties storage) {
        storage.put("ssh.port", Integer.toString(state.defaultSshPortStart));
        storage.put("rmi.registry.port", Integer.toString(state.defaultRmiRegistryPortStart));
        storage.put("rmi.server.port", Integer.toString(state.defaultRmiServerPortStart));
        storage.put("count", Integer.toString(state.instances.size()));
        int i = 0;
        for (InstanceState instance : state.instances.values()) {
            storage.put("item." + i + ".name", instance.name);
            storage.put("item." + i + ".root", Boolean.toString(instance.root));
            storage.put("item." + i + ".loc", instance.loc);
            storage.put("item." + i + ".pid", Integer.toString(instance.pid));
            storage.put("item." + i + ".opts", instance.opts != null ? instance.opts : "");
            i++;
        }
        while (storage.containsKey("item." + i + ".name")) {
            storage.remove("item." + i + ".name");
            storage.remove("item." + i + ".root");
            storage.remove("item." + i + ".loc");
            storage.remove("item." + i + ".pid");
            storage.remove("item." + i + ".opts");
            i++;
        }
    }

    private boolean getBool(org.apache.felix.utils.properties.Properties storage, String name, boolean def) {
        Object value = storage.get(name);
        if (value != null) {
            return Boolean.parseBoolean(value.toString());
        } else {
            return def;
        }
    }

    private int getInt(org.apache.felix.utils.properties.Properties storage, String name, int def) {
        Object value = storage.get(name);
        if (value != null) {
            return Integer.parseInt(value.toString());
        } else {
            return def;
        }
    }

    private String getString(org.apache.felix.utils.properties.Properties storage, String name, String def) {
        Object value = storage.get(name);
        return value != null ? value.toString() : def;
    }

    interface Task<T> {
        T call(State state) throws IOException;
    }

    <T> T execute(final Task<T> callback) {
        final File storageFile = new File(storageLocation, STORAGE_FILE);
        if (!storageFile.exists()) {
            storageFile.getParentFile().mkdirs();
            try {
                storageFile.createNewFile();
            } catch (IOException e) {
                // Ignore
            }
        }
        if (storageFile.exists()) {
            if (!storageFile.isFile()) {
                throw new IllegalStateException("Instance storage location should be a file: " + storageFile);
            }
            try {
                return FileLockUtils.execute(storageFile, new FileLockUtils.CallableWithProperties<T>() {
                    public T call(org.apache.felix.utils.properties.Properties properties) throws IOException {
                        State state = loadData(properties);
                        T t = callback.call(state);
                        saveData(state, properties);
                        return t;
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalStateException("Instance storage location does not exist: " + storageFile);
        }
    }

    public synchronized void refreshInstance() throws Exception {
    }

    private void logInfo(String message, boolean printOutput, Object... args) {
        if (LOGGER.isInfoEnabled() || printOutput) {
            String formatted = String.format(message, args);
            LOGGER.info(formatted);
            if (printOutput) {
                println(formatted);
            }
        }
    }

    public synchronized Instance createInstance(final String name, final InstanceSettings settings, final boolean printOutput) throws Exception {
        return execute(new Task<Instance>() {
            public Instance call(State state) throws IOException {
                if (state.instances.get(name) != null) {
                    throw new IllegalArgumentException("Instance '" + name + "' already exists");
                }
                String loc = settings.getLocation() != null ? settings.getLocation() : name;
                File karafBase = new File(loc);
                if (!karafBase.isAbsolute()) {
                    karafBase = new File(storageLocation, loc);
                }
                int sshPort = settings.getSshPort();
                if (sshPort <= 0) {
                    sshPort = ++state.defaultSshPortStart;
                }
                int rmiRegistryPort = settings.getRmiRegistryPort();
                if (rmiRegistryPort <= 0) {
                    rmiRegistryPort = ++state.defaultRmiRegistryPortStart;
                }
                int rmiServerPort = settings.getRmiServerPort();
                if (rmiServerPort <= 0) {
                    rmiServerPort = ++state.defaultRmiServerPortStart;
                }
                logInfo("Creating new instance on SSH port %d and registry port %d / RMI server port %d at: %s",
                        printOutput, sshPort, rmiRegistryPort, rmiServerPort, karafBase);

                mkdir(karafBase, "bin", printOutput);
                mkdir(karafBase, "etc", printOutput);
                mkdir(karafBase, "system", printOutput);
                mkdir(karafBase, "deploy", printOutput);
                mkdir(karafBase, "data", printOutput);

                copyResourceToDir(karafBase, "etc/config.properties", printOutput);
                copyResourceToDir(karafBase, "etc/jre.properties", printOutput);
                copyResourceToDir(karafBase, "etc/custom.properties", printOutput);
                copyResourceToDir(karafBase, "etc/java.util.logging.properties", printOutput);
                copyResourceToDir(karafBase, "etc/org.apache.felix.fileinstall-deploy.cfg", printOutput);
                copyResourceToDir(karafBase, "etc/org.apache.karaf.features.obr.cfg", printOutput);
                copyResourceToDir(karafBase, "etc/org.apache.karaf.features.repos.cfg", printOutput);
                copyResourceToDir(karafBase, "etc/org.apache.karaf.log.cfg", printOutput);
                copyResourceToDir(karafBase, "etc/org.ops4j.pax.logging.cfg", printOutput);
                copyResourceToDir(karafBase, "etc/org.ops4j.pax.url.mvn.cfg", printOutput);
//                copyResourceToDir(karafBase, "etc/startup.properties", printOutput);
                copyResourceToDir(karafBase, "etc/users.properties", printOutput);
                copyResourceToDir(karafBase, "etc/keys.properties", printOutput);

                copyResourceToDir(karafBase, FEATURES_CFG, printOutput);
                addFeaturesFromSettings(new File(karafBase, FEATURES_CFG), settings);

                // The startup.properties is now generated by the karaf maven plugin, so
                // we use the one from the root instance instead of embedding it
                File curbase = new File(System.getProperty("karaf.base"));
                copy(new File(curbase, "etc/startup.properties"), new File(karafBase, "etc/startup.properties"));

                HashMap<String, String> props = new HashMap<String, String>();
                props.put("${SUBST-KARAF-NAME}", name);
                props.put("${SUBST-KARAF-HOME}", System.getProperty("karaf.home"));
                props.put("${SUBST-KARAF-BASE}", karafBase.getPath());
                props.put("${SUBST-SSH-PORT}", Integer.toString(sshPort));
                props.put("${SUBST-RMI-REGISTRY-PORT}", Integer.toString(rmiRegistryPort));
                props.put("${SUBST-RMI-SERVER-PORT}", Integer.toString(rmiServerPort));
                copyFilteredResourceToDir(karafBase, "etc/system.properties", props, printOutput);
                copyFilteredResourceToDir(karafBase, "etc/org.apache.karaf.shell.cfg", props, printOutput);
                copyFilteredResourceToDir(karafBase, "etc/org.apache.karaf.management.cfg", props, printOutput);
                // If we use batch files, use batch files, else use bash scripts (even on cygwin)
                boolean windows = System.getProperty("os.name").startsWith("Win");
                boolean cygwin = windows && new File(System.getProperty("karaf.home"), "bin/admin").exists();
                if (windows && !cygwin) {
                    copyFilteredResourceToDir(karafBase, "bin/karaf.bat", props, printOutput);
                    copyFilteredResourceToDir(karafBase, "bin/start.bat", props, printOutput);
                    copyFilteredResourceToDir(karafBase, "bin/stop.bat", props, printOutput);
                } else {
                    copyFilteredResourceToDir(karafBase, "bin/karaf", props, printOutput);
                    copyFilteredResourceToDir(karafBase, "bin/start", props, printOutput);
                    copyFilteredResourceToDir(karafBase, "bin/stop", props, printOutput);
                    if (!cygwin) {
                        chmod(new File(karafBase, "bin/karaf"), "a+x");
                        chmod(new File(karafBase, "bin/start"), "a+x");
                        chmod(new File(karafBase, "bin/stop"), "a+x");
                    }
                }

                String javaOpts = settings.getJavaOpts();
                if (javaOpts == null || javaOpts.length() == 0) {
                    javaOpts = "-server -Xmx512M -Dcom.sun.management.jmxremote";
                }
                InstanceState is = new InstanceState();
                is.name = name;
                is.loc = karafBase.toString();
                is.opts = javaOpts;
                state.instances.put(name, is);
                InstanceImpl instance = new InstanceImpl(InstanceServiceImpl.this, name);
                InstanceServiceImpl.this.proxies.put(name, instance);
                return instance;
            }
        });
    }

    void addFeaturesFromSettings(File featuresCfg, final InstanceSettings settings) throws IOException {
        FileLockUtils.execute(featuresCfg, new FileLockUtils.RunnableWithProperties() {
            public void run(org.apache.felix.utils.properties.Properties properties) throws IOException {
                appendToPropList(properties, "featuresBoot", Collections.singletonList("ssh"));
                appendToPropList(properties, "featuresBoot", Collections.singletonList("framework"));
                appendToPropList(properties, "featuresBoot", settings.getFeatures());
                appendToPropList(properties, "featuresRepositories", settings.getFeatureURLs());
            }
        });
    }

    private void appendToPropList(org.apache.felix.utils.properties.Properties p, String key, List<String> elements) {
        if (elements == null) {
            return;
        }
        StringBuilder sb = new StringBuilder(p.get(key).toString().trim());
        for (String f : elements) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(f);
        }
        p.put(key, sb.toString());
    }

    public Instance[] getInstances() {
        return execute(new Task<Instance[]>() {
            public Instance[] call(State state) throws IOException {
                return proxies.values().toArray(new Instance[proxies.size()]);
            }
        });
    }

    public Instance getInstance(final String name) {
        return execute(new Task<Instance>() {
            public Instance call(State state) throws IOException {
                return proxies.get(name);
            }
        });
    }

    public void startInstance(final String name, final String javaOpts) {
        execute(new Task<Object>() {
            public Object call(State state) throws IOException {
                InstanceState instance = state.instances.get(name);
                if (instance == null) {
                    throw new IllegalArgumentException("Instance " + name + " not found");
                }
                checkPid(instance);
                if (instance.pid != 0) {
                    throw new IllegalStateException("Instance already started");
                }
                String opts = javaOpts;
                if (opts == null || opts.length() == 0) {
                    opts = instance.opts;
                }
                if (opts == null || opts.length() == 0) {
                    opts = "-server -Xmx512M -Dcom.sun.management.jmxremote";
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
                String location = instance.loc;
                String command = "\""
                        + new File(System.getProperty("java.home"), ScriptUtils.isWindows() ? "bin\\java.exe" : "bin/java").getCanonicalPath()
                        + "\" " + opts
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
                LOGGER.debug("Starting instance " + name + " with command: " + command);
                org.apache.karaf.jpm.Process process = new ProcessBuilderFactoryImpl().newBuilder()
                        .directory(new File(location))
                        .command(command)
                        .start();
                instance.pid = process.getPid();
                return null;
            }
        });
    }

    public void stopInstance(final String name) {
        execute(new Task<Object>() {
            public Object call(State state) throws IOException {
                InstanceState instance = state.instances.get(name);
                if (instance == null) {
                    throw new IllegalArgumentException("Instance " + name + " not found");
                }
                checkPid(instance);
                if (instance.pid == 0) {
                    throw new IllegalStateException("Instance already stopped");
                }
                cleanShutdown(instance);
                if (instance.pid > 0) {
                    Process process = new ProcessBuilderFactoryImpl().newBuilder().attach(instance.pid);
                    process.destroy();
                }
                return null;
            }
        });
    }

    public void destroyInstance(final String name) {
        execute(new Task<Object>() {
            public Object call(State state) throws IOException {
                InstanceState instance = state.instances.get(name);
                if (instance == null) {
                    throw new IllegalArgumentException("Instance " + name + " not found");
                }
                checkPid(instance);
                if (instance.pid != 0) {
                    throw new IllegalStateException("Instance not stopped");
                }
                deleteFile(new File(instance.loc));
                state.instances.remove(name);
                InstanceServiceImpl.this.proxies.remove(name);
                return null;
            }
        });
    }

    public void renameInstance(final String oldName, final String newName, final boolean printOutput) throws Exception {
        execute(new Task<Object>() {
            public Object call(State state) throws IOException {
                if (state.instances.get(newName) != null) {
                    throw new IllegalArgumentException("Instance " + newName + " already exists");
                }
                InstanceState instance = state.instances.get(oldName);
                if (instance == null) {
                    throw new IllegalArgumentException("Instance " + oldName + " not found");
                }
                if (instance.root) {
                    throw new IllegalArgumentException("Root instance cannot be renamed");
                }
                checkPid(instance);
                if (instance.pid != 0) {
                    throw new IllegalStateException("Instance not stopped");
                }

                println(Ansi.ansi().a("Renaming instance ")
                        .a(Ansi.Attribute.INTENSITY_BOLD).a(oldName).a(Ansi.Attribute.RESET)
                        .a(" to ")
                        .a(Ansi.Attribute.INTENSITY_BOLD).a(newName).a(Ansi.Attribute.RESET).toString());
                // rename directory
                String oldLocationPath = instance.loc;
                File oldLocation = new File(oldLocationPath);
                String basedir = oldLocation.getParent();
                File newLocation = new File(basedir, newName);
                oldLocation.renameTo(newLocation);
                // create the properties map including the instance name and instance location
                // TODO: replacing is bad, we should re-extract the needed files
                HashMap<String, String> props = new HashMap<String, String>();
                props.put(oldName, newName);
                props.put(oldLocationPath, newLocation.getPath());
                // replace all references to the "old" name by the new one in etc/system.properties
                // NB: it's replacement to avoid to override the user's changes
                filterResource(newLocation, "etc/system.properties", props);
                // replace all references to the "old" name by the new one in bin/karaf
                filterResource(newLocation, "bin/karaf", props);
                filterResource(newLocation, "bin/start", props);
                filterResource(newLocation, "bin/stop", props);
                filterResource(newLocation, "bin/karaf.bat", props);
                filterResource(newLocation, "bin/start.bat", props);
                filterResource(newLocation, "bin/stop.bat", props);
                // update instance
                instance.name = newName;
                instance.loc = newLocation.getPath();
                state.instances.put(newName, instance);
                state.instances.remove(oldName);
                InstanceImpl proxy = InstanceServiceImpl.this.proxies.remove(oldName);
                if (proxy == null) {
                    proxy = new InstanceImpl(InstanceServiceImpl.this, newName);
                } else {
                    proxy.doSetName(newName);
                }
                InstanceServiceImpl.this.proxies.put(newName, proxy);
                return null;
            }
        });
    }

    public synchronized Instance cloneInstance(final String name, final String cloneName, final InstanceSettings settings, final boolean printOutput) throws Exception {
        return execute(new Task<Instance>() {
            public Instance call(State state) throws IOException {
                if (state.instances.get(cloneName) != null) {
                    throw new IllegalArgumentException("Instance " + cloneName + " already exists");
                }
                InstanceState instance = state.instances.get(name);
                if (instance == null) {
                    throw new IllegalArgumentException("Instance " + name + " not found");
                }

                // define the clone instance location
                String cloneLocationPath = settings.getLocation() != null ? settings.getLocation() : cloneName;
                File cloneLocation = new File(cloneLocationPath);
                if (!cloneLocation.isAbsolute()) {
                    cloneLocation = new File(storageLocation, cloneLocationPath);
                }
                // copy instance directory
                String locationPath = instance.loc;
                File location = new File(locationPath);
                copy(location, cloneLocation);
                // create the properties map including the instance name, location, ssh and rmi port numbers
                // TODO: replacing stuff anywhere is not really good, we might end up replacing unwanted stuff
                // TODO: if no ports are overriden, shouldn't we choose new ports ?
                HashMap<String, String> props = new HashMap<String, String>();
                props.put(name, cloneName);
                props.put(locationPath, cloneLocationPath);
                if (settings.getSshPort() > 0)
                    props.put(Integer.toString(getInstanceSshPort(instance.name)), Integer.toString(settings.getSshPort()));
                if (settings.getRmiRegistryPort() > 0)
                    props.put(Integer.toString(getInstanceRmiRegistryPort(instance.name)), Integer.toString(settings.getRmiRegistryPort()));
                if (settings.getRmiServerPort() > 0)
                    props.put(Integer.toString(getInstanceRmiServerPort(instance.name)), Integer.toString(settings.getRmiServerPort()));
                // filtering clone files
                filterResource(cloneLocation, "etc/custom.properties", props);
                filterResource(cloneLocation, "etc/org.apache.karaf.management.cfg", props);
                filterResource(cloneLocation, "etc/org.apache.karaf.shell.cfg", props);
                filterResource(cloneLocation, "etc/org.ops4j.pax.logging.cfg", props);
                filterResource(cloneLocation, "etc/system.properties", props);
                filterResource(cloneLocation, "bin/karaf", props);
                filterResource(cloneLocation, "bin/start", props);
                filterResource(cloneLocation, "bin/stop", props);
                filterResource(cloneLocation, "bin/karaf.bat", props);
                filterResource(cloneLocation, "bin/start.bat", props);
                filterResource(cloneLocation, "bin/stop.bat", props);
                // create and add the clone instance in the registry
                String javaOpts = settings.getJavaOpts();
                if (javaOpts == null || javaOpts.length() == 0) {
                    javaOpts = "-server -Xmx512M -Dcom.sun.management.jmxremote";
                }
                InstanceState is = new InstanceState();
                is.name = cloneName;
                is.loc = cloneLocation.toString();
                is.opts = javaOpts;
                state.instances.put(cloneName, is);
                InstanceImpl cloneInstance = new InstanceImpl(InstanceServiceImpl.this, cloneName);
                InstanceServiceImpl.this.proxies.put(cloneName, cloneInstance);
                return cloneInstance;
            }
        });
    }

    private void checkPid(InstanceState instance) throws IOException {
        if (instance.pid != 0) {
            Process process = new ProcessBuilderFactoryImpl().newBuilder().attach(instance.pid);
            if (!process.isRunning()) {
                instance.pid = 0;
            }
        }
    }

    protected void cleanShutdown(InstanceState instance) {
        try {
            File file = new File(new File(instance.loc, "etc"), CONFIG_PROPERTIES_FILE_NAME);
            URL configPropURL = file.toURI().toURL();
            Properties props = loadPropertiesFile(configPropURL);
            props.put("karaf.base", new File(instance.loc).getCanonicalPath());
            props.put("karaf.home", System.getProperty("karaf.home"));
            props.put("karaf.data", new File(new File(instance.loc), "data").getCanonicalPath());
            for (Enumeration e = props.propertyNames(); e.hasMoreElements();) {
                String key = (String) e.nextElement();
                props.setProperty(key,
                        substVars(props.getProperty(key), key, null, props));
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
                long t = System.currentTimeMillis() + getStopTimeout();
                do {
                    Thread.sleep(100);
                    checkPid(instance);
                } while (System.currentTimeMillis() < t && instance.pid > 0);
            }
        } catch (Exception e) {
            LOGGER.debug("Unable to cleanly shutdown instance " + instance.name, e);
        }
    }

    int getInstanceSshPort(String name) {
        return getKarafPort(name, "etc/org.apache.karaf.shell.cfg", "sshPort");
    }

    void changeInstanceSshPort(String name, final int port) throws Exception {
        setKarafPort(name, "etc/org.apache.karaf.shell.cfg", "sshPort", port);
    }

    int getInstanceRmiRegistryPort(String name) {
        return getKarafPort(name, "etc/org.apache.karaf.management.cfg", "rmiRegistryPort");
    }

    void changeInstanceRmiRegistryPort(String name, final int port) throws Exception {
        setKarafPort(name, "etc/org.apache.karaf.management.cfg", "rmiRegistryPort", port);
    }

    int getInstanceRmiServerPort(String name) {
        return getKarafPort(name, "etc/org.apache.karaf.management.cfg", "rmiServerPort");
    }

    void changeInstanceRmiServerPort(String name, int port) throws Exception {
        setKarafPort(name, "etc/org.apache.karaf.management.cfg", "rmiServerPort", port);
    }

    private int getKarafPort(final String name, final String path, final String key) {
        return execute(new Task<Integer>() {
            public Integer call(State state) throws IOException {
                return InstanceServiceImpl.this.getKarafPort(state, name, path, key);
            }
        });
    }

    private Integer getKarafPort(State state, String name, String path, final String key) {
        InstanceState instance = state.instances.get(name);
        if (instance == null) {
            throw new IllegalArgumentException("Instance " + name + " not found");
        }
        File f = new File(instance.loc, path);
        try {
            return FileLockUtils.execute(f, new FileLockUtils.CallableWithProperties<Integer>() {
                public Integer call(org.apache.felix.utils.properties.Properties properties) throws IOException {
                    return Integer.parseInt(properties.get(key).toString());
                }
            });
        } catch (IOException e) {
            return 0;
        }
    }

    private void setKarafPort(final String name, final String path, final String key, final int port) throws IOException {
        execute(new Task<Object>() {
            public Object call(State state) throws IOException {
                InstanceState instance = state.instances.get(name);
                if (instance == null) {
                    throw new IllegalArgumentException("Instance " + name + " not found");
                }
                checkPid(instance);
                if (instance.pid != 0) {
                    throw new IllegalStateException("Instance is not stopped");
                }
                File f = new File(instance.loc, path);
                FileLockUtils.execute(f, new FileLockUtils.RunnableWithProperties() {
                    public void run(org.apache.felix.utils.properties.Properties properties) throws IOException {
                        properties.put(key, Integer.toString(port));
                    }
                });
                return null;
            }
        });
    }

    boolean isInstanceRoot(final String name) {
        return execute(new Task<Boolean>() {
            public Boolean call(State state) throws IOException {
                InstanceState instance = state.instances.get(name);
                if (instance == null) {
                    throw new IllegalArgumentException("Instance " + name + " not found");
                }
                return instance.root;
            }
        });
    }

    String getInstanceLocation(final String name) {
        return execute(new Task<String>() {
            public String call(State state) throws IOException {
                InstanceState instance = state.instances.get(name);
                if (instance == null) {
                    throw new IllegalArgumentException("Instance " + name + " not found");
                }
                return instance.loc;
            }
        });
    }

    int getInstancePid(final String name) {
        return execute(new Task<Integer>() {
            public Integer call(State state) throws IOException {
                InstanceState instance = state.instances.get(name);
                if (instance == null) {
                    throw new IllegalArgumentException("Instance " + name + " not found");
                }
                checkPid(instance);
                return instance.pid;
            }
        });
    }

    String getInstanceJavaOpts(final String name) {
        return execute(new Task<String>() {
            public String call(State state) throws IOException {
                InstanceState instance = state.instances.get(name);
                if (instance == null) {
                    throw new IllegalArgumentException("Instance " + name + " not found");
                }
                return instance.opts;
            }
        });
    }

    void changeInstanceJavaOpts(final String name, final String opts) {
        execute(new Task<String>() {
            public String call(State state) throws IOException {
                InstanceState instance = state.instances.get(name);
                if (instance == null) {
                    throw new IllegalArgumentException("Instance " + name + " not found");
                }
                instance.opts = opts;
                return null;
            }
        });
    }

    String getInstanceState(final String name) {
        return execute(new Task<String>() {
            public String call(State state) throws IOException {
                InstanceState instance = state.instances.get(name);
                if (instance == null) {
                    throw new IllegalArgumentException("Instance " + name + " not found");
                }
                int port = getKarafPort(state, name, "etc/org.apache.karaf.shell.cfg", "sshPort");
                if (!new File(instance.loc).isDirectory() || port <= 0) {
                    return Instance.ERROR;
                }
                checkPid(instance);
                if (instance.pid == 0) {
                    return Instance.STOPPED;
                } else {
                    try {
                        Socket s = new Socket("localhost", port);
                        s.close();
                        return Instance.STARTED;
                    } catch (Exception e) {
                        // ignore
                    }
                    return Instance.STARTING;
                }
            }
        });
    }

    private boolean deleteFile(File fileToDelete) {
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

    private void copyResourceToDir(File target, String resource, boolean printOutput) throws IOException {
        File outFile = new File(target, resource);
        if( !outFile.exists() ) {
            logInfo("Creating file: %s", printOutput, outFile.getPath());
            InputStream is = getClass().getClassLoader().getResourceAsStream("org/apache/karaf/instance/resources/" + resource);
            try {
                // Read it line at a time so that we can use the platform line ending when we write it out.
                PrintStream out = new PrintStream(new FileOutputStream(outFile));
                try {
                    Scanner scanner = new Scanner(is);
                    while (scanner.hasNextLine() ) {
                        String line = scanner.nextLine();
                        out.println(line);
                    }
                } finally {
                    safeClose(out);
                }
            } finally {
                safeClose(is);
            }
        }
    }

    private void println(String st) {
        System.out.println(st);
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

    private void filterResource(File basedir, String path, HashMap<String, String> props) throws IOException {
        File file = new File(basedir, path);
        File bak = new File(basedir, path + BACKUP_EXTENSION);
        if (!file.exists()) {
            return;
        }
        // rename the file to the backup one
        file.renameTo(bak);
        // copy and filter the bak file back to the original name
        copyAndFilterResource(new FileInputStream(bak), new FileOutputStream(file), props);
        // remove the bak file
        bak.delete();
    }

    private void copyFilteredResourceToDir(File target, String resource, HashMap<String, String> props, boolean printOutput) throws IOException {
        File outFile = new File(target, resource);
        if( !outFile.exists() ) {
            logInfo("Creating file: %s", printOutput, outFile.getPath());
            InputStream is = getClass().getClassLoader().getResourceAsStream("org/apache/karaf/instance/resources/" + resource);
            copyAndFilterResource(is, new FileOutputStream(outFile), props);
        }
    }

    private void copyAndFilterResource(InputStream source, OutputStream target, HashMap<String, String> props) throws IOException {
        try {
            // read it line at a time so that we can use the platform line ending when we write it out.
            PrintStream out = new PrintStream(target);
            try {
                Scanner scanner = new Scanner(source);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    line = filter(line, props);
                    out.println(line);
                }
            } finally {
                safeClose(out);
            }
        } finally {
            safeClose(source);
        }
    }

    private void safeClose(InputStream is) throws IOException {
        if (is == null) {
            return;
        }
        try {
            is.close();
        } catch (Throwable ignore) {
        }
    }

    private void safeClose(OutputStream is) throws IOException {
        if (is == null) {
            return;
        }
        try {
            is.close();
        } catch (Throwable ignore) {
        }
    }

    private String filter(String line, HashMap<String, String> props) {
        for (Map.Entry<String, String> i : props.entrySet()) {
            int p1 = line.indexOf(i.getKey());
            if( p1 >= 0 ) {
                String l1 = line.substring(0, p1);
                String l2 = line.substring(p1+i.getKey().length());
                line = l1+i.getValue()+l2;
            }
        }
        return line;
    }

    private void mkdir(File karafBase, String path, boolean printOutput) {
        File file = new File(karafBase, path);
        if( !file.exists() ) {
            logInfo("Creating dir: %s", printOutput, file.getPath());
            file.mkdirs();
        }
    }

    private int chmod(File serviceFile, String mode) throws IOException {
        java.lang.ProcessBuilder builder = new java.lang.ProcessBuilder();
        builder.command("chmod", mode, serviceFile.getCanonicalPath());
        java.lang.Process p = builder.start();

        // gnodet: Fix SMX4KNL-46: cpu goes to 100% after running the 'admin create' command
        // Not sure exactly what happens, but commenting the process io redirection seems
        // to work around the problem.
        //
        //PumpStreamHandler handler = new PumpStreamHandler(io.inputStream, io.outputStream, io.errorStream);
        //handler.attach(p);
        //handler.start();
        try {
            return p.waitFor();
        } catch (InterruptedException e) {
            throw (IOException) new InterruptedIOException().initCause(e);
        }
        //handler.stop();
    }

    private void copy(File source, File destination) throws IOException {
        if (source.getName().equals("cache.lock")) {
            // ignore cache.lock file
            return;
        }
        if (source.getName().equals("lock")) {
            // ignore lock file
            return;
        }
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }
            String[] children = source.list();
            for (String child : children) {
                if (!child.contains("instances") && !child.contains("lib"))
                    copy(new File(source, child), new File(destination, child));
            }
        } else {
            InputStream in = new FileInputStream(source);
            OutputStream out = new FileOutputStream(destination);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            in.close();
            out.close();
        }
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