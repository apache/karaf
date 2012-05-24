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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.instance.core.Instance;
import org.apache.karaf.jpm.Process;
import org.apache.karaf.jpm.ProcessBuilderFactory;
import org.apache.karaf.jpm.impl.ProcessBuilderFactoryImpl;
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
    private static final String DEFAULT_SHUTDOWN_COMMAND = "SHUTDOWN";

    private InstanceServiceImpl service;
    private String name;
    private String location;
    private String javaOpts;
    private Process process;
    private boolean root;

    private final ProcessBuilderFactory processBuilderFactory;

    public InstanceImpl(InstanceServiceImpl service, String name, String location, String javaOpts) {
        this(service, name, location, javaOpts, false);
        
    }
    
    public InstanceImpl(InstanceServiceImpl service, String name, String location, String javaOpts, boolean root) {
        this.service = service;
        this.name = name;
        this.location = location;
        this.javaOpts = javaOpts;
        this.root = root;
        this.processBuilderFactory = new ProcessBuilderFactoryImpl();
    }

    public void attach(int pid) throws IOException {
        checkProcess();
        if (this.process != null) {
            throw new IllegalStateException("Instances already started");
        }
        this.process = processBuilderFactory.newBuilder().attach(pid);
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
        try {
            String loc = this.getConfiguration(new File(location, "etc/org.apache.karaf.shell.cfg"), "sshPort");
            return Integer.parseInt(loc);
        } catch (Exception e) {
            return 0;
        }
    }

    public void changeSshPort(int port) throws Exception {
        checkProcess();
        if (this.process != null) {
            throw new IllegalStateException("Instances not stopped");
        }
        this.changeConfiguration(new File(location, "etc/org.apache.karaf.shell.cfg"),
                "sshPort", Integer.toString(port));
    }

    public int getRmiRegistryPort() {
        try {
            String loc = this.getConfiguration(new File(location, "etc/org.apache.karaf.management.cfg"), "rmiRegistryPort");
            return Integer.parseInt(loc);
        } catch (Exception e) {
            return 0;
        }
    }

    public void changeRmiRegistryPort(int port) throws Exception {
        checkProcess();
        if (this.process != null) {
            throw new IllegalStateException("Instances not stopped");
        }
        this.changeConfiguration(new File(location, "etc/org.apache.karaf.management.cfg"),
                "rmiRegistryPort", Integer.toString(port));
    }

    public int getRmiServerPort() {
        try {
            String loc = this.getConfiguration(new File(location, "etc/org.apache.karaf.management.cfg"), "rmiServerPort");
            return Integer.parseInt(loc);
        } catch (Exception e) {
            return 0;
        }
    }

    public void changeRmiServerPort(int port) throws Exception {
        checkProcess();
        if (this.process != null) {
            throw new IllegalStateException("Instances not stopped");
        }
        this.changeConfiguration(new File(location, "etc/org.apache.karaf.management.cfg"),
                "rmiServerPort", Integer.toString(port));
    }

    /**
     * Change a configuration property in a given configuration file.
     *
     * @param configurationFile the configuration file where to update the configuration property.
     * @param propertyName the property name.
     * @param propertyValue the property value.
     * @throws Exception if a failure occurs.
     */
    private void changeConfiguration(File configurationFile, String propertyName, String propertyValue) throws Exception {
        Properties props = new Properties();
        InputStream is = new FileInputStream(configurationFile);
        try {
            props.load(is);
        } finally {
            is.close();
        }
        props.put(propertyName, propertyValue);
        OutputStream os = new FileOutputStream(configurationFile);
        try {
            props.save(os);
        } finally {
            os.close();
        }
    }

    /**
     * Read a given configuration file to get the value of a given property.
     *
     * @param configurationFile the configuration file where to lookup property.
     * @param propertyName the property name to look for.
     * @return the property value.
     * @throws Exception in case of read failure.
     */
    private String getConfiguration(File configurationFile, String propertyName) throws Exception {
        Properties props = loadPropertiesFile(configurationFile.toURI().toURL());
        return props.getProperty(propertyName);
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
            throw new IllegalStateException("Instances already started");
        }
        if (javaOpts == null || javaOpts.length() == 0) {
            javaOpts = this.javaOpts;
        }
        if (javaOpts == null || javaOpts.length() == 0) {
            javaOpts = "-server -Xmx512M -Dcom.sun.management.jmxremote";
        }
        String karafOpts = System.getProperty("karaf.opts", "");  
        
        File libDir = new File(System.getProperty("karaf.home"), "lib");
        String classpath = createClasspathFromAllJars(libDir);
        String endorsedDirs = getSubDirs(libDir, "endorsed");
        String extDirs = getSubDirs(libDir, "ext");

        String command = "\"" + ScriptUtils.getJavaCommandPath() + "\" " + javaOpts + " " + karafOpts
                + " -Djava.util.logging.config.file=\"" + new File(location, "etc/java.util.logging.properties").getCanonicalPath() + "\""
                + " -Djava.endorsed.dirs=\"" + endorsedDirs + "\""
                + " -Djava.ext.dirs=\"" + extDirs + "\""
                + " -Dkaraf.home=\"" + System.getProperty("karaf.home") + "\""
                + " -Dkaraf.base=\"" + new File(location).getCanonicalPath() + "\""
                + " -Dkaraf.startLocalConsole=false"
                + " -Dkaraf.startRemoteShell=true"
                + " -classpath " + classpath.toString()
                + " org.apache.karaf.main.Main";
        LOG.debug("Starting instance " + name + " with command: " + command);
        this.process = processBuilderFactory.newBuilder()
                        .directory(new File(location))
                        .command(command)
                        .start();
        this.service.saveState();
    }

    private String getSubDirs(File libDir, String subDir) throws IOException {
        File jreLibDir = new File(new File(System.getProperty("java.home"), "jre"), "lib");
        File javaLibDir = new File(System.getProperty("java.home"), "lib");
        String sep = System.getProperty("path.separator");
        return new File(jreLibDir, subDir) + sep + new File(javaLibDir, subDir) + sep + new File(libDir, subDir).getCanonicalPath();
    }

    private String createClasspathFromAllJars(File libDir) throws IOException {
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
        return classpath.toString();
    }

    public synchronized void stop() throws Exception {
        checkProcess();
        if (this.process == null) {
            throw new IllegalStateException("Instances not started");
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
            throw new IllegalStateException("Instances not stopped");
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

            String host = props.getProperty(KARAF_SHUTDOWN_HOST, "localhost");
            
            String shutdown = props.getProperty(KARAF_SHUTDOWN_COMMAND, DEFAULT_SHUTDOWN_COMMAND);
            
            int port = getShutDownPort(props);

            // We found the port, try to send the command
            if (port > 0) {
                tryShutDownAndWait(host, shutdown, port, service.getStopTimeout());
            }
        } catch (Exception e) {
            LOG.debug("Unable to cleanly shutdown instance", e);
        }
    }

	private void tryShutDownAndWait(String host, String shutdownCommand, int port, long stopTimeout)
			throws UnknownHostException, IOException, InterruptedException {
		Socket s = new Socket(host, port);
		s.getOutputStream().write(shutdownCommand.getBytes());
		s.close();
		long t = System.currentTimeMillis() + stopTimeout;
		do {
		    Thread.sleep(100);
		    checkProcess();
		} while (System.currentTimeMillis() < t && process != null);
	}

	private int getShutDownPort(Properties props) throws FileNotFoundException,
			IOException {
        
        int port = Integer.parseInt(props.getProperty(KARAF_SHUTDOWN_PORT, "0"));

		// Try to get port from port file
		String portFile = props.getProperty(KARAF_SHUTDOWN_PORT_FILE);
		if (port == 0 && portFile != null) {
		    BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(portFile)));
		    String portStr = r.readLine();
		    port = Integer.parseInt(portStr);
		    r.close();
		}
		return port;
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

    protected Properties loadPropertiesFile(URL configPropURL) throws Exception {
        // Read the properties file.
        Properties configProps = new Properties();
        InputStream is = null;
        try {
            configProps.put("karaf.base", new File(location).getCanonicalPath());
            configProps.put("karaf.home", System.getProperty("karaf.home"));
            configProps.put("karaf.data", new File(new File(location), "data").getCanonicalPath());
            is = configPropURL.openConnection().getInputStream();
            configProps.load(is);
            return configProps;
        } catch (Exception ex) {
        	throw new RuntimeException("Error loading config properties from " + configPropURL, ex);
        } finally {
        	try {
                if (is != null) is.close();
            }
            catch (IOException ex2) {
                LOG.warn(ex2.getMessage(), ex2);
            }
        }
    }

}
