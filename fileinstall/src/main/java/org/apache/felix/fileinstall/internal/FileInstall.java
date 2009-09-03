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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.apache.felix.fileinstall.ArtifactListener;
import org.apache.felix.fileinstall.ArtifactTransformer;
import org.apache.felix.fileinstall.internal.Util;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This clever little bundle watches a directory and will install any jar file
 * if finds in that directory (as long as it is a valid bundle and not a
 * fragment).
 * 
 */
public class FileInstall implements BundleActivator, ManagedServiceFactory
{
    static ServiceTracker padmin;
    static ServiceTracker startLevel;
    static ServiceTracker cmTracker;
    static List /* <ArtifactListener> */ listeners = new ArrayList /* <ArtifactListener> */();
    BundleContext context;
    Map watchers = new HashMap();
    ConfigInstaller configInstaller;
    ServiceTracker listenersTracker;

    public void start(BundleContext context) throws Exception
    {
        this.context = context;
        addListener(new BundleTransformer());
        Hashtable props = new Hashtable();
        props.put(Constants.SERVICE_PID, getName());
        context.registerService(ManagedServiceFactory.class.getName(), this,
            props);

        padmin = new ServiceTracker(context, PackageAdmin.class.getName(), null);
        padmin.open();
        startLevel = new ServiceTracker(context, StartLevel.class.getName(), null);
        startLevel.open();
        cmTracker = new ServiceTracker(context, ConfigurationAdmin.class.getName(), null)
        {
            public Object addingService(ServiceReference serviceReference)
            {
                ConfigurationAdmin cm = (ConfigurationAdmin) super.addingService(serviceReference);
                configInstaller = new ConfigInstaller(context);
                addListener(configInstaller);
                return cm;
            }
            public void removedService(ServiceReference serviceReference, Object o)
            {
                configInstaller = null;
                removeListener(configInstaller);
                super.removedService(serviceReference, o);
            }
        };
        cmTracker.open();
        String flt = "(|(" + Constants.OBJECTCLASS + "=" + ArtifactInstaller.class.getName() + ")"
                     + "(" + Constants.OBJECTCLASS + "=" + ArtifactTransformer.class.getName() + "))";
        listenersTracker = new ServiceTracker(context, FrameworkUtil.createFilter(flt), null)
        {
            public Object addingService(ServiceReference serviceReference)
            {
                ArtifactListener listener = (ArtifactListener) super.addingService(serviceReference);
                addListener(listener);
                return listener;
            }
            public void removedService(ServiceReference serviceReference, Object o)
            {
                removeListener((ArtifactListener) o);
            }
        };
        listenersTracker.open();

        // Created the initial configuration
        Hashtable ht = new Hashtable();

        set(ht, DirectoryWatcher.POLL);
        set(ht, DirectoryWatcher.DIR);
        set(ht, DirectoryWatcher.DEBUG);
        set(ht, DirectoryWatcher.FILTER);
        set(ht, DirectoryWatcher.TMPDIR);
        set(ht, DirectoryWatcher.START_NEW_BUNDLES);
        updated("initial", ht);
    }

    // Adapted for FELIX-524
    private void set(Hashtable ht, String key)
    {
        Object o = context.getProperty(key);
        if (o == null)
        {
           o = System.getProperty(key.toUpperCase().replace('.', '_'));
            if (o == null)
            {
                return;
            }
        }
        ht.put(key, o);
    }

    public void stop(BundleContext context) throws Exception
    {
        for (Iterator w = watchers.values().iterator(); w.hasNext();)
        {
            try
            {
                DirectoryWatcher dir = (DirectoryWatcher) w.next();
                w.remove();
                dir.close();
            }
            catch (Exception e)
            {
                // Ignore
            }
        }
        listenersTracker.close();
        cmTracker.close();
        padmin.close();
    }

    public void deleted(String pid)
    {
        DirectoryWatcher watcher = (DirectoryWatcher) watchers.remove(pid);
        if (watcher != null)
        {
            watcher.close();
        }
    }

    public String getName()
    {
        return "org.apache.felix.fileinstall";
    }

    public void updated(String pid, Dictionary properties)
        throws ConfigurationException
    {
        deleted(pid);
        Util.performSubstitution(properties);    
        
        DirectoryWatcher watcher = new DirectoryWatcher(properties, context);
        watchers.put(pid, watcher);
        watcher.start();
    }

    private void addListener(ArtifactListener listener)
    {
        synchronized (listeners)
        {
            listeners.add(listener);
        }
    }

    private void removeListener(ArtifactListener listener)
    {
        synchronized (listeners)
        {
            listeners.remove(listener);
        }
    }

    static List getListeners()
    {
        synchronized (listeners)
        {
            return new ArrayList(listeners);
        }
    }

    static PackageAdmin getPackageAdmin()
    {
        return getPackageAdmin(10000);
    }

    static PackageAdmin getPackageAdmin(long timeout)
    {
        try
        {
            return (PackageAdmin) padmin.waitForService(timeout);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    static StartLevel getStartLevel()
    {
        return getStartLevel(10000);
    }

    static StartLevel getStartLevel(long timeout)
    {
        try
        {
            return (StartLevel) startLevel.waitForService(timeout);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    static ConfigurationAdmin getConfigurationAdmin()
    {
        return getConfigurationAdmin(10000);
    }

    static ConfigurationAdmin getConfigurationAdmin(long timeout)
    {
        try
        {
            return (ConfigurationAdmin) cmTracker.waitForService(timeout);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return null;
        }
    }

}