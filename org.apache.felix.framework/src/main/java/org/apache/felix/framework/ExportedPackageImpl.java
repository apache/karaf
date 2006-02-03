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
        m_version = version;
    }

    public Bundle getExportingBundle()
    {
        // If remove is pending due to a bundle update, then
        // return null per the spec.
        if (m_exporter.getInfo().isRemovalPending())
        {
            return null;
        }
        return m_exporter;
    }

    /**
     * Returns the exporting bundle whether the package is state or
     * not. This is called internally to get access to the exporting
     * bundle during a refresh operation, which is not possible using
     * <tt>getExportingBundle</tt> since the specification says that
     * method must return <tt>null</tt> for stale packages.
     * @return the exporting bundle for the package.
    **/
    protected Bundle getExportingBundleInternal()
    {
        return m_exporter;
    }
    
    public Bundle[] getImportingBundles()
    {
        // If remove is pending due to a bundle update, then
        // return null per the spec.
        if (m_exporter.getInfo().isRemovalPending())
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
            if (m_version == null)
            {
                m_versionString = "0.0.0";
            }
            else
            {
                m_versionString = m_version.toString();
            }
        }
        return m_versionString;
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