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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.framework.*;

class BundleImpl implements Bundle
{
    private Felix m_felix = null;
    private BundleInfo m_info = null;

    protected BundleImpl(Felix felix, BundleInfo info)
    {
        m_felix = felix;
        m_info = info;
    }

    Felix getFelix() // package protected
    {
        return m_felix;
    }

    BundleInfo getInfo() // package protected
    {
        return m_info;
    }

    void setInfo(BundleInfo info) // package protected
    {
        m_info = info;
    }

    /**
     * This method is a hack to make Felix compatible with Equinox'
     * Declarative Services implementation; this should be revisited
     * in the future.
     * @return the bundle context associated with this bundle.
    **/
    private BundleContext getContext()
    {
        BundleContext bc = m_info.getContext();
        if (bc == null)
        {
            m_info.setContext(new BundleContextImpl(m_felix, this));
        }
        return m_info.getContext();
    }

    public long getBundleId()
    {
        return m_info.getBundleId();
    }

    public URL getEntry(String name)
    {
        return m_felix.getBundleEntry(this, name);
    }

    public Enumeration getEntryPaths(String path)
    {
        return m_felix.getBundleEntryPaths(this, path);
    }

    public Enumeration findEntries(String path, String filePattern, boolean recurse)
    {
        return m_felix.findBundleEntries(this, path, filePattern, recurse);
    }

    public Dictionary getHeaders()
    {
        return m_felix.getBundleHeaders(this);
    }

    public long getLastModified()
    {
        return m_info.getLastModified();
    }

    public String getLocation()
    {
        return m_felix.getBundleLocation(this);
    }

    /**
     * Returns a URL to a named resource in the bundle.
     *
     * @return a URL to named resource, or null if not found.
    **/
    public URL getResource(String name)
    {
        return m_felix.getBundleResource(this, name);
    }

    /**
     * Returns an array of service references corresponding to
     * the bundle's registered services.
     *
     * @return an array of service references or null.
    **/
    public ServiceReference[] getRegisteredServices()
    {
        return m_felix.getBundleRegisteredServices(this);
    }

    public ServiceReference[] getServicesInUse()
    {
        return m_felix.getBundleServicesInUse(this);
    }

    public int getState()
    {
        return m_info.getState();
    }

    public String getSymbolicName()
    {
    	return (String) getHeaders().get(Constants.BUNDLE_SYMBOLICNAME);
    }

    public boolean hasPermission(Object obj)
    {
        return m_felix.bundleHasPermission(this, obj);
    }

    public Class loadClass(String name) throws ClassNotFoundException
    {
        return m_felix.loadBundleClass(this, name);
    }

    public void start() throws BundleException
    {
        m_felix.startBundle(this, true);
    }

    public void update() throws BundleException
    {
        update(null);
    }

    public void update(InputStream is) throws BundleException
    {
        m_felix.updateBundle(this, is);
    }

    public void stop() throws BundleException
    {
        m_felix.stopBundle(this, true);
    }

    public void uninstall() throws BundleException
    {
        m_felix.uninstallBundle(this);
    }

    public String toString()
    {
        return "[" + getBundleId() +"]";
    }

    //
    // PLACE FOLLOWING METHODS INTO PROPER LOCATION ONCE IMPLEMENTED.
    //

    public Dictionary getHeaders(String locale)
    {
        // TODO: Implement Bundle.getHeaders(String locale)
    	// Should be done after [#FELIX-27] resolution
        return null;
    }

    public Enumeration getResources(String name) throws IOException
    {
        // TODO: Implement Bundle.getResources()
        return null;
    }

    public boolean equals(Object obj)
    {
        if (obj instanceof BundleImpl)
        {
            return (((BundleImpl) obj).getInfo().getBundleId() == getInfo().getBundleId());
        }
        return false;
    }
}