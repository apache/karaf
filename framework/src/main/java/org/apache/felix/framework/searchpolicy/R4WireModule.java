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

import org.apache.felix.framework.util.Util;
import org.apache.felix.moduleloader.*;

public class R4WireModule implements IWire
{
    private final IModule m_importer;
    private final IRequirement m_requirement;
    private final IModule m_exporter;
    private final ICapability m_capability;
    private final Map m_pkgMap;

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
     * @see org.apache.felix.framework.searchpolicy.IWire#hasPackage(java.lang.String)
     */
    public boolean hasPackage(String pkgName)
    {
        return (m_pkgMap.get(pkgName) != null);
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
            try
            {
                Class clazz = m_exporter.getClassByDelegation(name);
                if (clazz != null)
                {
                    return clazz;
                }
            }
            catch (ClassNotFoundException ex)
            {
                // Do not throw the exception here, since we want
                // to continue search other package sources and
                // ultimately the module's own content.
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
            URL url = m_exporter.getResourceByDelegation(name);
            if (url != null)
            {
                return url;
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
        // Get the package of the target class.
        String pkgName = Util.getResourcePackage(name);

        // See if we have a resolved package for the resource's package.
        // If so, loop through all package sources and aggregate any
        // matching resource enumerations.
        ResolvedPackage rp = (ResolvedPackage) m_pkgMap.get(pkgName);
        if (rp != null)
        {
            Enumeration urls = m_exporter.getResourcesByDelegation(name);
            if (urls != null)
            {
                return urls;
            }

            // Don't throw ResourceNotFoundException because module
            // dependencies support split packages.
        }

        return null;
    }

    public String toString()
    {
        return m_importer + " -> " + m_capability + " -> " + m_exporter;
    }
}