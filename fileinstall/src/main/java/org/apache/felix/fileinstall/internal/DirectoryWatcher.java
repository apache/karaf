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
package org.apache.felix.fileinstall.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.apache.felix.fileinstall.ArtifactListener;
import org.apache.felix.fileinstall.ArtifactTransformer;
import org.apache.felix.fileinstall.ArtifactUrlTransformer;
import org.apache.felix.fileinstall.internal.Util.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * -DirectoryWatcher-
 *
 * This class runs a background task that checks a directory for new files or
 * removed files. These files can be configuration files or jars.
 * For jar files, its behavior is defined below:
 * - If there are new jar files, it installs them and optionally starts them.
 *    - If it fails to install a jar, it does not try to install it again until
 *      the jar has been modified.
 *    - If it fail to start a bundle, it attempts to start it in following
 *      iterations until it succeeds or the corresponding jar is uninstalled.
 * - If some jar files have been deleted, it uninstalls them.
 * - If some jar files have been updated, it updates them.
 *    - If it fails to update a bundle, it tries to update it in following
 *      iterations until it is successful.
 * - If any bundle gets updated or uninstalled, it refreshes the framework
 *   for the changes to take effect.
 * - If it detects any new installations, uninstallations or updations,
 *   it tries to start all the managed bundle unless it has been configured
 *   to only install bundles.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DirectoryWatcher extends Thread implements BundleListener
{
	public final static String FILENAME = "felix.fileinstall.filename";
    public final static String POLL = "felix.fileinstall.poll";
    public final static String DIR = "felix.fileinstall.dir";
    public final static String LOG_LEVEL = "felix.fileinstall.log.level";
    public final static String TMPDIR = "felix.fileinstall.tmpdir";
    public final static String FILTER = "felix.fileinstall.filter";
    public final static String START_NEW_BUNDLES = "felix.fileinstall.bundles.new.start";
    public final static String NO_INITIAL_DELAY = "felix.fileinstall.noInitialDelay";

    static final SecureRandom random = new SecureRandom();

    static final File javaIoTmpdir = new File(System.getProperty("java.io.tmpdir"));

    Dictionary properties;
    File watchedDirectory;
    File tmpDir;
    long poll;
    int logLevel;
    boolean startBundles;
    String filter;
    BundleContext context;
    String originatingFileName;
    boolean noInitialDelay;

    // Map of all installed artifacts
    Map/* <File, Artifact> */ currentManagedArtifacts = new HashMap/* <File, Artifact> */();

    // The scanner to report files changes
    Scanner scanner;

    // Represents files that could not be processed because of a missing artifact listener
    Set/* <File> */ processingFailures = new HashSet/* <File> */();

    // Represents artifacts that could not be installed
    Map/* <File, Artifact> */ installationFailures = new HashMap/* <File, Artifact> */();

    public DirectoryWatcher(Dictionary properties, BundleContext context)
    {
        super("fileinstall-" + getThreadName(properties));
        this.properties = properties;
        this.context = context;
        poll = getLong(properties, POLL, 2000);
        logLevel = getInt(properties, LOG_LEVEL, 0);
        originatingFileName = (String) properties.get(FILENAME);
        watchedDirectory = getFile(properties, DIR, new File("./load"));
        prepareDir(watchedDirectory);
        tmpDir = getFile(properties, TMPDIR, null);
        prepareTempDir();
        startBundles = getBoolean(properties, START_NEW_BUNDLES, true);  // by default, we start bundles.
        filter = (String) properties.get(FILTER);
        noInitialDelay = getBoolean(properties, NO_INITIAL_DELAY, false);
        this.context.addBundleListener(this);

        FilenameFilter flt;
        if (filter != null && filter.length() > 0)
        {
            flt = new FilenameFilter()
            {
                public boolean accept(File dir, String name) {
                    return name.matches(filter);
                }
            };
        }
        else
        {
            flt = null;
        }
        scanner = new Scanner(watchedDirectory, flt);
    }

    public static String getThreadName(Dictionary properties) {
        return (properties.get(DIR) != null ? properties.get(DIR) : "./load").toString();
    }

    public Dictionary getProperties() {
        return properties;
    }

    public void start() {
        if (noInitialDelay)
        {
            log(Logger.LOG_DEBUG, "Starting initial scan", null);
            initializeCurrentManagedBundles();
            Set/*<File>*/ files = scanner.scan(true);
            if (files != null)
            {
                process(files);
            }
        }
        super.start();
    }

    /**
     * Main run loop, will traverse the directory, and then handle the delta
     * between installed and newly found/lost bundles and configurations.
     *
     */
    public void run()
    {
        log(Logger.LOG_DEBUG,
            "{" + POLL + " (ms) = " + poll + ", "
                + DIR + " = " + watchedDirectory.getAbsolutePath() + ", "
                + LOG_LEVEL + " = " + logLevel + ", "
                + START_NEW_BUNDLES + " = " + startBundles + ", "
                + TMPDIR + " = " + tmpDir + ", "
                + FILTER + " = " + filter + "}", null);

        if (!noInitialDelay)
        {
            initializeCurrentManagedBundles();
        }

        while (!interrupted())
        {
            try
            {
                Set/*<File>*/ files = scanner.scan(false);
                // Check that there is a result.  If not, this means that the directory can not be listed,
                // so it's presumably not a valid directory (it may have been deleted by someone).
                // In such case, just sleep
                if (files == null)
                {
                    synchronized (this)
                    {
                        wait(poll);
                    }
                    continue;
                }

                process(files);

                synchronized (this)
                {
                    wait(poll);
                }
            }
            catch (InterruptedException e)
            {
                return;
            }
            catch (Throwable e)
            {
                try
                {
                    context.getBundle();
                }
                catch (IllegalStateException t)
                {
                    // FileInstall bundle has been uninstalled, exiting loop
                    return;
                }
                log(Logger.LOG_ERROR, "In main loop, we have serious trouble", e);
            }
        }
    }

    public void bundleChanged(BundleEvent bundleEvent)
    {
        if (bundleEvent.getType() == BundleEvent.UNINSTALLED)
        {
            for (Iterator it = currentManagedArtifacts.entrySet().iterator(); it.hasNext();)
            {
                Map.Entry entry = (Map.Entry) it.next();
                Artifact artifact = (Artifact) entry.getValue();
                if (artifact.getBundleId() == bundleEvent.getBundle().getBundleId())
                {
                    log(Logger.LOG_DEBUG,
                        "Bundle " + bundleEvent.getBundle().getBundleId()
                            + " has been uninstalled", null);
                    currentManagedArtifacts.remove(entry.getKey());
                    break;
                }
            }
        }
    }

    private void process(Set files)
    {
        List/*<ArtifactListener>*/ listeners = FileInstall.getListeners();
        List/*<Artifact>*/ deleted = new ArrayList/*<Artifact>*/();
        List/*<Artifact>*/ modified = new ArrayList/*<Artifact>*/();
        List/*<Artifact>*/ created = new ArrayList/*<Artifact>*/();

        // Try to process again files that could not be processed
        files.addAll(processingFailures);
        processingFailures.clear();

        for (Iterator it = files.iterator(); it.hasNext();)
        {
            File file = (File) it.next();
            boolean exists = file.exists();
            Artifact artifact = (Artifact) currentManagedArtifacts.get(file);
            // File has been deleted
            if (!exists)
            {
                if (artifact != null) {
                    deleteJaredDirectory(artifact);
                    deleteTransformedFile(artifact);
                    deleted.add(artifact);
                }
            }
            // File exists
            else
            {
                File jar  = file;
                URL jaredUrl = null;
                try
                {
                    jaredUrl = file.toURI().toURL();
                }
                catch (MalformedURLException e)
                {
                    // Ignore, can't happen
                }
                // Jar up the directory if needed
                if (file.isDirectory())
                {
                    prepareTempDir();
                    try
                    {
                        jar = new File(tmpDir, file.getName() + ".jar");
                        Util.jarDir(file, jar);
                        jaredUrl = new URL(JarDirUrlHandler.PROTOCOL, null, file.getPath());

                    }
                    catch (IOException e)
                    {
                        log(Logger.LOG_WARNING,
                            "Unable to create jar for: " + file.getAbsolutePath(), e);
                        continue;
                    }
                }
                // File has been modified
                if (artifact != null)
                {
                    artifact.setChecksum(scanner.getChecksum(file));
                    // If there's no listener, this is because this artifact has been installed before
                    // fileinstall has been restarted.  In this case, try to find a listener.
                    if (artifact.getListener() == null)
                    {
                        ArtifactListener listener = findListener(jar, listeners);
                        // If no listener can handle this artifact, we need to defer the
                        // processing for this artifact until one is found
                        if (listener == null)
                        {
                            processingFailures.add(file);
                            continue;
                        }
                        artifact.setListener(listener);
                    }
                    // If the listener can not handle this file anymore,
                    // uninstall the artifact and try as if is was new
                    if (!listeners.contains(artifact.getListener()) || !artifact.getListener().canHandle(jar))
                    {
                        deleted.add(artifact);
                        artifact = null;
                    }
                    // The listener is still ok
                    else
                    {
                        deleteTransformedFile(artifact);
                        artifact.setJaredDirectory(jar);
                        artifact.setJaredUrl(jaredUrl);
                        if (transformArtifact(artifact))
                        {
                            modified.add(artifact);
                        }
                        else
                        {
                            deleteJaredDirectory(artifact);
                            deleted.add(artifact);
                        }
                        continue;
                    }
                }
                // File has been added
                else
                {
                    // Find the listener
                    ArtifactListener listener = findListener(jar, listeners);
                    // If no listener can handle this artifact, we need to defer the
                    // processing for this artifact until one is found
                    if (listener == null)
                    {
                        processingFailures.add(file);
                        continue;
                    }
                    // Create the artifact
                    artifact = new Artifact();
                    artifact.setPath(file);
                    artifact.setJaredDirectory(jar);
                    artifact.setJaredUrl(jaredUrl);
                    artifact.setListener(listener);
                    artifact.setChecksum(scanner.getChecksum(file));
                    if (transformArtifact(artifact))
                    {
                        created.add(artifact);
                    }
                    else
                    {
                        deleteJaredDirectory(artifact);
                    }
                }
            }
        }
        // Handle deleted artifacts
        // We do the operations in the following order:
        // uninstall, update, install, refresh & start.
        Collection uninstalledBundles = uninstall(deleted);
        Collection updatedBundles = update(modified);
        Collection installedBundles = install(created);
        if (uninstalledBundles.size() > 0 || updatedBundles.size() > 0)
        {
            // Refresh if any bundle got uninstalled or updated.
            refresh();
        }

        if (startBundles)
        {
            // Try to start all the bundles that are not persistently stopped
            startAllBundles();
            // Try to start newly installed bundles
            start(installedBundles);
        }
    }

    ArtifactListener findListener(File artifact, List/* <ArtifactListener> */ listeners)
    {
        for (Iterator itL = listeners.iterator(); itL.hasNext();)
        {
            ArtifactListener listener = (ArtifactListener) itL.next();
            if (listener.canHandle(artifact))
            {
                return listener;
            }
        }
        return null;
    }

    boolean transformArtifact(Artifact artifact)
    {
        if (artifact.getListener() instanceof ArtifactTransformer)
        {
            prepareTempDir();
            try
            {
                File transformed = ((ArtifactTransformer) artifact.getListener()).transform(artifact.getJaredDirectory(), tmpDir);
                if (transformed != null)
                {
                    artifact.setTransformed(transformed);
                    return true;
                }
            }
            catch (Exception e)
            {
                log(Logger.LOG_WARNING,
                    "Unable to transform artifact: " + artifact.getPath().getAbsolutePath(), e);
            }
            return false;
        }
        else if (artifact.getListener() instanceof ArtifactUrlTransformer)
        {
            try
            {
                URL url = artifact.getJaredUrl();
                URL transformed = ((ArtifactUrlTransformer) artifact.getListener()).transform(url);
                if (transformed != null)
                {
                    artifact.setTransformedUrl(transformed);
                    return true;
                }
            }
            catch (Exception e)
            {
                log(Logger.LOG_WARNING,
                    "Unable to transform artifact: " + artifact.getPath().getAbsolutePath(), e);
            }
            return false;
        }
        return true;
    }

    private void deleteTransformedFile(Artifact artifact)
    {
        if (artifact.getTransformed() != null
                && !artifact.getTransformed().equals(artifact.getPath())
                && !artifact.getTransformed().delete())
        {
            log(Logger.LOG_WARNING,
                "Unable to delete transformed artifact: " + artifact.getTransformed().getAbsolutePath(), null);
        }
    }

    private void deleteJaredDirectory(Artifact artifact)
    {
        if (artifact.getJaredDirectory() != null
                && !artifact.getJaredDirectory().equals(artifact.getPath())
                && !artifact.getJaredDirectory().delete())
        {
            log(Logger.LOG_WARNING,
                "Unable to delete jared artifact: " + artifact.getJaredDirectory().getAbsolutePath(), null);
        }
    }


    private void prepareTempDir()
    {
        if (tmpDir == null)
        {
            for (;;)
            {
                File f = new File(javaIoTmpdir, "fileinstall-" + Long.toString(random.nextLong()));
                if (!f.exists() && f.mkdirs())
                {
                    tmpDir = f;
                    tmpDir.deleteOnExit();
                    break;
                }
            }
        }
        else
        {
            prepareDir(tmpDir);
        }
    }

    /**
     * Create the watched directory, if not existing.
     * Throws a runtime exception if the directory cannot be created,
     * or if the provided File parameter does not refer to a directory.
     *
     * @param dir
     *            The directory File Install will monitor
     */
    private void prepareDir(File dir)
    {
        if (!dir.exists() && !dir.mkdirs())
        {
            log(Logger.LOG_ERROR,
                "Cannot create folder "
                + dir
                + ". Is the folder write-protected?", null);
            throw new RuntimeException("Cannot create folder: " + dir);
        }

        if (!dir.isDirectory())
        {
            log(Logger.LOG_ERROR,
                "Cannot use "
                + dir
                + " because it's not a directory", null);
            throw new RuntimeException(
                "Cannot start FileInstall using something that is not a directory");
        }
    }

    /**
     * Log a message and optional throwable. If there is a log service we use
     * it, otherwise we log to the console
     *
     * @param message
     *            The message to log
     * @param e
     *            The throwable to log
     */
    void log(int msgLevel, String message, Throwable e)
    {
        Util.log(context, logLevel, msgLevel, message, e);
    }

    /**
     * Check if a bundle is a fragment.
     *
     * @param bundle
     * @return
     */
    boolean isFragment(Bundle bundle)
    {
        PackageAdmin padmin = FileInstall.getPackageAdmin();
        if (padmin != null)
        {
            return padmin.getBundleType(bundle) == PackageAdmin.BUNDLE_TYPE_FRAGMENT;
        }
        return false;
    }

    /**
     * Convenience to refresh the packages
     */
    void refresh()
    {
        PackageAdmin padmin = FileInstall.getPackageAdmin();
        if (padmin != null)
        {
            padmin.refreshPackages(null);
        }
    }

    /**
     * Retrieve a property as a long.
     *
     * @param properties the properties to retrieve the value from
     * @param property the name of the property to retrieve
     * @param dflt the default value
     * @return the property as a long or the default value
     */
    int getInt(Dictionary properties, String property, int dflt)
    {
        String value = (String) properties.get(property);
        if (value != null)
        {
            try
            {
                return Integer.parseInt(value);
            }
            catch (Exception e)
            {
                log(Logger.LOG_WARNING, property + " set, but not a int: " + value, null);
            }
        }
        return dflt;
    }

    /**
     * Retrieve a property as a long.
     *
     * @param properties the properties to retrieve the value from
     * @param property the name of the property to retrieve
     * @param dflt the default value
     * @return the property as a long or the default value
     */
    long getLong(Dictionary properties, String property, long dflt)
    {
        String value = (String) properties.get(property);
        if (value != null)
        {
            try
            {
                return Long.parseLong(value);
            }
            catch (Exception e)
            {
                log(Logger.LOG_WARNING, property + " set, but not a long: " + value, null);
            }
        }
        return dflt;
    }

    /**
     * Retrieve a property as a File.
     *
     * @param properties the properties to retrieve the value from
     * @param property the name of the property to retrieve
     * @param dflt the default value
     * @return the property as a File or the default value
     */
    File getFile(Dictionary properties, String property, File dflt)
    {
        String value = (String) properties.get(property);
        if (value != null)
        {
            return new File(value);
        }
        return dflt;
    }

    /**
     * Retrieve a property as a boolan.
     *
     * @param properties the properties to retrieve the value from
     * @param property the name of the property to retrieve
     * @param dflt the default value
     * @return the property as a boolean or the default value
     */
    boolean getBoolean(Dictionary properties, String property, boolean dflt)
    {
        String value = (String) properties.get(property);
        if (value != null)
        {
            return Boolean.valueOf(value).booleanValue();
        }
        return dflt;
    }

    public void close()
    {
        this.context.removeBundleListener(this);
        interrupt();
        for (Iterator iter = currentManagedArtifacts.values().iterator(); iter.hasNext();)
        {
            Artifact artifact = (Artifact) iter.next();
            deleteTransformedFile(artifact);
            deleteJaredDirectory(artifact);
        }
        try
        {
            join(10000);
        }
        catch (InterruptedException ie)
        {
            // Ignore
        }
    }

    /**
     * This method goes through all the currently installed bundles
     * and returns information about those bundles whose location
     * refers to a file in our {@link #watchedDirectory}.
     */
    private void initializeCurrentManagedBundles()
    {
        Bundle[] bundles = this.context.getBundles();
        String watchedDirPath = watchedDirectory.toURI().normalize().getPath();
        Map /*<File, Long>*/ checksums = new HashMap/*<File, Long>*/();
        for (int i = 0; i < bundles.length; i++)
        {
            // Convert to a URI because the location of a bundle
            // is typically a URI. At least, that's the case for
            // autostart bundles and bundles installed by fileinstall.
            // Normalisation is needed to ensure that we don't treat (e.g.)
            // /tmp/foo and /tmp//foo differently.
            String location = bundles[i].getLocation();
            String path = null;
            if (location != null &&
                    !location.equals(Constants.SYSTEM_BUNDLE_LOCATION))
            {
                URI uri;
                try
                {
                    uri = new URI(bundles[i].getLocation()).normalize();
                }
                catch (URISyntaxException e)
                {
                    // Let's try to interpret the location as a file path
                    uri = new File(location).toURI().normalize();
                }
                path = uri.getPath();
            }
            if (path == null)
            {
                // jar.getPath is null means we could not parse the location
                // as a meaningful URI or file path. e.g., location
                // represented an Opaque URI.
                // We can't do any meaningful processing for this bundle.
                continue;
            }
            final int index = path.lastIndexOf('/');
            if (index != -1 && path.startsWith(watchedDirPath))
            {
                Artifact artifact = new Artifact();
                artifact.setBundleId(bundles[i].getBundleId());
                artifact.setChecksum(Util.loadChecksum(bundles[i], context));
                artifact.setListener(null);
                artifact.setPath(new File(path));
                currentManagedArtifacts.put(new File(path), artifact);
                checksums.put(new File(path), new Long(artifact.getChecksum()));
            }
        }
        scanner.initialize(checksums);
    }

    /**
     * This method installs a collection of artifacts.
     * @param artifacts Collection of {@link Artifact}s to be installed
     * @return List of Bundles just installed
     */
    private Collection/* <Bundle> */ install(Collection/* <Artifact> */ artifacts)
    {
        List bundles = new ArrayList();
        for (Iterator iter = artifacts.iterator(); iter.hasNext();)
        {
            Artifact artifact = (Artifact) iter.next();
            Bundle bundle = install(artifact);
            if (bundle != null)
            {
                bundles.add(bundle);
            }
        }
        return bundles;
    }

    /**
     * This method uninstalls a collection of artifacts.
     * @param artifacts Collection of {@link Artifact}s to be uninstalled
     * @return Collection of Bundles that got uninstalled
     */
    private Collection/* <Bundle> */ uninstall(Collection/* <Artifact> */ artifacts)
    {
        List bundles = new ArrayList();
        for (Iterator iter = artifacts.iterator(); iter.hasNext();)
        {
            Artifact artifact = (Artifact) iter.next();
            Bundle bundle = uninstall(artifact);
            if (bundle != null)
            {
                bundles.add(bundle);
            }
        }
        return bundles;
    }

    /**
     * This method updates a collection of artifacts.
     *
     * @param artifacts    Collection of {@link Artifact}s to be updated.
     * @return Collection of bundles that got updated
     */
    private Collection/* <Bundle> */ update(Collection/* <Artifact> */ artifacts)
    {
        List bundles = new ArrayList();
        for (Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            Bundle bundle = update(artifact);
            if (bundle != null)
            {
                bundles.add(bundle);
            }
        }
        return bundles;
    }

    /**
     * Install an artifact and return the bundle object.
     * It uses {@link Artifact#getPath()} as location
     * of the new bundle. Before installing a file,
     * it sees if the file has been identified as a bad file in
     * earlier run. If yes, then it compares to see if the file has changed
     * since then. It installs the file if the file has changed.
     * If the file has not been identified as a bad file in earlier run,
     * then it always installs it.
     *
     * @param artifact the artifact to be installed
     * @return Bundle object that was installed
     */
    private Bundle install(Artifact artifact)
    {
        File path = artifact.getPath();
        Bundle bundle = null;
        try
        {
            // If the listener is an installer, ask for an update
            if (artifact.getListener() instanceof ArtifactInstaller)
            {
                ((ArtifactInstaller) artifact.getListener()).install(path);
            }
            // if the listener is an url transformer
            else if (artifact.getListener() instanceof ArtifactUrlTransformer)
            {
                Artifact badArtifact = (Artifact) installationFailures.get(path);
                if (badArtifact != null && badArtifact.getChecksum() == artifact.getChecksum())
                {
                    return null; // Don't attempt to install it; nothing has changed.
                }
                URL transformed = artifact.getTransformedUrl();
                String location = transformed.toString();
                BufferedInputStream in = new BufferedInputStream(transformed.openStream());
                try
                {
                    bundle = installOrUpdateBundle(location, in, artifact.getChecksum());
                }
                finally
                {
                    in.close();
                }
                artifact.setBundleId(bundle.getBundleId());
            }
            // if the listener is an artifact transformer
            else if (artifact.getListener() instanceof ArtifactTransformer)
            {
                Artifact badArtifact = (Artifact) installationFailures.get(path);
                if (badArtifact != null && badArtifact.getChecksum() == artifact.getChecksum())
                {
                    return null; // Don't attempt to install it; nothing has changed.
                }
                File transformed = artifact.getTransformed();
                String location = path.toURI().normalize().toString();
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(transformed != null ? transformed : path));
                try
                {
                    bundle = installOrUpdateBundle(location, in, artifact.getChecksum());
                }
                finally
                {
                    in.close();
                }
                artifact.setBundleId(bundle.getBundleId());
            }
            installationFailures.remove(path);
            currentManagedArtifacts.put(path, artifact);
            log(Logger.LOG_DEBUG, "Installed " + path, null);
        }
        catch (Exception e)
        {
            log(Logger.LOG_WARNING, "Failed to install artifact: " + path, e);

            // Add it our bad jars list, so that we don't
            // attempt to install it again and again until the underlying
            // jar has been modified.
            installationFailures.put(path, artifact);
        }
        return bundle;
    }

    private Bundle installOrUpdateBundle(String bundleLocation, BufferedInputStream is, long checksum) throws IOException, BundleException {
        is.mark(256 * 1024);
        JarInputStream jar = new JarInputStream(is);
        Manifest m = jar.getManifest();
        String sn = m.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
        String vStr = m.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
        Version v = vStr == null ? Version.emptyVersion : Version.parseVersion(vStr);
        Bundle[] bundles = context.getBundles();
        for (int i = 0; i < bundles.length; i++) {
            Bundle b = bundles[i];
            if (b.getSymbolicName() != null && b.getSymbolicName().equals(sn)) {
                vStr = (String) b.getHeaders().get(Constants.BUNDLE_VERSION);
                Version bv = vStr == null ? Version.emptyVersion : Version.parseVersion(vStr);
                if (v.equals(bv)) {
                    is.reset();
                    if (Util.loadChecksum(b, context) != checksum) {
                        log(Logger.LOG_WARNING,
                            "A bundle with the same symbolic name (" + sn + ") and version (" + vStr + ") is already installed.  Updating this bundle instead.", null);
                        stopTransient(b);
                        Util.storeChecksum(b, checksum, context);
                        b.update(is);
                    }
                    return b;
                }
            }
        }
        is.reset();
        Bundle b = context.installBundle(bundleLocation, is);
        Util.storeChecksum(b, checksum, context);
        return b;
    }

    /**
     * Uninstall a jar file.
     */
    private Bundle uninstall(Artifact artifact)
    {
        Bundle bundle = null;
        try
        {
            File path = artifact.getPath();
            // Find a listener for this artifact if needed
            if (artifact.getListener() == null) {
                artifact.setListener(findListener(path, FileInstall.getListeners()));
            }
            // Forget this artifact
            currentManagedArtifacts.remove(path);
            // Delete transformed file
            deleteTransformedFile(artifact);
            // if the listener is an installer, uninstall the artifact
            if (artifact.getListener() instanceof ArtifactInstaller)
            {
                ((ArtifactInstaller) artifact.getListener()).uninstall(path);
            }
            // else we need uninstall the bundle
            else if (artifact.getBundleId() != 0)
            {
                // old can't be null because of the way we calculate deleted list.
                bundle = context.getBundle(artifact.getBundleId());
                if (bundle == null)
                {
                    log(Logger.LOG_WARNING,
                        "Failed to uninstall bundle: "
                        + path + " with id: "
                        + artifact.getBundleId()
                        + ". The bundle has already been uninstalled", null);
                    return null;
                }
                log(Logger.LOG_DEBUG,
                    "Uninstalling bundle " + bundle.getBundleId() + " (" + bundle.getSymbolicName() + ")", null);
                bundle.uninstall();
            }
            log(Logger.LOG_DEBUG, "Uninstalled " + path, null);
        }
        catch (Exception e)
        {
            log(Logger.LOG_WARNING, "Failed to uninstall artifact: " + artifact.getPath(), e);
        }
        return bundle;
    }

    private Bundle update(Artifact artifact)
    {
        Bundle bundle = null;
        try
        {
            File path = artifact.getPath();
            // If the listener is an installer, ask for an update
            if (artifact.getListener() instanceof ArtifactInstaller)
            {
                ((ArtifactInstaller) artifact.getListener()).update(path);
            }
            // if the listener is an url transformer
            else if (artifact.getListener() instanceof ArtifactUrlTransformer)
            {
                URL transformed = artifact.getTransformedUrl();
                bundle = context.getBundle(artifact.getBundleId());
                if (bundle == null)
                {
                    log(Logger.LOG_WARNING,
                        "Failed to update bundle: "
                        + path + " with ID "
                        + artifact.getBundleId()
                        + ". The bundle has been uninstalled", null);
                    return null;
                }
                stopTransient(bundle);
                Util.storeChecksum(bundle, artifact.getChecksum(), context);
                InputStream in = (transformed != null) ? transformed.openStream() : new FileInputStream(path);
                try
                {
                    bundle.update(in);
                }
                finally
                {
                    in.close();
                }
            }
            // else we need to ask for an update on the bundle
            else if (artifact.getListener() instanceof ArtifactTransformer)
            {
                File transformed = artifact.getTransformed();
                bundle = context.getBundle(artifact.getBundleId());
                if (bundle == null)
                {
                    log(Logger.LOG_WARNING,
                        "Failed to update bundle: "
                        + path + " with ID "
                        + artifact.getBundleId()
                        + ". The bundle has been uninstalled", null);
                    return null;
                }
                stopTransient(bundle);
                Util.storeChecksum(bundle, artifact.getChecksum(), context);
                InputStream in = new FileInputStream(transformed != null ? transformed : path);
                try
                {
                    bundle.update(in);
                }
                finally
                {
                    in.close();
                }
            }
            log(Logger.LOG_DEBUG, "Updated " + path, null);
        }
        catch (Throwable t)
        {
            log(Logger.LOG_WARNING, "Failed to update artifact " + artifact.getPath(), t);
        }
        return bundle;
    }

    private void stopTransient(Bundle bundle) throws BundleException {
        // Stop the bundle transiently so that it will be restarted when startAllBundles() is called
        // but this avoids the need to restart the bundle twice (once for the update and another one
        // when refreshing packages).
        if (startBundles) {
            bundle.stop(Bundle.STOP_TRANSIENT);
        }
    }

    private void startAllBundles()
    {
        List bundles = new ArrayList();
        for (Iterator it = currentManagedArtifacts.values().iterator(); it.hasNext();)
        {
            Artifact artifact = (Artifact) it.next();
            if (artifact.getBundleId() > 0)
            {
                Bundle bundle = context.getBundle(artifact.getBundleId());
                if (bundle != null)
                {
                    if (bundle.getState() != Bundle.STARTING && bundle.getState() != Bundle.ACTIVE
                            && FileInstall.getStartLevel().isBundlePersistentlyStarted(bundle)
                            && FileInstall.getStartLevel().getStartLevel() > FileInstall.getStartLevel().getBundleStartLevel(bundle))
                    {
                        bundles.add(bundle);
                    }
                }
            }
        }
        start(bundles);
    }

    private void start(Collection/* <Bundle> */ bundles)
    {
        for (Iterator b = bundles.iterator(); b.hasNext(); )
        {
            start((Bundle) b.next());
        }
    }

    private void start(Bundle bundle)
    {
        // Fragments can not be started.
        // No need to check status of bundles
        // before starting, because OSGi treats this
        // as a noop when the bundle is already started
        if (!isFragment(bundle))
        {
            try
            {
                bundle.start();
                log(Logger.LOG_DEBUG, "Started bundle: " + bundle.getLocation(), null);
            }
            catch (BundleException e)
            {
                log(Logger.LOG_WARNING, "Error while starting bundle: " + bundle.getLocation(), e);
            }
        }
    }
}
