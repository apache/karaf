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
package org.apache.felix.framework.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.searchpolicy.*;
import org.osgi.framework.*;

public class ManifestParser
{
    private Logger m_logger = null;
    private Map m_headerMap = null;
    private R4Export[] m_exports = null;
    private R4Import[] m_imports = null;
    private R4Import[] m_dynamics = null;
    private R4LibraryHeader[] m_libraryHeaders = null;

    public ManifestParser(Logger logger, Map headerMap) throws BundleException
    {
        m_logger = logger;
        m_headerMap = headerMap;

        // Verify that only manifest version 2 is specified.
        String manifestVersion = get(Constants.BUNDLE_MANIFESTVERSION);
        if ((manifestVersion != null) && !manifestVersion.equals("2"))
        {
            throw new BundleException(
                "Unknown 'Bundle-ManifestVersion' value: " + manifestVersion);
        }

        // Verify bundle version syntax.
        Version.parseVersion(get(Constants.BUNDLE_VERSION));

        // Create map to check for duplicate imports/exports.
        Map dupeMap = new HashMap();

        //
        // Parse Export-Package.
        //

        // Get export packages from bundle manifest.
        R4Package[] pkgs = R4Package.parseImportOrExportHeader(
            (String) headerMap.get(Constants.EXPORT_PACKAGE));

        // Create non-duplicated export array.
        dupeMap.clear();
        for (int i = 0; i < pkgs.length; i++)
        {
            if (dupeMap.get(pkgs[i].getName()) == null)
            {
                // Verify that java.* packages are not exported.
                if (pkgs[i].getName().startsWith("java."))
                {
                    throw new BundleException(
                        "Exporting java.* packages not allowed: " + pkgs[i].getName());
                }
                dupeMap.put(pkgs[i].getName(), new R4Export(pkgs[i]));
            }
            else
            {
                // TODO: FRAMEWORK - Exports can be duplicated, so fix this.
                m_logger.log(Logger.LOG_WARNING,
                    "Duplicate export - " + pkgs[i].getName());
            }
        }
        m_exports = (R4Export[]) dupeMap.values().toArray(new R4Export[dupeMap.size()]);

        //
        // Parse Import-Package.
        //

        // Get import packages from bundle manifest.
        pkgs = R4Package.parseImportOrExportHeader(
            (String) headerMap.get(Constants.IMPORT_PACKAGE));

        // Create non-duplicated import array.
        dupeMap.clear();
        for (int i = 0; i < pkgs.length; i++)
        {
            if (dupeMap.get(pkgs[i].getName()) == null)
            {
                // Verify that java.* packages are not imported.
                if (pkgs[i].getName().startsWith("java."))
                {
                    throw new BundleException(
                        "Importing java.* packages not allowed: " + pkgs[i].getName());
                }
                dupeMap.put(pkgs[i].getName(), new R4Import(pkgs[i]));
            }
            else
            {
                throw new BundleException(
                    "Duplicate import - " + pkgs[i].getName());
            }
        }
        m_imports = (R4Import[]) dupeMap.values().toArray(new R4Import[dupeMap.size()]);

        //
        // Parse DynamicImport-Package.
        //

        // Get dynamic import packages from bundle manifest.
        pkgs = R4Package.parseImportOrExportHeader(
            (String) headerMap.get(Constants.DYNAMICIMPORT_PACKAGE));

        // Dynamic imports can have duplicates, so just create an array.
        m_dynamics = new R4Import[pkgs.length];
        for (int i = 0; i < pkgs.length; i++)
        {
            m_dynamics[i] = new R4Import(pkgs[i]);
        }

        //
        // Parse Bundle-NativeCode.
        //

        // Get native library entry names for module library sources.
        m_libraryHeaders =
            Util.parseLibraryStrings(
                m_logger,
                Util.parseDelimitedString(get(Constants.BUNDLE_NATIVECODE), ","));

        // Do final checks and normalization of manifest.
        if (getVersion().equals("2"))
        {
            checkAndNormalizeR4();
        }
        else
        {
            checkAndNormalizeR3();
        }
    }

    public String get(String key)
    {
        return (String) m_headerMap.get(key);
    }

    public String getVersion()
    {
        String manifestVersion = get(Constants.BUNDLE_MANIFESTVERSION);
        return (manifestVersion == null) ? "1" : manifestVersion;
    }

    public R4Export[] getExports()
    {
        return m_exports;
    }

    public R4Import[] getImports()
    {
        return m_imports;
    }

    public R4Import[] getDynamicImports()
    {
        return m_dynamics;
    }

    public R4LibraryHeader[] getLibraryHeaders()
    {
        return m_libraryHeaders;
    }

    public R4Library[] getLibraries(
        BundleCache cache, long id, int revision, String osName, String processor)
    {
        R4Library[] libraries = new R4Library[m_libraryHeaders.length];
        for (int i = 0; i < libraries.length; i++)
        {
            libraries[i] = new R4Library(
                m_logger, cache, id, revision, osName, processor, m_libraryHeaders[i]);
        }
        return libraries;
    }

    private void checkAndNormalizeR3() throws BundleException
    {
        // Check to make sure that R3 bundles have only specified
        // the 'specification-version' attribute and no directives
        // on their exports.
        for (int i = 0; (m_exports != null) && (i < m_exports.length); i++)
        {
            if (m_exports[i].getDirectives().length != 0)
            {
                throw new BundleException("R3 exports cannot contain directives.");
            }
            // NOTE: This is checking for "version" rather than "specification-version"
            // because the package class normalizes to "version" to avoid having
            // future special cases. This could be changed if more strict behavior
            // is required.
            if ((m_exports[i].getAttributes().length > 1) ||
                ((m_exports[i].getAttributes().length == 1) &&
                    (!m_exports[i].getAttributes()[0].getName().equals(Constants.VERSION_ATTRIBUTE))))
            {
                throw new BundleException(
                    "Export does not conform to R3 syntax: " + m_exports[i]);
            }
        }
        
        // Check to make sure that R3 bundles have only specified
        // the 'specification-version' attribute and no directives
        // on their imports.
        for (int i = 0; (m_imports != null) && (i < m_imports.length); i++)
        {
            if (m_imports[i].getDirectives().length != 0)
            {
                throw new BundleException("R3 imports cannot contain directives.");
            }
            // NOTE: This is checking for "version" rather than "specification-version"
            // because the package class normalizes to "version" to avoid having
            // future special cases. This could be changed if more strict behavior
            // is required.
            if ((m_imports[i].getVersionHigh() != null) ||
                (m_imports[i].getAttributes().length > 1) ||
                ((m_imports[i].getAttributes().length == 1) &&
                    (!m_imports[i].getAttributes()[0].getName().equals(Constants.VERSION_ATTRIBUTE))))
            {
                throw new BundleException(
                    "Import does not conform to R3 syntax: " + m_imports[i]);
            }
        }

        // Since all R3 exports imply an import, add a corresponding
        // import for each existing export. Create non-duplicated import array.
        Map map =  new HashMap();
        // Add existing imports.
        for (int i = 0; i < m_imports.length; i++)
        {
            map.put(m_imports[i].getName(), m_imports[i]);
        }
        // Add import for each export.
        for (int i = 0; i < m_exports.length; i++)
        {
            if (map.get(m_exports[i].getName()) == null)
            {
                map.put(m_exports[i].getName(), new R4Import(m_exports[i]));
            }
        }
        m_imports =
            (R4Import[]) map.values().toArray(new R4Import[map.size()]);

        // Add a "uses" directive onto each export of R3 bundles
        // that references every other import (which will include
        // exports, since export implies import); this is
        // necessary since R3 bundles assumed a single class space,
        // but R4 allows for multiple class spaces.
        String usesValue = "";
        for (int i = 0; (m_imports != null) && (i < m_imports.length); i++)
        {
            usesValue = usesValue
                + ((usesValue.length() > 0) ? "," : "")
                + m_imports[i].getName();
        }
        R4Directive uses = new R4Directive(
            Constants.USES_DIRECTIVE, usesValue);
        for (int i = 0; (m_exports != null) && (i < m_exports.length); i++)
        {
            m_exports[i] = new R4Export(
                m_exports[i].getName(),
                new R4Directive[] { uses },
                m_exports[i].getAttributes());
        }

        // Check to make sure that R3 bundles have no attributes or
        // directives on their dynamic imports.
        for (int i = 0; (m_dynamics != null) && (i < m_dynamics.length); i++)
        {
            if (m_dynamics[i].getDirectives().length != 0)
            {
                throw new BundleException("R3 dynamic imports cannot contain directives.");
            }
            if (m_dynamics[i].getAttributes().length != 0)
            {
                throw new BundleException("R3 dynamic imports cannot contain attributes.");
            }
        }
    }

    private void checkAndNormalizeR4() throws BundleException
    {
        // Verify that bundle symbolic name is specified.
        String symName = get(Constants.BUNDLE_SYMBOLICNAME);
        if (symName == null)
        {
            throw new BundleException("R4 bundle manifests must include bundle symbolic name.");
        }

        // Verify that there are no duplicate directives.
        Map map = new HashMap();
        for (int i = 0; (m_exports != null) && (i < m_exports.length); i++)
        {
            String targetVer = get(Constants.BUNDLE_VERSION);
            targetVer = (targetVer == null) ? "0.0.0" : targetVer;

            R4Attribute[] attrs = m_exports[i].getAttributes();
            R4Attribute[] newAttrs = new R4Attribute[attrs.length + 2];
            System.arraycopy(attrs, 0, newAttrs, 0, attrs.length);
            newAttrs[attrs.length] = new R4Attribute(
                Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, symName, false);
            newAttrs[attrs.length + 1] = new R4Attribute(
                Constants.BUNDLE_VERSION_ATTRIBUTE, targetVer, false);
            m_exports[i] = new R4Export(
                m_exports[i].getName(), m_exports[i].getDirectives(), newAttrs);
        }

        // Need to add symbolic name and bundle version to all R4 exports.
        for (int i = 0; (m_exports != null) && (i < m_exports.length); i++)
        {
            String targetVer = get(Constants.BUNDLE_VERSION);
            targetVer = (targetVer == null) ? "0.0.0" : targetVer;

            R4Attribute[] attrs = m_exports[i].getAttributes();
            R4Attribute[] newAttrs = new R4Attribute[attrs.length + 2];
            System.arraycopy(attrs, 0, newAttrs, 0, attrs.length);
            newAttrs[attrs.length] = new R4Attribute(
                Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, symName, false);
            newAttrs[attrs.length + 1] = new R4Attribute(
                Constants.BUNDLE_VERSION_ATTRIBUTE, targetVer, false);
            m_exports[i] = new R4Export(
                m_exports[i].getName(), m_exports[i].getDirectives(), newAttrs);
        }
    }
}