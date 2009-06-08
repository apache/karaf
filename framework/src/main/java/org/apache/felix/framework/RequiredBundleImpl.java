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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.felix.framework.ModuleImpl;
import org.apache.felix.framework.util.Util;
import org.apache.felix.moduleloader.ICapability;
import org.apache.felix.moduleloader.IModule;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.RequiredBundle;

class RequiredBundleImpl implements RequiredBundle
{
    private final Felix m_felix;
    private final BundleImpl m_bundle;
    private volatile String m_toString = null;
    private volatile String m_versionString = null;

    public RequiredBundleImpl(Felix felix, BundleImpl bundle)
    {
        m_felix = felix;
        m_bundle = bundle;
    }

    public String getSymbolicName()
    {
        return m_bundle.getSymbolicName();
    }

    public Bundle getBundle()
    {
        return m_bundle;
    }

    public Bundle[] getRequiringBundles()
    {
        // Spec says to return null for stale bundles.
        if (m_bundle.isStale())
        {
            return null;
        }

        // We need to find all modules that require any of the modules
        // associated with this bundle.
        List moduleList = new ArrayList();
        // Loop through all of this bundle's modules.
        IModule[] modules = m_bundle.getModules();
        for (int modIdx = 0; (modules != null) && (modIdx < modules.length); modIdx++)
        {
            // For each of this bundle's modules, loop through all of the
            // modules that require it and add them to the module list.
            IModule[] dependents = ((ModuleImpl) modules[modIdx]).getDependentRequirers();
            for (int depIdx = 0; (dependents != null) && (depIdx < dependents.length); depIdx++)
            {
                moduleList.add(dependents[modIdx]);
            }
        }
        // Convert the list of dependent modules into a non-duplicated
        // array of bundles.
        Set bundleSet = new HashSet();
        for (int modIdx = 0; modIdx < moduleList.size(); modIdx++)
        {
            Bundle bundle = ((IModule) moduleList.get(modIdx)).getBundle();
            if (bundle != null)
            {
                bundleSet.add(bundle);
            }
        }
        return (Bundle[]) bundleSet.toArray(new Bundle[bundleSet.size()]);
    }

    public Version getVersion()
    {
        return m_bundle.getCurrentModule().getVersion();
    }

    public boolean isRemovalPending()
    {
        return m_bundle.isRemovalPending();
    }

    public String toString()
    {
        if (m_toString == null)
        {
            m_toString = m_bundle.getSymbolicName()
                + "; version=" + m_bundle.getCurrentModule().getVersion();
        }
        return m_toString;
    }
}