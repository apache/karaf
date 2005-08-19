/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.framework;

import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.*;

class PackageAdminImpl implements PackageAdmin, Runnable
{
    private Felix m_felix = null;
    private Bundle[][] m_reqBundles = null;

    public PackageAdminImpl(Felix felix)
    {
        m_felix = felix;

        // Start a thread to perform asynchronous package refreshes.
        Thread t = new Thread(this, "FelixPackageAdmin");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Returns the exported package associated with the specified
     * package name.
     *
     * @param name the name of the exported package to find.
     * @return the exported package or null if no matching package was found.
    **/
    public ExportedPackage getExportedPackage(String name)
    {
        return m_felix.getExportedPackage(name);
    }

    /**
     * Returns the packages exported by the specified bundle.
     *
     * @param bundle the bundle whose exported packages are to be returned.
     * @return an array of packages exported by the bundle or null if the
     *         bundle does not export any packages.
    **/
    public ExportedPackage[] getExportedPackages(Bundle b)
    {
        return m_felix.getExportedPackages(b);
    }

    /**
     * The OSGi specification states that refreshing packages is
     * asynchronous; this method simply notifies the package admin
     * thread to do a refresh.
     * @param bundles array of bundles to refresh or <tt>null</tt> to refresh
     *                any bundles in need of refreshing.
    **/
    public synchronized void refreshPackages(Bundle[] bundles)
        throws SecurityException
    {
        // Save our request parameters and notify all.
        if (m_reqBundles == null)
        {
            m_reqBundles = new Bundle[][] { bundles };
        }
        else
        {
            Bundle[][] newReqBundles = new Bundle[m_reqBundles.length + 1][];
            System.arraycopy(m_reqBundles, 0,
                newReqBundles, 0, m_reqBundles.length);
            newReqBundles[m_reqBundles.length] = bundles;
            m_reqBundles = newReqBundles;
        }
        notifyAll();
    }

    /**
     * The OSGi specification states that package refreshes happen
     * asynchronously; this is the run() method for the package
     * refreshing thread.
    **/
    public void run()
    {
        // This thread loops forever, thus it should
        // be a daemon thread.
        Bundle[] bundles = null;
        while (true)
        {
            synchronized (this)
            {
                // Wait for a refresh request.
                while (m_reqBundles == null)
                {
                    try
                    {
                        wait();
                    }
                    catch (InterruptedException ex)
                    {
                    }
                }

                // Get the bundles parameter for the current
                // refresh request.
                if (m_reqBundles != null)
                {
                    bundles = m_reqBundles[0];
                }
            }

            // Perform refresh.
            m_felix.refreshPackages(bundles);

            // Remove the first request since it is now completed.
            synchronized (this)
            {
                if (m_reqBundles.length == 1)
                {
                    m_reqBundles = null;
                }
                else
                {
                    Bundle[][] newReqBundles = new Bundle[m_reqBundles.length - 1][];
                    System.arraycopy(m_reqBundles, 1,
                        newReqBundles, 0, m_reqBundles.length - 1);
                    m_reqBundles = newReqBundles;
                }
            }
        }
    }

    public ExportedPackage[] getExportedPackages(String name)
    {
        // TODO: Implement PackageAdmin.getExportedPackages()
        return null;
    }

    public boolean resolveBundles(Bundle[] bundles)
    {
        // TODO: Implement PackageAdmin.resolveBundles()
        return false;
    }

    public RequiredBundle[] getRequiredBundles(String symbolicName)
    {
        // TODO: Implement PackageAdmin.getRequiredBundles()
        return null;
    }

    public Bundle[] getBundles(String symbolicName, String versionRange)
    {
        // TODO: Implement PackageAdmin.getBundles()
        return null;
    }

    public Bundle[] getFragments(Bundle bundle)
    {
        // TODO: Implement PackageAdmin.getFragments()
        return null;
    }

    public Bundle[] getHosts(Bundle bundle)
    {
        // TODO: Implement PackageAdmin.getHosts()
        return null;
    }

    public Bundle getBundle(Class clazz)
    {
        // TODO: Implement PackageAdmin.getBundle()
        return null;
    }

    public int getBundleType(Bundle bundle)
    {
        // TODO: Implement PackageAdmin.getBundleType()
        return 0;
    }
}