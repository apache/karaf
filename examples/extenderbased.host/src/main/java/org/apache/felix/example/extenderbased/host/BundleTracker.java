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
package org.apache.felix.example.extenderbased.host;

import java.util.HashSet;
import java.util.Set;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

/**
 * This is a simple class that only tracks active bundles. It must be given
 * a bundle context upon creation, which it uses to listen for bundle events.
 * It may also be given a bundle tracker customizer object which it will
 * notify about changes in the list of active bundles. The bundle tracker
 * must be opened to track objects and closed when it is no longer needed.
**/
public class BundleTracker implements BundleTrackerCustomizer
{
    private Set m_bundleSet = new HashSet();
    private BundleContext m_context;
    private BundleTrackerCustomizer m_customizer;
    private SynchronousBundleListener m_listener;
    private boolean m_open;

    /**
     * Constructs a bundle tracker object that will use the specified
     * bundle context for listening to bundle events and will notify the
     * specified bundle tracker customizer about changes in the set of
     * active bundles.
     * @param context The bundle context to use to track bundles.
     * @param customizer The bundle tracker customerizer to inform about
     *        changes in the active set of bundles.
    **/
    public BundleTracker(BundleContext context, BundleTrackerCustomizer customizer)
    {
        m_context = context;
        m_customizer = (customizer == null) ? this : customizer;
        m_listener = new SynchronousBundleListener() {
            public void bundleChanged(BundleEvent evt)
            {
                synchronized (BundleTracker.this)
                {
                    if (!m_open)
                    {
                        return;
                    }

                    if (evt.getType() == BundleEvent.STARTED)
                    {
                        if (!m_bundleSet.contains(evt.getBundle()))
                        {
                            m_bundleSet.add(evt.getBundle());
                            m_customizer.addedBundle(evt.getBundle());
                        }
                    }
                    else if (evt.getType() == BundleEvent.STOPPED)
                    {
                        if (m_bundleSet.contains(evt.getBundle()))
                        {
                            m_bundleSet.remove(evt.getBundle());
                            m_customizer.removedBundle(evt.getBundle());
                        }
                    }
                }
            }
        };
    }

    /**
     * Returns the current set of active bundles.
     * @return The current set of active bundles.
    **/
    public synchronized Bundle[] getBundles()
    {
        return (Bundle[]) m_bundleSet.toArray(new Bundle[m_bundleSet.size()]);
    }

    /**
     * Call this method to start the tracking of active bundles.
    **/
    public synchronized void open()
    {
        if (!m_open)
        {
            m_open = true;

            m_context.addBundleListener(m_listener);

            Bundle[] bundles = m_context.getBundles();
            for (int i = 0; i < bundles.length; i++)
            {
                if (bundles[i].getState() == Bundle.ACTIVE)
                {
                    m_bundleSet.add(bundles[i]);
                    m_customizer.addedBundle(bundles[i]);
                }
            }
        }
    }

    /**
     * Call this method to stop the tracking of active bundles.
    **/
    public synchronized void close()
    {
        if (m_open)
        {
            m_open = false;

            m_context.removeBundleListener(m_listener);

            Bundle[] bundles = getBundles();
            for (int i = 0; i < bundles.length; i++)
            {
                if (m_bundleSet.remove(bundles[i]))
                {
                    m_customizer.removedBundle(bundles[i]);
                }
            }
        }
    }

    /**
     * A default implementation of the bundle tracker customizer that
     * does nothing.
     * @param bundle The bundle being added to the active set.
    **/
    public void addedBundle(Bundle bundle)
    {
        // Do nothing by default.
    }

    /**
     * A default implementation of the bundle tracker customizer that
     * does nothing.
     * @param bundle The bundle being removed from the active set.
    **/
    public void removedBundle(Bundle bundle)
    {
        // Do nothing by default.
    }
}