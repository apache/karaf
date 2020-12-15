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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

import org.apache.felix.utils.properties.InterpolationHelper;
import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.instance.core.Instance;
import org.apache.karaf.instance.core.InstanceService;
import org.apache.karaf.instance.core.InstanceSettings;
import org.apache.karaf.instance.main.Execute;
import org.apache.karaf.jpm.Process;
import org.apache.karaf.jpm.impl.ProcessBuilderFactoryImpl;
import org.apache.karaf.jpm.impl.ScriptUtils;
import org.apache.karaf.profile.Profile;
import org.apache.karaf.profile.ProfileBuilder;
import org.apache.karaf.profile.ProfileService;
import org.apache.karaf.shell.support.ansi.SimpleAnsi;
import org.apache.karaf.util.StreamUtils;
import org.apache.karaf.util.config.PropertiesLoader;
import org.apache.karaf.util.locks.FileLockUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstanceServiceImpl implements InstanceService {

    public static final String STORAGE_FILE = "instance.properties";
    public static final String BACKUP_EXTENSION = ".bak";
    private static final String FEATURES_CFG = "etc/org.apache.karaf.features.cfg";
    private static final String RESOURCE_BASE = "org/apache/karaf/instance/resources/";

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceServiceImpl.class);

    private static final String CONFIG_PROPERTIES_FILE_NAME = "config.properties";

    private static final String KARAF_SHUTDOWN_PORT = "karaf.shutdown.port";

    private static final String KARAF_SHUTDOWN_HOST = "karaf.shutdown.host";

    private static final String KARAF_SHUTDOWN_PORT_FILE = "karaf.shutdown.port.file";

    private static final String KARAF_SHUTDOWN_COMMAND = "karaf.shutdown.command";

    private static final String KARAF_SHUTDOWN_TIMEOUT = "karaf.shutdown.timeout";

    private static final String DEFAULT_SHUTDOWN_COMMAND = "SHUTDOWN";

    public static final String DEFAULT_JAVA_OPTS = "-Xmx512m -XX:+UnlockDiagnosticVMOptions";

    private LinkedHashMap<String, InstanceImpl> proxies = new LinkedHashMap<>();

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
        public State() {
            //read port start value from the root instance configuration
            try {
                Properties shellProperty = new Properties();
                shellProperty.load(new File(System.getProperty("karaf.etc"), "org.apache.karaf.shell.cfg"));
                defaultSshPortStart = getInt(shellProperty,"sshPort", 8101);
                Properties managementProperty = new Properties();
                managementProperty.load(new File(System.getProperty("karaf.etc"), "org.apache.karaf.management.cfg"));
                defaultRmiRegistryPortStart = getInt(managementProperty, "rmiRegistryPort", 1099);
                defaultRmiServerPortStart = getInt(managementProperty, "rmiServerPort", 1099);
            } catch (Exception e) {
                LOGGER.debug("Could not read port start value from the root instance configuration.", e);
            }
        }


    }

    public InstanceServiceImpl() {
        String prop = System.getProperty("karaf.instances");
        if (prop != null) {
            storageLocation = new File(prop);
        }
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

    private State loadData(Properties storage) {
        State state = new State();
        int count = getInt(storage, "count", 0);
        state.defaultSshPortStart = getInt(storage, "ssh.port", state.defaultSshPortStart);
        state.defaultRmiRegistryPortStart = getInt(storage, "rmi.registry.port", state.defaultRmiRegistryPortStart);
        state.defaultRmiServerPortStart = getInt(storage, "rmi.server.port", state.defaultRmiServerPortStart);
        state.instances = new LinkedHashMap<>();

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
        List<String> names = new ArrayList<>(this.proxies.keySet());
        for (String name : names) {
            if (!state.instances.containsKey(name)) {
                this.proxies.remove(name);
            }
        }
        return state;
    }

    private void saveData(State state, Properties storage) {
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

    private static boolean getBool(Properties storage, String name, boolean def) {
        Object value = storage.get(name);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value != null) {
            return Boolean.parseBoolean(value.toString());
        } else {
            return def;
        }
    }

    private static int getInt(Properties storage, String name, int def) {
        Object value = storage.get(name);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value != null) {
            return Integer.parseInt(value.toString());
        } else {
            return def;
        }
    }

    private static String getString(Properties storage, String name, String def) {
        Object value = storage.get(name);
        return value != null ? value.toString() : def;
    }

    interface Task<U, T> {
        T call(U state) throws IOException;
    }

    synchronized <T> T execute(final Task<State, T> callback, final boolean writeToFile) {
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
                return FileLockUtils.execute(storageFile, properties -> {
                    State state = loadData(properties);
                    T t = callback.call(state);
                    if (writeToFile) {
                        saveData(state, properties);
                    }
                    return t;
                }, writeToFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalStateException("Instance storage location does not exist: " + storageFile);
        }
    }

    private static void logInfo(String message, boolean printOutput, Object... args) {
        if (LOGGER.isInfoEnabled() || printOutput) {
            String formatted = String.format(message, args);
            LOGGER.info(formatted);
            if (printOutput) {
                println(formatted);
            }
        }
    }

    private static void logDebug(String message, boolean printOutput, Object... args) {
        if (LOGGER.isDebugEnabled() || printOutput) {
            String formatted = String.format(message, args);
            LOGGER.debug(formatted);
            if (printOutput) {
                println(formatted);
            }
        }
    }

    public synchronized Instance createInstance(final String name, final InstanceSettings settings, final boolean printOutput) throws Exception {
        return execute(state -> {
            if (state.instances.get(name) != null) {
                throw new IllegalArgumentException("Instance '" + name + "' already exists");
            }
            if (!settings.getProfiles().isEmpty()) {
                try {
                    ProfileApplier.verify();
                } catch (NoClassDefFoundError error) {
                    throw new IllegalArgumentException("Profile service package is not available");
                }
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
            String sshHost = settings.getAddress();
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
            mkdir(karafBase, "etc/scripts", printOutput);
            mkdir(karafBase, "system", printOutput);
            mkdir(karafBase, "deploy", printOutput);
            mkdir(karafBase, "data", printOutput);

            Map<String, URL> textResources = new HashMap<>(settings.getTextResources());
            Map<String, URL> binaryResources = new HashMap<>(settings.getBinaryResources());

            String[] resources =
            {
                    "etc/all.policy",
                    "etc/config.properties",
                    "etc/custom.properties",
                    "etc/distribution.info",
                    "etc/equinox-debug.properties",
                    "etc/java.util.logging.properties",
                    "etc/jmx.acl.cfg",
                    "etc/jmx.acl.java.lang.Memory.cfg",
                    "etc/jmx.acl.org.apache.karaf.bundle.cfg",
                    "etc/jmx.acl.org.apache.karaf.config.cfg",
                    "etc/jmx.acl.org.apache.karaf.security.jmx.cfg",
                    "etc/jmx.acl.osgi.compendium.cm.cfg",
                    "etc/jre.properties",
                    "etc/keys.properties",
                    "etc/org.apache.felix.eventadmin.impl.EventAdmin.cfg",
                    "etc/org.apache.felix.fileinstall-deploy.cfg",
                    "etc/org.apache.karaf.command.acl.bundle.cfg",
                    "etc/org.apache.karaf.command.acl.config.cfg",
                    "etc/org.apache.karaf.command.acl.feature.cfg",
                    "etc/org.apache.karaf.command.acl.jaas.cfg",
                    "etc/org.apache.karaf.command.acl.kar.cfg",
                    "etc/org.apache.karaf.command.acl.scope_bundle.cfg",
                    "etc/org.apache.karaf.command.acl.shell.cfg",
                    "etc/org.apache.karaf.command.acl.system.cfg",
                    "etc/org.apache.karaf.features.repos.cfg",
                    "etc/org.apache.karaf.jaas.cfg",
                    "etc/org.apache.karaf.kar.cfg",
                    "etc/org.apache.karaf.log.cfg",
                    "etc/org.ops4j.pax.logging.cfg",
                    "etc/org.ops4j.pax.url.mvn.cfg",
                    "etc/shell.init.script",
                    "etc/users.properties",
                    "etc/scripts/shell.completion.script",
                    FEATURES_CFG
            };
            copyResourcesToDir(resources, karafBase, textResources, printOutput);
            addFeaturesFromSettings(new File(karafBase, FEATURES_CFG), settings);

            // The startup.properties is now generated by the karaf maven plugin, so
            // we use the one from the root instance instead of embedding it
            File rootEtc = new File(System.getProperty("karaf.etc"));
            copy(new File(rootEtc, "startup.properties"), new File(karafBase, "etc/startup.properties"));

            // align child with any bundles we have overriden in the root instance
            File rootOverrides = new File(rootEtc, "overrides.properties");
            if (rootOverrides.exists()) {
                copy(rootOverrides, new File(karafBase, "etc/overrides.properties"));
            }

            HashMap<String, String> props = new HashMap<>();
            props.put("${SUBST-KARAF-NAME}", name);
            props.put("${SUBST-KARAF-HOME}", System.getProperty("karaf.home"));
            props.put("${SUBST-KARAF-BASE}", karafBase.getPath());
            props.put("${SUBST-SSH-PORT}", Integer.toString(sshPort));
            props.put("${SUBST-SSH-HOST}", sshHost);
            props.put("${SUBST-RMI-REGISTRY-PORT}", Integer.toString(rmiRegistryPort));
            props.put("${SUBST-RMI-SERVER-PORT}", Integer.toString(rmiServerPort));

            String[] filteredResources =
            {
                 "etc/system.properties",
                 "etc/org.apache.karaf.shell.cfg",
                 "etc/org.apache.karaf.management.cfg",
                 "bin/karaf",
                 "bin/start",
                 "bin/stop",
                 "bin/karaf.bat",
                 "bin/start.bat",
                 "bin/stop.bat"
            };
            copyFilteredResourcesToDir(filteredResources, karafBase, textResources, props, printOutput);

            try {
                makeFileExecutable(new File(karafBase, "bin/karaf"));
                makeFileExecutable(new File(karafBase, "bin/start"));
                makeFileExecutable(new File(karafBase, "bin/stop"));
            } catch (IOException e) {
                LOGGER.debug("Could not set file mode on scripts.", e);
            }

            for (String resource : textResources.keySet()) {
                copyFilteredResourceToDir(resource, karafBase, textResources, props, printOutput);
            }

            for (String resource : binaryResources.keySet()) {
                copyBinaryResourceToDir(resource, karafBase, binaryResources, printOutput);
            }

            if (!settings.getProfiles().isEmpty()) {
                ProfileApplier.applyProfiles(karafBase, settings.getProfiles(), printOutput);
            }

            String javaOpts = settings.getJavaOpts();
            if (javaOpts == null || javaOpts.length() == 0) {
                javaOpts = DEFAULT_JAVA_OPTS;
            }
            InstanceState is = new InstanceState();
            is.name = name;
            is.loc = karafBase.toString();
            is.opts = javaOpts;
            state.instances.put(name, is);
            InstanceImpl instance = new InstanceImpl(InstanceServiceImpl.this, name);
            InstanceServiceImpl.this.proxies.put(name, instance);
            return instance;
        }, true);
    }

    void addFeaturesFromSettings(File featuresCfg, final InstanceSettings settings) throws IOException {
        FileLockUtils.execute(featuresCfg, properties -> {
            appendToPropList(properties, "featuresBoot", settings.getFeatures());
            appendToPropList(properties, "featuresRepositories", settings.getFeatureURLs());
        }, true);
    }

    private static void appendToPropList(Properties p, String key, List<String> elements) {
        if (elements == null) {
            return;
        }
        StringBuilder sb = new StringBuilder(p.get(key).trim());
        for (String f : elements) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(f);
        }
        p.put(key, sb.toString());
    }

    public Instance[] getInstances() {
        return execute(state -> proxies.values().toArray(new Instance[proxies.size()]), false);
    }

    public Instance getInstance(final String name) {
        return execute(state -> proxies.get(name), false);
    }

    public void startInstance(final String name, final String javaOpts) {
        execute(state -> {
            InstanceState instance = state.instances.get(name);
            if (instance == null) {
                throw new IllegalArgumentException("Instance " + name + " not found");
            }
            checkPid(instance);
            if (instance.pid != 0) {
                throw new IllegalStateException("Instance already started");
            }
            doStart(instance, name, javaOpts);
            return null;
        }, true);
    }

    private void doStart(InstanceState instance, String name, String javaOpts) throws IOException {
        String opts = javaOpts;
        if (opts == null || opts.length() == 0) {
            opts = instance.opts;
        }
        if (opts == null || opts.length() == 0) {
            opts = DEFAULT_JAVA_OPTS;
        }

        // fallback and read karafOpts from KARAF_OPTS environment if no System property present
        String karafOptsEnv = System.getenv("KARAF_OPTS");
        String karafOpts = System.getProperty("karaf.opts", karafOptsEnv != null ? karafOptsEnv : "");

        String location = instance.loc;

        File libDir = new File(System.getProperty("karaf.home"), "lib");
        File bootLibDir = new File(libDir, "boot");
        File childLibDir = new File(location, "lib");

        StringBuilder classpath = classpathFromLibDir(bootLibDir);
        StringBuilder childClasspath = classpathFromLibDir(childLibDir);
        if (childClasspath.length() > 0 && !bootLibDir.equals(childLibDir)) {
            classpath.append(System.getProperty("path.separator"));
            classpath.append(childClasspath);
        }

        String jdkOpts;
        if (!System.getProperty("java.version").startsWith("1.")) {
            StringBuilder jdk9Classpath = classpathFromLibDir(new File(new File(System.getProperty("karaf.home"), "lib"), "jdk9plus"));
            if (jdk9Classpath.length() > 0) {
                classpath.append(System.getProperty("path.separator"));
                classpath.append(jdk9Classpath);
            }
            jdkOpts = " --add-reads=java.xml=java.logging" +
                      " --add-exports=java.base/org.apache.karaf.specs.locator=java.xml,ALL-UNNAMED" +
                      " --patch-module java.base=lib/endorsed/org.apache.karaf.specs.locator-" + System.getProperty("karaf.version") + ".jar" +
                      " --patch-module java.xml=lib/endorsed/org.apache.karaf.specs.java.xml-" + System.getProperty("karaf.version") + ".jar" +
                      " --add-opens java.base/java.security=ALL-UNNAMED" +
                      " --add-opens java.base/java.net=ALL-UNNAMED" +
                      " --add-opens java.base/java.lang=ALL-UNNAMED" +
                      " --add-opens java.base/java.util=ALL-UNNAMED" +
                      " --add-opens java.naming/javax.naming.spi=ALL-UNNAMED" +
                      " --add-opens java.rmi/sun.rmi.transport.tcp=ALL-UNNAMED" +
                      " --add-exports=java.base/sun.net.www.protocol.file=ALL-UNNAMED" +
                      " --add-exports=java.base/sun.net.www.protocol.ftp=ALL-UNNAMED" +
                      " --add-exports=java.base/sun.net.www.protocol.http=ALL-UNNAMED" +
                      " --add-exports=java.base/sun.net.www.protocol.https=ALL-UNNAMED" +
                      " --add-exports=java.base/sun.net.www.protocol.jar=ALL-UNNAMED" +
                      " --add-exports=java.base/sun.net.www.content.text=ALL-UNNAMED" +
                      " --add-exports=jdk.xml.dom/org.w3c.dom.html=ALL-UNNAMED" +
                      " --add-exports=jdk.naming.rmi/com.sun.jndi.url.rmi=ALL-UNNAMED";
        } else {
            jdkOpts = " -Djava.endorsed.dirs=\"" + new File(new File(new File(System.getProperty("java.home"), "jre"), "lib"), "endorsed") + System.getProperty("path.separator") + new File(new File(System.getProperty("java.home"), "lib"), "endorsed") + System.getProperty("path.separator") + new File(libDir, "endorsed").getCanonicalPath() + "\""
                    + " -Djava.ext.dirs=\"" + new File(new File(new File(System.getProperty("java.home"), "jre"), "lib"), "ext") + System.getProperty("path.separator") + new File(new File(System.getProperty("java.home"), "lib"), "ext") + System.getProperty("path.separator") + new File(libDir, "ext").getCanonicalPath() + "\"";
        }
        String command = "\""
                + new File(System.getProperty("java.home"), ScriptUtils.isWindows() ? "bin\\java.exe" : "bin/java").getCanonicalPath()
                + "\" " + opts
                + " " + karafOpts
                + " " + jdkOpts
                + " -Djava.util.logging.config.file=\"" + new File(location, "etc/java.util.logging.properties").getCanonicalPath() + "\""
                + " -Dkaraf.home=\"" + System.getProperty("karaf.home") + "\""
                + " -Dkaraf.base=\"" + new File(location).getCanonicalPath() + "\""
                + " -Dkaraf.data=\"" + new File(new File(location).getCanonicalPath(), "data") + "\""
                + " -Dkaraf.etc=\"" + new File(new File(location).getCanonicalPath(), "etc") + "\""
                + " -Dkaraf.log=\"" + new File(new File(new File(location).getCanonicalFile(), "data"), "log") + "\""
                + " -Djava.io.tmpdir=\"" + new File(new File(new File(location).getCanonicalFile(), "data") , "tmp") + "\""
                + " -Dkaraf.restart.jvm.supported=true"
                + " -Dkaraf.startLocalConsole=false"
                + " -Dkaraf.startRemoteShell=true"
                + " -classpath \"" + classpath.toString() + "\""
                + " org.apache.karaf.main.Main server";
        if (System.getenv("KARAF_REDIRECT") != null && !System.getenv("KARAF_REDIRECT").isEmpty()) {
            command = command + " >> " + System.getenv("KARAF_REDIRECT");
        }

        LOGGER.debug("Starting instance " + name + " with command: " + command);
        Process process = new ProcessBuilderFactoryImpl().newBuilder()
                .directory(new File(location))
                .command(command)
                .start();
        instance.pid = process.getPid();
    }

    private StringBuilder classpathFromLibDir(File libDir) throws IOException {
        File[] jars = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
        StringBuilder classpath = new StringBuilder();
        if (jars != null) {
            for (File jar : jars) {
                if (classpath.length() > 0) {
                    classpath.append(System.getProperty("path.separator"));
                }
                classpath.append(jar.getCanonicalPath());
            }
        }
        return classpath;
    }

    private void addJar(StringBuilder sb, String groupId, String artifactId) {
        File artifactDir = new File(System.getProperty("karaf.home") + File.separator +
                                    "system" + File.separator +
                                    groupId.replaceAll("\\.", File.separator) + File.separator +
                                    artifactId + File.separator);
        TreeMap<String, File> jars = new TreeMap<>();
        String[] versions = artifactDir.list();
        if (versions != null) {
            for (String version : versions) {
                File jar = new File(artifactDir, version + File.separator + artifactId + "-" + version + ".jar");
                if (jar.exists()) {
                    jars.put(version, jar);
                }
            }
        }
        if (jars.isEmpty()) {
            throw new IllegalStateException("Cound not find jar for " + groupId + "/" + artifactId);
        }
        if (sb.length() > 0) {
            sb.append(File.pathSeparator);
        }
        sb.append(jars.lastEntry().getValue().getAbsolutePath());
    }

    public void restartInstance(final String name, final String javaOpts) {
        execute(state -> {
            InstanceState instance = state.instances.get(name);
            if (instance == null) {
                throw new IllegalArgumentException("Instance " + name + " not found");
            }
            String current = System.getProperty("karaf.name");
            if (name.equals(current)) {
                String location = System.getProperty("karaf.home");
                StringBuilder classpath = new StringBuilder();
                addJar(classpath, "org.apache.karaf.instance", "org.apache.karaf.instance.core");
                addJar(classpath, "org.apache.karaf.shell", "org.apache.karaf.shell.core");
                addJar(classpath, "org.ops4j.pax.logging", "pax-logging-api");
                addJar(classpath, "jline", "jline");
                String command = "\""
                        + new File(System.getProperty("java.home"), ScriptUtils.isWindows() ? "bin\\java.exe" : "bin/java").getCanonicalPath()
                        + "\" "
                        + " -Djava.util.logging.config.file=\"" + new File(location, "etc/java.util.logging.properties").getCanonicalPath() + "\""
                        + " -Dkaraf.home=\"" + System.getProperty("karaf.home") + "\""
                        + " -Dkaraf.base=\"" + new File(location).getCanonicalPath() + "\""
                        + " -Dkaraf.data=\"" + new File(new File(location).getCanonicalPath(), "data") + "\""
                        + " -Dkaraf.etc=\"" + new File(new File(location).getCanonicalPath(), "etc") + "\""
                        + " -Dkaraf.log=\"" + new File(new File(new File(location).getCanonicalFile(), "data"), "log") + "\""
                        + " -Dkaraf.instances=\"" + System.getProperty("karaf.instances") + "\""
                        + " -classpath \"" + classpath.toString() + "\""
                        + " " + Execute.class.getName()
                        + " restart --java-opts \"" + javaOpts + "\" " + name;
                new ProcessBuilderFactoryImpl().newBuilder()
                        .directory(new File(System.getProperty("karaf.home")))
                        .command(command)
                        .start();
            } else {
                checkPid(instance);
                if (instance.pid != 0) {
                    cleanShutdown(instance);
                }
                doStart(instance, name, javaOpts);
            }
            return null;
        }, true);
    }

    public void stopInstance(final String name) {
        Integer pid = execute(state -> {
            int rootInstancePID = 0;
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
                if (!instance.root) {
                    Process process = new ProcessBuilderFactoryImpl().newBuilder().attach(instance.pid);
                    process.destroy();
                } else {
                    //can't simply destroy root instance here
                    //as it will lose the update in instances.properties
                    //because of no chance to run the saveData
                    rootInstancePID = instance.pid;
                }
                instance.pid = 0;

            }
            return rootInstancePID;
        }, true);
        if (pid != 0 && isInstanceRoot(name)) {
            Process process;
            try {
                process = new ProcessBuilderFactoryImpl().newBuilder().attach(pid);
                process.destroy();
            } catch (IOException e) {
                LOGGER.debug("Unable to cleanly shutdown root instance ", e);
            }
        }
    }

    public void destroyInstance(final String name) {
        execute(state -> {
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
        }, true);
    }

    public void renameInstance(final String oldName, final String newName, final boolean printOutput) throws Exception {
        execute(state -> {
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

            println("Renaming instance "
                    + SimpleAnsi.INTENSITY_BOLD + oldName + SimpleAnsi.INTENSITY_NORMAL
                    + " to "
                    + SimpleAnsi.INTENSITY_BOLD + newName + SimpleAnsi.INTENSITY_NORMAL);
            // rename directory
            String oldLocationPath = instance.loc;
            File oldLocation = new File(oldLocationPath);
            String basedir = oldLocation.getParent();
            File newLocation = new File(basedir, newName);
            oldLocation.renameTo(newLocation);
            // create the properties map including the instance name and instance location
            // TODO: replacing is bad, we should re-extract the needed files
            HashMap<String, String> props = new HashMap<>();
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
        }, true);
    }

    public synchronized Instance cloneInstance(final String name, final String cloneName, final InstanceSettings settings, final boolean printOutput) throws Exception {
        final int instanceSshPort = getInstanceSshPort(name);
        final int instanceRmiRegistryPort = getInstanceRmiRegistryPort(name);
        final int instanceRmiServerPort = getInstanceRmiServerPort(name);
        return execute(state -> {
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
            HashMap<String, String> props = new HashMap<>();
            props.put(name, cloneName);
            props.put(locationPath, cloneLocationPath);
            if (settings.getSshPort() > 0)
                props.put(Integer.toString(instanceSshPort), Integer.toString(settings.getSshPort()));
            if (settings.getRmiRegistryPort() > 0)
                props.put(Integer.toString(instanceRmiRegistryPort), Integer.toString(settings.getRmiRegistryPort()));
            if (settings.getRmiServerPort() > 0)
                props.put(Integer.toString(instanceRmiServerPort), Integer.toString(settings.getRmiServerPort()));

            // filtering clone files
            filterResource(cloneLocation, "etc/custom.properties", props);
            filterResource(cloneLocation, "etc/org.apache.karaf.management.cfg", props);
            filterResource(cloneLocation, "etc/org.apache.karaf.shell.cfg", props);
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
                javaOpts = DEFAULT_JAVA_OPTS;
            }
            InstanceState is = new InstanceState();
            is.name = cloneName;
            is.loc = cloneLocation.toString();
            is.opts = javaOpts;
            state.instances.put(cloneName, is);
            InstanceImpl cloneInstance = new InstanceImpl(InstanceServiceImpl.this, cloneName);
            InstanceServiceImpl.this.proxies.put(cloneName, cloneInstance);
            return cloneInstance;
        }, true);
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
            Properties props = PropertiesLoader.loadPropertiesFile(file.toURI().toURL(), false);
            props.put("karaf.base", new File(instance.loc).getCanonicalPath());
            props.put("karaf.home", System.getProperty("karaf.home"));
            props.put("karaf.data", new File(new File(instance.loc), "data").getCanonicalPath());
            props.put("karaf.etc", new File(new File(instance.loc), "etc").getCanonicalPath());
            props.put("karaf.log", new File(new File(new File(instance.loc), "data"), "log").getCanonicalPath());
            InterpolationHelper.performSubstitution(props, null, true, false, true);
            int port = Integer.parseInt(props.getProperty(KARAF_SHUTDOWN_PORT, "0"));
            String host = props.getProperty(KARAF_SHUTDOWN_HOST, "localhost");
            String portFile = props.getProperty(KARAF_SHUTDOWN_PORT_FILE);
            String shutdown = props.getProperty(KARAF_SHUTDOWN_COMMAND, DEFAULT_SHUTDOWN_COMMAND);
            if (port == 0 && portFile != null) {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(portFile)))) {
                    String portStr = r.readLine();
                    port = Integer.parseInt(portStr);
                }
            }
            // We found the port, try to send the command
            if (port > 0) {
                Socket s = new Socket(host, port);
                s.getOutputStream().write(shutdown.getBytes());
                s.close();
                long stopTimeout = Long.parseLong(props.getProperty(KARAF_SHUTDOWN_TIMEOUT,
                                                                    Long.toString(getStopTimeout())));
                long t = System.currentTimeMillis() + stopTimeout;
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

    String getInstanceSshHost(String name) {
        return getKarafHost(name, "etc/org.apache.karaf.shell.cfg", "sshHost");
    }

    int getInstanceRmiRegistryPort(String name) {
        return getKarafPort(name, "etc/org.apache.karaf.management.cfg", "rmiRegistryPort");
    }

    void changeInstanceRmiRegistryPort(String name, final int port) throws Exception {
        setKarafPort(name, "etc/org.apache.karaf.management.cfg", "rmiRegistryPort", port);
    }

    String getInstanceRmiRegistryHost(String name) {
        return getKarafHost(name, "etc/org.apache.karaf.management.cfg", "rmiRegistryHost");
    }

    int getInstanceRmiServerPort(String name) {
        return getKarafPort(name, "etc/org.apache.karaf.management.cfg", "rmiServerPort");
    }

    void changeInstanceRmiServerPort(String name, int port) throws Exception {
        setKarafPort(name, "etc/org.apache.karaf.management.cfg", "rmiServerPort", port);
    }

    String getInstanceRmiServerHost(String name) {
        return getKarafHost(name, "etc/org.apache.karaf.management.cfg", "rmiServerHost");
    }

    private int getKarafPort(final String name, final String path, final String key) {
        return execute(state -> getKarafPort(state, name, path, key), false);
    }


    private Integer getKarafPort(State state, String name, String path, final String key) {
        InstanceState instance = state.instances.get(name);
        if (instance == null) {
            throw new IllegalArgumentException("Instance " + name + " not found");
        }
        File f = new File(instance.loc, path);
        try {
            return FileLockUtils.execute(f, properties -> {
                Object obj = properties.get(key);
                return obj instanceof Number ? ((Number) obj).intValue() : Integer.parseInt(obj.toString());
            }, false);
        } catch (IOException e) {
            return 0;
        }
    }

    private void setKarafPort(final String name, final String path, final String key, final int port) throws IOException {
        execute(state -> {
            InstanceState instance = state.instances.get(name);
            if (instance == null) {
                throw new IllegalArgumentException("Instance " + name + " not found");
            }
            checkPid(instance);
            if (instance.pid != 0) {
                throw new IllegalStateException("Instance is not stopped");
            }
            File f = new File(instance.loc, path);
            FileLockUtils.execute(f, properties -> {
                properties.put(key, String.valueOf(port));
            }, true);
            return null;
        }, true);
    }

    private String getKarafHost(final String name, final String path, final String key) {
        return execute(state -> InstanceServiceImpl.this.getKarafHost(state, name, path, key), false);
    }

    private String getKarafHost(State state, String name, String path, final String key) {
        InstanceState instance = state.instances.get(name);
        if (instance == null) {
            throw new IllegalArgumentException("Instance " + name + " not found");
        }
        File f = new File(instance.loc, path);
        try {
            return FileLockUtils.execute(f, (Properties properties) -> properties.get(key).toString(), false);
        } catch (IOException e) {
            return "0.0.0.0";
        }
    }

    boolean isInstanceRoot(final String name) {
        return execute(state -> {
            InstanceState instance = state.instances.get(name);
            if (instance == null) {
                throw new IllegalArgumentException("Instance " + name + " not found");
            }
            return instance.root;
        }, false);
    }

    String getInstanceLocation(final String name) {
        return execute(state -> {
            InstanceState instance = state.instances.get(name);
            if (instance == null) {
                throw new IllegalArgumentException("Instance " + name + " not found");
            }
            return instance.loc;
        }, true);
    }

    int getInstancePid(final String name) {
        boolean updateInstanceProperties = isInstancePidNeedUpdate(name);
        return execute(state -> {
            InstanceState instance = state.instances.get(name);
            if (instance == null) {
                throw new IllegalArgumentException("Instance " + name + " not found");
            }
            checkPid(instance);
            return instance.pid;
        }, updateInstanceProperties);
    }

    String getInstanceJavaOpts(final String name) {
        return execute(state -> {
            InstanceState instance = state.instances.get(name);
            if (instance == null) {
                throw new IllegalArgumentException("Instance " + name + " not found");
            }
            return instance.opts;
        }, false);
    }

    void changeInstanceJavaOpts(final String name, final String opts) {
        execute(state -> {
            InstanceState instance = state.instances.get(name);
            if (instance == null) {
                throw new IllegalArgumentException("Instance " + name + " not found");
            }
            instance.opts = opts;
            return null;
        }, true);
    }

    String getInstanceState(final String name) {
        boolean updateInstanceProperties = isInstancePidNeedUpdate(name);
        return execute(state -> {
            InstanceState instance = state.instances.get(name);
            if (instance == null) {
                throw new IllegalArgumentException("Instance " + name + " not found");
            }
            int port = getKarafPort(state, name, "etc/org.apache.karaf.shell.cfg", "sshPort");
            String host = getKarafHost(state, name, "etc/org.apache.karaf.shell.cfg", "sshHost");
            if (host.equals("0.0.0.0")) {
                host = "localhost";
            }
            if (!new File(instance.loc).isDirectory() || port <= 0) {
                return Instance.ERROR;
            }
            checkPid(instance);
            if (instance.pid == 0) {
                return Instance.STOPPED;
            } else {
                try {
                    Socket s = new Socket(host, port);
                    s.close();
                    return Instance.STARTED;
                } catch (Exception e) {
                    // ignore
                }
                return Instance.STARTING;
            }
        }, updateInstanceProperties);
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
                for (File file : files) {
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

    private void copyResourcesToDir(String[] resourcesToCopy, File target, Map<String, URL> resources, boolean printOutput) throws IOException {
        for (String resource : resourcesToCopy) {
            copyResourceToDir(resource, target, resources, printOutput);
        }
    }

    private void copyResourceToDir(String resource, File target, Map<String, URL> resources, boolean printOutput) throws IOException {
        File outFile = new File(target, resource);
        if( !outFile.exists() ) {
            logDebug("Creating file: %s", printOutput, outFile.getPath());
            try (
                InputStream is = getResourceStream(resource, resources);
                OutputStream os = new FileOutputStream(outFile)
            ) {
                if (is == null) {
                    logInfo("\tWARNING: unable to find %s", true, resource);
                } else {
                    copyStream(is, os);
                }
            }
        }
    }

    private InputStream getResourceStream(String resource, Map<String, URL> resources) throws IOException {
        return resources.containsKey(resource)
                ? resources.remove(resource).openStream()
                : getClass().getClassLoader().getResourceAsStream(RESOURCE_BASE + resource);
    }

    private static void println(String st) {
        System.out.println(st);
    }

    protected static Properties loadPropertiesFile(URL configPropURL) throws Exception {
        try (
            InputStream is = configPropURL.openConnection().getInputStream()
        ){
            Properties configProps = new Properties();
            configProps.load(is);
            return configProps;
        }
        catch (Exception ex) {
            System.err.println("Error loading config properties from " + configPropURL);
            System.err.println("Main: " + ex);
            return null;
        }
    }

    /**
     * Read stream one line at a time so that we can use the platform
     * line ending when we write it out.
     * @param is
     * @param os
     */
    private void copyStream(InputStream is, OutputStream os) {
        PrintStream out = new PrintStream(os);
        Scanner scanner = new Scanner(is);
        while (scanner.hasNextLine() ) {
            String line = scanner.nextLine();
            out.println(line);
        }
        scanner.close();
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

    private void copyFilteredResourcesToDir(String[] resourcesToCopy, File target, Map<String, URL> resources, Map<String, String> props, boolean printOutput) throws IOException {
        for (String resource : resourcesToCopy) {
            copyFilteredResourceToDir(resource, target, resources, props, printOutput);
        }
    }

    private void copyFilteredResourceToDir(String resource, File target, Map<String, URL> resources, Map<String, String> props, boolean printOutput) throws IOException {
        File outFile = new File(target, resource);
        if (!outFile.exists()) {
            logDebug("Creating file: %s", printOutput, outFile.getPath());
            try (
                InputStream is = getResourceStream(resource, resources);
                OutputStream os = new FileOutputStream(outFile)
            ) {
                copyAndFilterResource(is, os, props);
            }
        }
    }

    private void copyAndFilterResource(InputStream source, OutputStream target, Map<String, String> props) throws IOException {
        // read it line at a time so that we can use the platform line ending when we write it out.
        PrintStream out = new PrintStream(target);
        Scanner scanner = new Scanner(source);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            line = filter(line, props);
            out.println(line);
        }
        scanner.close();
    }

    private void copyBinaryResourceToDir(String resource, File target, Map<String, URL> resources, boolean printOutput) throws IOException {
        File outFile = new File(target, resource);
        if( !outFile.exists() ) {
            logDebug("Creating file: %s", printOutput, outFile.getPath());
            try (
                InputStream is = getResourceStream(resource, resources);
                OutputStream os = new FileOutputStream(outFile)
            ) {
                StreamUtils.copy(is, os);
            }
        }
    }

    private String filter(String line, Map<String, String> props) {
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
            logDebug("Creating dir: %s", printOutput, file.getPath());
            file.mkdirs();
        }
    }

    private void makeFileExecutable(File serviceFile) throws IOException {
        try {
            Set<PosixFilePermission> permissions = new HashSet<>();
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);

            // Get the existing permissions and add the executable permissions to them
            Set<PosixFilePermission> filePermissions = Files.getPosixFilePermissions(serviceFile.toPath());
            filePermissions.addAll(permissions);
            Files.setPosixFilePermissions(serviceFile.toPath(), filePermissions);
        }
        catch (UnsupportedOperationException ex)
        {
            serviceFile.setExecutable(true, false);
        }
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
        if (source.getName().matches("transaction_\\d+\\.log")) {
            // ignore active txlog files
            return;
        }
        if (source.getName().endsWith(".instance")) {
            // ignore instance bundles cache
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
            try(
                InputStream in = new FileInputStream(source);
                OutputStream out = new FileOutputStream(destination)
            ) {
                StreamUtils.copy(in, out);
            }
        }
    }

    public void changeInstanceSshHost(String name, String host) throws Exception {
        setKarafHost(name, "etc/org.apache.karaf.shell.cfg", "sshHost", host);
    }

    private void setKarafHost(final String name, final String path, final String key, final String host) throws IOException {
        execute(state -> {
            InstanceState instance = state.instances.get(name);
            if (instance == null) {
                throw new IllegalArgumentException("Instance " + name + " not found");
            }
            checkPid(instance);
            if (instance.pid != 0) {
                throw new IllegalStateException("Instance is not stopped");
            }
            File f = new File(instance.loc, path);
            FileLockUtils.execute(f, properties -> { properties.put(key, host); }, true);
            return null;
        }, true);
    }

    private boolean isInstancePidNeedUpdate(final String name) {
        return execute(state -> {
            InstanceState instance = state.instances.get(name);
            if (instance == null) {
                throw new IllegalArgumentException("Instance " + name + " not found");
            }
            int originalPid = instance.pid;
            checkPid(instance);
            int newPid = instance.pid;
            return originalPid != newPid;
        }, false);
    }

    private static class ProfileApplier {

        // Verify that profile package is wired correctly
        static void verify() {
            Profile.class.getName();
        }

        static void applyProfiles(File karafBase, List<String> profiles, boolean printOutput) throws IOException {
            BundleContext bundleContext = FrameworkUtil.getBundle(ProfileApplier.class).getBundleContext();
            ServiceReference<ProfileService> reference = bundleContext.getServiceReference(ProfileService.class);
            ProfileService service = bundleContext.getService(reference);

            Profile profile = ProfileBuilder.Factory.create("temp")
                    .addParents(profiles)
                    .getProfile();
            Profile overlay = service.getOverlayProfile(profile);
            final Profile effective = service.getEffectiveProfile(overlay, false);

            Map<String, byte[]> configs = effective.getFileConfigurations();
            for (Map.Entry<String, byte[]> config : configs.entrySet()) {
                String pid = config.getKey();
                if (!pid.equals(Profile.INTERNAL_PID + Profile.PROPERTIES_SUFFIX)) {
                    Path configFile = Paths.get(karafBase.toString(), "etc", pid);
                    logDebug("Creating file: %s", printOutput, configFile.toString());
                    Files.write(configFile, config.getValue());
                }
            }
            FileLockUtils.execute(new File(karafBase, FEATURES_CFG), properties -> {
                appendToPropList(properties, "featuresBoot", effective.getFeatures());
                appendToPropList(properties, "featuresRepositories", effective.getRepositories());
            }, true);

            bundleContext.ungetService(reference);
        }


    }

}
