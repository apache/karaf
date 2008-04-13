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
package org.apache.felix.framework;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import org.osgi.framework.*;

class BundleImpl extends FelixBundle
{
    private final long m_id;
    private Felix m_felix = null;
    private BundleInfo m_info = null;

    protected BundleImpl(Felix felix, BundleInfo info)
    {
        m_felix = felix;
        m_info = info;
        m_id = info.getBundleId();
    }

    /* package private */ BundleInfo getInfo()
    {
        return m_info;
    }

    /*
     * Only used when refreshing a bundle.
    **/
    /* package private */ void setInfo(BundleInfo info)
    {
        m_info = info;
    }

    public BundleContext getBundleContext()
    {
// TODO: SECURITY - We need a security check here.
        return m_info.getBundleContext();
    }

    public long getBundleId()
    {
        return m_id;
    }

    public URL getEntry(String name)
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            try
            {
                ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                    AdminPermission.RESOURCE));
            }
            catch (Exception e)
            {
                return null; // No permission
            }
        }

        return m_felix.getBundleEntry(this, name);
    }

    public Enumeration getEntryPaths(String path)
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            try
            {
                ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                    AdminPermission.RESOURCE));
            }
            catch (Exception e)
            {
                return null; // No permission
            }
        }

        return m_felix.getBundleEntryPaths(this, path);
    }

    public Enumeration findEntries(String path, String filePattern, boolean recurse)
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            try
            {
                ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                    AdminPermission.RESOURCE));
            }
            catch (Exception e)
            {
                return null; // No permission
            }
        }

        return m_felix.findBundleEntries(this, path, filePattern, recurse);
    }

    public Dictionary getHeaders()
    {
        return getHeaders(Locale.getDefault().toString());
    }

    public Dictionary getHeaders(String locale)
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                AdminPermission.METADATA));
        }

        if (locale == null)
        {
            locale = Locale.getDefault().toString();
        }

        return m_felix.getBundleHeaders(this, locale);
    }

    public long getLastModified()
    {
        return m_info.getLastModified();
    }

    public String getLocation()
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                AdminPermission.METADATA));
        }
        return m_felix.getBundleLocation(this);
    }

    /**
     * Returns a URL to a named resource in the bundle.
     *
     * @return a URL to named resource, or null if not found.
    **/
    public URL getResource(String name)
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            try
            {
                ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                    AdminPermission.RESOURCE));
            }
            catch (Exception e)
            {
                return null; // No permission
            }
        }

        return m_felix.getBundleResource(this, name);
    }

    public Enumeration getResources(String name) throws IOException
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            try
            {
                ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                    AdminPermission.RESOURCE));
            }
            catch (Exception e)
            {
                return null; // No permission
            }
        }

        return m_felix.getBundleResources(this, name);
    }

    /**
     * Returns an array of service references corresponding to
     * the bundle's registered services.
     *
     * @return an array of service references or null.
    **/
    public ServiceReference[] getRegisteredServices()
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ServiceReference[] refs = m_felix.getBundleRegisteredServices(this);

            if (refs == null)
            {
                return refs;
            }

            List result = new ArrayList();

            for (int i = 0;i < refs.length;i++)
            {
                String[] objectClass = (String[]) refs[i].getProperty(
                    Constants.OBJECTCLASS);

                if (objectClass == null)
                {
                    continue;
                }

                for (int j = 0;j < objectClass.length;j++)
                {
                    try
                    {
                        ((SecurityManager) sm).checkPermission(new ServicePermission(
                            objectClass[j], ServicePermission.GET));

                        result.add(refs[i]);

                        break;
                    }
                    catch (Exception ex)
                    {
                        // Silently ignore.
                    }
                }
            }

            if (result.isEmpty())
            {
                return null;
            }

            return (ServiceReference[]) result.toArray(new ServiceReference[result.size()]);
        }
        else
        {
            return m_felix.getBundleRegisteredServices(this);
        }
    }

    public ServiceReference[] getServicesInUse()
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ServiceReference[] refs = m_felix.getBundleServicesInUse(this);

            if (refs == null)
            {
                return refs;
            }

            List result = new ArrayList();

            for (int i = 0;i < refs.length;i++)
            {
                String[] objectClass = (String[]) refs[i].getProperty(
                    Constants.OBJECTCLASS);

                if (objectClass == null)
                {
                    continue;
                }

                for (int j = 0;j < objectClass.length;j++)
                {
                    try
                    {
                        ((SecurityManager) sm).checkPermission(new ServicePermission(
                            objectClass[j], ServicePermission.GET));

                        result.add(refs[i]);

                        break;
                    }
                    catch (Exception ex)
                    {
                        // Silently ignore.
                    }
                }
            }

            if (result.isEmpty())
            {
                return null;
            }

            return (ServiceReference[]) result.toArray(new ServiceReference[result.size()]);
        }

        return m_felix.getBundleServicesInUse(this);
    }

    public int getState()
    {
        return m_info.getState();
    }

    public String getSymbolicName()
    {
        return m_felix.getBundleSymbolicName(this);
    }

    public boolean hasPermission(Object obj)
    {
        return m_felix.bundleHasPermission(this, obj);
    }

    public Class loadClass(String name) throws ClassNotFoundException
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            try
            {
                ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                    AdminPermission.CLASS));
            }
            catch (Exception e)
            {
                throw new ClassNotFoundException("No permission.", e);
            }
        }

        return m_felix.loadBundleClass(this, name);
    }

    public void start() throws BundleException
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                AdminPermission.EXECUTE));
        }

        m_felix.startBundle(this, true);
    }

    public void update() throws BundleException
    {
        update(null);
    }

    public void update(InputStream is) throws BundleException
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                AdminPermission.LIFECYCLE));
        }

        m_felix.updateBundle(this, is);
    }

    public void stop() throws BundleException
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                AdminPermission.EXECUTE));
        }

        m_felix.stopBundle(this, true);
    }

    public void uninstall() throws BundleException
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                AdminPermission.LIFECYCLE));
        }

        m_felix.uninstallBundle(this);
    }

    public String toString()
    {
        return m_felix.getBundleSymbolicName(this) + " [" + getBundleId() +"]";
    }

    Object getSignerMatcher()
    {
        return m_felix.getSignerMatcher(this);
    }
}