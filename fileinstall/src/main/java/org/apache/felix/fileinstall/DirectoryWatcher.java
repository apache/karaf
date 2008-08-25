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

/**
 * -DirectoryWatcher-
 * 
 * This class runs a background task that checks a directory for new files or
 * removed files. These files can be configuration files or jars.
 */
import java.io.*;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.cm.*;
import org.osgi.service.log.*;
import org.osgi.service.packageadmin.*;

public class DirectoryWatcher extends Thread
{
    final static String ALIAS_KEY = "_alias_factory_pid";
    public final static String POLL = "felix.fileinstall.poll";
    public final static String DIR = "felix.fileinstall.dir";
    public final static String DEBUG = "felix.fileinstall.debug";
    File watchedDirectory;
    long poll = 2000;
    long debug;
    BundleContext context;
    boolean reported;

    public DirectoryWatcher(Dictionary properties, BundleContext context)
    {
        super(properties.toString());
        this.context = context;
        poll = getLong(POLL, poll);
        debug = getLong(DEBUG, -1);

        String dir = (String) properties.get(DIR);
        if (dir == null)
        {
            dir = "./load";
        }
        this.watchedDirectory = new File(dir);
        this.watchedDirectory.mkdirs();
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
        Map currentManagedBundles = new HashMap(); // location -> Long(time)
        Map currentManagedConfigs = new HashMap(); // location -> Long(time)

        while (!interrupted())
        {
            try
            {
                Set/* <String> */ installed = new HashSet();
                Set/* <String> */ configs = new HashSet();
                traverse(installed, configs, watchedDirectory);
                doInstalled(currentManagedBundles, installed);
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

            for (Iterator e = discovered.iterator(); e.hasNext();)
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
                log(
                    "Can't find a Configuration Manager, configurations do not work",
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
        if (pid[1] != null)
        {
            ht.put(ALIAS_KEY, pid[1]);
        }
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
        ConfigurationAdmin cm = (ConfigurationAdmin) FileInstall.cmTracker.getService();
        if (factoryPid != null)
        {
            String filter = "(|(" + ALIAS_KEY + "=" + factoryPid + ")(.alias_factory_pid=" + factoryPid + "))";
            Configuration configs[] = cm.listConfigurations(filter);
            if (configs == null || configs.length == 0)
            {
                return cm.createFactoryConfiguration(pid, null);
            }
            else
            {
                return configs[0];
            }
        }
        else
        {
            return cm.getConfiguration(pid, null);
        }
    }

    /**
     * Install bundles that were discovered and uninstall bundles that are gone
     * from the current state.
     * 
     * @param current
     *            A map location -> path that holds the current state
     * @param discovered
     *            A set of paths that represent the just found bundles
     */
    void doInstalled(Map current, Set discovered)
    {
        boolean refresh = false;
        Bundle bundles[] = context.getBundles();
        for (int i = 0; i < bundles.length; i++)
        {
            Bundle bundle = bundles[i];
            String location = bundle.getLocation();
            if (discovered.contains(location))
            {
                // We have a bundle that is already installed
                // so we know it
                discovered.remove(location);

                File file = new File(location);

                // Modified date does not work on the Nokia
                // for some reason, so we take size into account
                // as well.
                long newSize = file.length();
                Long oldSizeObj = (Long) current.get(location);
                long oldSize = oldSizeObj == null ? 0 : oldSizeObj.longValue();

                if (file.lastModified() > bundle.getLastModified() + 4000 && oldSize != newSize)
                {
                    try
                    {
                        // We treat this as an update, it is modified,,
                        // different size, and it is present in the dir
                        // as well as in the list of bundles.
                        current.put(location, new Long(newSize));
                        InputStream in = new FileInputStream(file);
                        bundle.update(in);
                        refresh = true;
                        in.close();
                        log("Updated " + location, null);
                    }
                    catch (Exception e)
                    {
                        log("Failed to update bundle ", e);
                    }
                }

                // Fragments can not be started. All other
                // bundles are always started because OSGi treats this
                // as a noop when the bundle is already started
                if (!isFragment(bundle))
                {
                    try
                    {
                        bundle.start();
                    }
                    catch (Exception e)
                    {
                        log("Fail to start bundle " + location, e);
                    }
                }
            }
            else
            {
                // Hmm. We found a bundlethat looks like it came from our
                // watched directory but we did not find it this round.
                // Just remove it.
                if (bundle.getLocation().startsWith(
                    watchedDirectory.getAbsolutePath()))
                {
                    try
                    {
                        bundle.uninstall();
                        refresh = true;
                        log("Uninstalled " + location, null);
                    }
                    catch (Exception e)
                    {
                        log("failed to uninstall bundle: ", e);
                    }
                }
            }
        }

        List starters = new ArrayList();
        for (Iterator it = discovered.iterator(); it.hasNext();)
        {
            try
            {
                String path = (String) it.next();
                File file = new File(path);
                InputStream in = new FileInputStream(file);
                Bundle bundle = context.installBundle(path, in);
                in.close();

                // We do not start this bundle yet. We wait after
                // refresh because this will minimize the disruption
                // as well as temporary unresolved errors.
                starters.add(bundle);

                log("Installed " + file.getAbsolutePath(), null);
            }
            catch (Exception e)
            {
                log("failed to install/start bundle: ", e);
            }
        }

        if (refresh || starters.size() != 0)
        {
            refresh();
            for (Iterator b = starters.iterator(); b.hasNext();)
            {
                Bundle bundle = (Bundle) b.next();
                if (!isFragment(bundle))
                {
                    try
                    {
                        bundle.start();
                    }
                    catch (BundleException e)
                    {
                        log("Error while starting a newly installed bundle", e);
                    }
                }
            }
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
        }
        else
        {
            if (e == null)
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
     * Traverse the directory and fill the map with the found jars and
     * configurations keyed by the abs file path.
     * 
     * @param jars
     *            Returns the abspath -> file for found jars
     * @param configs
     *            Returns the abspath -> file for found configurations
     * @param jardir
     *            The directory to traverse
     */
    void traverse(Set jars, Set configs, File jardir)
    {
        String list[] = jardir.list();
        for (int i = 0; i < list.length; i++)
        {
            File file = new File(jardir, list[i]);
            if (list[i].endsWith(".jar"))
            {
                jars.add(file.getAbsolutePath());
            }
            else if (list[i].endsWith(".cfg"))
            {
                configs.add(file.getAbsolutePath());
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
    long getLong(String property, long dflt)
    {
        String value = context.getProperty(property);
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
}
