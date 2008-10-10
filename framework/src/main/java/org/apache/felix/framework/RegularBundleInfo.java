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

import java.util.*;

import org.apache.felix.framework.cache.BundleArchive;
import org.apache.felix.framework.searchpolicy.ModuleDefinition;
import org.apache.felix.framework.util.manifestparser.ManifestParser;
import org.apache.felix.moduleloader.ICapability;
import org.apache.felix.moduleloader.IModule;
import org.osgi.framework.*;

class RegularBundleInfo extends BundleInfo
{
    private BundleArchive m_archive = null;
    private String m_cachedSymbolicName = null;
    private long m_cachedSymbolicNameTimestamp;

    protected RegularBundleInfo(Logger logger, IModule module, BundleArchive archive)
    {
        super(logger, module);
        m_archive = archive;
    }

    /**
     *  Returns the bundle archive associated with this bundle.
     * @return the bundle archive associated with this bundle.
    **/
// TODO: SYSTEMBUNDLE - Should this be on BundleInfo and just return in SystemBundleInfo or exception?
    public BundleArchive getArchive()
    {
        return m_archive;
    }

    public synchronized String getSymbolicName()
    {
        // If the bundle has been updated, clear the cached symbolic name.
        if (getLastModified() > m_cachedSymbolicNameTimestamp)
        {
            m_cachedSymbolicName = null;
            m_cachedSymbolicNameTimestamp = getLastModified();
            try
            {
                // TODO: FRAMEWORK - Rather than reparsing every time, I wonder if
                //       we should be caching this value some place.
                final ICapability moduleCap = ManifestParser.parseBundleSymbolicName(getCurrentHeader());
                if (moduleCap != null)
                {
                    m_cachedSymbolicName = (String) moduleCap.getProperties().get(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
                }
            }
            catch (BundleException ex)
            {
                // Return null.
            }
        }
        return m_cachedSymbolicName;
    }

    public long getBundleId()
    {
        try
        {
            return m_archive.getId();
        }
        catch (Exception ex)
        {
            getLogger().log(
                Logger.LOG_ERROR,
                "Error getting the identifier from bundle archive.",
                ex);
            return -1;
        }
    }

    public String getLocation()
    {
        try
        {
            return m_archive.getLocation();
        }
        catch (Exception ex)
        {
            getLogger().log(
                Logger.LOG_ERROR,
                "Error getting location from bundle archive.",
                ex);
            return null;
        }
    }

    public int getStartLevel(int defaultLevel)
    {
        try
        {
            return m_archive.getStartLevel();
        }
        catch (Exception ex)
        {
            getLogger().log(
                Logger.LOG_ERROR,
                "Error reading start level from bundle archive.",
                ex);
            return defaultLevel;
        }
    }

    public void setStartLevel(int i)
    {
        try
        {
            m_archive.setStartLevel(i);
        }
        catch (Exception ex)
        {
            getLogger().log(
                Logger.LOG_ERROR,
                "Error writing start level to bundle archive.",
                ex);
        }
    }

    public Map getCurrentHeader()
    {
        return ((ModuleDefinition) getCurrentModule().getDefinition()).getHeaders();
    }

    public long getLastModified()
    {
        try
        {
            return m_archive.getLastModified();
        }
        catch (Exception ex)
        {
            getLogger().log(
                Logger.LOG_ERROR,
                "Error reading last modification time from bundle archive.",
                ex);
            return 0;
        }
    }

    public void setLastModified(long l)
    {
        try
        {
            m_archive.setLastModified(l);
        }
        catch (Exception ex)
        {
            getLogger().log(
                Logger.LOG_ERROR,
                "Error writing last modification time to bundle archive.",
                ex);
        }
    }

    public int getPersistentState()
    {
        try
        {
            return m_archive.getPersistentState();
        }
        catch (Exception ex)
        {
            getLogger().log(
                Logger.LOG_ERROR,
                "Error reading persistent state from bundle archive.",
                ex);
            return Bundle.INSTALLED;
        }
    }

    public void setPersistentStateInactive()
    {
        try
        {
            m_archive.setPersistentState(Bundle.INSTALLED);
        }
        catch (Exception ex)
        {
            getLogger().log(Logger.LOG_ERROR,
                "Error writing persistent state to bundle archive.",
                ex);
        }
    }

    public void setPersistentStateActive()
    {
        try
        {
            m_archive.setPersistentState(Bundle.ACTIVE);
        }
        catch (Exception ex)
        {
            getLogger().log(
                Logger.LOG_ERROR,
                "Error writing persistent state to bundle archive.",
                ex);
        }
    }

    public void setPersistentStateUninstalled()
    {
        try
        {
            m_archive.setPersistentState(Bundle.UNINSTALLED);
        }
        catch (Exception ex)
        {
            getLogger().log(
                Logger.LOG_ERROR,
                "Error writing persistent state to bundle archive.",
                ex);
        }
    }
}