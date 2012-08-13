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
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import org.apache.karaf.main.lock.SimpleFileLock;
import org.apache.karaf.main.util.Utils;
import org.osgi.framework.Constants;

public class ConfigProperties {
    /**
     * The system property for specifying the Karaf home directory.  The home directory
     * hold the binary install of Karaf.
     */
    public static final String PROP_KARAF_HOME = "karaf.home";
    /**
     * The environment variable for specifying the Karaf home directory.  The home directory
     * hold the binary install of Karaf.
     */
    public static final String ENV_KARAF_HOME = "KARAF_HOME";
    /**
     * The system property for specifying the Karaf base directory.  The base directory
     * holds the configuration and data for a Karaf instance.
     */
    public static final String PROP_KARAF_BASE = "karaf.base";
    /**
     * The environment variable for specifying the Karaf base directory.  The base directory
     * holds the configuration and data for a Karaf instance.
     */
    public static final String ENV_KARAF_BASE = "KARAF_BASE";
    /**
     * The system property for specifying the Karaf data directory. The data directory
     * holds the bundles data and cache for a Karaf instance.
     */
    public static final String PROP_KARAF_DATA = "karaf.data";
    /**
     * The environment variable for specifying the Karaf data directory. The data directory
     * holds the bundles data and cache for a Karaf instance.
     */
    public static final String ENV_KARAF_DATA = "KARAF_DATA";
    /**
     * The system property for specifying the Karaf data directory. The data directory
     * holds the bundles data and cache for a Karaf instance.
     */
    public static final String PROP_KARAF_INSTANCES = "karaf.instances";
    /**
     * The system property for specifying the Karaf data directory. The data directory
     * holds the bundles data and cache for a Karaf instance.
     */
    public static final String ENV_KARAF_INSTANCES = "KARAF_INSTANCES";
    /**
     * The system property for hosting the current Karaf version.
     */
    public static final String PROP_KARAF_VERSION = "karaf.version";
    /**
     * The default name used for the configuration properties file.
     */
    private static final String CONFIG_PROPERTIES_FILE_NAME = "config.properties";
    /**
     * The default name used for the system properties file.
     */
    public static final String SYSTEM_PROPERTIES_FILE_NAME = "system.properties";

    /**
     * Config property which identifies directories which contain bundles to be loaded by SMX
     */
    private static final String BUNDLE_LOCATIONS = "bundle.locations";
    
    /**
     * The lock implementation
     */
    private static final String PROPERTY_LOCK_CLASS = "karaf.lock.class";

    private static final String PROPERTY_LOCK_DELAY = "karaf.lock.delay";

    private static final String PROPERTY_LOCK_LEVEL = "karaf.lock.level";

    private static final String DEFAULT_REPO = "karaf.default.repository";
    
    private static final String KARAF_FRAMEWORK = "karaf.framework";

    private static final String KARAF_FRAMEWORK_FACTORY = "karaf.framework.factory";

    private static final String KARAF_SHUTDOWN_TIMEOUT = "karaf.shutdown.timeout";

    private static final String KARAF_SHUTDOWN_PORT = "karaf.shutdown.port";

    private static final String KARAF_SHUTDOWN_HOST = "karaf.shutdown.host";

    private static final String KARAF_SHUTDOWN_PORT_FILE = "karaf.shutdown.port.file";

    private static final String KARAF_SHUTDOWN_COMMAND = "karaf.shutdown.command";

    private static final String KARAF_SHUTDOWN_PID_FILE = "karaf.shutdown.pid.file";
    
    private static final String KARAF_STARTUP_MESSAGE = "karaf.startup.message";
    
    private static final String KARAF_DELAY_CONSOLE = "karaf.delay.console";

    private static final String DEFAULT_SHUTDOWN_COMMAND = "SHUTDOWN";

    private static final String PROPERTY_LOCK_CLASS_DEFAULT = SimpleFileLock.class.getName();

    private static final String SECURITY_PROVIDERS = "org.apache.karaf.security.providers";
    


    /**
     * If a lock should be used before starting the runtime
     */
    private static final String PROPERTY_USE_LOCK = "karaf.lock";

    File karafHome;
    File karafBase;
    File karafData;
    File karafInstances;
    
    Properties props;
    String[] securityProviders;
    int defaultStartLevel = 100;
    int lockStartLevel = 1;
    int lockDelay = 1000;
    int shutdownTimeout = 5 * 60 * 1000;
    boolean useLock;
    String lockClass;
    String frameworkFactoryClass;
    URI frameworkBundle;
    String defaultRepo;
    String bundleLocations;
    int defaultBundleStartlevel;
    String pidFile;
    int shutdownPort;
    String shutdownHost;
    String portFile;
    String shutdownCommand;
    String includes;
    String optionals;
    File etcFolder;
    String startupMessage;
    boolean delayConsoleStart;
    
    public ConfigProperties() throws Exception {
        this.karafHome = Utils.getKarafHome(ConfigProperties.class, PROP_KARAF_HOME, ENV_KARAF_HOME);
        this.karafBase = Utils.getKarafDirectory(PROP_KARAF_BASE, ENV_KARAF_BASE, karafHome, false, true);
        this.karafData = Utils.getKarafDirectory(PROP_KARAF_DATA, ENV_KARAF_DATA, new File(karafBase, "data"), true, true);
        
        if (Boolean.getBoolean("karaf.restart.clean")) {
            Utils.deleteDirectory(this.karafData);
            this.karafData = Utils.getKarafDirectory(PROP_KARAF_DATA, ENV_KARAF_DATA, new File(karafBase, "data"), true, true);
        }
        
        this.karafInstances = Utils.getKarafDirectory(PROP_KARAF_INSTANCES, ENV_KARAF_INSTANCES, new File(karafHome, "instances"), false, false);

        Package p = Package.getPackage("org.apache.karaf.main");
        if (p != null && p.getImplementationVersion() != null)
            System.setProperty(PROP_KARAF_VERSION, p.getImplementationVersion());
        System.setProperty(PROP_KARAF_HOME, karafHome.getPath());
        System.setProperty(PROP_KARAF_BASE, karafBase.getPath());
        System.setProperty(PROP_KARAF_DATA, karafData.getPath());
        System.setProperty(PROP_KARAF_INSTANCES, karafInstances.getPath());

        this.etcFolder = new File(karafBase, "etc");
        if (!etcFolder.exists()) {
            throw new FileNotFoundException("etc folder not found: " + etcFolder.getAbsolutePath());
        }
        PropertiesLoader.loadSystemProperties(new File(etcFolder, SYSTEM_PROPERTIES_FILE_NAME));

        File file = new File(etcFolder, CONFIG_PROPERTIES_FILE_NAME);
        this.props = PropertiesLoader.loadConfigProperties(file);
        
        String prop = props.getProperty(SECURITY_PROVIDERS);
        this.securityProviders = (prop != null) ? prop.split(",") : new String[] {};
        this.defaultStartLevel = Integer.parseInt(props.getProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL));
        System.setProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, Integer.toString(this.defaultStartLevel));
        this.lockStartLevel = Integer.parseInt(props.getProperty(PROPERTY_LOCK_LEVEL, Integer.toString(lockStartLevel)));                
        this.lockDelay = Integer.parseInt(props.getProperty(PROPERTY_LOCK_DELAY, Integer.toString(lockDelay)));
        this.props.setProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, Integer.toString(lockStartLevel));
        this.shutdownTimeout = Integer.parseInt(props.getProperty(KARAF_SHUTDOWN_TIMEOUT, Integer.toString(shutdownTimeout)));
        this.useLock = Boolean.parseBoolean(props.getProperty(PROPERTY_USE_LOCK, "true"));
        this.lockClass = props.getProperty(PROPERTY_LOCK_CLASS, PROPERTY_LOCK_CLASS_DEFAULT);
        initFrameworkStorage(karafData);
        this.frameworkFactoryClass = props.getProperty(KARAF_FRAMEWORK_FACTORY);
        this.frameworkBundle = getFramework();
        this.defaultRepo = System.getProperty(DEFAULT_REPO, "system");
        this.bundleLocations = props.getProperty(BUNDLE_LOCATIONS);
        this.defaultBundleStartlevel = getDefaultBundleStartLevel(60);
        this.pidFile = props.getProperty(KARAF_SHUTDOWN_PID_FILE);
        this.shutdownPort = Integer.parseInt(props.getProperty(KARAF_SHUTDOWN_PORT, "0"));
        this.shutdownHost = props.getProperty(KARAF_SHUTDOWN_HOST, "localhost");
        this.portFile = props.getProperty(KARAF_SHUTDOWN_PORT_FILE);
        this.shutdownCommand = props.getProperty(KARAF_SHUTDOWN_COMMAND, DEFAULT_SHUTDOWN_COMMAND);
        this.startupMessage = props.getProperty(KARAF_STARTUP_MESSAGE, "Apache Karaf starting up. Press Enter to open the shell now...");
        this.delayConsoleStart = Boolean.parseBoolean(props.getProperty(KARAF_DELAY_CONSOLE, "true"));
        System.setProperty(KARAF_DELAY_CONSOLE, new Boolean(this.delayConsoleStart).toString());
    }
    
    private String getProperyOrFail(String propertyName) {
        String value = props.getProperty(propertyName);
        if (value == null) {
            throw new IllegalArgumentException("Property " + propertyName + " must be set in the etc/" + CONFIG_PROPERTIES_FILE_NAME + " configuration file");
        }
        return value;
    }
    
    private URI getFramework() throws URISyntaxException {
        String framework = getProperyOrFail(KARAF_FRAMEWORK);
        String frameworkBundleUri = getProperyOrFail(KARAF_FRAMEWORK + "." + framework);
        return new URI(frameworkBundleUri);
    }

    private void initFrameworkStorage(File karafData) throws Exception {
        String frameworkStoragePath = props.getProperty(Constants.FRAMEWORK_STORAGE);
        if (frameworkStoragePath == null) {
            File storage = new File(karafData.getPath(), "cache");
            try {
                storage.mkdirs();
            } catch (SecurityException se) {
                throw new Exception(se.getMessage()); 
            }
            props.setProperty(Constants.FRAMEWORK_STORAGE, storage.getAbsolutePath());
        }
    }
    
    private int getDefaultBundleStartLevel(int ibsl) {
        try {
            String str = props.getProperty("karaf.startlevel.bundle");
            if (str != null) {
                ibsl = Integer.parseInt(str);
            }
        } catch (Throwable t) {
        }
        return ibsl;
    }
    
}
