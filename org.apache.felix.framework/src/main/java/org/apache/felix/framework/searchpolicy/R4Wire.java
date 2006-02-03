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
package org.apache.felix.framework.searchpolicy;

import java.net.URL;

import org.apache.felix.framework.util.Util;
import org.apache.felix.moduleloader.*;

public class R4Wire
{
    private IModule m_importer = null;
    private IModule m_exporter = null;
    private R4Export m_export= null;

    public R4Wire(IModule importer, IModule exporter, R4Export export)
    {
        m_importer = importer;
        m_exporter = exporter;
        m_export = export;
    }

    public IModule getImportingModule()
    {
        return m_importer;
    }

    public IModule getExportingModule()
    {
        return m_exporter;
    }

    public R4Export getExport()
    {
        return m_export;
    }

    public Class getClass(String name) throws ClassNotFoundException
    {
        Class clazz = null;

        // Get the package of the target class.
        String pkgName = Util.getClassPackage(name);

        // Only check when the package of the target class is
        // the same as the package for the wire.
        if (m_export.getName().equals(pkgName))
        {
            // Before delegating to the exporting module to satisfy
            // the class load, we must check the include/exclude filters
            // from the target package to make sure that the class is
            // actually visible. However, if the exporting module is the
            // same as the requesting module, then filtering is not
            // performed since a module has complete access to itself.
            if ((m_exporter == m_importer) || m_export.isIncluded(name))
            {
                clazz = m_exporter.getContentLoader().getClass(name);
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

    public URL getResource(String name) throws ResourceNotFoundException
    {
        URL url = null;

        // Get the package of the target class.
        String pkgName = Util.getResourcePackage(name);

        // Only check when the package of the target resource is
        // the same as the package for the wire.
        if (m_export.getName().equals(pkgName))
        {
            url = m_exporter.getContentLoader().getResource(name);

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

    public String toString()
    {
        return m_importer + " -> " + m_export.getName() + " -> " + m_exporter;
    }
}