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

import org.apache.felix.framework.cache.*;
import java.io.File;
import java.io.InputStream;
import java.util.Map;

import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.StringMap;
import org.apache.felix.moduleloader.IContent;
import org.apache.felix.moduleloader.IModule;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;

/**
 * <p>
 * This class represents the bundle archive of the system bundle. It is a
 * special case that is mostly just an empty implementation, since the system
 * bundle is not a real archive.
 * </p>
**/
public class SystemBundleArchive extends BundleArchive
{
    private BundleCache m_cache;
    private Map m_headerMap = new StringMap(false);
    private BundleRevision m_revision;

    public SystemBundleArchive(BundleCache cache)
    {
        m_cache = cache;

        try
        {
            m_revision = new BundleRevision(null, null, null) {

                public Map getManifestHeader() throws Exception
                {
                    return m_headerMap;
                }

                public IContent getContent() throws Exception
                {
                    return null;
                }

                public IContent[] getContentPath() throws Exception
                {
                    return null;
                }

                public String findLibrary(String libName) throws Exception
                {
                    return null;
                }

                public void dispose() throws Exception
                {
                }
            };
        }
        catch (Exception ex)
        {
            // This should never happen.
        }
    }

    public long getId()
    {
        return 0;
    }

    public String getLocation() throws Exception
    {
        return FelixConstants.SYSTEM_BUNDLE_LOCATION;
    }

    public String getCurrentLocation() throws Exception
    {
        return null;
    }

    public void setCurrentLocation(String location) throws Exception
    {
    }

    public int getPersistentState() throws Exception
    {
        return Bundle.ACTIVE;
    }

    public void setPersistentState(int state) throws Exception
    {
    }

    public int getStartLevel() throws Exception
    {
        return FelixConstants.SYSTEMBUNDLE_DEFAULT_STARTLEVEL;
    }

    public void setStartLevel(int level) throws Exception
    {
    }

    public File getDataFile(String fileName) throws Exception
    {
        return m_cache.getSystemBundleDataFile(fileName);
    }

    public BundleActivator getActivator(IModule module)
        throws Exception
    {
        return null;
    }

    public void setActivator(Object obj) throws Exception
    {
    }

    public int getRevisionCount()
    {
        return 1;
    }

    public BundleRevision getRevision(int i)
    {
        return m_revision;
    }

    public void revise(String location, InputStream is)
        throws Exception
    {
    }

    public  void purge() throws Exception
    {
    }

    public void dispose() throws Exception
    {
    }

    public Map getManifestHeader(int revision)
    {
        return m_headerMap;
    }

    public void setManifestHeader(Map headerMap)
    {
        m_headerMap = headerMap;
    }
}