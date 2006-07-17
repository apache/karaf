/*
 *   Copyright 2006 The Apache Software Foundation
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

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.ExportedPackage;

class ExportedPackageImpl implements ExportedPackage
{
    private Felix m_felix = null;
    private BundleImpl m_exporter = null;
    private String m_name = null;
    private Version m_version = null;
    private String m_toString = null;
    private String m_versionString = null;

    public ExportedPackageImpl(
        Felix felix, BundleImpl exporter, String name, Version version)
    {
        m_felix = felix;
        m_exporter = exporter;
        m_name = name;
        m_version = (version == null) ? Version.emptyVersion : version;
    }

    public Bundle getExportingBundle()
    {
        // If the package is stale, then return null per the spec.
        if (m_exporter.getInfo().isStale())
        {
            return null;
        }
        return m_exporter;
    }

    public Bundle[] getImportingBundles()
    {
        // If the package is stale, then return null per the spec.
        if (m_exporter.getInfo().isStale())
        {
            return null;
        }
        return m_felix.getImportingBundles(this);
    }

    public String getName()
    {
        return m_name;
    }

    public String getSpecificationVersion()
    {
        if (m_versionString == null)
        {
            m_versionString = m_version.toString();
        }
        return m_versionString;
    }

    public Version getVersion()
    {
        return m_version;
    }

    public boolean isRemovalPending()
    {
        return m_exporter.getInfo().isRemovalPending();
    }

    public String toString()
    {
        if (m_toString == null)
        {
            m_toString = m_name
                + "; version=" + getSpecificationVersion();
        }
        return m_toString;
    }
}