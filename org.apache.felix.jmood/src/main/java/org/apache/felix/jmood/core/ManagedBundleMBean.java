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
import java.util.Hashtable;

import org.osgi.framework.BundleException;
public interface ManagedBundleMBean {

    public abstract long getBundleId();

    public abstract String[] getExportedPackages() throws ServiceNotAvailableException;

    public abstract String[] getFragments() throws ServiceNotAvailableException;

    public abstract Hashtable getHeaders();

    public abstract String[] getHosts() throws ServiceNotAvailableException;

    public abstract String[] getImportedPackages() throws ServiceNotAvailableException;

    public abstract long getLastModified();

    public abstract String[] getRegisteredServices();

    public abstract String[] getBundleDependencies() throws ServiceNotAvailableException;

    public abstract String[] getRequiringBundles() throws ServiceNotAvailableException;

    public abstract String[] getServicesInUse();

    public abstract int getStartLevel() throws ServiceNotAvailableException;

    public abstract String getState();

    public abstract String getSymbolicName();

    public abstract boolean isBundlePersistentlyStarted() throws ServiceNotAvailableException;

    public abstract boolean isFragment() throws ServiceNotAvailableException;

    public abstract boolean isRemovalPending() throws ServiceNotAvailableException;

    public abstract boolean isRequired() throws ServiceNotAvailableException;
    
    public abstract void start() throws BundleException;

    public abstract void stop() throws BundleException;
    
    public abstract void update() throws BundleException;
    
    public abstract void uninstall() throws BundleException;
    
    public abstract void updateFromUrl(String url) throws MalformedURLException, BundleException, IOException;
    
    public abstract void refreshBundle() throws BundleNotAvailableException, ServiceNotAvailableException;
    
    public abstract void resolveBundle() throws BundleNotAvailableException, ServiceNotAvailableException;

}