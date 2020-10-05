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

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.info.ServerInfo;
import org.apache.karaf.main.internal.Systemd;
import org.apache.karaf.main.lock.Lock;
import org.apache.karaf.main.lock.LockCallBack;
import org.apache.karaf.main.lock.NoLock;
import org.apache.karaf.main.util.ArtifactResolver;
import org.apache.karaf.main.util.BootstrapLogManager;
import org.apache.karaf.main.util.SimpleMavenResolver;
import org.apache.karaf.main.util.Utils;
import org.apache.karaf.util.config.PropertiesLoader;
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

    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    private ConfigProperties config;
    private Framework framework;
    private final String[] args;
    private int exitCode;
    private ShutdownCallback shutdownCallback;
    private KarafActivatorManager activatorManager;
    private volatile Lock lock;
    private final KarafLockCallback lockCallback = new KarafLockCallback();
    private volatile boolean exiting;
    private AutoCloseable shutdownThread;
    private Thread monitorThread;
    private URLClassLoader classLoader;

    /**
     * <p>
     * This method performs the main task of constructing an framework instance
     * and starting its execution. The following functions are performed
     * when invoked:
     * </p>
     * <ol>
     *   <li><i><b>Read the system properties file.</b></i> This is a file
     *       containing properties to be pushed into <code>System.setProperty()</code>
     *       before starting the framework. This mechanism is mainly shorthand
     *       for people starting the framework from the command line to avoid having
     *       to specify a bunch of <code>-D</code> system property definitions.
     *       The only properties defined in this file that will impact the framework's
     *       behavior are the those concerning setting HTTP proxies, such as
     *       <code>http.proxyHost</code>, <code>http.proxyPort</code>, and
     *       <code>http.proxyAuth</code>.
     *   </li>
     *   <li><i><b>Perform system property variable substitution on system
     *       properties.</b></i> Any system properties in the system property
     *       file whose value adheres to <code>${&lt;system-prop-name&gt;}</code>
     *       syntax will have their value substituted with the appropriate
     *       system property value.
     *   </li>
     *   <li><i><b>Read the framework's configuration property file.</b></i> This is
     *       a file containing properties used to configure the framework
     *       instance and to pass configuration information into
     *       bundles installed into the framework instance. The configuration
     *       property file is called <code>config.properties</code> by default
     *       and is located in the <code>conf/</code> directory of the Felix
     *       installation directory, which is the parent directory of the
     *       directory containing the <code>felix.jar</code> file. It is possible
     *       to use a different location for the property file by specifying
     *       the desired URL using the <code>felix.config.properties</code>
     *       system property; this should be set using the <code>-D</code> syntax
     *       when executing the JVM. Refer to the
     *       <code>Felix</code> constructor documentation for more
     *       information on the framework configuration options.
     *   </li>
     *   <li><i><b>Perform system property variable substitution on configuration
     *       properties.</b></i> Any configuration properties whose value adheres to
     *       <code>${&lt;system-prop-name&gt;}</code> syntax will have their value
     *       substituted with the appropriate system property value.
     *   </li>
     *   <li><i><b>Ensure the default bundle cache has sufficient information to
     *       initialize.</b></i> The default implementation of the bundle cache
     *       requires either a profile name or a profile directory in order to
     *       start. The configuration properties are checked for at least one
     *       of the <code>felix.cache.profile</code> or <code>felix.cache.profiledir</code>
     *       properties. If neither is found, the user is asked to supply a profile
     *       name that is added to the configuration property set. See the
     *       <a href="cache/DefaultBundleCache.html"><code>DefaultBundleCache</code></a>
     *       documentation for more details its configuration options.
     *   </li>
     *   <li><i><b>Creates and starts a framework instance.</b></i> A
     *       case insensitive
     *       <a href="util/StringMap.html"><code>StringMap</code></a>
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
     * <code>Felix</code> constructor documentation for more information on
     * framework configuration options.
     * </p>
     * @param args An array of arguments, all of which are ignored.
     * @throws Exception If an error occurs.
     **/
    public static void main(String[] args) throws Exception {
        while (true) {
            boolean restart = false;
            boolean restartJvm = false;
            // karaf.restart.jvm take priority over karaf.restart
            System.setProperty("karaf.restart.jvm", "false");
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
                restartJvm = Boolean.getBoolean("karaf.restart.jvm");
                main.updateInstancePidAfterShutdown();
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
                if (restartJvm && restart) {
                    System.exit(10);
                } else if (!restart) {
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
    
    public void updateInstancePidAfterShutdown() throws Exception {
        if (config == null) {
            config = new ConfigProperties();
        }
        InstanceHelper.updateInstancePid(config.karafHome, config.karafBase, false);
    }

    public void launch() throws Exception {
        if (Arrays.asList(args).contains("clean")) {
            // clean instance
            final Path dataDir = new File(System.getProperty(ConfigProperties.PROP_KARAF_DATA)).toPath();
            if (Files.exists(dataDir)) {
                try {
                    Files.walkFileTree(dataDir, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attributes) throws IOException {
                            Files.delete(file);
                            return super.visitFile(file, attributes);
                        }
                        @Override
                        public FileVisitResult postVisitDirectory(final Path dir, final IOException exception) throws IOException {
                            Files.delete(dir);
                            return super.postVisitDirectory(dir, exception);
                        }
                    });
                } catch (final IOException ioException) {
                    LOG.log(Level.WARNING, "Can't delete " + dataDir + " (" + ioException.getMessage() + ")", ioException);
                }
                Files.createDirectories(dataDir.resolve("tmp"));
            }
        }
        if (config == null) {
            config = new ConfigProperties();
        }
        config.performInit();
        if (config.delayConsoleStart) {
            System.out.println(config.startupMessage);
        }
        String log4jConfigPath = System.getProperty("karaf.etc") + "/org.ops4j.pax.logging.cfg";
        BootstrapLogManager.setProperties(config.props, log4jConfigPath);
        /* KARAF-5798: write the PID whether or not the lock has been acquired */
        InstanceHelper.writePid(config.pidFile);
        BootstrapLogManager.configureLogger(LOG);

        for (String provider : config.securityProviders) {
            addSecurityProvider(provider);
        }
        
        List<File> bundleDirs = getBundleRepos();
        ArtifactResolver resolver = new SimpleMavenResolver(bundleDirs);

        // Start up the OSGI framework
        classLoader = createClassLoader(resolver);
        FrameworkFactory factory = loadFrameworkFactory(classLoader);
        framework = factory.newFramework(config.props);

        setLogger();

        framework.init();
        framework.getBundleContext().addFrameworkListener(lockCallback);
        framework.start();

        FrameworkStartLevel sl = framework.adapt(FrameworkStartLevel.class);
        sl.setInitialBundleStartLevel(config.defaultBundleStartlevel);

        // If we have a clean state, install everything
        if (framework.getBundleContext().getBundles().length == 1) {

            LOG.info("Installing and starting initial bundles");
            File startupPropsFile = new File(config.karafEtc, STARTUP_PROPERTIES_FILE_NAME);
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
        monitorThread = monitor();
        registerSignalHandler();
        watchdog();
    }

    /*
     * Hack to set felix logger
     * KARAF-3706: disable the logger related code to avoid the exception
     * It needs to be revisited when the FELIX-4871 is fixed.
     */
    private void setLogger() {
        try {
            if (framework.getClass().getName().startsWith("org.apache.felix.")) {
                Field field = framework.getClass().getDeclaredField("m_logger");
                field.setAccessible(true);
                Object logger = field.get(framework);
                Method method = logger.getClass().getDeclaredMethod("setLogger", Object.class);
                method.setAccessible(true);
                method.invoke(logger, new Object() {
                    public void log(int level, String message, Throwable exception) {
                        Level lvl;
                        switch (level) {
                            case 1:
                                lvl = Level.SEVERE;
                                break;
                            case 2:
                                lvl = Level.WARNING;
                                break;
                            case 3:
                                lvl = Level.INFO;
                                break;
                            case 4:
                                lvl = Level.FINE;
                                break;
                            default:
                                lvl = Level.FINEST;
                                break;
                        }
                        Logger.getLogger("Felix").log(lvl, message, exception);
                    }
                });
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void registerSignalHandler() {
        if (!Boolean.valueOf(System.getProperty("karaf.handle.sigterm", "true"))) {
            return;
        }

        try {
            final Class<?> signalClass = Class.forName("sun.misc.Signal");
            final Class<?> signalHandlerClass = Class.forName("sun.misc.SignalHandler");

            Object signalHandler = Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {
                    signalHandlerClass
                },
                    (proxy, method, args) -> {
                        new Thread(() -> {
                            try {
                                exiting = true;
                                framework.stop();
                            } catch (BundleException e) {
                                e.printStackTrace();
                            }
                        }).start();
                        return null;
                    }
            );

            signalClass.getMethod("handle", signalClass, signalHandlerClass).invoke(
                null,
                signalClass.getConstructor(String.class).newInstance("TERM"),
                signalHandler
            );
        } catch (Exception e) {
        }
    }

    private Thread monitor() {
        Thread th = new Thread("Karaf Lock Monitor Thread") {
            public void run() {
                try {
                    doMonitor();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        th.start();
        return th;
    }

    private void doMonitor() throws Exception {
        lock = createLock();
        File dataDir = new File(System.getProperty(ConfigProperties.PROP_KARAF_DATA));
        int livenessFailureCount = 0;
        boolean locked = false;
        while (!exiting) {
            if (lock.lock()) {
                livenessFailureCount = 0;
                if (!locked) {
                    lockCallback.lockAcquired();
                    locked = true;
                }
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
                    try {
                        Thread.sleep(config.lockDelay);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
                if (!exiting) {
                    livenessFailureCount++;
                    if (livenessFailureCount > config.lockLostThreshold) {
                        locked = false;
                        lockCallback.lockLost();
                    }
                } else {
                    lockCallback.stopShutdownThread();
                }
            } else {
                if (locked) {
                    livenessFailureCount++;
                    if (livenessFailureCount <= config.lockLostThreshold) {
                        lockCallback.waitingForLock();
                    } else {
                        locked = false;
                        lockCallback.lockLost();
                    }
                } else {
                    if (config.lockSlaveBlock) {
                        LOG.log(Level.SEVERE, "Can't lock, and lock is exclusive");
                        System.err.println("Can't lock (another instance is running), and lock is exclusive");
                        System.exit(5);
                    } else {
                        lockCallback.waitingForLock();
                    }
                }
            }
            try {
                Thread.sleep(config.lockDelay);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }

    Lock getLock() {
        return lock;
    }

    private void watchdog() {
        if(Boolean.getBoolean("karaf.systemd.enabled")) {
            new Thread("Karaf Systemd Watchdog Thread") {
                public void run() {
                    try {
                        doWatchdog();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    private void doWatchdog() throws Exception {
        Systemd systemd = new Systemd();
        long timeout = systemd.getWatchdogTimeout(TimeUnit.MILLISECONDS);

        int code;
        while (!exiting && timeout > 0) {
            code = systemd.notifyWatchdog();
            if(code < 0) {
                System.err.println("Systemd sd_notify failed with error code: " + code);
                break;
            }

            Thread.sleep(timeout / 2);
        }
    }

    private URLClassLoader createClassLoader(ArtifactResolver resolver) throws Exception {
        List<URL> urls = new ArrayList<>();
        urls.add(resolver.resolve(config.frameworkBundle).toURL());
        File[] libs = new File(config.karafHome, "lib").listFiles();
        if (libs != null) {
            for (File f : libs) {
                if (f.isFile() && f.canRead() && f.getName().endsWith(".jar")) {
                    urls.add(f.toURI().toURL());
                }
            }
        }
        return new URLClassLoader(urls.toArray(new URL[0]), getParentClassLoader());
    }

    protected ClassLoader getParentClassLoader() {
        return Main.class.getClassLoader();
    }

    private FrameworkFactory loadFrameworkFactory(ClassLoader classLoader) throws Exception {
        String factoryClass = config.frameworkFactoryClass;
        if (factoryClass == null) {
            InputStream is = classLoader.getResourceAsStream("META-INF/services/" + FrameworkFactory.class.getName());
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
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
        List<BundleInfo> bundeList = new ArrayList<>();
        for (String key : startupProps.keySet()) {
            try {
                BundleInfo bi = new BundleInfo();
                bi.uri = new URI(key);
                String startlevelSt = startupProps.getProperty(key).trim();
                bi.startLevel = Integer.valueOf(startlevelSt);
                bundeList.add(bi);
            } catch (Exception e) {
                throw new RuntimeException("Error loading startup bundle list from " + startupPropsFile + " at " + key, e);
            }
        }
        return bundeList; 
    }

    private void installAndStartBundles(ArtifactResolver resolver, BundleContext context, List<BundleInfo> bundles) {
        final URI home = !bundles.isEmpty() ? config.karafHome.toURI() : null;
        final URI base = !bundles.isEmpty() ? config.karafBase.toURI() : null;
        for (BundleInfo bundleInfo : bundles) {
            try {
                Bundle b;
                if (bundleInfo.uri.toString().startsWith("reference:file:")) {
                    URI temp = URI.create(bundleInfo.uri.toString().substring("reference:file:".length()));
                    URI resolvedURI = resolver.resolve(temp);
                    final String asciiString = resolvedURI.toASCIIString();
                    if (asciiString.startsWith(home.toASCIIString()) ||
                        asciiString.startsWith(base.toASCIIString())) {
                        b = context.installBundle(URI.create("reference:" + asciiString).toString());
                    } else {
                        throw new IllegalArgumentException("Can't resolve bundle '" + bundleInfo.uri + "'");
                    }
                } else {
                    URI resolvedURI = resolver.resolve(bundleInfo.uri);
                    b = context.installBundle(bundleInfo.uri.toString(), resolvedURI.toURL().openStream());
                }
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
        String fragmentHostHeader = b.getHeaders().get(Constants.FRAGMENT_HOST);
        return fragmentHostHeader == null || fragmentHostHeader.trim().length() == 0;
    }

    private List<File> getBundleRepos() {
        List<File> bundleDirs = new ArrayList<>();
        File homeSystemRepo = new File(config.karafHome, config.defaultRepo);
        if (!homeSystemRepo.isDirectory()) {
            throw new RuntimeException("system repo folder not found: " + homeSystemRepo.getAbsolutePath());
        }
        bundleDirs.add(homeSystemRepo);

        File baseSystemRepo = new File(config.karafBase, config.defaultRepo);
        if (baseSystemRepo.isDirectory() && !baseSystemRepo.equals(homeSystemRepo)) {
            bundleDirs.add(baseSystemRepo);
        }

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

    public ConfigProperties getConfig() {
        return config;
    }

    public void setConfig(ConfigProperties config) {
        this.config = config;
    }

    public void awaitShutdown() throws Exception {
        if (framework == null) {
            return;
        }
        while (true) {
            FrameworkEvent event = framework.waitForStop(0);
            if (event.getType() == FrameworkEvent.STOPPED_UPDATE) {
                if (lock != null) {
                    lock.release();
                }
                while (framework.getState() != Bundle.STARTING && framework.getState() != Bundle.ACTIVE) {
                    Thread.sleep(10);
                }
                monitorThread = monitor();
            } else {
                return;
            }
        }
    }

    public boolean destroy() throws Exception {
        if (framework == null) {
            return true;
        }
        try {
            int timeout = config.shutdownTimeout;
            if (config.shutdownTimeout <= 0) {
                timeout = Integer.MAX_VALUE;
            }
            
            if (shutdownCallback != null) {
                shutdownCallback.waitingForShutdown(timeout);
            }

            exiting = true;

            if (framework.getState() == Bundle.ACTIVE || framework.getState() == Bundle.STARTING) {
                new Thread(() -> {
                    try {
                        framework.stop();
                    } catch (BundleException e) {
                        System.err.println("Error stopping karaf: " + e.getMessage());
                    }
                }).start();
            }

            int step = 5000;      
            while (timeout > 0) {
                timeout -= step;
                FrameworkEvent event = framework.waitForStop(step);
                if (event.getType() != FrameworkEvent.WAIT_TIMEDOUT) {
                    if (activatorManager != null) {
                        activatorManager.stopKarafActivators();
                    }
                    return true;
                }
            }

            return false;
        } finally {
            if (lock != null) {
                exiting = true;
                if (monitorThread != null) {
                    try {
                        monitorThread.interrupt();
                        monitorThread.join();
                    } finally {
                        monitorThread = null;
                    }
                }
                lock.release();
            }
            if (classLoader != null) {
                classLoader.close();
            }
        }
    }
    
    private final class KarafLockCallback implements LockCallBack, FrameworkListener {
        private Object startLevelLock = new Object();

        @Override
        public void lockLost() {
            stopShutdownThread();
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

        public void stopShutdownThread() {
            if (shutdownThread != null) {
                try {
                    shutdownThread.close();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    shutdownThread = null;
                }
            }
        }

        @Override
        public void lockAcquired() {
            LOG.info("Lock acquired. Setting startlevel to " + config.defaultStartLevel);
            /* KARAF-5798: instance PID should reflect the current running master */
            InstanceHelper.updateInstancePid(config.karafHome, config.karafBase, true);
            shutdownThread = InstanceHelper.setupShutdown(config, framework);
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
