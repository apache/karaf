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

import java.util.Map;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.StringMap;
import org.apache.felix.moduleloader.IModule;
import org.osgi.framework.Bundle;

class SystemBundleInfo extends BundleInfo
{
    private Map m_headerMap = new StringMap(false);
    private int m_startLevel = FelixConstants.SYSTEMBUNDLE_DEFAULT_STARTLEVEL;

    SystemBundleInfo(Logger logger, IModule module)
    {
        super(logger, module);
    }

    public String getSymbolicName()
    {
        return FelixConstants.SYSTEM_BUNDLE_SYMBOLICNAME;
    }

    public long getBundleId()
    {
        return 0;
    }

    public String getLocation()
    {
        return FelixConstants.SYSTEM_BUNDLE_LOCATION;
    }

    public synchronized int getStartLevel(int defaultLevel)
    {
        return m_startLevel;
    }

    public synchronized void setStartLevel(int i)
    {
        m_startLevel = i;
    }

    public Map getCurrentHeader()
    {
        return m_headerMap;
    }

    public long getLastModified()
    {
        return 0;
    }

    public void setLastModified(long l)
    {
        // Ignore.
    }

    public int getPersistentState()
    {
        return Bundle.ACTIVE;
    }

    public void setPersistentStateInactive()
    {
        // Ignore.
    }

    public void setPersistentStateActive()
    {
        // Ignore.
    }

    public void setPersistentStateUninstalled()
    {
        // Ignore.
    }
}