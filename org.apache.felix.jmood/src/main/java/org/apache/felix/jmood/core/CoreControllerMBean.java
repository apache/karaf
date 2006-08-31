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

package org.apache.felix.jmood.core;

import java.io.IOException;
import java.net.MalformedURLException;

import org.osgi.framework.BundleException;

/**
 * The CoreController mbean provides mechanisms to exert control over the
 * framework. For many operations, it provides a batch mechanism to avoid 
 * excessive message passing when interacting remotely. 
 *
 */
public interface CoreControllerMBean {

    public abstract void startBundle(String bundleSymbolicName)
            throws BundleException, BundleNotAvailableException;

    public abstract void batchStartBundles(String[] bundleSymbolicNames)
            throws BundleException, BundleNotAvailableException;

    public abstract void stopBundle(String bundleSymbolicName)
            throws BundleException, BundleNotAvailableException;

    public abstract void batchStopBundles(String[] bundleSymbolicNames)
            throws BundleException, BundleNotAvailableException;

    public abstract void updateBundle(String bundleSymbolicName)
            throws BundleException, BundleNotAvailableException;
    
    public abstract void uninstallBundle(String bundleSymbolicName)
			throws BundleNotAvailableException, BundleException;

    public abstract void batchUpdateBundles(String[] bundleSymbolicNames)
            throws BundleException, BundleNotAvailableException;

    public abstract void updateBundleFromUrl(String bundleSymbolicName,
            String url) throws BundleException, BundleNotAvailableException,
            MalformedURLException, IOException;

    public abstract void batchUpdateBundleFromUrl(String[] bundleSymbolicNames,
            String[] urls) throws BundleException, BundleNotAvailableException,
            MalformedURLException, IOException;

    public abstract void installBundle(String bundleLocation)
            throws BundleException;

    public abstract void batchInstallBundle(String[] bundleLocations)
            throws BundleException;

    public abstract void setBundleStartLevel(String bundleSymbolicName,
            int newlevel) throws BundleNotAvailableException,
            ServiceNotAvailableException;

    public abstract void batchSetBundleStartLevel(String[] bundleSymbolicNames,
            int[] newlevels);

    public abstract void refreshPackages(String[] bundleSymbolicNames)
            throws BundleNotAvailableException, ServiceNotAvailableException;

    public abstract void resolveBundles(String[] bundleSymbolicNames)
            throws BundleNotAvailableException, ServiceNotAvailableException;

    public abstract void setPlatformStartLevel(int newlevel) throws ServiceNotAvailableException;

    public abstract int getPlatformStartLevel() throws ServiceNotAvailableException;

    /**
     * 
     * @param newlevel
     * @throws ServiceNotAvailableException if StartLevel service not available, or RuntimeMBeanException that wraps an IllegalArgumentException, as specified 
     * by the Start Level service.  
     */
    public abstract void setInitialBundleStartLevel(int newlevel) throws ServiceNotAvailableException;

    public abstract int getInitialBundleStartLevel() throws ServiceNotAvailableException;

    public abstract void restartFramework() throws NotImplementedException;

    public abstract void shutdownFramework() throws NotImplementedException;

    public abstract void updateFramework() throws NotImplementedException;

}