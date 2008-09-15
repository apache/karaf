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
import java.util.Enumeration;

import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.Capability;
import org.apache.felix.moduleloader.*;

public class R4Wire implements IWire
{
    private final IModule m_importer;
    private final IRequirement m_requirement;
    private final IModule m_exporter;
    private final ICapability m_capability;

    public R4Wire(IModule importer, IRequirement requirement,
        IModule exporter, ICapability capability)
    {
        m_importer = importer;
        m_requirement = requirement;
        m_exporter = exporter;
        m_capability = capability;
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
    public boolean hasPackage(String pkgName)
    {
        return (m_capability.getNamespace().equals(ICapability.PACKAGE_NAMESPACE) &&
            m_capability.getProperties().get(ICapability.PACKAGE_PROPERTY).equals(pkgName));
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
        if (m_capability.getNamespace().equals(ICapability.PACKAGE_NAMESPACE) &&
            m_capability.getProperties().get(ICapability.PACKAGE_PROPERTY).equals(pkgName))
        {
            // If the importer and the exporter are the same, then
            // just ask for the class from the exporting module's
            // content directly.
            if (m_exporter == m_importer)
            {
                clazz = m_exporter.getContentLoader().getClass(name);
            }
            // Otherwise, check the include/exclude filters from the target
            // package to make sure that the class is actually visible. In
            // this case since the importing and exporting modules are different,
            // we delegate to the exporting module, rather than its content,
            // so that it can follow any internal wires it may have (e.g.,
            // if the package has multiple sources).
            else if (m_capability.getNamespace().equals(ICapability.PACKAGE_NAMESPACE)
                && ((Capability) m_capability).isIncluded(name))
            {
                clazz = m_exporter.getClass(name);
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
        if (m_capability.getNamespace().equals(ICapability.PACKAGE_NAMESPACE) &&
            m_capability.getProperties().get(ICapability.PACKAGE_PROPERTY).equals(pkgName))
        {
            // If the importer and the exporter are the same, then
            // just ask for the resource from the exporting module's
            // content directly.
            if (m_exporter == m_importer)
            {
                url = m_exporter.getContentLoader().getResource(name);
            }
            // Otherwise, delegate to the exporting module, rather than its
            // content, so that it can follow any internal wires it may have
            // (e.g., if the package has multiple sources).
            else
            {
                url = m_exporter.getResource(name);
            }

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
        if (m_capability.getNamespace().equals(ICapability.PACKAGE_NAMESPACE) &&
            m_capability.getProperties().get(ICapability.PACKAGE_PROPERTY).equals(pkgName))
        {
            urls = m_exporter.getContentLoader().getResources(name);

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

    public String toString()
    {
        if (m_capability.getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
        {
            return m_importer + " -> "
                + m_capability.getProperties().get(ICapability.PACKAGE_PROPERTY)
                + " -> " + m_exporter;
        }
        return m_importer + " -> " + m_capability + " -> " + m_exporter;
    }
}