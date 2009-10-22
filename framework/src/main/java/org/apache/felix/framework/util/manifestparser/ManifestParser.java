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
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.VersionRange;
import org.apache.felix.moduleloader.ICapability;
import org.apache.felix.moduleloader.IModule;
import org.apache.felix.moduleloader.IRequirement;
import org.osgi.framework.*;

public class ManifestParser
{
    private final Logger m_logger;
    private final Map m_configMap;
    private final Map m_headerMap;
    private volatile int m_activationPolicy = IModule.EAGER_ACTIVATION;
    private volatile String m_activationIncludeDir;
    private volatile String m_activationExcludeDir;
    private volatile boolean m_isExtension = false;
    private volatile String m_bundleSymbolicName;
    private volatile Version m_bundleVersion;
    private volatile ICapability[] m_capabilities;
    private volatile IRequirement[] m_requirements;
    private volatile IRequirement[] m_dynamicRequirements;
    private volatile R4LibraryClause[] m_libraryHeaders;
    private volatile boolean m_libraryHeadersOptional = false;

    public ManifestParser(Logger logger, Map configMap, IModule owner, Map headerMap)
        throws BundleException
    {
        m_logger = logger;
        m_configMap = configMap;
        m_headerMap = headerMap;

        // Verify that only manifest version 2 is specified.
        String manifestVersion = getManifestVersion(m_headerMap);
        if ((manifestVersion != null) && !manifestVersion.equals("2"))
        {
            throw new BundleException(
                "Unknown 'Bundle-ManifestVersion' value: " + manifestVersion);
        }

        // Create lists to hold capabilities and requirements.
        List capList = new ArrayList();
        List reqList = new ArrayList();

        //
        // Parse bundle version.
        //

        m_bundleVersion = Version.emptyVersion;
        if (headerMap.get(Constants.BUNDLE_VERSION) != null)
        {
            try
            {
                m_bundleVersion = Version.parseVersion((String) headerMap.get(Constants.BUNDLE_VERSION));
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

        ICapability moduleCap = parseBundleSymbolicName(owner, m_headerMap);
        if (moduleCap != null)
        {
            m_bundleSymbolicName = (String)
                moduleCap.getProperties().get(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);

            // Add a module capability and a host capability to all
            // non-fragment bundles. A host capability is the same
            // as a module capability, but with a different capability
            // namespace. Module capabilities resolve required-bundle
            // dependencies, while host capabilities resolve fragment-host
            // dependencies.
            if (headerMap.get(Constants.FRAGMENT_HOST) == null)
            {
                capList.add(moduleCap);
                capList.add(new Capability(
                    owner, ICapability.HOST_NAMESPACE, null,
                    ((Capability) moduleCap).getAttributes()));
            }
        }

        //
        // Parse Export-Package.
        //

        // Get exported packages from bundle manifest.
        ICapability[] exportCaps = parseExportHeader(
            owner, (String) headerMap.get(Constants.EXPORT_PACKAGE));

        // Verify that "java.*" packages are not exported.
        for (int capIdx = 0; capIdx < exportCaps.length; capIdx++)
        {
            // Verify that the named package has not already been declared.
            String pkgName = (String)
                exportCaps[capIdx].getProperties().get(ICapability.PACKAGE_PROPERTY);
            // Verify that java.* packages are not exported.
            if (pkgName.startsWith("java."))
            {
                throw new BundleException(
                    "Exporting java.* packages not allowed: " + pkgName);
            }
            capList.add(exportCaps[capIdx]);
        }

        // Create an array of all capabilities.
        m_capabilities = (ICapability[]) capList.toArray(new ICapability[capList.size()]);

        //
        // Parse Fragment-Host.
        //

        IRequirement req = parseFragmentHost(m_logger, m_headerMap);
        if (req != null)
        {
            reqList.add(req);
        }

        //
        // Parse Require-Bundle
        //

        IRequirement[] bundleReq = parseRequireBundleHeader(
            (String) headerMap.get(Constants.REQUIRE_BUNDLE));
        for (int reqIdx = 0; reqIdx < bundleReq.length; reqIdx++)
        {
            reqList.add(bundleReq[reqIdx]);
        }

        //
        // Parse Import-Package.
        //

        // Get import packages from bundle manifest.
        IRequirement[] importReqs = parseImportHeader(
            (String) headerMap.get(Constants.IMPORT_PACKAGE));

        // Verify there are no duplicate import declarations.
        Set dupeSet = new HashSet();
        for (int reqIdx = 0; reqIdx < importReqs.length; reqIdx++)
        {
            // Verify that the named package has not already been declared.
            String pkgName = ((Requirement) importReqs[reqIdx]).getTargetName();
            if (!dupeSet.contains(pkgName))
            {
                // Verify that java.* packages are not imported.
                if (pkgName.startsWith("java."))
                {
                    throw new BundleException(
                        "Importing java.* packages not allowed: " + pkgName);
                }
                dupeSet.add(pkgName);
            }
            else
            {
                throw new BundleException("Duplicate import - " + pkgName);
            }
            // If it has not already been imported, then add it to the list
            // of requirements.
            reqList.add(importReqs[reqIdx]);
        }

        // Create an array of all requirements.
        m_requirements = (IRequirement[]) reqList.toArray(new IRequirement[reqList.size()]);

        //
        // Parse DynamicImport-Package.
        //

        // Get dynamic import packages from bundle manifest.
        m_dynamicRequirements = parseImportHeader(
            (String) headerMap.get(Constants.DYNAMICIMPORT_PACKAGE));

        // Dynamic imports can have duplicates, so just check for import
        // of java.*.
        for (int reqIdx = 0; reqIdx < m_dynamicRequirements.length; reqIdx++)
        {
            // Verify that java.* packages are not imported.
            String pkgName = ((Requirement) m_dynamicRequirements[reqIdx]).getTargetName();
            if (pkgName.startsWith("java."))
            {
                throw new BundleException(
                    "Dynamically importing java.* packages not allowed: " + pkgName);
            }
            else if (!pkgName.equals("*") && pkgName.endsWith("*") && !pkgName.endsWith(".*"))
            {
                throw new BundleException(
                    "Partial package name wild carding is not allowed: " + pkgName);
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
            (m_libraryHeaders[m_libraryHeaders.length - 1].getLibraryEntries() == null))
        {
            m_libraryHeadersOptional = true;
            R4LibraryClause[] tmp = new R4LibraryClause[m_libraryHeaders.length - 1];
            System.arraycopy(m_libraryHeaders, 0, tmp, 0, m_libraryHeaders.length - 1);
            m_libraryHeaders = tmp;
        }

        //
        // Parse activation policy.
        //

        // This sets m_activationPolicy, m_includedPolicyClasses, and
        // m_excludedPolicyClasses.
        parseActivationPolicy(headerMap);

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
        String manifestVersion = getManifestVersion(m_headerMap);
        return (manifestVersion == null) ? "1" : manifestVersion;
    }

    private static String getManifestVersion(Map headerMap)
    {
        String manifestVersion = (String) headerMap.get(Constants.BUNDLE_MANIFESTVERSION);
        return (manifestVersion == null) ? null : manifestVersion.trim();
    }

    public int getActivationPolicy()
    {
        return m_activationPolicy;
    }

    public String getActivationIncludeDirective()
    {
        return m_activationIncludeDir;
    }

    public String getActivationExcludeDirective()
    {
        return m_activationExcludeDir;
    }

    public boolean isExtension()
    {
        return m_isExtension;
    }

    public String getSymbolicName()
    {
        return m_bundleSymbolicName;
    }

    public Version getBundleVersion()
    {
        return m_bundleVersion;
    }

    public ICapability[] getCapabilities()
    {
        return m_capabilities;
    }

    public IRequirement[] getRequirements()
    {
        return m_requirements;
    }

    public IRequirement[] getDynamicRequirements()
    {
        return m_dynamicRequirements;
    }

    public R4LibraryClause[] getLibraryClauses()
    {
        return m_libraryHeaders;
    }

    /**
     * <p>
     * This method returns the selected native library metadata from
     * the manifest. The information is not the raw metadata from the
     * manifest, but is the native library clause selected according
     * to the OSGi native library clause selection policy. The metadata
     * returned by this method will be attached directly to a module and
     * used for finding its native libraries at run time. To inspect the
     * raw native library metadata refer to <tt>getLibraryClauses()</tt>.
     * </p>
     * <p>
     * This method returns one of three values:
     * </p>
     * <ul>
     * <li><tt>null</tt> - if the are no native libraries for this module;
     *     this may also indicate the native libraries are optional and
     *     did not match the current platform.</li>
     * <li>Zero-length <tt>R4Library</tt> array - if no matching native library
     *     clause was found; this bundle should not resolve.</li>
     * <li>Nonzero-length <tt>R4Library</tt> array - the native libraries
     *     associated with the matching native library clause.</li>
     * </ul>
     *
     * @return <tt>null</tt> if there are no native libraries, a zero-length
     *         array if no libraries matched, or an array of selected libraries.
    **/
    public R4Library[] getLibraries()
    {
        R4Library[] libs = null;
        try
        {
            R4LibraryClause clause = getSelectedLibraryClause();
            if (clause != null)
            {
                String[] entries = clause.getLibraryEntries();
                libs = new R4Library[entries.length];
                int current = 0;
                for (int i = 0; i < libs.length; i++)
                {
                    String name = getName(entries[i]);
                    boolean found = false;
                    for (int j = 0; !found && (j < current); j++)
                    {
                        found = getName(entries[j]).equals(name);
                    }
                    if (!found)
                    {
                        libs[current++] = new R4Library(
                            clause.getLibraryEntries()[i],
                            clause.getOSNames(), clause.getProcessors(), clause.getOSVersions(),
                            clause.getLanguages(), clause.getSelectionFilter());
                    }
                }
                if (current < libs.length)
                {
                    R4Library[] tmp = new R4Library[current];
                    System.arraycopy(libs, 0, tmp, 0, current);
                    libs = tmp;
                }
            }
        }
        catch (Exception ex)
        {
            libs = new R4Library[0];
        }
        return libs;
    }

    private String getName(String path)
    {
        int idx = path.lastIndexOf('/');
        if (idx > -1)
        {
            return path.substring(idx);
        }
        return path;
    }

    private R4LibraryClause getSelectedLibraryClause() throws BundleException
    {
        if ((m_libraryHeaders != null) && (m_libraryHeaders.length > 0))
        {
            List clauseList = new ArrayList();

            // Search for matching native clauses.
            for (int i = 0; i < m_libraryHeaders.length; i++)
            {
                if (m_libraryHeaders[i].match(m_configMap))
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
        for (int capIdx = 0;
            (m_capabilities != null) && (capIdx < m_capabilities.length);
            capIdx++)
        {
            if (m_capabilities[capIdx].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                // R3 bundles cannot have directives on their exports.
                if (((Capability) m_capabilities[capIdx]).getDirectives().length != 0)
                {
                    throw new BundleException("R3 exports cannot contain directives.");
                }

                // Remove and ignore all attributes other than version.
                // NOTE: This is checking for "version" rather than "specification-version"
                // because the package class normalizes to "version" to avoid having
                // future special cases. This could be changed if more strict behavior
                // is required.
                if (((Capability) m_capabilities[capIdx]).getAttributes() != null)
                {
                    // R3 package capabilities should only have name and
                    // version attributes.
                    R4Attribute pkgName = null;
                    R4Attribute pkgVersion = new R4Attribute(ICapability.VERSION_PROPERTY, Version.emptyVersion, false);
                    for (int attrIdx = 0;
                        attrIdx < ((Capability) m_capabilities[capIdx]).getAttributes().length;
                        attrIdx++)
                    {
                        if (((Capability) m_capabilities[capIdx]).getAttributes()[attrIdx]
                            .getName().equals(ICapability.PACKAGE_PROPERTY))
                        {
                            pkgName = ((Capability) m_capabilities[capIdx]).getAttributes()[attrIdx];
                        }
                        else if (((Capability) m_capabilities[capIdx]).getAttributes()[attrIdx]
                            .getName().equals(ICapability.VERSION_PROPERTY))
                        {
                            pkgVersion = ((Capability) m_capabilities[capIdx]).getAttributes()[attrIdx];
                        }
                        else
                        {
                            m_logger.log(Logger.LOG_WARNING,
                                "Unknown R3 export attribute: "
                                    + ((Capability) m_capabilities[capIdx]).getAttributes()[attrIdx].getName());
                        }
                    }

                    // Recreate the export to remove any other attributes
                    // and add version if missing.
                    m_capabilities[capIdx] = new Capability(
                        m_capabilities[capIdx].getModule(),
                        ICapability.PACKAGE_NAMESPACE,
                        null,
                        new R4Attribute[] { pkgName, pkgVersion } );
                }
            }
        }

        // Check to make sure that R3 bundles have only specified
        // the 'specification-version' attribute and no directives
        // on their imports; ignore all unknown attributes.
        for (int reqIdx = 0; (m_requirements != null) && (reqIdx < m_requirements.length); reqIdx++)
        {
            if (m_requirements[reqIdx].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                // R3 bundles cannot have directives on their imports.
                if (((Requirement) m_requirements[reqIdx]).getDirectives().length != 0)
                {
                    throw new BundleException("R3 imports cannot contain directives.");
                }

                // Remove and ignore all attributes other than version.
                // NOTE: This is checking for "version" rather than "specification-version"
                // because the package class normalizes to "version" to avoid having
                // future special cases. This could be changed if more strict behavior
                // is required.
                if (((Requirement) m_requirements[reqIdx]).getAttributes() != null)
                {
                    // R3 package requirements should only have name and
                    // version attributes.
                    R4Attribute pkgName = null;
                    R4Attribute pkgVersion =
                        new R4Attribute(ICapability.VERSION_PROPERTY,
                            new VersionRange(Version.emptyVersion, true, null, true), false);
                    for (int attrIdx = 0;
                        attrIdx < ((Requirement) m_requirements[reqIdx]).getAttributes().length;
                        attrIdx++)
                    {
                        if (((Requirement) m_requirements[reqIdx]).getAttributes()[attrIdx]
                            .getName().equals(ICapability.PACKAGE_PROPERTY))
                        {
                            pkgName = ((Requirement) m_requirements[reqIdx]).getAttributes()[attrIdx];
                        }
                        else if (((Requirement) m_requirements[reqIdx]).getAttributes()[attrIdx]
                          .getName().equals(ICapability.VERSION_PROPERTY))
                        {
                            pkgVersion = ((Requirement) m_requirements[reqIdx]).getAttributes()[attrIdx];
                        }
                        else
                        {
                            m_logger.log(Logger.LOG_WARNING,
                                "Unknown R3 import attribute: "
                                    + ((Requirement) m_requirements[reqIdx]).getAttributes()[attrIdx].getName());
                        }
                    }

                    // Recreate the import to remove any other attributes
                    // and add version if missing.
                    m_requirements[reqIdx] = new Requirement(
                        ICapability.PACKAGE_NAMESPACE,
                        null,
                        new R4Attribute[] { pkgName, pkgVersion });
                }
            }
        }

        // Since all R3 exports imply an import, add a corresponding
        // requirement for each existing export capability. Do not
        // duplicate imports.
        Map map =  new HashMap();
        // Add existing imports.
        for (int i = 0; i < m_requirements.length; i++)
        {
            if (m_requirements[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                map.put(
                    ((Requirement) m_requirements[i]).getTargetName(),
                    m_requirements[i]);
            }
        }
        // Add import requirement for each export capability.
        for (int i = 0; i < m_capabilities.length; i++)
        {
            if (m_capabilities[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE) &&
                (map.get(m_capabilities[i].getProperties().get(ICapability.PACKAGE_PROPERTY)) == null))
            {
                // Convert Version to VersionRange.
                R4Attribute[] attrs = (R4Attribute[]) ((Capability) m_capabilities[i]).getAttributes().clone();
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

                map.put(
                    m_capabilities[i].getProperties().get(ICapability.PACKAGE_PROPERTY),
                    new Requirement(ICapability.PACKAGE_NAMESPACE, null, attrs));
            }
        }
        m_requirements =
            (IRequirement[]) map.values().toArray(new IRequirement[map.size()]);

        // Add a "uses" directive onto each export of R3 bundles
        // that references every other import (which will include
        // exports, since export implies import); this is
        // necessary since R3 bundles assumed a single class space,
        // but R4 allows for multiple class spaces.
        String usesValue = "";
        for (int i = 0; (m_requirements != null) && (i < m_requirements.length); i++)
        {
            if (m_requirements[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                usesValue = usesValue
                    + ((usesValue.length() > 0) ? "," : "")
                    + ((Requirement) m_requirements[i]).getTargetName();
            }
        }
        R4Directive uses = new R4Directive(
            Constants.USES_DIRECTIVE, usesValue);
        for (int i = 0; (m_capabilities != null) && (i < m_capabilities.length); i++)
        {
            if (m_capabilities[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                m_capabilities[i] = new Capability(
                    m_capabilities[i].getModule(),
                    ICapability.PACKAGE_NAMESPACE,
                    new R4Directive[] { uses },
                    ((Capability) m_capabilities[i]).getAttributes());
            }
        }

        // Check to make sure that R3 bundles have no attributes or
        // directives on their dynamic imports.
        for (int i = 0;
            (m_dynamicRequirements != null) && (i < m_dynamicRequirements.length);
            i++)
        {
            if (((Requirement) m_dynamicRequirements[i]).getDirectives().length != 0)
            {
                throw new BundleException("R3 dynamic imports cannot contain directives.");
            }
            if (((Requirement) m_dynamicRequirements[i]).getAttributes().length != 0)
            {
//                throw new BundleException("R3 dynamic imports cannot contain attributes.");
            }
        }
    }

    private void checkAndNormalizeR4() throws BundleException
    {
        // Verify that bundle symbolic name is specified.
        if (m_bundleSymbolicName == null)
        {
            throw new BundleException("R4 bundle manifests must include bundle symbolic name.");
        }

        m_capabilities = checkAndNormalizeR4Exports(
            m_capabilities, m_bundleSymbolicName, m_bundleVersion);

        R4Directive extension = parseExtensionBundleHeader((String)
            m_headerMap.get(Constants.FRAGMENT_HOST));

        if (extension != null)
        {
            if (!(Constants.EXTENSION_FRAMEWORK.equals(extension.getValue()) || 
                Constants.EXTENSION_BOOTCLASSPATH.equals(extension.getValue())))
            {
                throw new BundleException(
                    "Extension bundle must have either 'extension:=framework' or 'extension:=bootclasspath'");
            }
            checkExtensionBundle();
            m_isExtension = true;
        }
    }

    private static ICapability[] checkAndNormalizeR4Exports(
        ICapability[] caps, String bsn, Version bv)
        throws BundleException
    {
        // Verify that the exports do not specify bundle symbolic name
        // or bundle version.
        for (int i = 0; (caps != null) && (i < caps.length); i++)
        {
            if (caps[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                R4Attribute[] attrs = ((Capability) caps[i]).getAttributes();
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
                    Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, bsn, false);
                newAttrs[attrs.length + 1] = new R4Attribute(
                    Constants.BUNDLE_VERSION_ATTRIBUTE, bv, false);
                caps[i] = new Capability(
                    caps[i].getModule(),
                    ICapability.PACKAGE_NAMESPACE,
                    ((Capability) caps[i]).getDirectives(),
                    newAttrs);
            }
        }

        return caps;
    }

    private void checkExtensionBundle() throws BundleException
    {
        if (m_headerMap.containsKey(Constants.IMPORT_PACKAGE) ||
            m_headerMap.containsKey(Constants.REQUIRE_BUNDLE) ||
            m_headerMap.containsKey(Constants.BUNDLE_NATIVECODE) ||
            m_headerMap.containsKey(Constants.DYNAMICIMPORT_PACKAGE) ||
            m_headerMap.containsKey(Constants.BUNDLE_ACTIVATOR))
        {
            throw new BundleException("Invalid extension bundle manifest");
        }
    }

    private static ICapability parseBundleSymbolicName(IModule owner, Map headerMap)
        throws BundleException
    {
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

            // Get bundle version.
            Version bundleVersion = Version.emptyVersion;
            if (headerMap.get(Constants.BUNDLE_VERSION) != null)
            {
                try
                {
                    bundleVersion = Version.parseVersion(
                        (String) headerMap.get(Constants.BUNDLE_VERSION));
                }
                catch (RuntimeException ex)
                {
                    // R4 bundle versions must parse, R3 bundle version may not.
                    String mv = getManifestVersion(headerMap);
                    if (mv != null)
                    {
                        throw ex;
                    }
                    bundleVersion = Version.emptyVersion;
                }
            }

            // Create a module capability and return it.
            String symName = (String) clauses[0][CLAUSE_PATHS_INDEX][0];
            R4Attribute[] attrs = new R4Attribute[2];
            attrs[0] = new R4Attribute(
                Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, symName, false);
            attrs[1] = new R4Attribute(
                Constants.BUNDLE_VERSION_ATTRIBUTE, bundleVersion, false);
            return new Capability(
                owner,
                ICapability.MODULE_NAMESPACE,
                (R4Directive[]) clauses[0][CLAUSE_DIRECTIVES_INDEX],
                attrs);
        }

        return null;
    }

    private static IRequirement parseFragmentHost(Logger logger, Map headerMap)
        throws BundleException
    {
        IRequirement req = null;

        String mv = getManifestVersion(headerMap);
        if ((mv != null) && mv.equals("2"))
        {
            Object[][][] clauses = parseStandardHeader(
                (String) headerMap.get(Constants.FRAGMENT_HOST));
            if (clauses.length > 0)
            {
                // Make sure that only one fragment host symbolic name is specified.
                if (clauses.length > 1)
                {
                    throw new BundleException(
                        "Fragments cannot have multiple hosts: "
                            + headerMap.get(Constants.FRAGMENT_HOST));
                }
                else if (clauses[0][CLAUSE_PATHS_INDEX].length > 1)
                {
                    throw new BundleException(
                        "Fragments cannot have multiple hosts: "
                            + headerMap.get(Constants.FRAGMENT_HOST));
                }

                // If the bundle version matching attribute is specified, then
                // convert it to the proper type.
                for (int attrIdx = 0;
                    attrIdx < clauses[0][CLAUSE_ATTRIBUTES_INDEX].length;
                    attrIdx++)
                {
                    R4Attribute attr = (R4Attribute) clauses[0][CLAUSE_ATTRIBUTES_INDEX][attrIdx];
                    if (attr.getName().equals(Constants.BUNDLE_VERSION_ATTRIBUTE))
                    {
                        clauses[0][CLAUSE_ATTRIBUTES_INDEX][attrIdx] =
                            new R4Attribute(
                                Constants.BUNDLE_VERSION_ATTRIBUTE,
                                VersionRange.parse(attr.getValue().toString()),
                                attr.isMandatory());
                    }
                }

                // Prepend the host symbolic name to the array of attributes.
                R4Attribute[] attrs = (R4Attribute[]) clauses[0][CLAUSE_ATTRIBUTES_INDEX];
                R4Attribute[] newAttrs = new R4Attribute[attrs.length + 1];
                newAttrs[0] = new R4Attribute(
                    Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE,
                    clauses[0][CLAUSE_PATHS_INDEX][0], false);
                System.arraycopy(attrs, 0, newAttrs, 1, attrs.length);

                req = new Requirement(ICapability.HOST_NAMESPACE,
                    (R4Directive[]) clauses[0][CLAUSE_DIRECTIVES_INDEX],
                    newAttrs);
            }
        }
        else
        {
            logger.log(Logger.LOG_WARNING, "Only R4 bundles can be fragments.");
        }

        return req;
    }

    public static ICapability[] parseExportHeader(
        IModule owner, String header, String bsn, Version bv)
        throws BundleException
    {
        ICapability[] caps = parseExportHeader(owner, header);
        try
        {
            caps = checkAndNormalizeR4Exports(caps, bsn, bv);
        }
        catch (BundleException ex)
        {
            caps = null;
        }
        return caps;
    }

    private static ICapability[] parseExportHeader(IModule owner, String header)
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

            // Always add the default version if not specified.
            if ((v == null) && (sv == null))
            {
                v = new R4Attribute(
                    Constants.VERSION_ATTRIBUTE, Version.emptyVersion, false);
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
                // Make sure a package name was specified.
                if (((String) clauses[clauseIdx][CLAUSE_PATHS_INDEX][pathIdx]).length() == 0)
                {
                    throw new IllegalArgumentException(
                        "An empty package name was specified: " + header);
                }
                // Prepend the package name to the array of attributes.
                R4Attribute[] attrs = (R4Attribute[]) clauses[clauseIdx][CLAUSE_ATTRIBUTES_INDEX];
                R4Attribute[] newAttrs = new R4Attribute[attrs.length + 1];
                newAttrs[0] = new R4Attribute(
                    ICapability.PACKAGE_PROPERTY,
                    clauses[clauseIdx][CLAUSE_PATHS_INDEX][pathIdx], false);
                System.arraycopy(attrs, 0, newAttrs, 1, attrs.length);

                // Create package capability and add to capability list.
                capList.add(
                    new Capability(
                        owner,
                        ICapability.PACKAGE_NAMESPACE,
                        (R4Directive[]) clauses[clauseIdx][CLAUSE_DIRECTIVES_INDEX],
                        newAttrs));
            }
        }

        return (ICapability[]) capList.toArray(new ICapability[capList.size()]);
    }

    private static IRequirement[] parseImportHeader(String header)
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
                // Make sure a package name was specified.
                if (((String) clauses[clauseIdx][CLAUSE_PATHS_INDEX][pathIdx]).length() == 0)
                {
                    throw new IllegalArgumentException(
                        "An empty package name was specified: " + header);
                }
                // Prepend the package name to the array of attributes.
                R4Attribute[] attrs = (R4Attribute[]) clauses[clauseIdx][CLAUSE_ATTRIBUTES_INDEX];
                R4Attribute[] newAttrs = new R4Attribute[attrs.length + 1];
                newAttrs[0] = new R4Attribute(
                    ICapability.PACKAGE_PROPERTY,
                    clauses[clauseIdx][CLAUSE_PATHS_INDEX][pathIdx], false);
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

    private static IRequirement[] parseRequireBundleHeader(String header)
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
                    clauses[clauseIdx][CLAUSE_PATHS_INDEX][pathIdx], false);
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

    public static R4Directive parseExtensionBundleHeader(String header)
        throws BundleException
    {
        Object[][][] clauses = parseStandardHeader(header);

        R4Directive result = null;

        if (clauses.length == 1)
        {
            // See if there is the "extension" directive.
            for (int i = 0;
                (result == null) && (i < clauses[0][CLAUSE_DIRECTIVES_INDEX].length);
                i++)
            {
                if (Constants.EXTENSION_DIRECTIVE.equals(((R4Directive)
                    clauses[0][CLAUSE_DIRECTIVES_INDEX][i]).getName()))
                {
                    // If the extension directive is specified, make sure
                    // the target is the system bundle.
                    if (FelixConstants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(clauses[0][CLAUSE_PATHS_INDEX][0]) ||
                        Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(clauses[0][CLAUSE_PATHS_INDEX][0]))
                    {
                        result = (R4Directive) clauses[0][CLAUSE_DIRECTIVES_INDEX][i];
                    }
                    else
                    {
                        throw new BundleException(
                            "Only the system bundle can have extension bundles.");
                    }
                }
            }
        }

        return result;
    }

    private void parseActivationPolicy(Map headerMap)
    {
        m_activationPolicy = IModule.EAGER_ACTIVATION;

        Object[][][] clauses = parseStandardHeader(
            (String) headerMap.get(Constants.BUNDLE_ACTIVATIONPOLICY));

        if (clauses.length > 0)
        {
            // Just look for a "path" matching the lazy policy, ignore
            // everything else.
            for (int i = 0;
                i < clauses[0][CLAUSE_PATHS_INDEX].length;
                i++)
            {
                if (clauses[0][CLAUSE_PATHS_INDEX][i].equals(Constants.ACTIVATION_LAZY))
                {
                    m_activationPolicy = IModule.LAZY_ACTIVATION;
                    for (int j = 0; j < clauses[0][CLAUSE_DIRECTIVES_INDEX].length; j++)
                    {
                        R4Directive dir = (R4Directive) clauses[0][CLAUSE_DIRECTIVES_INDEX][j];
                        if (dir.getName().equalsIgnoreCase(Constants.INCLUDE_DIRECTIVE))
                        {
                            m_activationIncludeDir = dir.getValue();
                        }
                        else if (dir.getName().equalsIgnoreCase(Constants.EXCLUDE_DIRECTIVE))
                        {
                            m_activationExcludeDir = dir.getValue();
                        }
                    }
                    break;
                }
            }
        }
    }

    public static final int CLAUSE_PATHS_INDEX = 0;
    public static final int CLAUSE_DIRECTIVES_INDEX = 1;
    public static final int CLAUSE_ATTRIBUTES_INDEX = 2;

    // Like this: path; path; dir1:=dirval1; dir2:=dirval2; attr1=attrval1; attr2=attrval2,
    //            path; path; dir1:=dirval1; dir2:=dirval2; attr1=attrval1; attr2=attrval2
    private static Object[][][] parseStandardHeader(String header)
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
    private static Object[][] parseStandardHeaderClause(String clauseString)
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
    private static R4LibraryClause[] parseLibraryStrings(Logger logger, String[] libStrs)
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
