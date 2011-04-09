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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import org.apache.karaf.admin.AdminService;
import org.apache.karaf.admin.Instance;
import org.apache.karaf.admin.InstanceSettings;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminServiceImpl implements AdminService {
    public static final String STORAGE_FILE = "instance.properties";
    public static final String BACKUP_EXTENSION = ".bak";
    private static final String FEATURES_CFG = "etc/org.apache.karaf.features.cfg";

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminServiceImpl.class);

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

    public synchronized Instance createInstance(String name, InstanceSettings settings) throws Exception {
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
        println(Ansi.ansi().a("Creating new instance on SSH port ").a(sshPort).a(" and RMI ports ").a(rmiRegistryPort).a("/").a(rmiServerPort).a(" at: ").a(Ansi.Attribute.INTENSITY_BOLD).a(karafBase).a(Ansi.Attribute.RESET).toString());

        mkdir(karafBase, "bin");
        mkdir(karafBase, "etc");
        mkdir(karafBase, "system");
        mkdir(karafBase, "deploy");
        mkdir(karafBase, "data");

        copyResourceToDir(karafBase, "etc/config.properties", true);
        copyResourceToDir(karafBase, "etc/jre.properties", true);
        copyResourceToDir(karafBase, "etc/custom.properties", true);
        copyResourceToDir(karafBase, "etc/java.util.logging.properties", true);
        copyResourceToDir(karafBase, "etc/org.apache.felix.fileinstall-deploy.cfg", true);
        copyResourceToDir(karafBase, "etc/org.apache.karaf.log.cfg", true);
        copyResourceToDir(karafBase, FEATURES_CFG, true);
        copyResourceToDir(karafBase, "etc/org.ops4j.pax.logging.cfg", true);
        copyResourceToDir(karafBase, "etc/org.ops4j.pax.url.mvn.cfg", true);
        copyResourceToDir(karafBase, "etc/startup.properties", true);
        copyResourceToDir(karafBase, "etc/users.properties", true);

        HashMap<String, String> props = new HashMap<String, String>();
        props.put("${SUBST-KARAF-NAME}", name);
        props.put("${SUBST-KARAF-HOME}", System.getProperty("karaf.home"));
        props.put("${SUBST-KARAF-BASE}", karafBase.getPath());
        props.put("${SUBST-SSH-PORT}", Integer.toString(sshPort));
        props.put("${SUBST-RMI-REGISTRY-PORT}", Integer.toString(rmiRegistryPort));
        props.put("${SUBST-RMI-SERVER-PORT}", Integer.toString(rmiServerPort));
        copyFilteredResourceToDir(karafBase, "etc/system.properties", props);
        copyFilteredResourceToDir(karafBase, "etc/org.apache.karaf.shell.cfg", props);
        copyFilteredResourceToDir(karafBase, "etc/org.apache.karaf.management.cfg", props);
        // If we use batch files, use batch files, else use bash scripts (even on cygwin)
        boolean windows = System.getProperty("os.name").startsWith("Win");
        boolean cygwin = windows && new File( System.getProperty("karaf.home"), "bin/admin" ).exists();
        if( windows && !cygwin ) {
            copyFilteredResourceToDir(karafBase, "bin/karaf.bat", props);
            copyFilteredResourceToDir(karafBase, "bin/start.bat", props);
            copyFilteredResourceToDir(karafBase, "bin/stop.bat", props);
        } else {
            copyFilteredResourceToDir(karafBase, "bin/karaf", props);
            copyFilteredResourceToDir(karafBase, "bin/start", props);
            copyFilteredResourceToDir(karafBase, "bin/stop", props);
            if ( !cygwin ) {
                chmod(new File(karafBase, "bin/karaf"), "a+x");
                chmod(new File(karafBase, "bin/start"), "a+x");
                chmod(new File(karafBase, "bin/stop"), "a+x");
            }
        }
        
        handleFeatures(new File(karafBase, FEATURES_CFG), settings);

        String javaOpts = settings.getJavaOpts();
        if (javaOpts == null || javaOpts.length() == 0) {
            javaOpts = "-server -Xmx512M -Dcom.sun.management.jmxremote";
        }
        Instance instance = new InstanceImpl(this, name, karafBase.toString(), settings.getJavaOpts());
        instances.put(name, instance);
        saveState();
        return instance;
    }

    void handleFeatures(File featuresCfg, InstanceSettings settings) throws IOException {
        Properties p = loadStorage(featuresCfg);

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
        return instances.get(name);
    }

    synchronized void forget(String name) {
        instances.remove(name);
    }

    public synchronized void renameInstance(String oldName, String newName) throws Exception {
        if (instances.get(newName) != null) {
            throw new IllegalArgumentException("Instance " + newName + " already exists");
        }
        Instance instance = instances.get(oldName);
        if (instance == null) {
            throw new IllegalArgumentException("Instance " + oldName + " not found");
        }
        if (instance.isRoot()) {
            throw new IllegalArgumentException("You can't rename the root instance");
        }
        if (instance.getPid() != 0) {
            throw new IllegalStateException("Instance not stopped");
        }

        println(Ansi.ansi().a("Renaming instance ")
                .a(Ansi.Attribute.INTENSITY_BOLD).a(oldName).a(Ansi.Attribute.RESET)
                .a(" to ")
                .a(Ansi.Attribute.INTENSITY_BOLD).a(newName).a(Ansi.Attribute.RESET).toString());
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
        saveStorage(storage, new File(storageLocation, STORAGE_FILE), "Admin Service storage");
    }
    
    private void copyResourceToDir(File target, String resource, boolean text) throws Exception {
        File outFile = new File(target, resource);
        if( !outFile.exists() ) {
            println(Ansi.ansi().a("Creating file: ").a(Ansi.Attribute.INTENSITY_BOLD).a(outFile.getPath()).a(Ansi.Attribute.RESET).toString());
            InputStream is = getClass().getClassLoader().getResourceAsStream("org/apache/karaf/admin/" + resource);
            try {
                if( text ) {
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
                } else {
                    // Binary so just write it out the way it came in.
                    FileOutputStream out = new FileOutputStream(new File(target, resource));
                    try {
                        int c=0;
                        while((c=is.read())>=0) {
                            out.write(c);
                        }
                    } finally {
                        safeClose(out);
                    }
                }
            } finally {
                safeClose(is);
            }
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

    private void copyFilteredResourceToDir(File target, String resource, HashMap<String, String> props) throws Exception {
        File outFile = new File(target, resource);
        if( !outFile.exists() ) {
            println(Ansi.ansi().a("Creating file: ").a(Ansi.Attribute.INTENSITY_BOLD).a(outFile.getPath()).a(Ansi.Attribute.RESET).toString());
            InputStream is = getClass().getClassLoader().getResourceAsStream("org/apache/karaf/admin/" + resource);
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

    private void mkdir(File karafBase, String path) {
        File file = new File(karafBase, path);
        if( !file.exists() ) {
            println(Ansi.ansi().a("Creating dir:  ").a(Ansi.Attribute.INTENSITY_BOLD).a(file.getPath()).a(Ansi.Attribute.RESET).toString());
            file.mkdirs();
        }
    }

    private int chmod(File serviceFile, String mode) throws Exception {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("chmod", mode, serviceFile.getCanonicalPath());
        Process p = builder.start();

        // gnodet: Fix SMX4KNL-46: cpu goes to 100% after running the 'admin create' command
        // Not sure exactly what happens, but commenting the process io redirection seems
        // to work around the problem.
        //
        //PumpStreamHandler handler = new PumpStreamHandler(io.inputStream, io.outputStream, io.errorStream);
        //handler.attach(p);
        //handler.start();
        int status = p.waitFor();
        //handler.stop();
        return status;
    }

}
