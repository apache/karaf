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
package org.apache.felix.karaf.gshell.admin.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.FileInputStream;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Properties;

import org.apache.felix.karaf.gshell.admin.AdminService;
import org.apache.felix.karaf.gshell.admin.Instance;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.fusesource.jansi.Ansi;

public class AdminServiceImpl implements AdminService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminServiceImpl.class);

    private Map<String, Instance> instances = new HashMap<String, Instance>();

    private int defaultPortStart = 8101;

    private File storageLocation;

    public File getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(File storage) {
        this.storageLocation = storage;
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

    private void saveStorage(Properties props, File location) throws IOException {
        OutputStream os = null;
        try {
            os = new FileOutputStream(location);
            props.store(os, "Admin Service storage");
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }

    public synchronized void init() throws Exception {
        try {
            if (!storageLocation.isFile()) {
                if (storageLocation.exists()) {
                    LOGGER.error("Instances storage location should be a file: " + storageLocation);
                }
                return;
            }
            Properties storage = loadStorage(storageLocation);
            int count = Integer.parseInt(storage.getProperty("count", "0"));
            defaultPortStart = Integer.parseInt(storage.getProperty("port", Integer.toString(defaultPortStart)));
            Map<String, Instance> newInstances = new HashMap<String, Instance>();
            for (int i = 0; i < count; i++) {
                String name = storage.getProperty("item." + i + ".name", null);
                String loc = storage.getProperty("item." + i + ".loc", null);
                int pid = Integer.parseInt(storage.getProperty("item." + i + ".pid", "0"));
                if (name != null) {
                    InstanceImpl instance = new InstanceImpl(this, name, loc);
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

    public synchronized Instance createInstance(String name, int port, String location) throws Exception {
        if (instances.get(name) != null) {
            throw new IllegalArgumentException("Instance '" + name + "' already exists");
        }
        File serviceMixBase = new File(location != null ? location : ("instances/" + name)).getCanonicalFile();
        int sshPort = port;
        if (sshPort <= 0) {
            sshPort = ++defaultPortStart;
        }
        println(Ansi.ansi().a("Creating new instance on port ").a(sshPort).a(" at: ").a(Ansi.Attribute.INTENSITY_BOLD).a(serviceMixBase).a(Ansi.Attribute.RESET).toString());

        mkdir(serviceMixBase, "bin");
        mkdir(serviceMixBase, "etc");
        mkdir(serviceMixBase, "system");
        mkdir(serviceMixBase, "deploy");
        mkdir(serviceMixBase, "data");

        copyResourceToDir(serviceMixBase, "etc/config.properties", true);
        copyResourceToDir(serviceMixBase, "etc/java.util.logging.properties", true);
        copyResourceToDir(serviceMixBase, "etc/org.apache.felix.karaf.log.cfg", true);
        copyResourceToDir(serviceMixBase, "etc/org.apache.felix.karaf.features.cfg", true);
        copyResourceToDir(serviceMixBase, "etc/org.apache.felix.karaf.management.cfg", true);
        copyResourceToDir(serviceMixBase, "etc/org.ops4j.pax.logging.cfg", true);
        copyResourceToDir(serviceMixBase, "etc/org.ops4j.pax.url.mvn.cfg", true);
        copyResourceToDir(serviceMixBase, "etc/startup.properties", true);
        copyResourceToDir(serviceMixBase, "etc/users.properties", true);

        HashMap<String, String> props = new HashMap<String, String>();
        props.put("${karaf.name}", name);
        props.put("${karaf.home}", System.getProperty("karaf.home"));
        props.put("${karaf.base}", serviceMixBase.getPath());
        props.put("${karaf.sshPort}", Integer.toString(sshPort));
        copyFilteredResourceToDir(serviceMixBase, "etc/system.properties", props);
        copyFilteredResourceToDir(serviceMixBase, "etc/org.apache.felix.karaf.shell.cfg", props);
        if( System.getProperty("os.name").startsWith("Win") ) {
            copyFilteredResourceToDir(serviceMixBase, "bin/start.bat", props);
            copyFilteredResourceToDir(serviceMixBase, "bin/stop.bat", props);
        } else {
            copyFilteredResourceToDir(serviceMixBase, "bin/start", props);
            copyFilteredResourceToDir(serviceMixBase, "bin/stop", props);
            chmod(new File(serviceMixBase, "bin/start"), "a+x");
            chmod(new File(serviceMixBase, "bin/stop"), "a+x");
        }
        Instance instance = new InstanceImpl(this, name, serviceMixBase.toString());
        instances.put(name, instance);
        saveState();
        return instance;
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

    synchronized void saveState() throws IOException {
        Properties storage = new Properties();
        Instance[] data = getInstances();
        storage.setProperty("port", Integer.toString(defaultPortStart));
        storage.setProperty("count", Integer.toString(data.length));
        for (int i = 0; i < data.length; i++) {
            storage.setProperty("item." + i + ".name", data[i].getName());
            storage.setProperty("item." + i + ".loc", data[i].getLocation());
            storage.setProperty("item." + i + ".pid", Integer.toString(data[i].getPid()));
        }
        saveStorage(storage, storageLocation);
    }

    private void copyResourceToDir(File target, String resource, boolean text) throws Exception {
        File outFile = new File(target, resource);
        if( !outFile.exists() ) {
            println(Ansi.ansi().a("Creating file: ").a(Ansi.Attribute.INTENSITY_BOLD).a(outFile.getPath()).a(Ansi.Attribute.RESET).toString());
            InputStream is = getClass().getClassLoader().getResourceAsStream("org/apache/felix/karaf/gshell/admin/" + resource);
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

    private void copyFilteredResourceToDir(File target, String resource, HashMap<String, String> props) throws Exception {
        File outFile = new File(target, resource);
        if( !outFile.exists() ) {
            println(Ansi.ansi().a("Creating file: ").a(Ansi.Attribute.INTENSITY_BOLD).a(outFile.getPath()).a(Ansi.Attribute.RESET).toString());
            InputStream is = getClass().getClassLoader().getResourceAsStream("org/apache/felix/karaf/gshell/admin/" + resource);
            try {
                // Read it line at a time so that we can use the platform line ending when we write it out.
                PrintStream out = new PrintStream(new FileOutputStream(outFile));
                try {
                    Scanner scanner = new Scanner(is);
                    while (scanner.hasNextLine() ) {
                        String line = scanner.nextLine();
                        line = filter(line, props);
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

    private void mkdir(File serviceMixBase, String path) {
        File file = new File(serviceMixBase, path);
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
