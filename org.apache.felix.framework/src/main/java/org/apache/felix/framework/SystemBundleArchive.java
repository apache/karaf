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

import java.io.File;
import java.util.Map;

import org.apache.felix.framework.cache.BundleArchive;
import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;

public class SystemBundleArchive implements BundleArchive
{
    private Map m_headerMap = null;

    public long getId()
    {
        return 0;
    }
    
    public String getLocation()
        throws Exception
    {
        return FelixConstants.SYSTEM_BUNDLE_LOCATION;
    }

    public int getPersistentState()
        throws Exception
    {
        return Bundle.ACTIVE;
    }

    public void setPersistentState(int state)
        throws Exception
    {
    }

    public int getStartLevel()
        throws Exception
    {
        return FelixConstants.SYSTEMBUNDLE_DEFAULT_STARTLEVEL;
    }

    public void setStartLevel(int level)
        throws Exception
    {
    }

    public File getDataFile(String fileName)
        throws Exception
    {
        return null;
    }

    public BundleActivator getActivator(ClassLoader loader)
        throws Exception
    {
        return null;
    }

    public void setActivator(Object obj)
        throws Exception
    {
    }

    public int getRevisionCount()
        throws Exception
    {
        return 1;
    }

    public Map getManifestHeader(int revision)
        throws Exception
    {
        return m_headerMap;
    }
    
    protected void setManifestHeader(Map headerMap)
    {
        m_headerMap = headerMap;
    }

    public String[] getClassPath(int revision)
        throws Exception
    {
        return null;
    }

    public String findLibrary(int revision, String libName)
        throws Exception
    {
        return null;
    }
}