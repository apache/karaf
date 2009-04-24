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

import java.util.*;

import org.apache.felix.fileinstall.util.Util;
import org.osgi.framework.*;
import org.osgi.service.cm.*;
import org.osgi.service.packageadmin.*;
import org.osgi.util.tracker.*;

/**
 * This clever little bundle watches a directory and will install any jar file
 * if finds in that directory (as long as it is a valid bundle and not a
 * fragment).
 * 
 */
public class FileInstall implements BundleActivator, ManagedServiceFactory
{
    static ServiceTracker padmin;
    static ServiceTracker cmTracker;
    BundleContext context;
    Map watchers = new HashMap();

    public void start(BundleContext context) throws Exception
    {
        this.context = context;
        Hashtable props = new Hashtable();
        props.put(Constants.SERVICE_PID, getName());
        context.registerService(ManagedServiceFactory.class.getName(), this,
            props);

        padmin = new ServiceTracker(context, PackageAdmin.class.getName(), null);
        padmin.open();
        cmTracker = new ServiceTracker(context, ConfigurationAdmin.class.getName(), null);
        cmTracker.open();

        // Created the initial configuration
        Hashtable ht = new Hashtable();

        set(ht, DirectoryWatcher.POLL);
        set(ht, DirectoryWatcher.DIR);
        set(ht, DirectoryWatcher.DEBUG);
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
        performSubstitution( properties );    
        
        DirectoryWatcher watcher = new DirectoryWatcher(properties, context);
        watchers.put(pid, watcher);
        watcher.start();
    }

    private void performSubstitution( Dictionary properties )
    {
        for (Enumeration e = properties.keys(); e.hasMoreElements(); )
        {
            String name = (String) e.nextElement();
            properties.put(name,
                Util.substVars(( String ) properties.get(name), name, null, properties));
        }
    }
}
