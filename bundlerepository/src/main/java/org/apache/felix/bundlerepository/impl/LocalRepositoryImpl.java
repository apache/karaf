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
package org.apache.felix.bundlerepository.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.utils.log.Logger;
import org.osgi.framework.AllServiceListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.apache.felix.bundlerepository.*;

public class LocalRepositoryImpl implements Repository, SynchronousBundleListener, AllServiceListener
{
    private final BundleContext m_context;
    private final Logger m_logger;
    private long m_snapshotTimeStamp = 0;
    private Map m_localResourceList = new HashMap();

    public LocalRepositoryImpl(BundleContext context, Logger logger)
    {
        m_context = context;
        m_logger = logger;
        initialize();
    }

    public void bundleChanged(BundleEvent event)
    {
        if (event.getType() == BundleEvent.INSTALLED)
        {
            synchronized (this)
            {
                addBundle(event.getBundle(), m_logger);
                m_snapshotTimeStamp = System.currentTimeMillis();
            }
        }
        else if (event.getType() == BundleEvent.UNINSTALLED)
        {
            synchronized (this)
            {
                removeBundle(event.getBundle(), m_logger);
                m_snapshotTimeStamp = System.currentTimeMillis();
            }
        }
    }

    public void serviceChanged(ServiceEvent event)
    {
        Bundle bundle = event.getServiceReference().getBundle();
        if (bundle.getState() == Bundle.ACTIVE && event.getType() != ServiceEvent.MODIFIED)
        {
            synchronized (this)
            {
                removeBundle(bundle, m_logger);
                addBundle(bundle, m_logger);
                m_snapshotTimeStamp = System.currentTimeMillis();
            }
        }
    }

    private void addBundle(Bundle bundle, Logger logger)
    {
        
        /*
         * Concurrency note: This method MUST be called in a context which
         * is synchronized on this instance to prevent data structure
         * corruption.
         */

        // Ignore system bundle
        if (bundle.getBundleId() == 0)
        {
            return;
        }
        try
        {
            m_localResourceList.put(new Long(bundle.getBundleId()), new LocalResourceImpl(bundle));
        }
        catch (InvalidSyntaxException ex)
        {
            // This should never happen since we are generating filters,
            // but ignore the resource if it does occur.
            m_logger.log(Logger.LOG_WARNING, ex.getMessage(), ex);
        }
    }
    
    private void removeBundle(Bundle bundle, Logger logger)
    {
        
        /*
         * Concurrency note: This method MUST be called in a context which
         * is synchronized on this instance to prevent data structure
         * corruption.
         */
        
        m_localResourceList.remove(new Long(bundle.getBundleId()));
    }
    
    public void dispose()
    {
        m_context.removeBundleListener(this);
        m_context.removeServiceListener(this);
    }

    public String getURI()
    {
        return LOCAL;
    }

    public String getName()
    {
        return "Locally Installed Repository";
    }

    public synchronized long getLastModified()
    {
        return m_snapshotTimeStamp;
    }

    public synchronized Resource[] getResources()
    {
        return (Resource[]) m_localResourceList.values().toArray(new Resource[m_localResourceList.size()]);
    }

    private void initialize()
    {
        // register for bundle and service events now
        m_context.addBundleListener(this);
        m_context.addServiceListener(this);

        // Generate the resource list from the set of installed bundles.
        // Lock so we can ensure that no bundle events arrive before we 
        // are done getting our state snapshot.
        Bundle[] bundles = null;
        synchronized (this)
        {
            // Create a local resource object for each bundle, which will
            // convert the bundle headers to the appropriate resource metadata.
            bundles = m_context.getBundles();
            for (int i = 0; (bundles != null) && (i < bundles.length); i++)
            {
                addBundle(bundles[i], m_logger);
            }

            m_snapshotTimeStamp = System.currentTimeMillis();
        }
    }

}
