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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import org.apache.karaf.instance.core.Instance;
import org.apache.karaf.instance.core.InstanceService;
import org.apache.karaf.instance.core.InstanceSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstanceServiceImpl implements InstanceService {

    public static final String STORAGE_FILE = "instance.properties";
    public static final String BACKUP_EXTENSION = ".bak";
    private static final String FEATURES_CFG = "etc/org.apache.karaf.features.cfg";

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceServiceImpl.class);

    private Map<String, Instance> instances = new HashMap<String, Instance>();

    private int defaultSshPortStart = 8101;

    private int defaultRmiRegistryPortStart = 1099;

    private int defaultRmiServerPortStart = 44444;

    private File storageLocation;

    private long stopTimeout = 30000;

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

    private Properties loadStorage(File location) throws IOException {
        InputStream is = null;
        try {
            is = new FileInputStream(location);
            Properties props = new Properties();
            props.load(is);
            return props;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private void saveStorage(Properties props, File location, String comment) throws IOException {
        OutputStream os = null;
        try {
            os = new FileOutputStream(location);
            props.store(os, comment);
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }

    public synchronized void init() throws Exception {
        try {
            File storageFile = new File(storageLocation, STORAGE_FILE);
            if (!storageFile.isFile()) {
                if (storageFile.exists()) {
                    LOGGER.error("Instances storage location should be a file: " + storageFile);
                }
                return;
            }
            Properties storage = loadStorage(storageFile);
            int count = Integer.parseInt(storage.getProperty("count", "0"));
            defaultSshPortStart = Integer.parseInt(storage.getProperty("ssh.port", Integer.toString(defaultSshPortStart)));
            defaultRmiRegistryPortStart = Integer.parseInt(storage.getProperty("rmi.registry.port", Integer.toString(defaultRmiRegistryPortStart)));
            defaultRmiServerPortStart = Integer.parseInt(storage.getProperty("rmi.server.port", Integer.toString(defaultRmiServerPortStart)));
            Map<String, Instance> newInstances = new HashMap<String, Instance>();
            for (int i = 0; i < count; i++) {
                String name = storage.getProperty("item." + i + ".name", null);
                String loc = storage.getProperty("item." + i + ".loc", null);
                String opts = storage.getProperty("item." + i + ".opts", null);
                int pid = Integer.parseInt(storage.getProperty("item." + i + ".pid", "0"));
                boolean root = Boolean.parseBoolean(storage.getProperty("item." + i + ".root", "false"));
                if (name != null) {
                    InstanceImpl instance = new InstanceImpl(this, name, loc, opts, root);
                    if (pid > 0) {
                        try {
                            instance.attach(pid);
                        } catch (IOException e) {
                            // Ignore
                        }
                    }
                    newInstances.put(name, instance);
                }
            }
            instances = newInstances;
        } catch (Exception e) {
            LOGGER.warn("Unable to reload Karaf instance list", e);
        }
    }
   
    public synchronized void refreshInstance() throws Exception {
        try {
            init();
            File storageFile = new File(storageLocation, STORAGE_FILE);
            if (!storageFile.isFile()) {
                if (storageFile.exists()) {
                    LOGGER.error("Instances storage location should be a file: " + storageFile);
                }
                return;
            }
            Properties storage = loadStorage(storageFile);
            int count = Integer.parseInt(storage.getProperty("count", "0"));
            for (int i = 0; i < count; i++) {
                String name = storage.getProperty("item." + i + ".name", null);
                int pid = Integer.parseInt(storage.getProperty("item." + i + ".pid", "0"));
                if (name != null) {
                    InstanceImpl instance = (InstanceImpl)instances.get(name);
                    if (pid > 0 && instance != null && !instance.isAttached()) {
                        try {
                            instance.attach(pid);
                        } catch (IOException e) {
                            // Ignore
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to reload Karaf instance list", e);
        }
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

    public synchronized Instance createInstance(String name, InstanceSettings settings, boolean printOutput) throws Exception {
        try {
            init();
        } catch (Exception e) {
            LOGGER.warn("Unable to reload Karaf instance list", e);
        }
        if (instances.get(name) != null) {
            throw new IllegalArgumentException("Instance '" + name + "' already exists");
        }
        String loc = settings.getLocation() != null ? settings.getLocation() : name;
        File karafBase = new File(loc);
        if (!karafBase.isAbsolute()) {
            karafBase = new File(storageLocation, loc);
        }
        int sshPort = settings.getSshPort();
        if (sshPort <= 0) {
            sshPort = ++defaultSshPortStart;
        }
        int rmiRegistryPort = settings.getRmiRegistryPort();
        if (rmiRegistryPort <= 0) {
            rmiRegistryPort = ++defaultRmiRegistryPortStart;
        }
        int rmiServerPort = settings.getRmiServerPort();
        if (rmiServerPort <= 0) {
            rmiServerPort = ++defaultRmiServerPortStart;
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
        copyResourceToDir(karafBase, "etc/org.apache.karaf.log.cfg", printOutput);
        copyResourceToDir(karafBase, "etc/org.ops4j.pax.logging.cfg", printOutput);
        copyResourceToDir(karafBase, "etc/org.ops4j.pax.url.mvn.cfg", printOutput);
        // copyResourceToDir(karafBase, "etc/startup.properties", printOutput);
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
        copyFilteredResourceToDir(karafBase, "bin/karaf.bat", props, printOutput);
        copyFilteredResourceToDir(karafBase, "bin/start.bat", props, printOutput);
        copyFilteredResourceToDir(karafBase, "bin/stop.bat", props, printOutput);
        copyFilteredResourceToDir(karafBase, "bin/karaf", props, printOutput);
        copyFilteredResourceToDir(karafBase, "bin/start", props, printOutput);
        copyFilteredResourceToDir(karafBase, "bin/stop", props, printOutput);
        if ( !windows ) {
            chmod(new File(karafBase, "bin/karaf"), "a+x");
            chmod(new File(karafBase, "bin/start"), "a+x");
            chmod(new File(karafBase, "bin/stop"), "a+x");
        }


        String javaOpts = settings.getJavaOpts();
        if (javaOpts == null || javaOpts.length() == 0) {
            javaOpts = "-server -Xmx512M -Dcom.sun.management.jmxremote";
        }
        Instance instance = new InstanceImpl(this, name, karafBase.toString(), settings.getJavaOpts());
        instances.put(name, instance);
        saveState();
        return instance;
    }

    void addFeaturesFromSettings(File featuresCfg, InstanceSettings settings) throws IOException {
        Properties p = loadStorage(featuresCfg);
        appendToPropList(p, "featuresBoot", Collections.singletonList("ssh"));
        appendToPropList(p, "featuresBoot", settings.getFeatures());
        appendToPropList(p, "featuresRepositories", settings.getFeatureURLs());
        saveStorage(p, featuresCfg, "Features Configuration");
    }

    private void appendToPropList(Properties p, String key, List<String> elements) {
        if (elements == null) {
            return;
        }
        StringBuilder sb = new StringBuilder(p.getProperty(key).trim());
        for (String f : elements) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(f);
        }
        p.setProperty(key, sb.toString());
    }
    
    public synchronized Instance[] getInstances() {
        return instances.values().toArray(new Instance[0]);
    }

    public synchronized Instance getInstance(String name) {
        try {
            init();
        } catch (Exception e) {
            LOGGER.warn("Unable to reload Karaf instance list", e);
        }
        return instances.get(name);
    }

    synchronized void forget(String name) {
        instances.remove(name);
    }

    public synchronized void renameInstance(String oldName, String newName, boolean printOutput) throws Exception {
        try {
            init();
        } catch (Exception e) {
            LOGGER.warn("Unable to reload Karaf instance list", e);
        }
        if (instances.get(newName) != null) {
            throw new IllegalArgumentException("Instance " + newName + " already exists");
        }
        Instance instance = instances.get(oldName);
        if (instance == null) {
            throw new IllegalArgumentException("Instance " + oldName + " not found");
        }
        if (instance.isRoot()) {
            throw new IllegalArgumentException("Root instance cannot be renamed");
        }
        if (instance.getPid() != 0) {
            throw new IllegalStateException("Instance not stopped");
        }

        logInfo("Renaming instance %s to %s", printOutput, oldName, newName);
        
        // remove the old instance
        instances.remove(oldName);
        // update instance
        instance.setName(newName);
        // rename directory
        String oldLocationPath = instance.getLocation();
        File oldLocation = new File(oldLocationPath);
        String basedir = oldLocation.getParent();
        File newLocation = new File(basedir, newName);
        oldLocation.renameTo(newLocation);
        // update the instance location
        instance.setLocation(newLocation.getPath());
        // create the properties map including the instance name and instance location
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
        // add the renamed instances
        instances.put(newName, instance);
        // save instance definition in the instances.properties
        saveState();
    }

    public synchronized Instance cloneInstance(String name, String cloneName, InstanceSettings settings, boolean printOutput) throws Exception {
        try {
            init();
        } catch (Exception e) {
            LOGGER.warn("Unable to reload Karaf instance list", e);
        }
        if (instances.get(cloneName) != null) {
            throw new IllegalArgumentException("Instance " + cloneName + " already exists");
        }
        Instance instance = instances.get(name);
        if (instance == null) {
            throw new IllegalArgumentException("Instance " + name + " not found");
        }

        logInfo("Cloning instance %s into %s", printOutput, name, cloneName);
        
        // define the clone instance location
        String cloneLocationPath = settings.getLocation() != null ? settings.getLocation() : cloneName;
        File cloneLocation = new File(cloneLocationPath);
        if (!cloneLocation.isAbsolute()) {
            cloneLocation = new File(storageLocation, cloneLocationPath);
        }
        // copy instance directory
        String locationPath = instance.getLocation();
        File location = new File(locationPath);
        copy(location, cloneLocation);
        // create the properties map including the instance name, location, ssh and rmi port numbers
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(name, cloneName);
        props.put(locationPath, cloneLocationPath);
        if (settings.getSshPort() > 0)
            props.put(new Integer(instance.getSshPort()).toString(), new Integer(settings.getSshPort()).toString());
        if (settings.getRmiRegistryPort() > 0)
            props.put(new Integer(instance.getRmiRegistryPort()).toString(), new Integer(settings.getRmiRegistryPort()).toString());
        if (settings.getRmiServerPort() > 0)
            props.put(new Integer(instance.getRmiServerPort()).toString(), new Integer(settings.getRmiServerPort()).toString());
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
        Instance cloneInstance = new InstanceImpl(this, cloneName, cloneLocation.toString(), settings.getJavaOpts());
        instances.put(cloneName, cloneInstance);
        saveState();
        return cloneInstance;
    }

    synchronized void saveState() throws IOException {
        Properties storage = new Properties();
        Instance[] data = getInstances();
        storage.setProperty("ssh.port", Integer.toString(defaultSshPortStart));
        storage.setProperty("rmi.registry.port", Integer.toString(defaultRmiRegistryPortStart));
        storage.setProperty("rmi.server.port", Integer.toString(defaultRmiServerPortStart));
        storage.setProperty("count", Integer.toString(data.length));
        for (int i = 0; i < data.length; i++) {
            storage.setProperty("item." + i + ".name", data[i].getName());
            storage.setProperty("item." + i + ".root", data[i].isRoot() + "");
            storage.setProperty("item." + i + ".loc", data[i].getLocation());
            storage.setProperty("item." + i + ".pid", Integer.toString(data[i].getPid()));
            storage.setProperty("item." + i + ".opts", data[i].getJavaOpts() != null ? data[i].getJavaOpts() : "");
        }
        saveStorage(storage, new File(storageLocation, STORAGE_FILE), "Instances Service storage");
    }
    
    private void copyResourceToDir(File target, String resource, boolean printOutput) throws Exception {
        File outFile = new File(target, resource);

        if (outFile.exists()) {
            return;
        }
        
        logInfo("Creating file: %s", printOutput, outFile.getPath());

        String sourcePath = "org/apache/karaf/instance/resources/" + resource;
        InputStream is = getClass().getClassLoader().getResourceAsStream(sourcePath);
        if (is == null) {
            throw new IOException("Unable to find resource " + sourcePath + " on classpath");
        }
        try {
            // Read it line at a time so that we can use the platform line
            // ending when we write it out.
            PrintStream out = new PrintStream(new FileOutputStream(outFile));
            try {
                Scanner scanner = new Scanner(is);
                while (scanner.hasNextLine()) {
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

    private void println(String st) {
        System.out.println(st);
    }

    private void filterResource(File basedir, String path, HashMap<String, String> props) throws Exception {
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

    private void copyFilteredResourceToDir(File target, String resource, HashMap<String, String> props, boolean printOutput) throws Exception {
        File outFile = new File(target, resource);
        if( !outFile.exists() ) {
            logInfo("Creating file: %s", printOutput, outFile.getPath());
            InputStream is = getClass().getClassLoader().getResourceAsStream("org/apache/karaf/instance/resources/" + resource);
            copyAndFilterResource(is, new FileOutputStream(outFile), props);
        }
    }

    private void copyAndFilterResource(InputStream source, OutputStream target, HashMap<String, String> props) throws Exception {
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

    private int chmod(File serviceFile, String mode) throws Exception {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("chmod", mode, serviceFile.getCanonicalPath());
        Process p = builder.start();
        int status = p.waitFor();
        return status;
    }

    private void copy(File source, File destination) throws Exception {
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

}
