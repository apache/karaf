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
import org.apache.felix.framework.util.Util;
import org.apache.felix.moduleloader.ICapability;
import org.apache.felix.moduleloader.IModule;
import org.apache.felix.moduleloader.ModuleImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.RequiredBundle;

class RequiredBundleImpl implements RequiredBundle
{
    private final Felix m_felix;
    private final FelixBundle m_bundle;

    public RequiredBundleImpl(Felix felix, FelixBundle bundle)
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
        if (m_bundle.getInfo().isStale())
        {
            return null;
        }

        // We need to find all modules that require any of the modules
        // associated with this bundle.
        List moduleList = new ArrayList();
        // Loop through all of this bundle's modules.
        IModule[] modules = m_bundle.getInfo().getModules();
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
            long id = Util.getBundleIdFromModuleId(((IModule) moduleList.get(modIdx)).getId());
            Bundle bundle = m_felix.getBundle(id);
            if (bundle != null)
            {
                bundleSet.add(bundle);
            }
        }
        return (Bundle[]) bundleSet.toArray(new Bundle[bundleSet.size()]);
    }

    public Version getVersion()
    {
        ICapability[] caps = 
            Util.getCapabilityByNamespace(
                m_bundle.getInfo().getCurrentModule(), ICapability.MODULE_NAMESPACE);
        if ((caps != null) && (caps.length > 0))
        {
            return (Version) caps[0].getProperties().get(Constants.BUNDLE_VERSION_ATTRIBUTE);
        }
        return null;
    }

    public boolean isRemovalPending()
    {
        return m_bundle.getInfo().isRemovalPending();
    }
}