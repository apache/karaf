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
import org.apache.felix.framework.capabilityset.Capability;
import org.apache.felix.framework.capabilityset.Requirement;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.CapabilityImpl;

public class WireImpl implements Wire
{
    private final Module m_importer;
    private final Requirement m_req;
    private final Module m_exporter;
    private final Capability m_cap;

    public WireImpl(Module importer, Requirement ip, Module exporter, Capability ep)
    {
        m_importer = importer;
        m_req = ip;
        m_exporter = exporter;
        m_cap = ep;
    }

    public Module getImporter()
    {
        return m_importer;
    }

    public Requirement getRequirement()
    {
        return m_req;
    }

    public Module getExporter()
    {
        return m_exporter;
    }

    public Capability getCapability()
    {
        return m_cap;
    }

    public String toString()
    {
        return m_req + " -> " + "[" + m_exporter + "]";
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getClass(java.lang.String)
     */
    public boolean hasPackage(String pkgName)
    {
        return (m_cap.getNamespace().equals(Capability.PACKAGE_NAMESPACE) &&
            m_cap.getAttribute(Capability.PACKAGE_ATTR).getValue().equals(pkgName));
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getClass(java.lang.String)
     */
    public Class getClass(String name) throws ClassNotFoundException
    {
        Class clazz = null;

        // Get the package of the target class.
        String pkgName = Util.getClassPackage(name);

        // Only check when the package of the target class is
        // the same as the package for the wire.
        if (m_cap.getNamespace().equals(Capability.PACKAGE_NAMESPACE) &&
            m_cap.getAttribute(Capability.PACKAGE_ATTR).getValue().equals(pkgName))
        {
            // Check the include/exclude filters from the target package
            // to make sure that the class is actually visible. We delegate
            // to the exporting module, rather than its content, so it can
            // it can follow any internal wires it may have (e.g., if the
            // package has multiple sources).
            if (((CapabilityImpl) m_cap).isIncluded(name))
            {
                clazz = m_exporter.getClassByDelegation(name);
            }

            // If no class was found, then we must throw an exception
            // since the exporter for this package did not contain the
            // requested class.
            if (clazz == null)
            {
                throw new ClassNotFoundException(name);
            }
        }

        return clazz;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getResource(java.lang.String)
     */
    public URL getResource(String name) throws ResourceNotFoundException
    {
        URL url = null;

        // Get the package of the target class.
        String pkgName = Util.getResourcePackage(name);

        // Only check when the package of the target resource is
        // the same as the package for the wire.
        if (m_cap.getNamespace().equals(Capability.PACKAGE_NAMESPACE) &&
            m_cap.getAttribute(Capability.PACKAGE_ATTR).getValue().equals(pkgName))
        {
            // Delegate to the exporting module, rather than its
            // content, so that it can follow any internal wires it may have
            // (e.g., if the package has multiple sources).
            url = m_exporter.getResourceByDelegation(name);

            // If no resource was found, then we must throw an exception
            // since the exporter for this package did not contain the
            // requested class.
            if (url == null)
            {
                throw new ResourceNotFoundException(name);
            }
        }

        return url;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getResources(java.lang.String)
     */
    public Enumeration getResources(String name) throws ResourceNotFoundException
    {
        Enumeration urls = null;

        // Get the package of the target class.
        String pkgName = Util.getResourcePackage(name);

        // Only check when the package of the target resource is
        // the same as the package for the wire.
        if (m_cap.getNamespace().equals(Capability.PACKAGE_NAMESPACE) &&
            m_cap.getAttribute(Capability.PACKAGE_ATTR).getValue().equals(pkgName))
        {
            urls = m_exporter.getResourcesByDelegation(name);

            // If no resource was found, then we must throw an exception
            // since the exporter for this package did not contain the
            // requested class.
            if (urls == null)
            {
                throw new ResourceNotFoundException(name);
            }
        }

        return urls;
    }
}