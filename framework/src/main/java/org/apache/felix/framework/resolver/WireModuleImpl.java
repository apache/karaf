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
package org.apache.felix.framework.resolver;

import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import org.apache.felix.framework.capabilityset.Capability;
import org.apache.felix.framework.capabilityset.Requirement;
import org.apache.felix.framework.util.Util;

public class WireModuleImpl implements Wire
{
    private final Module m_importer;
    private final Requirement m_req;
    private final Module m_exporter;
    private final Capability m_cap;
    private final List<String> m_packages;

    public WireModuleImpl(Module importer, Requirement requirement,
        Module exporter, Capability capability, List<String> packages)
    {
        m_importer = importer;
        m_req = requirement;
        m_exporter = exporter;
        m_cap = capability;
        m_packages = packages;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getImporter()
     */
    public Module getImporter()
    {
        return m_importer;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getRequirement()
     */
    public Requirement getRequirement()
    {
        return m_req;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getExporter()
     */
    public Module getExporter()
    {
        return m_exporter;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getCapability()
     */
    public Capability getCapability()
    {
        return m_cap;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#hasPackage(java.lang.String)
     */
    public boolean hasPackage(String pkgName)
    {
        return m_packages.contains(pkgName);
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getClass(java.lang.String)
     */
    public Class getClass(String name) throws ClassNotFoundException
    {
        // Get the package of the target class.
        String pkgName = Util.getClassPackage(name);
        if (m_packages.contains(pkgName))
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
        if (m_packages.contains(pkgName))
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
        if (m_packages.contains(pkgName))
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
        return m_req + " -> " + "[" + m_exporter + "]";
    }
}