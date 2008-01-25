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
package org.apache.felix.framework.searchpolicy;

import java.net.URL;
import java.util.*;

import org.apache.felix.framework.searchpolicy.R4SearchPolicyCore.ResolvedPackage;
import org.apache.felix.framework.searchpolicy.R4SearchPolicyCore.PackageSource;
import org.apache.felix.framework.util.CompoundEnumeration;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.Capability;
import org.apache.felix.moduleloader.*;

public class R4WireModule implements IWire
{
    private IModule m_importer = null;
    private IRequirement m_requirement = null;
    private IModule m_exporter = null;
    private ICapability m_capability = null;
    private Map m_pkgMap = null;
    
    public R4WireModule(IModule importer, IRequirement requirement,
        IModule exporter, ICapability capability, Map pkgMap)
    {
        m_importer = importer;
        m_requirement = requirement;
        m_exporter = exporter;
        m_capability = capability;
        m_pkgMap = pkgMap;
    }
    
    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getImporter()
     */
    public IModule getImporter()
    {
        return m_importer;
    }
    
    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getRequirement()
     */
    public IRequirement getRequirement()
    {
        return m_requirement;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getExporter()
     */
    public IModule getExporter()
    {
        return m_exporter;
    }
    
    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getCapability()
     */
    public ICapability getCapability()
    {
        return m_capability;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getClass(java.lang.String)
     */
    public Class getClass(String name) throws ClassNotFoundException
    {
        // Get the package of the target class.
        String pkgName = Util.getClassPackage(name);

        ResolvedPackage rp = (ResolvedPackage) m_pkgMap.get(pkgName);
        if (rp != null)
        {
            for (int srcIdx = 0; srcIdx < rp.m_sourceList.size(); srcIdx++)
            {
                PackageSource ps = (PackageSource) rp.m_sourceList.get(srcIdx);
                if ((ps.m_module == m_importer) ||
                    ((ps.m_capability instanceof Capability) &&
                    ((Capability) ps.m_capability).isIncluded(name)))
                {
                    Class clazz = ps.m_module.getContentLoader().getClass(name);
                    if (clazz != null)
                    {
                        return clazz;
                    }
                }
            }
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getResource(java.lang.String)
     */
    public URL getResource(String name) throws ResourceNotFoundException
    {
        // Get the package of the target class.
        String pkgName = Util.getResourcePackage(name);

        ResolvedPackage rp = (ResolvedPackage) m_pkgMap.get(pkgName);
        if (rp != null)
        {
            for (int srcIdx = 0; srcIdx < rp.m_sourceList.size(); srcIdx++)
            {
                PackageSource ps = (PackageSource) rp.m_sourceList.get(srcIdx);
                URL url = ps.m_module.getContentLoader().getResource(name);
                if (url != null)
                {
                    return url;
                }
            }

            // Don't throw ResourceNotFoundException because module
            // dependencies support split packages.
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getResources(java.lang.String)
     */
    public Enumeration getResources(String name) throws ResourceNotFoundException
    {
        // List to hold all enumerations from all package sources.
        List enums = new ArrayList();

        // Get the package of the target class.
        String pkgName = Util.getResourcePackage(name);

        // See if we have a resolved package for the resource's package.
        // If so, loop through all package sources and aggregate any
        // matching resource enumerations.
        ResolvedPackage rp = (ResolvedPackage) m_pkgMap.get(pkgName);
        if (rp != null)
        {
            for (int srcIdx = 0; srcIdx < rp.m_sourceList.size(); srcIdx++)
            {
                PackageSource ps = (PackageSource) rp.m_sourceList.get(srcIdx);
                Enumeration urls = ps.m_module.getContentLoader().getResources(name);
                if (urls != null)
                {
                    enums.add(urls);
                }
            }

            // Don't throw ResourceNotFoundException because module
            // dependencies support split packages.
        }

        return (enums.size() == 0)
            ? null
            : new CompoundEnumeration(
                (Enumeration[]) enums.toArray(new Enumeration[enums.size()]));

    }

    public String toString()
    {
        return m_importer + " -> " + m_capability + " -> " + m_exporter;
    }
}