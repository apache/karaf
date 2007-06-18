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
package org.apache.felix.ipojo.plugin;

import java.util.Properties;

/**
 * Hold values for an OSGi jar "bundle" manifest.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Apache Felix Project</a>
 * @version $Rev$, $Date$
 */
public class OsgiManifest
{
    /**
     * Bundle manifest header constants from the OSGi R4 framework constants.
     */
    private static final String BUNDLE_CATEGORY = "Bundle-Category";
    // private static final String BUNDLE_CLASSPATH = "Bundle-ClassPath";
    private static final String BUNDLE_COPYRIGHT = "Bundle-Copyright";
    private static final String BUNDLE_DESCRIPTION = "Bundle-Description";
    private static final String BUNDLE_NAME = "Bundle-Name";
    private static final String BUNDLE_NATIVECODE = "Bundle-NativeCode";
    private static final String EXPORT_PACKAGE = "Export-Package";
    private static final String EXPORT_SERVICE = "Export-Service";
    private static final String IMPORT_PACKAGE = "Import-Package";
    private static final String DYNAMICIMPORT_PACKAGE = "DynamicImport-Package";
    private static final String IMPORT_SERVICE = "Import-Service";
    private static final String BUNDLE_VENDOR = "Bundle-Vendor";
    private static final String BUNDLE_VERSION = "Bundle-Version";
    private static final String BUNDLE_DOCURL = "Bundle-DocURL";
    private static final String BUNDLE_CONTACTADDRESS = "Bundle-ContactAddress";
    private static final String BUNDLE_ACTIVATOR = "Bundle-Activator";
    private static final String BUNDLE_UPDATELOCATION = "Bundle-UpdateLocation";
    private static final String BUNDLE_REQUIREDEXECUTIONENVIRONMENT = "Bundle-RequiredExecutionEnvironment";
    private static final String BUNDLE_SYMBOLICNAME = "Bundle-SymbolicName";
    private static final String BUNDLE_LOCALIZATION = "Bundle-Localization";
    private static final String REQUIRE_BUNDLE = "Require-Bundle";
    private static final String FRAGMENT_HOST = "Fragment-Host";
    private static final String BUNDLE_MANIFESTVERSION = "Bundle-ManifestVersion";
    private static final String BUNDLE_URL = "Bundle-URL";
    private static final String BUNDLE_SOURCE = "Bundle-Source";
    private static final String BUNDLE_DATE = "Bundle-Date";
    private static final String METADATA_LOCATION = "Metadata-Location";
    private static final String SERVICE_COMPONENT = "Service-Component";
    
    // iPOJO Manifest Headers
    private static final String IPOJO_METADATA ="iPOJO-Metadata";
    private static final String IPOJO_COMPONENTS ="iPOJO-Components";

    /**
     * Instance variables corresponding to the R4 framework manifest headers
     */
    private String bundleCategory;
    // private String bundleClassPath;
    private String bundleCopyright;
    private String bundleDescription;
    private String bundleName;
    private String bundleNativeCode;
    private String exportPackage;
    private String exportService;
    private String importPackage;
    private String dynamicImportPackage;
    private String importService;
    private String bundleVendor;
    private String bundleVersion;
    private String bundleDocUrl;
    private String bundleContactAddress;
    private String bundleActivator;
    private String bundleUpdateLocation;
    private String bundleRequiredExecutionEnvironment;
    private String bundleSymbolicName;
    private String bundleLocalization;
    private String requireBundle;
    private String fragmentHost;
    private String bundleManifestVersion;

    /**
     * Instance variables supporting non-framework manifest headers
     */
    private String bundleUrl;
    private String bundleSource;
    private String bundleDate;
    private String metadataLocation;
    private String serviceComponent;
    private String ignorePackage;
    
    /**
     * iPOJO Headers
     */
    private String iPOJOMetadata;
    private String iPOJOComponents;

    private Properties entries = new Properties();

    public Properties getEntries()
    {
        /*
         * setEntryValue( BUNDLE_CLASSPATH, getBundleClassPath(), null);
         */
        setEntryValue(BUNDLE_CATEGORY, getBundleCategory(), null);
        setEntryValue(BUNDLE_COPYRIGHT, getBundleCopyright(), null);
        setEntryValue(BUNDLE_DESCRIPTION, getBundleDescription(), null);
        setEntryValue(BUNDLE_NAME, getBundleName(), null);
        setEntryValue(BUNDLE_NATIVECODE, getBundleNativeCode(), null);
        setEntryValue(EXPORT_PACKAGE, getExportPackage(), null);
        setEntryValue(EXPORT_SERVICE, getExportService(), null);
        setEntryValue(IMPORT_PACKAGE, getImportPackage(), null);
        setEntryValue(DYNAMICIMPORT_PACKAGE, getDynamicImportPackage(), null);
        setEntryValue(IMPORT_SERVICE, getImportService(), null);
        setEntryValue(BUNDLE_VENDOR, getBundleVendor(), null);
        setEntryValue(BUNDLE_VERSION, getBundleVersion(), null);
        setEntryValue(BUNDLE_DOCURL, getBundleDocUrl(), null);
        setEntryValue(BUNDLE_CONTACTADDRESS, getBundleContactAddress(), null);
        setEntryValue(BUNDLE_ACTIVATOR, getBundleActivator(), null);
        setEntryValue(BUNDLE_UPDATELOCATION, getBundleUpdateLocation(), null);
        setEntryValue(BUNDLE_REQUIREDEXECUTIONENVIRONMENT,
            getBundleRequiredExecutionEnvironment(), null);
        setEntryValue(BUNDLE_SYMBOLICNAME, getBundleSymbolicName(), null);
        setEntryValue(BUNDLE_LOCALIZATION, getBundleLocalization(), null);
        setEntryValue(REQUIRE_BUNDLE, getRequireBundle(), null);
        setEntryValue(FRAGMENT_HOST, getFragmentHost(), null);
        setEntryValue(BUNDLE_MANIFESTVERSION, getBundleManifestVersion(), null);
        setEntryValue(BUNDLE_URL, getBundleUrl(), null);
        setEntryValue(BUNDLE_SOURCE, getBundleSource(), null);
        setEntryValue(BUNDLE_DATE, getBundleDate(), null);
        setEntryValue(METADATA_LOCATION, getMetadataLocation(), null);
        setEntryValue(SERVICE_COMPONENT, getServiceComponent(), null);
        
        // iPOJO's metadata
        setEntryValue(IPOJO_METADATA, getiPOJOMetadata(), null);
        setEntryValue(IPOJO_COMPONENTS, getiPOJOComponents(), null);
        return entries;
    }

    public String getBundleCategory()
    {
        return bundleCategory;
    }

    public void setBundleCategory(String bundleCategory)
    {
        this.bundleCategory = bundleCategory;
    }

    /*
     * public String getBundleClasspath() { return bundleClasspath; }
     * 
     * public void setBundleClasspath(String bundleClasspath) {
     * this.bundleClasspath = bundleClasspath; }
     */

    public String getBundleCopyright()
    {
        return bundleCopyright;
    }

    public void setBundleCopyright(String bundleCopyright)
    {
        this.bundleCopyright = bundleCopyright;
    }

    public String getBundleDescription()
    {
        return bundleDescription;
    }

    public void setBundleDescription(String bundleDescription)
    {
        this.bundleDescription = bundleDescription;
    }

    public String getBundleName()
    {
        return bundleName;
    }

    public void setBundleName(String bundleName)
    {
        this.bundleName = bundleName;
    }

    public String getBundleNativeCode()
    {
        return bundleNativeCode;
    }

    public void setBundleNativeCode(String bundleNativeCode)
    {
        this.bundleNativeCode = bundleNativeCode;
    }

    public String getExportPackage()
    {
        return exportPackage;
    }

    public void setExportPackage(String exportPackage)
    {
        this.exportPackage = trim(exportPackage);
    }

    public String getExportService()
    {
        return exportService;
    }

    public void setExportService(String exportService)
    {
        this.exportService = trim(exportService);
    }

    public String getImportPackage()
    {
        return importPackage;
    }

    public void setImportPackage(String importPackage)
    {
        this.importPackage = trim(importPackage);
    }

    public String getDynamicImportPackage()
    {
        return dynamicImportPackage;
    }

    public void setDynamicImportPackage(String dynamicImportPackage)
    {
        this.dynamicImportPackage = trim(dynamicImportPackage);
    }

    public String getImportService()
    {
        return importService;
    }

    public void setImportService(String importService)
    {
        this.importService = importService;
    }

    public String getBundleVendor()
    {
        return bundleVendor;
    }

    public void setBundleVendor(String bundleVendor)
    {
        this.bundleVendor = bundleVendor;
    }

    public String getBundleVersion()
    {
        return bundleVersion;
    }

    public void setBundleVersion(String bundleVersion)
    {
        this.bundleVersion = bundleVersion;
    }

    public String getBundleDocUrl()
    {
        return bundleDocUrl;
    }

    public void setBundleDocUrl(String bundleDocUrl)
    {
        this.bundleDocUrl = bundleDocUrl;
    }

    public String getBundleContactAddress()
    {
        return bundleContactAddress;
    }

    public void setBundleContactAddress(String bundleContactAddress)
    {
        this.bundleContactAddress = bundleContactAddress;
    }

    public String getBundleActivator()
    {
        return bundleActivator;
    }

    public void setBundleActivator(String bundleActivator)
    {
        this.bundleActivator = trim(bundleActivator);
    }

    public String getBundleUpdateLocation()
    {
        return bundleUpdateLocation;
    }

    public void setBundleUpdateLocation(String bundleUpdateLocation)
    {
        this.bundleUpdateLocation = bundleUpdateLocation;
    }

    public String getBundleRequiredExecutionEnvironment()
    {
        return bundleRequiredExecutionEnvironment;
    }

    public void setBundleRequiredExecutionEnvironment(
        String bundleRequiredExecutionEnvironment)
    {
        this.bundleRequiredExecutionEnvironment = bundleRequiredExecutionEnvironment;
    }

    public String getBundleSymbolicName()
    {
        return bundleSymbolicName;
    }

    public void setBundleSymbolicName(String bundleSymbolicName)
    {
        this.bundleSymbolicName = trim(bundleSymbolicName);
    }

    public String getBundleLocalization()
    {
        return bundleLocalization;
    }

    public void setBundleLocalization(String bundleLocalization)
    {
        this.bundleLocalization = bundleLocalization;
    }

    public String getRequireBundle()
    {
        return requireBundle;
    }

    public void setRequireBundle(String requireBundle)
    {
        this.requireBundle = trim(requireBundle);
    }

    public String getFragmentHost()
    {
        return fragmentHost;
    }

    public void setFragmentHost(String fragmentHost)
    {
        this.fragmentHost = trim(fragmentHost);
    }

    public String getBundleManifestVersion()
    {
        return bundleManifestVersion;
    }

    public void setBundleManifestVersion(String bundleManifestVersion)
    {
        this.bundleManifestVersion = bundleManifestVersion;
    }

    public String getBundleUrl()
    {
        return bundleUrl;
    }

    public void setBundleUrl(String bundleUrl)
    {
        this.bundleUrl = bundleUrl;
    }

    public String getBundleSource()
    {
        return bundleSource;
    }

    public void setBundleSource(String bundleSource)
    {
        this.bundleSource = bundleSource;
    }

    public String getBundleDate()
    {
        return bundleDate;
    }

    public void setBundleDate(String bundleDate)
    {
        this.bundleDate = bundleDate;
    }

    public String getMetadataLocation()
    {
        return metadataLocation;
    }

    public void setMetadataLocation(String metadataLocation)
    {
        this.metadataLocation = metadataLocation;
    }

    public String getServiceComponent()
    {
        return serviceComponent;
    }

    public void setServiceComponent(String serviceComponent)
    {
        this.serviceComponent = serviceComponent;
    }

    public String getIgnorePackage()
    {
        return ignorePackage;
    }

    public void setIgnorePackage(String ignorePackage)
    {
        this.ignorePackage = ignorePackage;
    }

    /**
     * Removes all whitespace in the entry.
     * 
     * @param entry
     *            The entry to be cleaned up.
     * @return A copy of the entry string without any whitespace.
     */
    private String trim(String entry)
    {
        StringBuffer buf = new StringBuffer(entry.length());
        for (int i = 0; i < entry.length(); i++)
        {
            char ch = entry.charAt(i);
            if (ch > 32)
            {
                buf.append(ch);
            }
        }
        return buf.toString();
    }

    private void setEntryValue(String key, String value, String defaultValue)
    {
        if (value != null)
        {
            entries.put(key, value);
        }
        else if (defaultValue != null)
        {
            entries.put(key, defaultValue);
        }
    }
    
    
    // iPOJO Headers
    public String getiPOJOMetadata() {
    	return iPOJOMetadata;
    }
    
    public void setiPOJOMetadata( String metadata) {
    	this.iPOJOMetadata = metadata;
    }
    
    public String getiPOJOComponents() {
    	return iPOJOComponents;
    }
    
    public void setiPOJOComponents( String metadata) {
    	this.iPOJOComponents = metadata;
    }
}
