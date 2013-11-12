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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.karaf.info.ServerInfo;
import org.apache.karaf.main.lock.Lock;
import org.apache.karaf.main.lock.LockCallBack;
import org.apache.karaf.main.lock.NoLock;
import org.apache.karaf.main.util.ArtifactResolver;
import org.apache.karaf.main.util.BootstrapLogManager;
import org.apache.karaf.main.util.SimpleMavenResolver;
import org.apache.karaf.main.util.StringMap;
import org.apache.karaf.main.util.Utils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;

/**
 * <p>
 * This class is the default way to instantiate and execute the framework. It is not
 * intended to be the only way to instantiate and execute the framework; rather, it is
 * one example of how to do so. When embedding the framework in a host application,
 * this class can serve as a simple guide of how to do so. It may even be
 * worthwhile to reuse some of its property handling capabilities. This class
 * is completely static and is only intended to start a single instance of
 * the framework.
 * </p>
 */
public class Main {
    /**
     * The default name used for the startup properties file.
     */
    public static final String STARTUP_PROPERTIES_FILE_NAME = "startup.properties";


    Logger LOG = Logger.getLogger(this.getClass().getName());

    private ConfigProperties config;
    private Framework framework = null;
    private final String[] args;
    private int exitCode;
    private ShutdownCallback shutdownCallback;
    private KarafActivatorManager activatorManager;
    private Lock lock;
    private KarafLockCallback lockCallback;
    private boolean exiting;
    
    /**
     * <p>
     * This method performs the main task of constructing an framework instance
     * and starting its execution. The following functions are performed
     * when invoked:
     * </p>
     * <ol>
     *   <li><i><b>Read the system properties file.<b></i> This is a file
     *       containing properties to be pushed into <tt>System.setProperty()</tt>
     *       before starting the framework. This mechanism is mainly shorthand
     *       for people starting the framework from the command line to avoid having
     *       to specify a bunch of <tt>-D</tt> system property definitions.
     *       The only properties defined in this file that will impact the framework's
     *       behavior are the those concerning setting HTTP proxies, such as
     *       <tt>http.proxyHost</tt>, <tt>http.proxyPort</tt>, and
     *       <tt>http.proxyAuth</tt>.
     *   </li>
     *   <li><i><b>Perform system property variable substitution on system
     *       properties.</b></i> Any system properties in the system property
     *       file whose value adheres to <tt>${&lt;system-prop-name&gt;}</tt>
     *       syntax will have their value substituted with the appropriate
     *       system property value.
     *   </li>
     *   <li><i><b>Read the framework's configuration property file.</b></i> This is
     *       a file containing properties used to configure the framework
     *       instance and to pass configuration information into
     *       bundles installed into the framework instance. The configuration
     *       property file is called <tt>config.properties</tt> by default
     *       and is located in the <tt>conf/</tt> directory of the Felix
     *       installation directory, which is the parent directory of the
     *       directory containing the <tt>felix.jar</tt> file. It is possible
     *       to use a different location for the property file by specifying
     *       the desired URL using the <tt>felix.config.properties</tt>
     *       system property; this should be set using the <tt>-D</tt> syntax
     *       when executing the JVM. Refer to the
     *       <a href="Felix.html#Felix(java.util.Map, java.util.List)">
     *       <tt>Felix</tt></a> constructor documentation for more
     *       information on the framework configuration options.
     *   </li>
     *   <li><i><b>Perform system property variable substitution on configuration
     *       properties.</b></i> Any configuration properties whose value adheres to
     *       <tt>${&lt;system-prop-name&gt;}</tt> syntax will have their value
     *       substituted with the appropriate system property value.
     *   </li>
     *   <li><i><b>Ensure the default bundle cache has sufficient information to
     *       initialize.</b></i> The default implementation of the bundle cache
     *       requires either a profile name or a profile directory in order to
     *       start. The configuration properties are checked for at least one
     *       of the <tt>felix.cache.profile</tt> or <tt>felix.cache.profiledir</tt>
     *       properties. If neither is found, the user is asked to supply a profile
     *       name that is added to the configuration property set. See the
     *       <a href="cache/DefaultBundleCache.html"><tt>DefaultBundleCache</tt></a>
     *       documentation for more details its configuration options.
     *   </li>
     *   <li><i><b>Creates and starts a framework instance.</b></i> A
     *       case insensitive
     *       <a href="util/StringMap.html"><tt>StringMap</tt></a>
     *       is created for the configuration property file and is passed
     *       into the framework.
     *   </li>
     * </ol>
     * <p>
     * It should be noted that simply starting an instance of the framework is not enough
     * to create an interactive session with it. It is necessary to install
     * and start bundles that provide an interactive impl; this is generally
     * done by specifying an "auto-start" property in the framework configuration
     * property file. If no interactive impl bundles are installed or if
     * the configuration property file cannot be found, the framework will appear to
     * be hung or deadlocked. This is not the case, it is executing correctly,
     * there is just no way to interact with it. Refer to the
     * <a href="Felix.html#Felix(java.util.Map, java.util.List)">
     * <tt>Felix</tt></a> constructor documentation for more information on
     * framework configuration options.
     * </p>
     * @param args An array of arguments, all of which are ignored.
     * @throws Exception If an error occurs.
     **/
    public static void main(String[] args) throws Exception {
        while (true) {
            boolean restart = false;
            System.setProperty("karaf.restart", "false");
            final Main main = new Main(args);
            try {
                main.launch();
            } catch (Throwable ex) {
                // Also log to sytem.err in case logging is not yet initialized
                System.err.println(ex.getMessage());

                main.LOG.log(Level.SEVERE, "Could not launch framework", ex);
                main.destroy();
                main.setExitCode(-1);
            }
            try {
                main.awaitShutdown();
                boolean stopped = main.destroy();
                restart = Boolean.getBoolean("karaf.restart");
                if (!stopped) {
                    if (restart) {
                        System.err.println("Timeout waiting for framework to stop.  Restarting now.");
                    } else {
                        System.err.println("Timeout waiting for framework to stop.  Exiting VM.");
                        main.setExitCode(-3);
                    }
                }
            } catch (Throwable ex) {
                main.setExitCode(-2);
                System.err.println("Error occurred shutting down framework: " + ex);
                ex.printStackTrace();
            } finally {
                if (!restart) {
                    System.exit(main.getExitCode());
                } else {
                    System.gc();
                }
            }
        }
    }

    public Main(String[] args) {
        this.args = args;
    }

    public void setShutdownCallback(ShutdownCallback shutdownCallback) {
        this.shutdownCallback = shutdownCallback;
    }

    public void launch() throws Exception {
        config = new ConfigProperties();
        if (config.delayConsoleStart) {
            System.out.println(config.startupMessage);
        }
        BootstrapLogManager.setProperties(config.props);
        lock = createLock();
        lockCallback = new KarafLockCallback();
        InstanceHelper.updateInstancePid(config.karafHome, config.karafBase);
        LOG.addHandler(BootstrapLogManager.getDefaultHandler());

        for (String provider : config.securityProviders) {
            addSecurityProvider(provider);
        }
        
        List<File> bundleDirs = getBundleRepos();
        ArtifactResolver resolver = new SimpleMavenResolver(bundleDirs);

        // Start up the OSGI framework
        ClassLoader classLoader = createClassLoader(resolver);
        FrameworkFactory factory = loadFrameworkFactory(classLoader);
        framework = factory.newFramework(new StringMap(config.props, false));
        framework.init();
        framework.getBundleContext().addFrameworkListener(lockCallback);
        framework.start();

        FrameworkStartLevel sl = framework.adapt(FrameworkStartLevel.class);
        sl.setInitialBundleStartLevel(config.defaultBundleStartlevel);

        // If we have a clean state, install everything
        if (framework.getBundleContext().getBundles().length == 1) {

            LOG.info("Installing and starting initial bundles");
            File startupPropsFile = new File(config.etcFolder, STARTUP_PROPERTIES_FILE_NAME);
            List<BundleInfo> bundles = readBundlesFromStartupProperties(startupPropsFile);        
            installAndStartBundles(resolver, framework.getBundleContext(), bundles);
            LOG.info("All initial bundles installed and set to start");
        }

        ServerInfo serverInfo = new ServerInfoImpl(args, config);
        framework.getBundleContext().registerService(ServerInfo.class, serverInfo, null);

        activatorManager = new KarafActivatorManager(classLoader, framework);
        activatorManager.startKarafActivators();
        
        setStartLevel(config.lockStartLevel);
        // Progress bar
        if (config.delayConsoleStart) {
            new StartupListener(LOG, framework.getBundleContext());
        }
        monitor();
    }

    private void monitor() {
        new Thread() {
            public void run() {
                doMonitor();
            }
        }.start();
    }

    private void doMonitor() {
        File dataDir = new File(System.getProperty(ConfigProperties.PROP_KARAF_DATA));
        while (!exiting) {
            try {
                if (lock.lock()) {
                    lockCallback.lockAquired();
                    for (;;) {
                        if (!dataDir.isDirectory()) {
                            LOG.info("Data directory does not exist anymore, halting");
                            framework.stop();
                            System.exit(-1);
                            return;
                        }
                        if (!lock.isAlive() || exiting) {
                            break;
                        }
                        Thread.sleep(config.lockDelay);
                    }
                    if (!exiting) {
                        lockCallback.lockLost();
                    }
                } else {
                    lockCallback.waitingForLock();
                }
                Thread.sleep(config.lockDelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    Lock getLock() {
        return lock;
    }
    
    private ClassLoader createClassLoader(ArtifactResolver resolver) throws Exception {
        List<URL> urls = new ArrayList<URL>();
        urls.add(resolver.resolve(config.frameworkBundle).toURL());
        File[] libs = new File(config.karafHome, "lib").listFiles();
        if (libs != null) {
            for (File f : libs) {
                if (f.isFile() && f.canRead() && f.getName().endsWith(".jar")) {
                    urls.add(f.toURI().toURL());
                }
            }
        }
        return new URLClassLoader(urls.toArray(new URL[urls.size()]), Main.class.getClassLoader());
    }
    
    private FrameworkFactory loadFrameworkFactory(ClassLoader classLoader) throws Exception {
        String factoryClass = config.frameworkFactoryClass;
        if (factoryClass == null) {
            InputStream is = classLoader.getResourceAsStream("META-INF/services/" + FrameworkFactory.class.getName());
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            factoryClass = br.readLine();
            br.close();
        }
        FrameworkFactory factory = (FrameworkFactory) classLoader.loadClass(factoryClass).newInstance();
        return factory;
    }

    private Lock createLock() {
        if (!config.useLock) {
            return new NoLock();
        }
        try {
            return (Lock) Lock.class.getClassLoader().loadClass(config.lockClass).getConstructor(Properties.class).newInstance(config.props);
        } catch (Exception e) {
            if (e instanceof InvocationTargetException){
                throw new RuntimeException("Exception instantiating lock class " + config.lockClass
                                            + "\n" + ((InvocationTargetException)e).getTargetException().getMessage(), e);
            }else{
                throw new RuntimeException("Exception instantiating lock class " + config.lockClass, e);
            }
        }
    }

    private static void addSecurityProvider(String provider) {
        try {
            Security.addProvider((Provider) Class.forName(provider).newInstance());
        } catch (Throwable t) {
            System.err.println("Unable to register security provider: " + t);
        }
    }
    
    public List<BundleInfo> readBundlesFromStartupProperties(File startupPropsFile) {
        Properties startupProps = PropertiesLoader.loadPropertiesOrFail(startupPropsFile);
        Enumeration<Object> keyIt = startupProps.keys();
        List<BundleInfo> bundeList = new ArrayList<BundleInfo>();
        while (keyIt.hasMoreElements()) {
            String key = (String) keyIt.nextElement();
            try {
                BundleInfo bi = new BundleInfo();
                bi.uri = new URI(key);
                String startlevelSt = startupProps.getProperty(key).trim();
                bi.startLevel = new Integer(startlevelSt);
                bundeList.add(bi);
            } catch (Exception e) {
                throw new RuntimeException("Error loading startup bundle list from " + startupPropsFile + " at " + key, e);
            }
        }
        return bundeList; 
    }

    private void installAndStartBundles(ArtifactResolver resolver, BundleContext context, List<BundleInfo> bundles) {
        for (BundleInfo bundleInfo : bundles) {
            try {
                URI resolvedURI = resolver.resolve(bundleInfo.uri);
                Bundle b = context.installBundle(bundleInfo.uri.toString(), resolvedURI.toURL().openStream());
                b.adapt(BundleStartLevel.class).setStartLevel(bundleInfo.startLevel);
                if (isNotFragment(b)) {
                    b.start();
                }
            } catch (Exception  e) {
                throw new RuntimeException("Error installing bundle listed in " + STARTUP_PROPERTIES_FILE_NAME
                        + " with url: " + bundleInfo.uri + " and startlevel: " + bundleInfo.startLevel, e);
            }
        }
    }

    private boolean isNotFragment(Bundle b) {
        String fragmentHostHeader = (String) b.getHeaders().get(Constants.FRAGMENT_HOST);
        return fragmentHostHeader == null || fragmentHostHeader.trim().length() == 0;
    }

    private List<File> getBundleRepos() {
        List<File> bundleDirs = new ArrayList<File>();
        File baseSystemRepo = new File(config.karafHome, config.defaultRepo);
        if (!baseSystemRepo.exists() && baseSystemRepo.isDirectory()) {
            throw new RuntimeException("system repo folder not found: " + baseSystemRepo.getAbsolutePath());
        }
        bundleDirs.add(baseSystemRepo);

        File homeSystemRepo = new File(config.karafHome, config.defaultRepo);
        bundleDirs.add(homeSystemRepo);

        String locations = config.bundleLocations;
        if (locations != null) {
            StringTokenizer st = new StringTokenizer(locations, "\" ", true);
            if (st.countTokens() > 0) {
                String location;
                do {
                    location = Utils.nextLocation(st);
                    if (location != null) {
                        File f;
                        if (config.karafBase.equals(config.karafHome)) {
                            f = new File(config.karafHome, location);
                        } else {
                            f = new File(config.karafBase, location);
                        }
                        if (f.exists() && f.isDirectory()) {
                            bundleDirs.add(f);
                        } else {
                            System.err.println("Bundle location " + location
                                    + " does not exist or is not a directory.");
                        }
                    }
                }

                while (location != null);
            }
        }
        return bundleDirs;
    }

    /**
     * Retrieve the arguments used when launching Karaf
     *
     * @return the arguments of the main karaf process
     */
    public String[] getArgs() {
        return args;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public Framework getFramework() {
        return framework;
    }

    protected void setStartLevel(int level) {
        framework.adapt(FrameworkStartLevel.class).setStartLevel(level);
    }

    public void awaitShutdown() throws Exception {
        if (framework == null) {
            return;
        }
        while (true) {
            FrameworkEvent event = framework.waitForStop(0);
            if (event.getType() != FrameworkEvent.STOPPED_UPDATE) {
                return;
            }
        }
    }

    public boolean destroy() throws Exception {
        if (framework == null) {
            return true;
        }
        try {
            int step = 5000;

            // Notify the callback asap
            if (shutdownCallback != null) {
                shutdownCallback.waitingForShutdown(step);
            }

            exiting = true;

            if (framework.getState() == Bundle.ACTIVE || framework.getState() == Bundle.STARTING) {
                new Thread() {
                    public void run() {
                        try {
                            framework.stop();
                        } catch (BundleException e) {
                            System.err.println("Error stopping karaf: " + e.getMessage());
                        }
                    }
                }.start();
            }

            int timeout = config.shutdownTimeout;
            if (config.shutdownTimeout <= 0) {
                timeout = Integer.MAX_VALUE;
            }
            while (timeout > 0) {
                timeout -= step;
                if (shutdownCallback != null) {
                    shutdownCallback.waitingForShutdown(step * 2);
                }
                FrameworkEvent event = framework.waitForStop(step);
                if (event.getType() != FrameworkEvent.WAIT_TIMEDOUT) {
                    activatorManager.stopKarafActivators();
                    return true;
                }
            }
            return false;
        } finally {
            if (lock != null) {
                exiting = true;
                lock.release();
            }
        }
    }
    
    private final class KarafLockCallback implements LockCallBack, FrameworkListener {
        private Object startLevelLock = new Object();

        @Override
        public void lockLost() {
            if (framework.getState() == Bundle.ACTIVE) {
                LOG.warning("Lock lost. Setting startlevel to " + config.lockStartLevel);
                synchronized (startLevelLock) {
                    setStartLevel(config.lockStartLevel);

                    // we have to wait for the start level to be reduced here because
                    // if the lock is regained before the start level is fully changed
                    // things may not come up as expected
                    LOG.fine("Waiting for start level change to complete...");
                    try {
                        startLevelLock.wait(config.shutdownTimeout);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void lockAquired() {
            LOG.info("Lock acquired. Setting startlevel to " + config.defaultStartLevel);
            InstanceHelper.setupShutdown(config, framework);
            setStartLevel(config.defaultStartLevel);
        }

        @Override
        public void waitingForLock() {
            LOG.fine("Waiting for the lock ...");
        }

        @Override
        public void frameworkEvent(FrameworkEvent event) {
            if (event.getType() == FrameworkEvent.STARTLEVEL_CHANGED) {
                synchronized (startLevelLock) {
                    LOG.fine("Start level change complete.");
                    startLevelLock.notifyAll();
                }
            }
       }
    }

}
