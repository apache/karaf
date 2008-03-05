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
import org.osgi.service.packageadmin.*;

public class DirectoryWatcher extends Thread
{
    final static String ALIAS_KEY = "_alias_factory_pid";
    public final static String POLL = "felix.fileinstall.poll";
    public final static String DIR = "felix.fileinstall.dir";
    public final static String DEBUG = "felix.fileinstall.debug";
    File jardir;
    boolean cont = true;
    long poll = 2000;
    long debug;
    Map foundBundles = new HashMap();
    Map foundConfigs = new HashMap();
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

        jardir = new File(dir);
        jardir.mkdirs();
    }

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
                System.out.println(property + " set, but not a long: " + value);
            }
        }
        return dflt;
    }

    public void run()
    {
        System.out.println(POLL + "  (ms)   " + poll);
        System.out.println(DIR + "            " + jardir.getAbsolutePath());
        System.out.println(DEBUG + "          " + debug);

        while (cont)
        {
            try
            {
                Map installed = new HashMap();
                Map configs = new HashMap();
                traverse(installed, configs, jardir);
                doInstalled(foundBundles, installed);
                doConfigs(foundConfigs, configs);
                Thread.sleep(poll);
            }
            catch (InterruptedException e)
            {

            }
            catch (Throwable e)
            {
                log("In main loop, we have serious trouble", e);
            }
        }
    }

    private void doConfigs(Map old, Map newConfigs)
    {
        try
        {
            Set oldKeys = new HashSet(old.keySet());
            for (Iterator e = newConfigs.entrySet().iterator(); e.hasNext();)
            {
                Map.Entry entry = (Map.Entry) e.next();
                String path = (String) entry.getKey();
                File f = (File) entry.getValue();
                if (!oldKeys.contains(path))
                {
                    // new
                    Long l = new Long(f.lastModified());
                    if (setConfig(f))
                    {
                        old.put(path, l);
                    }
                }
                else
                {
                    long lastModified = f.lastModified();
                    long oldTime = ((Long) old.get(path)).longValue();
                    if (oldTime < lastModified)
                    {
                        if (setConfig(f))
                        {
                            old.put(path, new Long(lastModified));
                        }
                    }
                }
                oldKeys.remove(path);
            }
            for (Iterator e = oldKeys.iterator(); e.hasNext();)
            {
                String path = (String) e.next();
                File f = new File(path);
                if (deleteConfig(f))
                {
                    foundConfigs.remove(path);
                }
            }
        }
        catch (Exception ee)
        {
            log("Processing config: ", ee);
        }
    }

    private boolean setConfig(File f) throws Exception
    {
        ConfigurationAdmin cm = (ConfigurationAdmin) FileInstall.cmTracker.getService();
        if (cm == null)
        {
            if (debug != 0 && !reported)
            {
                System.err.println("Can't find a Configuration Manager, configurations do not work");
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

    private boolean deleteConfig(File f) throws Exception
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
            return new String[]{pid, factoryPid};
        }
        else
        {
            return new String[]{pid, null};
        }
    }

    private Configuration getConfiguration(String pid, String factoryPid)
        throws Exception
    {
        ConfigurationAdmin cm = (ConfigurationAdmin) FileInstall.cmTracker.getService();
        if (factoryPid != null)
        {
            Configuration configs[] = cm.listConfigurations("(|(" + ALIAS_KEY + "=" + factoryPid + ")(.alias_factory_pid=" + factoryPid + "))");
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

    private void doInstalled(Map sizes, Map installed)
    {
        boolean refresh = false;
        Bundle bundles[] = context.getBundles();
        for (int i = 0; i < bundles.length; i++)
        {
            Bundle bundle = bundles[i];
            String location = bundle.getLocation();
            File file = (File) installed.get(location);
            if (file != null)
            {
                // Modified date does not work on the Nokia
                // for some reason, so we take size into account
                // as well.
                long newSize = file.length();
                Long oldSizeObj = (Long) sizes.get(location);
                long oldSize = oldSizeObj == null ? 0 : oldSizeObj.longValue();

                installed.remove(location);
                if (file.lastModified() > bundle.getLastModified() + 4000 && oldSize != newSize)
                {
                    try
                    {
                        sizes.put(location, new Long(newSize));
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
                if (bundle.getLocation().startsWith(jardir.getAbsolutePath()))
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

        for (Iterator it = installed.values().iterator(); it.hasNext();)
        {
            try
            {
                File file = (File) it.next();
                InputStream in = new FileInputStream(file);
                Bundle bundle = context.installBundle(file.getAbsolutePath(),
                    in);
                refresh = true;
                in.close();
                if (!isFragment(bundle))
                {
                    bundle.start();
                }
                log("Installed " + file.getAbsolutePath(), null);
            }
            catch (Exception e)
            {
                log("failed to install/start bundle: ", e);
            }
        }
        if (refresh)
        {
            refresh();
        }
    }

    private void log(String string, Throwable e)
    {
        System.err.println(string + ": " + e);
        if (debug > 0 && e != null)
        {
            e.printStackTrace();
        }
    }

    private void traverse(Map jars, Map configs, File jardir2)
    {
        String list[] = jardir.list();
        for (int i = 0; i < list.length; i++)
        {
            File file = new File(jardir2, list[i]);
            if (list[i].endsWith(".jar"))
            {
                jars.put(file.getAbsolutePath(), file);
            }
            else if (list[i].endsWith(".cfg"))
            {
                configs.put(file.getAbsolutePath(), file);
            }
        }
    }

    private boolean isFragment(Bundle bundle)
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

    private void refresh()
    {
        PackageAdmin padmin;
        try
        {
            padmin = (PackageAdmin) FileInstall.padmin.waitForService(10000);
            padmin.refreshPackages(null);
        }
        catch (InterruptedException e)
        {
        // stupid exception
        }
    }

    public void close()
    {
        cont = false;
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