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

import org.apache.felix.framework.searchpolicy.R4Export;
import org.apache.felix.moduleloader.IModule;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.ExportedPackage;

class ExportedPackageImpl implements ExportedPackage
{
    private Felix m_felix = null;
    private BundleImpl m_exportingBundle = null;
    private IModule m_exportingModule = null;
    private R4Export m_export = null;
    private String m_toString = null;
    private String m_versionString = null;

    public ExportedPackageImpl(
        Felix felix, BundleImpl exporter, IModule module, R4Export export)
    {
        m_felix = felix;
        m_exportingBundle = exporter;
        m_exportingModule = module;
        m_export = export;
    }

    public Bundle getExportingBundle()
    {
        // If the package is stale, then return null per the spec.
        if (m_exportingBundle.getInfo().isStale())
        {
            return null;
        }
        return m_exportingBundle;
    }

    public Bundle[] getImportingBundles()
    {
        // If the package is stale, then return null per the spec.
        if (m_exportingBundle.getInfo().isStale())
        {
            return null;
        }
        return m_felix.getImportingBundles(this);
    }

    public String getName()
    {
        return m_export.getName();
    }

    public String getSpecificationVersion()
    {
        if (m_versionString == null)
        {
            m_versionString = (m_export.getVersion() == null)
                ? Version.emptyVersion.toString()
                : m_export.getVersion().toString();
        }
        return m_versionString;
    }

    public Version getVersion()
    {
        return (m_export.getVersion() == null)
            ? Version.emptyVersion
            : m_export.getVersion();
    }

    public boolean isRemovalPending()
    {
        return m_exportingModule.isRemovalPending();
    }

    public String toString()
    {
        if (m_toString == null)
        {
            m_toString = m_export.getName()
                + "; version=" + getSpecificationVersion();
        }
        return m_toString;
    }
}