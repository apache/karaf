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
package org.apache.felix.framework.util.manifestparser;

import java.util.*;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.cache.BundleRevision;
import org.apache.felix.framework.util.*;
import org.apache.felix.moduleloader.ICapability;
import org.apache.felix.moduleloader.IRequirement;
import org.osgi.framework.*;

public class ManifestParser
{
    private Logger m_logger = null;
    private PropertyResolver m_config = null;
    private Map m_headerMap = null;
    private String m_bundleSymbolicName = null;
    private Version m_bundleVersion = null;
    private ICapability[] m_capabilities = null;
    private IRequirement[] m_requirements = null;
    private IRequirement[] m_dynamicReqs = null;
    private R4Export[] m_exports = null;
    private R4Import[] m_imports = null;
    private R4Import[] m_dynamics = null;
    private R4LibraryClause[] m_libraryHeaders = null;
    private boolean m_libraryHeadersOptional = false;

    public ManifestParser(Logger logger, PropertyResolver config, Map headerMap)
        throws BundleException
    {
        m_logger = logger;
        m_config = config;
        m_headerMap = headerMap;

        // Verify that only manifest version 2 is specified.
        String manifestVersion = (String) m_headerMap.get(Constants.BUNDLE_MANIFESTVERSION);
        if ((manifestVersion != null) && !manifestVersion.equals("2"))
        {
            throw new BundleException(
                "Unknown 'Bundle-ManifestVersion' value: " + manifestVersion);
        }

        // Create map to check for duplicate imports/exports
        // and lists to hold capabilities and requirements.
        List capList = new ArrayList();
        List reqList = new ArrayList();
        Map dupeMap = new HashMap();

        //
        // Get bundle version.
        //

        if (m_headerMap.get(Constants.BUNDLE_VERSION) != null)
        {
            try
            {
                m_bundleVersion = Version.parseVersion((String) m_headerMap.get(Constants.BUNDLE_VERSION));
            }
            catch (RuntimeException ex)
            {
                // R4 bundle versions must parse, R3 bundle version may not.
                if (getManifestVersion().equals("2"))
                {
                    throw ex;
                }
                m_bundleVersion = Version.emptyVersion;
            }
        }

        //
        // Parse bundle symbolic name.
        //

        Object[][][] clauses = parseStandardHeader(
            (String) headerMap.get(Constants.BUNDLE_SYMBOLICNAME));
        if (clauses.length > 0)
        {
            if (clauses.length > 1)
            {
                throw new BundleException(
                    "Cannot have multiple symbolic names: "
                        + headerMap.get(Constants.BUNDLE_SYMBOLICNAME));
            }
            else if (clauses[0][CLAUSE_PATHS_INDEX].length > 1)
            {
                throw new BundleException(
                    "Cannot have multiple symbolic names: "
                        + headerMap.get(Constants.BUNDLE_SYMBOLICNAME));
            }
            m_bundleSymbolicName = (String) clauses[0][CLAUSE_PATHS_INDEX][0];
            R4Attribute[] attrs = new R4Attribute[2];
            attrs[0] = new R4Attribute(
                Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, m_bundleSymbolicName, false);
            attrs[1] = new R4Attribute(
                Constants.BUNDLE_VERSION_ATTRIBUTE, m_bundleVersion, false);
            capList.add(new Capability(ICapability.MODULE_NAMESPACE, null, attrs));
        }

        //
        // Parse Export-Package.
        //

        // Get export packages from bundle manifest.
        m_exports = (R4Export[]) parseImportExportHeader(
            (String) headerMap.get(Constants.EXPORT_PACKAGE), true);

        // Get export packages from bundle manifest.
        ICapability[] exportCaps = parseExportHeader(
            (String) headerMap.get(Constants.EXPORT_PACKAGE));
//System.out.println("PARSED EXPORT CAPABILITIES:");
//for (int capIdx = 0; capIdx < exportCaps.length; capIdx++)
//{
//    System.out.println(exportCaps[capIdx]);
//}

        // Create non-duplicated export array.
        dupeMap.clear();
        for (int pkgIdx = 0; pkgIdx < m_exports.length; pkgIdx++)
        {
            // Verify that the named package has not already been declared.
            if (dupeMap.get(m_exports[pkgIdx].getName()) == null)
            {
                // Verify that java.* packages are not exported.
                if (m_exports[pkgIdx].getName().startsWith("java."))
                {
                    throw new BundleException(
                        "Exporting java.* packages not allowed: "
                        + m_exports[pkgIdx].getName());
                }
                dupeMap.put(m_exports[pkgIdx].getName(), m_exports[pkgIdx]);
            }
            else
            {
                // TODO: FRAMEWORK - Exports can be duplicated, so fix this.
                m_logger.log(Logger.LOG_WARNING, "Duplicate export - "
                    + m_exports[pkgIdx].getName());
            }
        }
        // This following line won't be necessary once duplicate exports are supported.
        m_exports = (R4Export[]) dupeMap.values().toArray(new R4Export[dupeMap.size()]);

        //
        // Parse Require-Bundle
        //
        IRequirement[] bundleReq = parseRequireBundleHeader(
            (String) headerMap.get(Constants.REQUIRE_BUNDLE));
//System.out.println("PARSED BUNDLE REQUIREMENTS:");
//for (int reqIdx = 0; reqIdx < bundleReq.length; reqIdx++)
//{
//    System.out.println(bundleReq[reqIdx]);
//}

        //
        // Parse Import-Package.
        //

        // Get import packages from bundle manifest.
        m_imports = (R4Import[]) parseImportExportHeader(
            (String) headerMap.get(Constants.IMPORT_PACKAGE), false);

        // Get import packages from bundle manifest.
        IRequirement[] importReqs = parseImportHeader(
            (String) headerMap.get(Constants.IMPORT_PACKAGE));
//System.out.println("PARSED IMPORT REQUIREMENTS:");
//for (int reqIdx = 0; reqIdx < importReqs.length; reqIdx++)
//{
//    System.out.println(importReqs[reqIdx]);
//}

        // Create non-duplicated import array.
        dupeMap.clear();
        for (int pkgIdx = 0; pkgIdx < m_imports.length; pkgIdx++)
        {
            // Verify that the named package has not already been declared.
            if (dupeMap.get(m_imports[pkgIdx].getName()) == null)
            {
                // Verify that java.* packages are not imported.
                if (m_imports[pkgIdx].getName().startsWith("java."))
                {
                    throw new BundleException(
                        "Importing java.* packages not allowed: "
                        + m_imports[pkgIdx].getName());
                }
                dupeMap.put(m_imports[pkgIdx].getName(), m_imports[pkgIdx]);
            }
            else
            {
                throw new BundleException(
                    "Duplicate import - " + m_imports[pkgIdx].getName());
            }
        }

        //
        // Parse DynamicImport-Package.
        //

        // Get dynamic import packages from bundle manifest.
        m_dynamics = (R4Import[]) parseImportExportHeader(
            (String) headerMap.get(Constants.DYNAMICIMPORT_PACKAGE), false);

        // Get import packages from bundle manifest.
        IRequirement[] dynReqs = parseImportHeader(
            (String) headerMap.get(Constants.DYNAMICIMPORT_PACKAGE));
//System.out.println("PARSED DYNAMIC IMPORT REQUIREMENTS:");
//for (int reqIdx = 0; reqIdx < dynReqs.length; reqIdx++)
//{
//    System.out.println(dynReqs[reqIdx]);
//}

        // Dynamic imports can have duplicates, so just check for import
        // of java.*.
        List dynList = new ArrayList();
        for (int pkgIdx = 0; pkgIdx < m_dynamics.length; pkgIdx++)
        {
            // Verify that java.* packages are not imported.
            if (m_dynamics[pkgIdx].getName().startsWith("java."))
            {
                throw new BundleException(
                    "Dynamically importing java.* packages not allowed: "
                    + m_dynamics[pkgIdx].getName());
            }
        }

        //
        // Parse Bundle-NativeCode.
        //

        // Get native library entry names for module library sources.
        m_libraryHeaders =
            parseLibraryStrings(
                m_logger,
                parseDelimitedString((String) m_headerMap.get(Constants.BUNDLE_NATIVECODE), ","));

        // Check to see if there was an optional native library clause, which is
        // represented by a null library header; if so, record it and remove it.
        if ((m_libraryHeaders.length > 0) &&
            (m_libraryHeaders[m_libraryHeaders.length - 1].getLibraryFiles() == null))
        {
            m_libraryHeadersOptional = true;
            R4LibraryClause[] tmp = new R4LibraryClause[m_libraryHeaders.length - 1];
            System.arraycopy(m_libraryHeaders, 0, tmp, 0, m_libraryHeaders.length - 1);
            m_libraryHeaders = tmp;
        }

        // Do final checks and normalization of manifest.
        if (getManifestVersion().equals("2"))
        {
            checkAndNormalizeR4();
        }
        else
        {
            checkAndNormalizeR3();
        }
    }

    public String getManifestVersion()
    {
        String manifestVersion = (String) m_headerMap.get(Constants.BUNDLE_MANIFESTVERSION);
        return (manifestVersion == null) ? "1" : manifestVersion;
    }

    public String getSymbolicName()
    {
        return m_bundleSymbolicName;
    }

    public Version getBundleVersion()
    {
        return m_bundleVersion;
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

    public R4LibraryClause[] getLibraryClauses()
    {
        return m_libraryHeaders;
    }

    /**
     * <p>
     * This method returns the selected native library metadata from
     * the manifest. The information is not the raw metadata from the
     * manifest, but is native library metadata clause selected according
     * to the OSGi native library clause selection policy. The metadata
     * returned by this method will be attached directly to a module and
     * used for finding its native libraries at run time. To inspect the
     * raw native library metadata refer to <tt>getLibraryClauses()</tt>.
     * </p>
     * @param revision the bundle revision for the module.
     * @return an array of selected library metadata objects from the manifest.
     * @throws BundleException if any problems arise.
     */
    public R4Library[] getLibraries(BundleRevision revision) throws BundleException
    {
        R4LibraryClause clause = getSelectedLibraryClause();

        if (clause != null)
        {
            R4Library[] libraries = new R4Library[clause.getLibraryFiles().length];
            for (int i = 0; i < libraries.length; i++)
            {
                libraries[i] = new R4Library(
                    m_logger, revision, clause.getLibraryFiles()[i],
                    clause.getOSNames(), clause.getProcessors(), clause.getOSVersions(),
                    clause.getLanguages(), clause.getSelectionFilter());
            }
            return libraries;
        }
        return null;
    }

    private R4LibraryClause getSelectedLibraryClause() throws BundleException
    {
        if ((m_libraryHeaders != null) && (m_libraryHeaders.length > 0))
        {
            List clauseList = new ArrayList();

            // Search for matching native clauses.
            for (int i = 0; i < m_libraryHeaders.length; i++)
            {
                if (m_libraryHeaders[i].match(m_config))
                {
                    clauseList.add(m_libraryHeaders[i]);
                }
            }

            // Select the matching native clause.
            int selected = 0;
            if (clauseList.size() == 0)
            {
                // If optional clause exists, no error thrown.
                if (m_libraryHeadersOptional)
                {
                    return null;
                }
                else
                {
                    throw new BundleException("Unable to select a native library clause.");
                }
            }
            else if (clauseList.size() == 1)
            {
                selected = 0;
            }
            else if (clauseList.size() > 1)
            {
                selected = firstSortedClause(clauseList);
            }
            return ((R4LibraryClause) clauseList.get(selected));
        }

        return null;
    }

    private int firstSortedClause(List clauseList)
    {
        ArrayList indexList = new ArrayList();
        ArrayList selection = new ArrayList();

        // Init index list
        for (int i = 0; i < clauseList.size(); i++)
        {
            indexList.add("" + i);
        }

        // Select clause with 'osversion' range declared
        // and get back the max floor of 'osversion' ranges.
        Version osVersionRangeMaxFloor = new Version(0, 0, 0);
        for (int i = 0; i < indexList.size(); i++)
        {
            int index = Integer.parseInt(indexList.get(i).toString());
            String[] osversions = ((R4LibraryClause) clauseList.get(index)).getOSVersions();
            if (osversions != null)
            {
                selection.add("" + indexList.get(i));
            }
            for (int k = 0; (osversions != null) && (k < osversions.length); k++)
            {
                VersionRange range = VersionRange.parse(osversions[k]);
                if ((range.getLow()).compareTo(osVersionRangeMaxFloor) >= 0)
                {
                    osVersionRangeMaxFloor = range.getLow();
                }
            }
        }

        if (selection.size() == 1)
        {
            return Integer.parseInt(selection.get(0).toString());
        }
        else if (selection.size() > 1)
        {
            // Keep only selected clauses with an 'osversion'
            // equal to the max floor of 'osversion' ranges.
            indexList = selection;
            selection = new ArrayList();
            for (int i = 0; i < indexList.size(); i++)
            {
                int index = Integer.parseInt(indexList.get(i).toString());
                String[] osversions = ((R4LibraryClause) clauseList.get(index)).getOSVersions();
                for (int k = 0; k < osversions.length; k++)
                {
                    VersionRange range = VersionRange.parse(osversions[k]);
                    if ((range.getLow()).compareTo(osVersionRangeMaxFloor) >= 0)
                    {
                        selection.add("" + indexList.get(i));
                    }
                }
            }
        }

        if (selection.size() == 0)
        {
            // Re-init index list.
            selection.clear();
            indexList.clear();
            for (int i = 0; i < clauseList.size(); i++)
            {
                indexList.add("" + i);
            }
        }
        else if (selection.size() == 1)
        {
            return Integer.parseInt(selection.get(0).toString());
        }
        else
        {
            indexList = selection;
            selection.clear();
        }

        // Keep only clauses with 'language' declared.
        for (int i = 0; i < indexList.size(); i++)
        {
            int index = Integer.parseInt(indexList.get(i).toString());
            if (((R4LibraryClause) clauseList.get(index)).getLanguages() != null)
            {
                selection.add("" + indexList.get(i));
            }
        }

        // Return the first sorted clause
        if (selection.size() == 0)
        {
            return 0;
        }
        else
        {
            return Integer.parseInt(selection.get(0).toString());
        }
    }

    private void checkAndNormalizeR3() throws BundleException
    {
        // Check to make sure that R3 bundles have only specified
        // the 'specification-version' attribute and no directives
        // on their exports; ignore all unknown attributes.
        for (int expIdx = 0;
            (m_exports != null) && (expIdx < m_exports.length);
            expIdx++)
        {
            if (m_exports[expIdx].getDirectives().length != 0)
            {
                throw new BundleException("R3 exports cannot contain directives.");
            }

            // Remove and ignore all attributes other than version.
            // NOTE: This is checking for "version" rather than "specification-version"
            // because the package class normalizes to "version" to avoid having
            // future special cases. This could be changed if more strict behavior
            // is required.
            if (m_exports[expIdx].getAttributes() != null)
            {
                R4Attribute versionAttr = null;
                for (int attrIdx = 0;
                    attrIdx < m_exports[expIdx].getAttributes().length;
                    attrIdx++)
                {
                    if (m_exports[expIdx].getAttributes()[attrIdx]
                        .getName().equals(Constants.VERSION_ATTRIBUTE))
                    {
                        versionAttr = m_exports[expIdx].getAttributes()[attrIdx];
                    }
                    else
                    {
                        m_logger.log(Logger.LOG_WARNING,
                            "Unknown R3 export attribute: "
                                + m_exports[expIdx].getAttributes()[attrIdx].getName());
                    }
                }

                // Recreate the export if necessary to remove other attributes.
                if ((versionAttr != null) && (m_exports[expIdx].getAttributes().length > 1))
                {
                    m_exports[expIdx] = new R4Export(
                        m_exports[expIdx].getName(),
                        null,
                        new R4Attribute[] { versionAttr } );
                }
                else if ((versionAttr == null) && (m_exports[expIdx].getAttributes().length > 0))
                {
                    m_exports[expIdx] = new R4Export(
                        m_exports[expIdx].getName(), null, null);
                }
            }
        }

        // Check to make sure that R3 bundles have only specified
        // the 'specification-version' attribute and no directives
        // on their imports; ignore all unknown attributes.
        for (int impIdx = 0;
            (m_imports != null) && (impIdx < m_imports.length);
            impIdx++)
        {
            if (m_imports[impIdx].getDirectives().length != 0)
            {
                throw new BundleException("R3 imports cannot contain directives.");
            }

            // Remove and ignore all attributes other than version.
            // NOTE: This is checking for "version" rather than "specification-version"
            // because the package class normalizes to "version" to avoid having
            // future special cases. This could be changed if more strict behavior
            // is required.
            if (m_imports[impIdx].getAttributes() != null)
            {
                R4Attribute versionAttr = null;
                for (int attrIdx = 0;
                    attrIdx < m_imports[impIdx].getAttributes().length;
                    attrIdx++)
                {
                    if (m_imports[impIdx].getAttributes()[attrIdx]
                        .getName().equals(Constants.VERSION_ATTRIBUTE))
                    {
                        versionAttr = m_imports[impIdx].getAttributes()[attrIdx];
                    }
                    else
                    {
                        m_logger.log(Logger.LOG_WARNING,
                            "Unknown R3 import attribute: "
                                + m_imports[impIdx].getAttributes()[attrIdx].getName());
                    }
                }

                // Recreate the import if necessary to remove other attributes.
                if ((versionAttr != null) && (m_imports[impIdx].getAttributes().length > 1))
                {
                    m_imports[impIdx] = new R4Import(
                        m_imports[impIdx].getName(),
                        null,
                        new R4Attribute[] { versionAttr } );
                }
                else if ((versionAttr == null) && (m_imports[impIdx].getAttributes().length > 0))
                {
                    m_imports[impIdx] = new R4Import(
                        m_imports[impIdx].getName(), null, null);
                }
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
                // Convert Version to VersionRange.
                R4Attribute[] attrs = (R4Attribute[]) m_exports[i].getAttributes().clone();
                for (int attrIdx = 0; (attrs != null) && (attrIdx < attrs.length); attrIdx++)
                {
                    if (attrs[attrIdx].getName().equals(Constants.VERSION_ATTRIBUTE))
                    {
                        attrs[attrIdx] = new R4Attribute(
                            attrs[attrIdx].getName(),
                            VersionRange.parse(attrs[attrIdx].getValue().toString()),
                            attrs[attrIdx].isMandatory());
                    }
                }
                map.put(m_exports[i].getName(),
                    new R4Import(
                        m_exports[i].getName(),
                        m_exports[i].getDirectives(),
                        attrs));
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
        String symName = (String) m_headerMap.get(Constants.BUNDLE_SYMBOLICNAME);
        if (symName == null)
        {
            throw new BundleException("R4 bundle manifests must include bundle symbolic name.");
        }

        // Verify that the exports do not specify bundle symbolic name
        // or bundle version.
        for (int i = 0; (m_exports != null) && (i < m_exports.length); i++)
        {
            String targetVer = (String) m_headerMap.get(Constants.BUNDLE_VERSION);
            targetVer = (targetVer == null) ? "0.0.0" : targetVer;

            R4Attribute[] attrs = m_exports[i].getAttributes();
            for (int attrIdx = 0; attrIdx < attrs.length; attrIdx++)
            {
                // Find symbolic name and version attribute, if present.
                if (attrs[attrIdx].getName().equals(Constants.BUNDLE_VERSION_ATTRIBUTE) ||
                    attrs[attrIdx].getName().equals(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE))
                {
                    throw new BundleException(
                        "Exports must not specify bundle symbolic name or bundle version.");
                }
            }

            // Now that we know that there are no bundle symbolic name and version
            // attributes, add them since the spec says they are there implicitly.
            R4Attribute[] newAttrs = new R4Attribute[attrs.length + 2];
            System.arraycopy(attrs, 0, newAttrs, 0, attrs.length);
            newAttrs[attrs.length] = new R4Attribute(
                Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, symName, false);
            newAttrs[attrs.length + 1] = new R4Attribute(
                Constants.BUNDLE_VERSION_ATTRIBUTE, Version.parseVersion(targetVer), false);
            m_exports[i] = new R4Export(
                m_exports[i].getName(), m_exports[i].getDirectives(), newAttrs);
        }
    }

    public static ICapability[] parseExportHeader(String header)
    {
        Object[][][] clauses = parseStandardHeader(header);

// TODO: FRAMEWORK - Perhaps verification/normalization should be completely
// separated from parsing, since verification/normalization may vary.

        // If both version and specification-version attributes are specified,
        // then verify that the values are equal.
        Map attrMap = new HashMap();
        for (int clauseIdx = 0; clauseIdx < clauses.length; clauseIdx++)
        {
            // Put attributes for current clause in a map for easy lookup.
            attrMap.clear();
            for (int attrIdx = 0;
                attrIdx < clauses[clauseIdx][CLAUSE_ATTRIBUTES_INDEX].length;
                attrIdx++)
            {
                R4Attribute attr = (R4Attribute) clauses[clauseIdx][CLAUSE_ATTRIBUTES_INDEX][attrIdx];
                attrMap.put(attr.getName(), attr);
            }

            // Check for "version" and "specification-version" attributes
            // and verify they are the same if both are specified.
            R4Attribute v = (R4Attribute) attrMap.get(Constants.VERSION_ATTRIBUTE);
            R4Attribute sv = (R4Attribute) attrMap.get(Constants.PACKAGE_SPECIFICATION_VERSION);
            if ((v != null) && (sv != null))
            {
                // Verify they are equal.
                if (!((String) v.getValue()).trim().equals(((String) sv.getValue()).trim()))
                {
                    throw new IllegalArgumentException(
                        "Both version and specificat-version are specified, but they are not equal.");
                }
            }
    
            // Ensure that only the "version" attribute is used and convert
            // it to the appropriate type.
            if ((v != null) || (sv != null))
            {
                // Convert version attribute to type Version.
                attrMap.remove(Constants.PACKAGE_SPECIFICATION_VERSION);
                v = (v == null) ? sv : v;
                attrMap.put(Constants.VERSION_ATTRIBUTE,
                    new R4Attribute(
                        Constants.VERSION_ATTRIBUTE,
                        Version.parseVersion(v.getValue().toString()),
                        v.isMandatory()));

                // Re-copy the attribute array since it has changed.
                clauses[clauseIdx][CLAUSE_ATTRIBUTES_INDEX] =
                    attrMap.values().toArray(new R4Attribute[attrMap.size()]);
            }
        }

        // Now convert generic header clauses into capabilities.
        List capList = new ArrayList();
        for (int clauseIdx = 0; clauseIdx < clauses.length; clauseIdx++)
        {
            for (int pathIdx = 0;
                pathIdx < clauses[clauseIdx][CLAUSE_PATHS_INDEX].length;
                pathIdx++)
            {
                // Prepend the package name to the array of attributes.
                R4Attribute[] attrs = (R4Attribute[]) clauses[clauseIdx][CLAUSE_ATTRIBUTES_INDEX];
                R4Attribute[] newAttrs = new R4Attribute[attrs.length + 1];
                newAttrs[0] = new R4Attribute(
                    ICapability.PACKAGE_PROPERTY,
                    (String) clauses[clauseIdx][CLAUSE_PATHS_INDEX][pathIdx], false);
                System.arraycopy(attrs, 0, newAttrs, 1, attrs.length);

                // Create package capability and add to capability list.
                capList.add(
                    new Capability(
                        ICapability.PACKAGE_NAMESPACE,
                        (R4Directive[]) clauses[clauseIdx][CLAUSE_DIRECTIVES_INDEX],
                        newAttrs));
            }
        }

        return (ICapability[]) capList.toArray(new ICapability[capList.size()]);
    }

    public static IRequirement[] parseImportHeader(String header)
    {
        Object[][][] clauses = parseStandardHeader(header);

// TODO: FRAMEWORK - Perhaps verification/normalization should be completely
// separated from parsing, since verification/normalization may vary.

        // Verify that the values are equals if the package specifies
        // both version and specification-version attributes.
        Map attrMap = new HashMap();
        for (int clauseIdx = 0; clauseIdx < clauses.length; clauseIdx++)
        {
            // Put attributes for current clause in a map for easy lookup.
            attrMap.clear();
            for (int attrIdx = 0;
                attrIdx < clauses[clauseIdx][CLAUSE_ATTRIBUTES_INDEX].length;
                attrIdx++)
            {
                R4Attribute attr = (R4Attribute) clauses[clauseIdx][CLAUSE_ATTRIBUTES_INDEX][attrIdx];
                attrMap.put(attr.getName(), attr);
            }

            // Check for "version" and "specification-version" attributes
            // and verify they are the same if both are specified.
            R4Attribute v = (R4Attribute) attrMap.get(Constants.VERSION_ATTRIBUTE);
            R4Attribute sv = (R4Attribute) attrMap.get(Constants.PACKAGE_SPECIFICATION_VERSION);
            if ((v != null) && (sv != null))
            {
                // Verify they are equal.
                if (!((String) v.getValue()).trim().equals(((String) sv.getValue()).trim()))
                {
                    throw new IllegalArgumentException(
                        "Both version and specificat-version are specified, but they are not equal.");
                }
            }
    
            // Ensure that only the "version" attribute is used and convert
            // it to the VersionRange type.
            if ((v != null) || (sv != null))
            {
                attrMap.remove(Constants.PACKAGE_SPECIFICATION_VERSION);
                v = (v == null) ? sv : v;
                attrMap.put(Constants.VERSION_ATTRIBUTE,
                    new R4Attribute(
                        Constants.VERSION_ATTRIBUTE,
                        VersionRange.parse(v.getValue().toString()),
                        v.isMandatory()));
            }

            // If bundle version is specified, then convert its type to VersionRange.
            v = (R4Attribute) attrMap.get(Constants.BUNDLE_VERSION_ATTRIBUTE);
            if (v != null)
            {
                attrMap.put(Constants.BUNDLE_VERSION_ATTRIBUTE,
                    new R4Attribute(
                        Constants.BUNDLE_VERSION_ATTRIBUTE,
                        VersionRange.parse(v.getValue().toString()),
                        v.isMandatory()));
            }

            // Re-copy the attribute array in case it has changed.
            clauses[clauseIdx][CLAUSE_ATTRIBUTES_INDEX] =
                attrMap.values().toArray(new R4Attribute[attrMap.size()]);
        }

        // Now convert generic header clauses into requirements.
        List reqList = new ArrayList();
        for (int clauseIdx = 0; clauseIdx < clauses.length; clauseIdx++)
        {
            for (int pathIdx = 0;
                pathIdx < clauses[clauseIdx][CLAUSE_PATHS_INDEX].length;
                pathIdx++)
            {
                // Prepend the package name to the array of attributes.
                R4Attribute[] attrs = (R4Attribute[]) clauses[clauseIdx][CLAUSE_ATTRIBUTES_INDEX];
                R4Attribute[] newAttrs = new R4Attribute[attrs.length + 1];
                newAttrs[0] = new R4Attribute(
                    ICapability.PACKAGE_PROPERTY,
                    (String) clauses[clauseIdx][CLAUSE_PATHS_INDEX][pathIdx], false);
                System.arraycopy(attrs, 0, newAttrs, 1, attrs.length);

                // Create package requirement and add to requirement list.
                reqList.add(
                    new Requirement(
                        ICapability.PACKAGE_NAMESPACE,
                        (R4Directive[]) clauses[clauseIdx][CLAUSE_DIRECTIVES_INDEX],
                        newAttrs));
            }
        }

        return (IRequirement[]) reqList.toArray(new IRequirement[reqList.size()]);
    }

    public static IRequirement[] parseRequireBundleHeader(String header)
    {
        Object[][][] clauses = parseStandardHeader(header);

// TODO: FRAMEWORK - Perhaps verification/normalization should be completely
// separated from parsing, since verification/normalization may vary.

        // Convert bundle version attribute to VersionRange type.
        for (int clauseIdx = 0; clauseIdx < clauses.length; clauseIdx++)
        {
            for (int attrIdx = 0;
                attrIdx < clauses[clauseIdx][CLAUSE_ATTRIBUTES_INDEX].length;
                attrIdx++)
            {
                R4Attribute attr = (R4Attribute) clauses[clauseIdx][CLAUSE_ATTRIBUTES_INDEX][attrIdx];
                if (attr.getName().equals(Constants.BUNDLE_VERSION_ATTRIBUTE))
                {
                    clauses[clauseIdx][CLAUSE_ATTRIBUTES_INDEX][attrIdx] =
                        new R4Attribute(
                            Constants.BUNDLE_VERSION_ATTRIBUTE,
                            VersionRange.parse(attr.getValue().toString()),
                            attr.isMandatory());
                }
            }
        }

        // Now convert generic header clauses into requirements.
        List reqList = new ArrayList();
        for (int clauseIdx = 0; clauseIdx < clauses.length; clauseIdx++)
        {
            for (int pathIdx = 0;
                pathIdx < clauses[clauseIdx][CLAUSE_PATHS_INDEX].length;
                pathIdx++)
            {
                // Prepend the symbolic name to the array of attributes.
                R4Attribute[] attrs = (R4Attribute[]) clauses[clauseIdx][CLAUSE_ATTRIBUTES_INDEX];
                R4Attribute[] newAttrs = new R4Attribute[attrs.length + 1];
                newAttrs[0] = new R4Attribute(
                    Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE,
                    (String) clauses[clauseIdx][CLAUSE_PATHS_INDEX][pathIdx], false);
                System.arraycopy(attrs, 0, newAttrs, 1, attrs.length);

                // Create package requirement and add to requirement list.
                reqList.add(
                    new Requirement(
                        ICapability.MODULE_NAMESPACE,
                        (R4Directive[]) clauses[clauseIdx][CLAUSE_DIRECTIVES_INDEX],
                        newAttrs));
            }
        }

        return (IRequirement[]) reqList.toArray(new IRequirement[reqList.size()]);
    }

    public static R4Package[] parseImportExportHeader(String header, boolean export)
    {
        Object[][][] clauses = parseStandardHeader(header);

// TODO: FRAMEWORK - Perhaps verification/normalization should be completely
// separated from parsing, since verification/normalization may vary.

        // Verify that the values are equals if the package specifies
        // both version and specification-version attributes.
        Map attrMap = new HashMap();
        for (int clauseIdx = 0; clauseIdx < clauses.length; clauseIdx++)
        {
            // Put attributes for current clause in a map for easy lookup.
            attrMap.clear();
            for (int attrIdx = 0;
                attrIdx < clauses[clauseIdx][CLAUSE_ATTRIBUTES_INDEX].length;
                attrIdx++)
            {
                R4Attribute attr = (R4Attribute) clauses[clauseIdx][CLAUSE_ATTRIBUTES_INDEX][attrIdx];
                attrMap.put(attr.getName(), attr);
            }

            // Check for "version" and "specification-version" attributes
            // and verify they are the same if both are specified.
            R4Attribute v = (R4Attribute) attrMap.get(Constants.VERSION_ATTRIBUTE);
            R4Attribute sv = (R4Attribute) attrMap.get(Constants.PACKAGE_SPECIFICATION_VERSION);
            if ((v != null) && (sv != null))
            {
                // Verify they are equal.
                if (!((String) v.getValue()).trim().equals(((String) sv.getValue()).trim()))
                {
                    throw new IllegalArgumentException(
                        "Both version and specificat-version are specified, but they are not equal.");
                }
            }
    
            // Ensure that only the "version" attribute is used and convert
            // it to the appropriate type.
            if ((v != null) || (sv != null))
            {
                attrMap.remove(Constants.PACKAGE_SPECIFICATION_VERSION);
                v = (v == null) ? sv : v;
                if (export)
                {
                    attrMap.put(Constants.VERSION_ATTRIBUTE,
                        new R4Attribute(
                            Constants.VERSION_ATTRIBUTE,
                            Version.parseVersion(v.getValue().toString()),
                            v.isMandatory()));
                }
                else
                {
                    attrMap.put(Constants.VERSION_ATTRIBUTE,
                        new R4Attribute(
                            Constants.VERSION_ATTRIBUTE,
                            VersionRange.parse(v.getValue().toString()),
                            v.isMandatory()));
                }
            }

            // If bundle version is specified, then convert its type to Version.
            // Only imports can specify this attribue.
            v = (R4Attribute) attrMap.get(Constants.BUNDLE_VERSION_ATTRIBUTE);
            if (!export && (v != null))
            {
                attrMap.put(Constants.BUNDLE_VERSION_ATTRIBUTE,
                    new R4Attribute(
                        Constants.BUNDLE_VERSION_ATTRIBUTE,
                        VersionRange.parse(v.getValue().toString()),
                        v.isMandatory()));
            }

            // Re-copy the attribute array in case it has changed.
            clauses[clauseIdx][CLAUSE_ATTRIBUTES_INDEX] =
                attrMap.values().toArray(new R4Attribute[attrMap.size()]);
        }

        // Now convert generic header clauses into packages.
        List pkgList = new ArrayList();
        for (int clauseIdx = 0; clauseIdx < clauses.length; clauseIdx++)
        {
            for (int pathIdx = 0;
                pathIdx < clauses[clauseIdx][CLAUSE_PATHS_INDEX].length;
                pathIdx++)
            {
                if (export)
                {
                    pkgList.add(
                        new R4Export(
                            (String) clauses[clauseIdx][CLAUSE_PATHS_INDEX][pathIdx],
                            (R4Directive[]) clauses[clauseIdx][CLAUSE_DIRECTIVES_INDEX],
                            (R4Attribute[]) clauses[clauseIdx][CLAUSE_ATTRIBUTES_INDEX]));
                }
                else
                {
                    pkgList.add(
                        new R4Import(
                            (String) clauses[clauseIdx][CLAUSE_PATHS_INDEX][pathIdx],
                            (R4Directive[]) clauses[clauseIdx][CLAUSE_DIRECTIVES_INDEX],
                            (R4Attribute[]) clauses[clauseIdx][CLAUSE_ATTRIBUTES_INDEX]));
                }
            }
        }

        return (export)
            ? (R4Package[]) pkgList.toArray(new R4Export[pkgList.size()])
            : (R4Package[]) pkgList.toArray(new R4Import[pkgList.size()]);
    }

    public static final int CLAUSE_PATHS_INDEX = 0;
    public static final int CLAUSE_DIRECTIVES_INDEX = 1;
    public static final int CLAUSE_ATTRIBUTES_INDEX = 2;

    // Like this: path; path; dir1:=dirval1; dir2:=dirval2; attr1=attrval1; attr2=attrval2,
    //            path; path; dir1:=dirval1; dir2:=dirval2; attr1=attrval1; attr2=attrval2
    public static Object[][][] parseStandardHeader(String header)
    {
        Object[][][] clauses = null;

        if (header != null)
        {
            if (header.length() == 0)
            {
                throw new IllegalArgumentException(
                    "A header cannot be an empty string.");
            }

            String[] clauseStrings = parseDelimitedString(
                header, FelixConstants.CLASS_PATH_SEPARATOR);

            List completeList = new ArrayList();
            for (int i = 0; (clauseStrings != null) && (i < clauseStrings.length); i++)
            {
                completeList.add(parseStandardHeaderClause(clauseStrings[i]));
            }
            clauses = (Object[][][]) completeList.toArray(new Object[completeList.size()][][]);
        }

        return (clauses == null) ? new Object[0][][] : clauses;
    }

    // Like this: path; path; dir1:=dirval1; dir2:=dirval2; attr1=attrval1; attr2=attrval2
    public static Object[][] parseStandardHeaderClause(String clauseString)
        throws IllegalArgumentException
    {
        // Break string into semi-colon delimited pieces.
        String[] pieces = parseDelimitedString(
            clauseString, FelixConstants.PACKAGE_SEPARATOR);

        // Count the number of different paths; paths
        // will not have an '=' in their string. This assumes
        // that paths come first, before directives and
        // attributes.
        int pathCount = 0;
        for (int pieceIdx = 0; pieceIdx < pieces.length; pieceIdx++)
        {
            if (pieces[pieceIdx].indexOf('=') >= 0)
            {
                break;
            }
            pathCount++;
        }

        // Error if no paths were specified.
        if (pathCount == 0)
        {
            throw new IllegalArgumentException(
                "No paths specified in header: " + clauseString);
        }

        // Create an array of paths.
        String[] paths = new String[pathCount];
        System.arraycopy(pieces, 0, paths, 0, pathCount);

        // Parse the directives/attributes.
        Map dirsMap = new HashMap();
        Map attrsMap = new HashMap();
        int idx = -1;
        String sep = null;
        for (int pieceIdx = pathCount; pieceIdx < pieces.length; pieceIdx++)
        {
            // Check if it is a directive.
            if ((idx = pieces[pieceIdx].indexOf(FelixConstants.DIRECTIVE_SEPARATOR)) >= 0)
            {
                sep = FelixConstants.DIRECTIVE_SEPARATOR;
            }
            // Check if it is an attribute.
            else if ((idx = pieces[pieceIdx].indexOf(FelixConstants.ATTRIBUTE_SEPARATOR)) >= 0)
            {
                sep = FelixConstants.ATTRIBUTE_SEPARATOR;
            }
            // It is an error.
            else
            {
                throw new IllegalArgumentException("Not a directive/attribute: " + clauseString);
            }

            String key = pieces[pieceIdx].substring(0, idx).trim();
            String value = pieces[pieceIdx].substring(idx + sep.length()).trim();

            // Remove quotes, if value is quoted.
            if (value.startsWith("\"") && value.endsWith("\""))
            {
                value = value.substring(1, value.length() - 1);
            }

            // Save the directive/attribute in the appropriate array.
            if (sep.equals(FelixConstants.DIRECTIVE_SEPARATOR))
            {
                // Check for duplicates.
                if (dirsMap.get(key) != null)
                {
                    throw new IllegalArgumentException(
                        "Duplicate directive: " + key);
                }
                dirsMap.put(key, new R4Directive(key, value));
            }
            else
            {
                // Check for duplicates.
                if (attrsMap.get(key) != null)
                {
                    throw new IllegalArgumentException(
                        "Duplicate attribute: " + key);
                }
                attrsMap.put(key, new R4Attribute(key, value, false));
            }
        }

        // Create directive array.
        R4Directive[] dirs = (R4Directive[])
            dirsMap.values().toArray(new R4Directive[dirsMap.size()]);

        // Create attribute array.
        R4Attribute[] attrs = (R4Attribute[])
            attrsMap.values().toArray(new R4Attribute[attrsMap.size()]);

        // Create an array to hold the parsed paths, directives, and attributes.
        Object[][] clause = new Object[3][];
        clause[CLAUSE_PATHS_INDEX] = paths;
        clause[CLAUSE_DIRECTIVES_INDEX] = dirs;
        clause[CLAUSE_ATTRIBUTES_INDEX] = attrs;

        return clause;
    }

    /**
     * Parses delimited string and returns an array containing the tokens. This
     * parser obeys quotes, so the delimiter character will be ignored if it is
     * inside of a quote. This method assumes that the quote character is not
     * included in the set of delimiter characters.
     * @param value the delimited string to parse.
     * @param delim the characters delimiting the tokens.
     * @return an array of string tokens or null if there were no tokens.
    **/
    public static String[] parseDelimitedString(String value, String delim)
    {
        if (value == null)
        {
           value = "";
        }

        List list = new ArrayList();

        int CHAR = 1;
        int DELIMITER = 2;
        int STARTQUOTE = 4;
        int ENDQUOTE = 8;

        StringBuffer sb = new StringBuffer();

        int expecting = (CHAR | DELIMITER | STARTQUOTE);
        
        for (int i = 0; i < value.length(); i++)
        {
            char c = value.charAt(i);

            boolean isDelimiter = (delim.indexOf(c) >= 0);
            boolean isQuote = (c == '"');

            if (isDelimiter && ((expecting & DELIMITER) > 0))
            {
                list.add(sb.toString().trim());
                sb.delete(0, sb.length());
                expecting = (CHAR | DELIMITER | STARTQUOTE);
            }
            else if (isQuote && ((expecting & STARTQUOTE) > 0))
            {
                sb.append(c);
                expecting = CHAR | ENDQUOTE;
            }
            else if (isQuote && ((expecting & ENDQUOTE) > 0))
            {
                sb.append(c);
                expecting = (CHAR | STARTQUOTE | DELIMITER);
            }
            else if ((expecting & CHAR) > 0)
            {
                sb.append(c);
            }
            else
            {
                throw new IllegalArgumentException("Invalid delimited string: " + value);
            }
        }

        if (sb.length() > 0)
        {
            list.add(sb.toString().trim());
        }

        return (String[]) list.toArray(new String[list.size()]);
    }

    /**
     * Parses native code manifest headers.
     * @param libStrs an array of native library manifest header
     *        strings from the bundle manifest.
     * @return an array of <tt>LibraryInfo</tt> objects for the
     *         passed in strings.
    **/
    public static R4LibraryClause[] parseLibraryStrings(Logger logger, String[] libStrs)
        throws IllegalArgumentException
    {
        if (libStrs == null)
        {
            return new R4LibraryClause[0];
        }

        List libList = new ArrayList();

        for (int i = 0; i < libStrs.length; i++)
        {
            R4LibraryClause clause = R4LibraryClause.parse(logger, libStrs[i]);
            libList.add(clause);
        }

        return (R4LibraryClause[]) libList.toArray(new R4LibraryClause[libList.size()]);
    }
}