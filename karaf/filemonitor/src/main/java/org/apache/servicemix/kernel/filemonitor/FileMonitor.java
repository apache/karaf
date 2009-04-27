/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.kernel.filemonitor;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Watches a deploy directory for files that are added, updated or removed then
 * processing them. Currently we support OSGi bundles, OSGi configuration files
 * and expanded directories of OSGi bundles.
 * 
 * @version $Revision: 1.1 $
 */
public class FileMonitor {

    public final static String CONFIG_DIR = "org.apache.servicemix.filemonitor.configDir";
    public final static String DEPLOY_DIR = "org.apache.servicemix.filemonitor.monitorDir";
    public final static String GENERATED_JAR_DIR = "org.apache.servicemix.filemonitor.generatedJarDir";
    public final static String SCAN_INTERVAL = "org.apache.servicemix.filemonitor.scanInterval";
    public final static String PREFERENCE_KEY = "FileMonitor";

    protected static final String ALIAS_KEY = "_alias_factory_pid";

    private static final Log LOGGER = LogFactory.getLog(FileMonitor.class);

    private FileMonitorActivator activator;
    private File configDir;
    private File deployDir;
    private File generateDir;
    private Scanner scanner = new Scanner();
    private long scanInterval = 500L;
    private boolean loggedConfigAdminWarning;
    private List<Bundle> bundlesToStart = new ArrayList<Bundle>();
    private List<Bundle> bundlesToUpdate = new ArrayList<Bundle>();
    private Map<String, String> artifactToBundle = new HashMap<String, String>();
    private final Set<String> pendingTransformationArtifacts = new HashSet<String>();
    private final Set<Bundle> pendingStartBundles = new HashSet<Bundle>();
    private ServiceListener deployerListener;
    private BundleListener bundleListener;
    private Executor executor;
    private String pid;

    public FileMonitor() {
        String base = System.getProperty("servicemix.base", ".");
        configDir = new File(base, "etc");
        deployDir = new File(base, "deploy");
        generateDir = new File(base, "data/generated-bundles");
    }

    @SuppressWarnings("unchecked")
    public FileMonitor(FileMonitorActivator activator, Dictionary properties, String pid) {
        this();

        this.activator = activator;
        this.pid = pid;

        File value = getFileValue(properties, CONFIG_DIR);
        if (value != null) {
            configDir = value;
        }
        value = getFileValue(properties, DEPLOY_DIR);
        if (value != null) {
            deployDir = value;
        }
        value = getFileValue(properties, GENERATED_JAR_DIR);
        if (value != null) {
            generateDir = value;
        }
        Long i = getLongValue(properties, SCAN_INTERVAL);
        if (i != null) {
            scanInterval = i;
        }
    }

    public void start() {
        executor = Executors.newSingleThreadExecutor();
        if (configDir != null) {
            configDir.mkdirs();
        }
        deployDir.mkdirs();
        generateDir.mkdirs();

        List<File> dirs = new ArrayList<File>();
        if (configDir != null) {
            dirs.add(configDir);
        }
        dirs.add(deployDir);
        scanner.setScanDirs(dirs);
        scanner.setScanInterval(scanInterval);
        // load the scan results for this monitor
        loadScannerState();
        scanner.addListener(new Scanner.BulkListener() {
            public void filesChanged(List<String> filenames) throws Exception {
                onFilesChanged(filenames);
            }
        });

        LOGGER.info("Starting to monitor the deploy directory: " + deployDir + " every " + scanInterval
                    + " millis");
        if (configDir != null) {
            LOGGER.info("Config directory is at: " + configDir);
        }
        LOGGER.info("Will generate bundles from expanded source directories to: " + generateDir);

        executor.execute(new Runnable() {
            public void run() {
                scanner.start();
            }
        });
    }

    public void stop() {
        // first stop the scanner
        scanner.stop();
        // load the scan results for this monitor
        saveScannerState();
    }

    // Properties
    // -------------------------------------------------------------------------

    public BundleContext getContext() {
        return activator.getContext();
    }

    public FileMonitorActivator getActivator() {
        return activator;
    }

    public void setActivator(FileMonitorActivator activator) {
        this.activator = activator;
    }

    public File getConfigDir() {
        return configDir;
    }

    public void setConfigDir(File configDir) {
        this.configDir = configDir;
    }

    public File getDeployDir() {
        return deployDir;
    }

    public void setDeployDir(File deployDir) {
        this.deployDir = deployDir;
    }

    public File getGenerateDir() {
        return generateDir;
    }

    public void setGenerateDir(File generateDir) {
        this.generateDir = generateDir;
    }

    public long getScanInterval() {
        return scanInterval;
    }

    public void setScanInterval(long scanInterval) {
        this.scanInterval = scanInterval;
    }

    // Implementation methods
    // -------------------------------------------------------------------------

    protected synchronized void onFilesChanged(Collection<String> filenames) {
        bundlesToStart.clear();
        bundlesToUpdate.clear();
        Set<File> bundleJarsCreated = new HashSet<File>();

        for (String filename : filenames) {
            File file = new File(filename);
            try {
                LOGGER.debug("File changed: " + filename);

                // Handle config files
                if (isValidConfigFile(file)) {
                    if (file.exists()) {
                        updateConfiguration(file);
                    } else {
                        deleteConfiguration(file);
                    }
                    continue;
                }

                // Handle exploded artifacts removal
                if (!file.exists() && file.getName().equals("MANIFEST.MF")) {
                    File parentFile = file.getParentFile();
                    if (parentFile.getName().equals("META-INF")) {
                        File bundleDir = parentFile.getParentFile();
                        if (isValidBundleSourceDirectory(bundleDir)) {
                            undeployBundle(bundleDir);
                            continue;
                        }
                    }
                }

                // Handle exploded artifacts add
                File jardir = getExpandedBundleRootDirectory(file);
                if (jardir != null) {
                    if (bundleJarsCreated.contains(jardir)) {
                        continue;
                    }
                    bundleJarsCreated.add(jardir);
                    file = createBundleJar(jardir);
                }

                // Transformation step
                if (file.exists()) {
                    File f = transformArtifact(file);
                    if (f == null) {
                        LOGGER.warn("Unsupported deployment: " + filename);
                        rescheduleTransformation(file);
                        continue;
                    }
                    file = f;
                } else {
                    String transformedFile = artifactToBundle.get(filename);
                    if (transformedFile != null) {
                        file = new File(transformedFile);
                        if (file.exists()) {
                            file.delete();
                        }
                    }
                }

                // Handle final bundles
                if (isValidArtifactFile(file)) {
                    if (file.exists()) {
                        deployBundle(file);
                    } else {
                        undeployBundle(file);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to process: " + file + ". Reason: " + e, e);
            }
        }
        refreshPackagesAndStartOrUpdateBundles();
    }

    private void rescheduleTransformation(File file) {
        synchronized (pendingTransformationArtifacts) {
            pendingTransformationArtifacts.add(file.getAbsolutePath());
        }
        if (deployerListener == null) {
            try {
                String filter = "(" + Constants.OBJECTCLASS + "=" + DeploymentListener.class.getName() + ")";
                deployerListener = new ServiceListener() {
                    public void serviceChanged(ServiceEvent event) {
                        executor.execute(new Runnable() {
                            public void run() {
                                Set<String> files;
                                synchronized (pendingTransformationArtifacts) {
                                    files = new HashSet<String>(pendingTransformationArtifacts);
                                    pendingTransformationArtifacts.clear();
                                }
                                onFilesChanged(files);
                            }
                        });
                    }
                };
                getContext().addServiceListener(deployerListener, filter);
            } catch (InvalidSyntaxException e) {
                // Ignore
            }
        }
    }

    private File transformArtifact(File file) throws Exception {
        // Check registered deployers
        ServiceReference[] srvRefs = getContext().getAllServiceReferences(DeploymentListener.class.getName(),
                                                                          null);
        if (srvRefs != null) {
            for (ServiceReference sr : srvRefs) {
                try {
                    DeploymentListener deploymentListener = (DeploymentListener)getContext().getService(sr);
                    if (deploymentListener.canHandle(file)) {
                        File transformedFile = deploymentListener.handle(file, getGenerateDir());
                        artifactToBundle.put(file.getAbsolutePath(), transformedFile.getAbsolutePath());
                        return transformedFile;
                    }
                } finally {
                    getContext().ungetService(sr);
                }
            }
        }
        JarFile jar = null;
        try {
            // Handle OSGi bundles with the default deployer
            if (file.getName().endsWith("txt") || file.getName().endsWith("xml")
                || file.getName().endsWith("properties")) {
                // that's file type which is not supported as bundle and avoid
                // exception in the log
                return null;
            }
            jar = new JarFile(file);
            Manifest m = jar.getManifest();
            if (m.getMainAttributes().getValue(new Attributes.Name("Bundle-SymbolicName")) != null
                && m.getMainAttributes().getValue(new Attributes.Name("Bundle-Version")) != null) {
                return file;
            }
        } catch (Exception e) {
            LOGGER.debug("Error transforming artifact " + file.getName(), e);
        } finally {
            if (jar != null) {
                jar.close();
            }
        }
        return null;
    }

    protected void deployBundle(File file) throws IOException, BundleException {
        LOGGER.info("Deploying: " + file.getCanonicalPath());

        InputStream in = new FileInputStream(file);

        try {
            Bundle bundle = getBundleForJarFile(file);
            if (bundle != null) {
                bundlesToUpdate.add(bundle);
            } else {
                bundle = getContext().installBundle(file.getCanonicalFile().toURI().toString(), in);
                if (!isBundleFragment(bundle)) {
                    bundlesToStart.add(bundle);
                }
            }
        } finally {
            closeQuietly(in);
        }
    }

    protected void undeployBundle(File file) throws BundleException, IOException {
        LOGGER.info("Undeploying: " + file.getCanonicalPath());
        Bundle bundle = getBundleForJarFile(file);

        if (bundle == null) {
            LOGGER.warn("Could not find Bundle for file: " + file.getCanonicalPath());
        } else {
            if (!isBundleFragment(bundle)) {
                bundle.stop();
            }
            bundle.uninstall();
        }
    }

    protected Bundle getBundleForJarFile(File file) throws IOException {
        String absoluteFilePath = file.getAbsoluteFile().toURI().toString();
        Bundle bundles[] = getContext().getBundles();
        for (Bundle bundle : bundles) {
            String location = bundle.getLocation();
            if (filePathsMatch(absoluteFilePath, location)) {
                return bundle;
            }
        }
        return null;
    }

    protected static boolean filePathsMatch(String p1, String p2) {
        p1 = normalizeFilePath(p1);
        p2 = normalizeFilePath(p2);
        return (p1 != null && p1.equalsIgnoreCase(p2));
    }

    protected static String normalizeFilePath(String path) {
        if (path != null) {
            path = path.replaceFirst("file:/*", "");
            path = path.replaceAll("[\\\\/]+", "/");
        }
        return path;
    }

    protected void updateConfiguration(File file) throws IOException, InvalidSyntaxException {
        ConfigurationAdmin configurationAdmin = activator.getConfigurationAdmin();
        if (configurationAdmin == null) {
            if (!loggedConfigAdminWarning) {
                LOGGER.warn("No ConfigurationAdmin so cannot deploy configurations");
                loggedConfigAdminWarning = true;
            }
        } else {
            Properties properties = new Properties();
            InputStream in = new FileInputStream(file);
            try {
                properties.load(in);
                interpolation(properties);
                closeQuietly(in);
                String[] pid = parsePid(file);
                Hashtable<Object, Object> hashtable = new Hashtable<Object, Object>();
                hashtable.putAll(properties);
                if (pid[1] != null) {
                    hashtable.put(ALIAS_KEY, pid[1]);
                }

                Configuration config = getConfiguration(pid[0], pid[1]);
                if (config.getBundleLocation() != null) {
                    config.setBundleLocation(null);
                }
                config.update(hashtable);
            } finally {
                closeQuietly(in);
            }
        }
    }

    protected void interpolation(Properties properties) {
        for (Enumeration e = properties.propertyNames(); e.hasMoreElements();) {
            String key = (String)e.nextElement();
            String val = properties.getProperty(key);
            Matcher matcher = Pattern.compile("\\$\\{([^}]+)\\}").matcher(val);
            while (matcher.find()) {
                String rep = System.getProperty(matcher.group(1));
                if (rep != null) {
                    val = val.replace(matcher.group(0), rep);
                    matcher.reset(val);
                }
            }
            properties.put(key, val);
        }
    }

    protected void deleteConfiguration(File file) throws IOException, InvalidSyntaxException {
        String[] pid = parsePid(file);
        Configuration config = getConfiguration(pid[0], pid[1]);
        config.delete();
    }

    protected Configuration getConfiguration(String pid, String factoryPid) throws IOException,
        InvalidSyntaxException {
        ConfigurationAdmin configurationAdmin = activator.getConfigurationAdmin();
        if (factoryPid != null) {
            Configuration[] configs = configurationAdmin.listConfigurations("(|(" + ALIAS_KEY + "=" + pid
                                                                            + ")(.alias_factory_pid="
                                                                            + factoryPid + "))");
            if (configs == null || configs.length == 0) {
                return configurationAdmin.createFactoryConfiguration(pid, null);
            } else {
                return configs[0];
            }
        } else {
            return configurationAdmin.getConfiguration(pid, null);
        }
    }

    protected String[] parsePid(File file) {
        String path = file.getName();
        String pid = path.substring(0, path.length() - 4);
        int n = pid.indexOf('-');
        if (n > 0) {
            String factoryPid = pid.substring(n + 1);
            pid = pid.substring(0, n);
            return new String[] {pid, factoryPid};
        } else {
            return new String[] {pid, null};
        }
    }

    protected PackageAdmin getPackageAdmin() {
        ServiceTracker packageAdminTracker = activator.getPackageAdminTracker();
        if (packageAdminTracker != null) {
            try {
                return (PackageAdmin)packageAdminTracker.waitForService(5000L);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        return null;
    }

    protected boolean isBundleFragment(Bundle bundle) {
        PackageAdmin packageAdmin = getPackageAdmin();
        if (packageAdmin != null) {
            return packageAdmin.getBundleType(bundle) == PackageAdmin.BUNDLE_TYPE_FRAGMENT;
        }
        return false;
    }

    protected void refreshPackagesAndStartOrUpdateBundles() {
        for (Bundle bundle : bundlesToUpdate) {
            try {
                bundle.update();
                LOGGER.info("Updated: " + bundle);
            } catch (BundleException e) {
                LOGGER.warn("Failed to update bundle: " + bundle + ". Reason: " + e, e);
            }
        }

        for (Bundle bundle : bundlesToStart) {
            try {
                bundle.start();
                LOGGER.info("Started: " + bundle);
            } catch (BundleException e) {
                LOGGER.warn("Failed to start bundle: " + bundle + ". Reason: " + e, e);
                rescheduleStart(bundle);
            }
        }

        PackageAdmin packageAdmin = getPackageAdmin();
        if (packageAdmin != null) {
            packageAdmin.refreshPackages(null);
        }
    }

    private void rescheduleStart(Bundle bundle) {
        synchronized (pendingStartBundles) {
            pendingStartBundles.add(bundle);
            if (bundleListener == null) {
                bundleListener = new BundleListener() {
                    public void bundleChanged(BundleEvent event) {
                        if (event.getType() == BundleEvent.RESOLVED) {
                            executor.execute(new Runnable() {
                                public void run() {
                                    retryPendingStartBundles();
                                }
                            });
                        } else if (event.getType() == BundleEvent.UNINSTALLED) {
                        	// bundle was uninstalled meanwhile, so remove
                        	// it from the list of pending bundles
                        	pendingStartBundles.remove(event.getBundle());
                        }
                    }
                };
                getContext().addBundleListener(bundleListener);
            }
        }
    }

    protected void retryPendingStartBundles() {
        synchronized (pendingStartBundles) {
            Set<Bundle> bundles = new HashSet<Bundle>();
            bundles.addAll(pendingStartBundles);
            pendingStartBundles.clear();
            boolean success = false;
            for (Bundle bundle : bundles) {
                try {
                    bundle.start();
                    LOGGER.info("Pending bundle started: " + bundle);
                    success = true;
                } catch (BundleException e) {
                    LOGGER.info("Pending bundle not started: " + bundle);
                    pendingStartBundles.add(bundle);
                }
            }
            if (success) {
                PackageAdmin packageAdmin = getPackageAdmin();
                if (packageAdmin != null) {
                    packageAdmin.refreshPackages(null);
                }
            }
        }
    }

    protected File createBundleJar(File dir) throws BundleException, IOException {
        File destFile = new File(generateDir, dir.getName() + ".jar");
        if (destFile.exists()) {
            undeployBundle(destFile);
            destFile.delete();
        }
        JarUtil.jarDir(dir.getPath(), destFile.getPath());
        return destFile;
    }

    /**
     * Returns the root directory of the expanded OSGi bundle if the file is
     * part of an expanded OSGi bundle or null if it is not
     */
    protected File getExpandedBundleRootDirectory(File file) throws IOException {
        File parent = file.getParentFile();
        if (file.isDirectory()) {
            String rootPath = deployDir.getCanonicalPath();
            if (file.getCanonicalPath().equals(rootPath)) {
                return null;
            }
            if (containsManifest(file)) {
                return file;
            }
        }
        if (isValidBundleSourceDirectory(parent)) {
            return getExpandedBundleRootDirectory(parent);
        }
        return null;
    }

    /**
     * Returns true if the given directory is a valid child directory within the
     * {@link #deployDir}
     */
    protected boolean isValidBundleSourceDirectory(File dir) throws IOException {
        if (dir != null) {
            String parentPath = dir.getCanonicalPath();
            String rootPath = deployDir.getCanonicalPath();
            return !parentPath.equals(rootPath) && parentPath.startsWith(rootPath);
        } else {
            return false;
        }
    }

    /**
     * Returns true if the given directory is a valid child file within the
     * {@link #deployDir} or a child of {@link #generateDir}
     */
    protected boolean isValidArtifactFile(File file) throws IOException {
        if (file != null) {
            String filePath = file.getParentFile().getCanonicalPath();
            String deployPath = deployDir.getCanonicalPath();
            String generatePath = generateDir.getCanonicalPath();
            return filePath.equals(deployPath) || filePath.startsWith(generatePath);
        } else {
            return false;
        }
    }

    /**
     * Returns true if the given directory is a valid child file within the
     * {@link #configDir}
     */
    protected boolean isValidConfigFile(File file) throws IOException {
        if (file != null && file.getName().endsWith(".cfg")) {
            String filePath = file.getParentFile().getCanonicalPath();
            String configPath = configDir.getCanonicalPath();
            return filePath.equals(configPath);
        } else {
            return false;
        }
    }

    /**
     * Returns true if the given directory contains a valid manifest file
     */
    protected boolean containsManifest(File dir) {
        File metaInfDir = new File(dir, "META-INF");
        if (metaInfDir.exists() && metaInfDir.isDirectory()) {
            File manifest = new File(metaInfDir, "MANIFEST.MF");
            return manifest.exists() && !manifest.isDirectory();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    protected File getFileValue(Dictionary properties, String key) {
        Object value = properties.get(key);
        if (value instanceof File) {
            return (File)value;
        } else if (value != null) {
            return new File(value.toString());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected Long getLongValue(Dictionary properties, String key) {
        Object value = properties.get(key);
        if (value instanceof Long) {
            return (Long)value;
        } else if (value != null) {
            return Long.parseLong(value.toString());
        }
        return null;
    }

    protected void closeQuietly(Closeable in) {
        try {
            in.close();
        } catch (IOException e) {
            LOGGER.warn("Failed to close stream. " + e, e);
        }
    }

    protected PreferencesService getPreferenceService() {
        if (activator.getPreferenceServiceTracker() != null) {
            try {
                return (PreferencesService)activator.getPreferenceServiceTracker().waitForService(5000L);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        return null;
    }

    protected void saveScannerState() {
        try {
            Map<String, Long> results = scanner.getLastScanResults();
            Preferences prefs = getPreferenceService().getUserPreferences(PREFERENCE_KEY).node(pid);
            Iterator<String> it = results.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                Long value = results.get(key);
                prefs.putLong(key, value);
            }
            prefs.flush();
        } catch (Exception e) {
            LOGGER.error("Error persisting FileMonitor state", e);
        }
    }

    protected void loadScannerState() {
        try {
            Preferences prefs = getPreferenceService().getUserPreferences(PREFERENCE_KEY).node(pid);
            Map<String, Long> lastResult = new HashMap<String, Long>();
            for (String key : prefs.keys()) {
                lastResult.put(key, prefs.getLong(key, -1));
            }
            scanner.setLastScanResults(lastResult);
        } catch (Exception e) {
            LOGGER.error("Error loading FileMonitor state", e);
        }
    }
}
