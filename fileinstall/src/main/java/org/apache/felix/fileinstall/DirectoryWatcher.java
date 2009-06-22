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
package org.apache.felix.fileinstall;

import java.io.*;
import java.util.*;
import java.net.URISyntaxException;

import org.apache.felix.fileinstall.util.Util;
import org.osgi.framework.*;
import org.osgi.service.cm.*;
import org.osgi.service.log.*;
import org.osgi.service.packageadmin.*;

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
 * @author Peter Kriens
 * @author Sanjeeb Sahoo
 */
public class DirectoryWatcher extends Thread
{
	public final static String FILENAME = "felix.fileinstall.filename";
    public final static String POLL = "felix.fileinstall.poll";
    public final static String DIR = "felix.fileinstall.dir";
    public final static String DEBUG = "felix.fileinstall.debug";
    public final static String START_NEW_BUNDLES =
        "felix.fileinstall.bundles.new.start";
    File watchedDirectory;
    long poll = 2000;
    long debug;
    boolean startBundles = true; // by default, we start bundles.
    BundleContext context;
    boolean reported;
    String originatingFileName;
    
    Map/* <String, Jar> */ currentManagedBundles = new HashMap();

    // Represents jars that could not be installed
    Map/* <String, Jar> */ installationFailures = new HashMap();

    // Represents jars that could not be installed
    Set/* <Bundle> */ startupFailures = new HashSet();
    
    public DirectoryWatcher(Dictionary properties, BundleContext context)
    {
        super(properties.toString());
        this.context = context;
        poll = getLong(properties, POLL, poll);
        debug = getLong(properties, DEBUG, -1);
        originatingFileName = (String) properties.get(FILENAME);
        
        String dir = (String) properties.get(DIR);
        if (dir == null)
        {
            dir = "./load";
        }
        watchedDirectory = new File(dir);
        
        prepareWatchedDir(watchedDirectory);
        
        Object value = properties.get(START_NEW_BUNDLES);
        if (value != null)
        {
            startBundles = "true".equalsIgnoreCase((String)value);
        }
    }

    /**
     * Main run loop, will traverse the directory, and then handle the delta
     * between installed and newly found/lost bundles and configurations.
     *
     */
    public void run()
    {
        log(POLL + "  (ms)   " + poll, null);
        log(DIR + "            " + watchedDirectory.getAbsolutePath(), null);
        log(DEBUG + "          " + debug, null);
        log(START_NEW_BUNDLES + "          " + startBundles, null);
        initializeCurrentManagedBundles();
        Map currentManagedConfigs = new HashMap(); // location -> Long(time)
        while (!interrupted())
        {
            try
            {
                Map/* <String, Jar> */ installed = new HashMap();
                Set/* <String> */ configs = new HashSet();
                traverse(installed, configs, watchedDirectory);
                doInstalled(installed);
                doConfigs(currentManagedConfigs, configs);
                Thread.sleep(poll);
            }
            catch (InterruptedException e)
            {
                return;
            }
            catch (Throwable e)
            {
                log("In main loop, we have serious trouble", e);
            }
        }
    }
    
    /**
     * Create the watched directory, if not existing.
     * Throws a runtime exception if the directory cannot be created,
     * or if the provided File parameter does not refer to a directory.
     * 
     * @param watchedDirectory 
     *            The directory File Install will monitor
     */
    private void prepareWatchedDir(File watchedDirectory)
    {
        if (!watchedDirectory.exists() && !watchedDirectory.mkdirs())
        {
            log("Cannot create folder "
                + watchedDirectory
                + ". Is the folder write-protected?", null);
            throw new RuntimeException("Cannot create folder: " + watchedDirectory);
        }

        if (!watchedDirectory.isDirectory())
        {
            log("Cannot watch "
                + watchedDirectory
                + " because it's not a directory", null);
            throw new RuntimeException(
                "Cannot start FileInstall to watch something that is not a directory");
        }
    }

    /**
     * Handle the changes between the configurations already installed and the
     * newly found/lost configurations.
     *
     * @param current
     *            Existing installed configurations abspath -> File
     * @param discovered
     *            Newly found configurations
     */
    void doConfigs(Map current, Set discovered)
    {
        try
        {
            // Set all old keys as inactive, we remove them
            // when we find them to be active, will be left
            // with the inactive ones.
            Set inactive = new HashSet(current.keySet());

            for (Iterator e = discovered.iterator(); e.hasNext(); )
            {
                String path = (String) e.next();
                File f = new File(path);

                if (!current.containsKey(path))
                {
                    // newly found entry, set the config immedialey
                    Long l = new Long(f.lastModified());
                    if (setConfig(f))
                    {
                        // Remember it for the next round
                        current.put(path, l);
                    }
                }
                else
                {
                    // Found an existing one.
                    // Check if it has been updated
                    long lastModified = f.lastModified();
                    long oldTime = ((Long) current.get(path)).longValue();
                    if (oldTime < lastModified)
                    {
                        if (setConfig(f))
                        {
                            // Remember it for the next round.
                            current.put(path, new Long(lastModified));
                        }
                    }
                }
                // Mark this one as active
                inactive.remove(path);
            }
            for (Iterator e = inactive.iterator(); e.hasNext();)
            {
                String path = (String) e.next();
                File f = new File(path);
                if (deleteConfig(f))
                {
                    current.remove(path);
                }
            }
        }
        catch (Exception ee)
        {
            log("Processing config: ", ee);
        }
    }

    /**
     * Set the configuration based on the config file.
     *
     * @param f
     *            Configuration file
     * @return
     * @throws Exception
     */
    boolean setConfig(File f) throws Exception
    {
        ConfigurationAdmin cm = (ConfigurationAdmin) FileInstall.cmTracker.getService();
        if (cm == null)
        {
            if (debug != 0 && !reported)
            {
                log("Can't find a Configuration Manager, configurations do not work",
                    null);
                reported = true;
            }
            return false;
        }

        Properties p = new Properties();
        InputStream in = new FileInputStream(f);
        p.load(in);
        in.close();
        String pid[] = parsePid(f.getName());
        Hashtable ht = new Hashtable();
        ht.putAll(p);
        ht.put(FILENAME, f.getName());
        Configuration config = getConfiguration(pid[0], pid[1]);
        if (config.getBundleLocation() != null)
        {
            config.setBundleLocation(null);
        }
        config.update(ht);
        return true;
    }

    /**
     * Remove the configuration.
     *
     * @param f
     *            File where the configuration in whas defined.
     * @return
     * @throws Exception
     */
    boolean deleteConfig(File f) throws Exception
    {
        String pid[] = parsePid(f.getName());
        Configuration config = getConfiguration(pid[0], pid[1]);
        config.delete();
        return true;
    }

    String[] parsePid(String path)
    {
        String pid = path.substring(0, path.length() - 4);
        int n = pid.indexOf('-');
        if (n > 0)
        {
            String factoryPid = pid.substring(n + 1);
            pid = pid.substring(0, n);
            return new String[]
                {
                    pid, factoryPid
                };
        }
        else
        {
            return new String[]
                {
                    pid, null
                };
        }
    }

    Configuration getConfiguration(String pid, String factoryPid)
        throws Exception
    {
	    Configuration oldConfiguration = findExistingConfiguration(pid, factoryPid);
        if (oldConfiguration != null)
        {
            log("Updating configuration from " + pid
                + (factoryPid == null ? "" : "-" + factoryPid) + ".cfg", null);
            return oldConfiguration;
        }
        else
        {
            ConfigurationAdmin cm = (ConfigurationAdmin) FileInstall.cmTracker.getService();
            Configuration newConfiguration = null;
            if (factoryPid != null)
            {
                newConfiguration = cm.createFactoryConfiguration(pid, null);
            }
            else
            {
                newConfiguration = cm.getConfiguration(pid, null);
            }
            return newConfiguration;
        }
    }
    
    Configuration findExistingConfiguration(String pid, String factoryPid) throws Exception
    {
        String suffix = factoryPid == null ? ".cfg" : "-" + factoryPid + ".cfg";

        ConfigurationAdmin cm = (ConfigurationAdmin) FileInstall.cmTracker.getService();
        String filter = "(" + FILENAME + "=" + pid + suffix + ")";
        Configuration[] configurations = cm.listConfigurations(filter);
        if (configurations != null && configurations.length > 0)
        {
            return configurations[0];
        }
        else
        {
            return null;
        }
    }

    /**
     * This is the core of this class.
     * Install bundles that were discovered, uninstall bundles that are gone
     * from the current state and update the ones that have been changed.
     * Keep {@link #currentManagedBundles} up-to-date.
     *
     * @param discovered
     *            A map of path to {@link Jar} that holds the discovered state
     */
    void doInstalled(Map discovered)
    {
        // Find out all the new, deleted and common bundles.
        // new = discovered - current,
        Set newBundles = new HashSet(discovered.values());
        newBundles.removeAll(currentManagedBundles.values());

        // deleted = current - discovered
        Set deletedBundles = new HashSet(currentManagedBundles.values());
        deletedBundles.removeAll(discovered.values());

        // existing = intersection of current & discovered
        Set existingBundles = new HashSet(discovered.values());
        existingBundles.retainAll(currentManagedBundles.values());

        // We do the operations in the following order:
        // uninstall, update, install, refresh & start.
        Collection uninstalledBundles = uninstall(deletedBundles);
        Collection updatedBundles = update(existingBundles);
        Collection installedBundles = install(newBundles);
        if (uninstalledBundles.size() > 0 || updatedBundles.size() > 0)
        {
            // Refresh if any bundle got uninstalled or updated.
            // This can lead to restart of recently updated bundles, but
            // don't worry about that at this point of time.
            refresh();
        }

        // Try to start all the bundles that we could not start last time.
        // Make a copy, because start() changes the underlying collection
        start(new HashSet(startupFailures));

        if (startBundles
            && ((uninstalledBundles.size() > 0)
                || (updatedBundles.size() > 0)
                || (installedBundles.size() > 0)))
        {
            // Something has changed in the system, so
            // try to start all the bundles.
            startAllBundles();
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
    void log(String message, Throwable e)
    {
        LogService log = getLogService();
        if (log == null)
        {
            System.out.println(message + (e == null ? "" : ": " + e));
            if (debug > 0 && e != null)
            {
                e.printStackTrace(System.out);
            }
        }
        else
        {
            if (e != null)
            {
                log.log(LogService.LOG_ERROR, message, e);
                if (debug > 0 && e != null)
                {
                    e.printStackTrace();
                }
            }
            else
            {
                log.log(LogService.LOG_INFO, message);
            }
        }
    }

    /**
     * Answer the Log Service
     *
     * @return
     */
    LogService getLogService()
    {
        ServiceReference ref = context.getServiceReference(LogService.class.getName());
        if (ref != null)
        {
            LogService log = (LogService) context.getService(ref);
            return log;
        }
        return null;
    }

    /**
     * Traverse the directory and fill the set with the found jars and
     * configurations.
     *
     * @param jars
     *            Returns path -> {@link Jar} map for found jars
     * @param configs
     *            Returns the abspath -> file for found configurations
     * @param jardir
     *            The directory to traverse
     */
    void traverse(Map/* <String, Jar> */ jars, Set configs, File jardir)
    {
        String list[] = jardir.list();
        if (list == null)
        {
            prepareWatchedDir(jardir);
            list = jardir.list();
        }
        for (int i = 0; (list != null) && (i < list.length); i++)
        {
            File file = new File(jardir, list[i]);
            if (list[i].endsWith(".cfg"))
            {
                configs.add(file.getAbsolutePath());
            }
            else if (Util.isValidJar(file.getAbsolutePath()))
            {
                Jar jar = new Jar(file);
                jars.put(jar.getPath(), jar);
            }
        }
    }

    /**
     * Check if a bundle is a fragment.
     *
     * @param bundle
     * @return
     */
    boolean isFragment(Bundle bundle)
    {
        PackageAdmin padmin;
        if (FileInstall.padmin == null)
        {
            return false;
        }

        try
        {
            padmin = (PackageAdmin) FileInstall.padmin.waitForService(10000);
            if (padmin != null)
            {
                return padmin.getBundleType(bundle) == PackageAdmin.BUNDLE_TYPE_FRAGMENT;
            }
        }
        catch (InterruptedException e)
        {
            // stupid exception
        }
        return false;
    }

    /**
     * Convenience to refresh the packages
     */
    void refresh()
    {
        PackageAdmin padmin;
        try
        {
            padmin = (PackageAdmin) FileInstall.padmin.waitForService(10000);
            padmin.refreshPackages(null);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Answer the long from a property.
     *
     * @param property
     * @param dflt
     * @return
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
                log(property + " set, but not a long: " + value, null);
            }
        }
        return dflt;
    }

    public void close()
    {
        interrupt();
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
        for (int i = 0; i < bundles.length; i++)
        {
            try
            {
                Jar jar = new Jar(bundles[i]);
                String path =  jar.getPath();
                if (path == null)
                {
                    // jar.getPath is null means we could not parse the location
                    // as a meaningful URI or file path. e.g., location
                    // represented an Opaque URI.
                    // We can't do any meaningful processing for this bundle.
                    continue;
                }
                final int index = path.lastIndexOf('/');
                if (index != -1 && path.substring(0, index + 1).equals(watchedDirPath))
                {
                    currentManagedBundles.put(path, jar);
                }
            }
            catch (URISyntaxException e)
            {
                // Ignore and continue.
                // This can never happen for bundles that have been installed
                // by FileInstall, as we always use proper filepath as location.
            }
        }
    }

    /**
     * This method installs a collection of jar files.
     * @param jars Collection of {@link Jar} to be installed
     * @return List of Bundles just installed
     */
    private Collection/* <Bundle> */ install(Collection jars)
    {
        List bundles = new ArrayList();
        for (Iterator iter = jars.iterator(); iter.hasNext();)
        {
            Jar jar = (Jar) iter.next();

            Bundle bundle = install(jar);
            if (bundle != null)
            {
                bundles.add(bundle);
            }
        }
        return bundles;
    }

    /**
     * @param jars Collection of {@link Jar} to be uninstalled
     * @return Collection of Bundles that got uninstalled
     */
    private Collection/* <Bundle> */ uninstall(Collection jars)
    {
        List bundles = new ArrayList();
        for (Iterator iter = jars.iterator(); iter.hasNext();)
        {
            final Jar jar = (Jar) iter.next();
            Bundle b = uninstall(jar);
            if (b != null)
            {
                bundles.add(b);
            }
        }
        return bundles;
    }

    private void start(Collection bundles)
    {
        for (Iterator b = bundles.iterator(); b.hasNext(); )
        {
            start((Bundle) b.next());
        }
    }

    /**
     * Update the bundles if the underlying files have changed.
     * This method reads the information about jars to be updated,
     * compares them with information available in {@link #currentManagedBundles}.
     * If the file is newer, it updates the bundle.
     *
     * @param jars    Collection of {@link Jar}s representing state of files.
     * @return Collection of bundles that got updated
     */
    private Collection/* <Bundle> */ update(Collection jars)
    {
        List bundles = new ArrayList();
        for (Iterator iter = jars.iterator(); iter.hasNext(); )
        {
            Jar e = (Jar) iter.next();
            Jar c = (Jar) currentManagedBundles.get(e.getPath());
            if (e.isNewer(c))
            {
                Bundle b = update(c);
                if (b != null)
                {
                    bundles.add(b);
                }
            }
        }
        return bundles;
    }

    /**
     * Install a jar and return the bundle object.
     * It uses {@link org.apache.felix.fileinstall.Jar#getPath()} as location
     * of the new bundle. Before installing a file,
     * it sees if the file has been identified as a bad file in
     * earlier run. If yes, then it compares to see if the file has changed
     * since then. It installs the file if the file has changed.
     * If the file has not been identified as a bad file in earlier run,
     * then it always installs it.
     *
     * @param jar the jar to be installed
     * @return Bundle object that was installed
     */
    private Bundle install(Jar jar)
    {
        Bundle bundle = null;
        try
        {
            String path = jar.getPath();
            Jar badJar = (Jar) installationFailures.get(jar.getPath());
            if (badJar != null && badJar.getLastModified() == jar.getLastModified())
            {
                return null; // Don't attempt to install it; nothing has changed.
            }
            File file = new File(path);
            InputStream in = new FileInputStream(file);
            try
            {
                bundle = context.installBundle(path, in);
            }
            finally
            {
                in.close();
            }
            installationFailures.remove(path);
            currentManagedBundles.put(path, new Jar(bundle));
            log("Installed " + file.getAbsolutePath(), null);
        }
        catch (Exception e)
        {
            log("Failed to install bundle: " + jar.getPath(), e);

            // Add it our bad jars list, so that we don't
            // attempt to install it again and again until the underlying
            // jar has been modified.
            installationFailures.put(jar.getPath(), jar);
        }
        return bundle;
    }

    /**
     * Uninstall a jar file.
     */
    private Bundle uninstall(Jar jar)
    {
        try
        {
            Jar old = (Jar) currentManagedBundles.remove(jar.getPath());

            // old can't be null because of the way we calculate deleted list.
            Bundle bundle = context.getBundle(old.getBundleId());
            if (bundle == null)
            {
            	log("Failed to uninstall bundle: "
                    + jar.getPath() + " with id: "
                    + old.getBundleId()
                    + ". The bundle has already been uninstalled", null);
            	return null;
            }
            bundle.uninstall();
            startupFailures.remove(bundle);
            log("Uninstalled " + jar.getPath(), null);
            return bundle;
        }
        catch (Exception e)
        {
            log("Failed to uninstall bundle: " + jar.getPath(), e);
        }
        return null;
    }

    private Bundle update(Jar jar)
    {
        InputStream in = null;
        try
        {
            File file = new File(jar.getPath());
            in = new FileInputStream(file);
            Bundle bundle = context.getBundle(jar.getBundleId());
            if (bundle == null)
            {
            	log("Failed to update bundle: "
                    + jar.getPath() + " with ID "
                    + jar.getBundleId()
                    + ". The bundle has been uninstalled", null);
            	return null;
            }
            bundle.update(in);
            startupFailures.remove(bundle);
            jar.setLastModified(bundle.getLastModified());
            jar.setLength(file.length());
            log("Updated " + jar.getPath(), null);
            return bundle;
        }
        catch (Exception e)
        {
            log("Failed to update bundle " + jar.getPath(), e);
        }
        finally
        {
            if (in != null)
            {
                try
                {
                    in.close();
                }
                catch (IOException e)
                {
                }
            }
        }
        return null;
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
                startupFailures.remove(bundle);
                log("Started bundle: " + bundle.getLocation(), null);
            }
            catch (BundleException e)
            {
                log("Error while starting bundle: " + bundle.getLocation(), e);
                startupFailures.add(bundle);
            }
        }
    }

    /**
     * Start all bundles that we are currently managing.
     */
    private void startAllBundles()
    {
        for (Iterator jars = currentManagedBundles.values().iterator(); jars.hasNext(); )
        {
            Jar jar = (Jar) jars.next();
            Bundle bundle = context.getBundle(jar.getBundleId());
            start(bundle);
        }
    }
}