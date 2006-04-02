/*
 *   Copyright 2005 The Apache Software Foundation
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

package org.apache.felix.tools.maven.plugin;

import java.util.Properties;

/**
 * Hold values for an OSGi jar "bundle" manifest.
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Apache Felix Project</a>
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

    private static final String BUNDLE_SOURCE = "Bundle-Source";
    private static final String BUNDLE_DATE = "Bundle-Date";
    private static final String METADATA_LOCATION = "Metadata-Location";

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
    private String bundleSource;
    private String bundleDate;
    private String metadataLocation;

    private Properties entries = new Properties();

    public Properties getEntries()
    {
        if ( getBundleCategory() != null )
        {
            entries.put( BUNDLE_CATEGORY, getBundleCategory() );
        }

        /*
         if (getBundleClassPath() != null)
         {
         entries.put(BUNDLE_CLASSPATH, getBundleClassPath());
         }
         */

        if ( getBundleCopyright() != null )
        {
            entries.put( BUNDLE_COPYRIGHT, getBundleCopyright() );
        }

        if ( getBundleDescription() != null )
        {
            entries.put( BUNDLE_DESCRIPTION, getBundleDescription() );
        }

        if ( getBundleName() != null )
        {
            entries.put( BUNDLE_NAME, getBundleName() );
        }

        if ( getBundleNativeCode() != null )
        {
            entries.put( BUNDLE_NATIVECODE, getBundleNativeCode() );
        }

        if ( getExportPackage() != null )
        {
            entries.put( EXPORT_PACKAGE, getExportPackage() );
        }

        if ( getExportService() != null )
        {
            entries.put( EXPORT_SERVICE, getExportService() );
        }

        if ( getImportPackage() != null )
        {
            entries.put( IMPORT_PACKAGE, getImportPackage() );
        }

        if ( getDynamicImportPackage() != null )
        {
            entries.put( DYNAMICIMPORT_PACKAGE, getDynamicImportPackage() );
        }

        if ( getImportService() != null )
        {
            entries.put( IMPORT_SERVICE, getImportService() );
        }

        if ( getBundleVendor() != null )
        {
            entries.put( BUNDLE_VENDOR, getBundleVendor() );
        }

        if ( getBundleVersion() != null )
        {
            entries.put( BUNDLE_VERSION, getBundleVersion() );
        }

        if ( getBundleDocUrl() != null )
        {
            entries.put( BUNDLE_DOCURL, getBundleDocUrl() );
        }

        if ( getBundleContactAddress() != null )
        {
            entries.put( BUNDLE_CONTACTADDRESS, getBundleContactAddress() );
        }

        if ( getBundleActivator() != null )
        {
            entries.put( BUNDLE_ACTIVATOR, getBundleActivator() );
        }

        if ( getBundleUpdateLocation() != null )
        {
            entries.put( BUNDLE_UPDATELOCATION, getBundleUpdateLocation() );
        }

        if ( getBundleRequiredExecutionEnvironment() != null )
        {
            entries.put( BUNDLE_REQUIREDEXECUTIONENVIRONMENT, getBundleRequiredExecutionEnvironment() );
        }

        if ( getBundleSymbolicName() != null )
        {
            entries.put( BUNDLE_SYMBOLICNAME, getBundleSymbolicName() );
        }

        if ( getBundleLocalization() != null )
        {
            entries.put( BUNDLE_LOCALIZATION, getBundleLocalization() );
        }

        if ( getRequireBundle() != null )
        {
            entries.put( REQUIRE_BUNDLE, getRequireBundle() );
        }

        if ( getFragmentHost() != null )
        {
            entries.put( FRAGMENT_HOST, getFragmentHost() );
        }

        if ( getBundleManifestVersion() != null )
        {
            entries.put( BUNDLE_MANIFESTVERSION, getBundleManifestVersion() );
        }

        if ( getBundleSource() != null )
        {
            entries.put( BUNDLE_SOURCE, getBundleSource() );
        }

        if ( getBundleDate() != null )
        {
            entries.put( BUNDLE_DATE, getBundleDate() );
        }

        if ( getMetadataLocation() != null )
        {
            entries.put( METADATA_LOCATION, getMetadataLocation() );
        }

        return entries;
    }

    public String getBundleCategory()
    {
        return bundleCategory;
    }

    public void setBundleCategory( String bundleCategory )
    {
        this.bundleCategory = bundleCategory;
    }

    /*
     public String getBundleClasspath()
     {
     return bundleClasspath;
     }

     public void setBundleClasspath(String bundleClasspath)
     {
     this.bundleClasspath = bundleClasspath;
     }
     */

    public String getBundleCopyright()
    {
        return bundleCopyright;
    }

    public void setBundleCopyright( String bundleCopyright )
    {
        this.bundleCopyright = bundleCopyright;
    }

    public String getBundleDescription()
    {
        return bundleDescription;
    }

    public void setBundleDescription( String bundleDescription )
    {
        this.bundleDescription = bundleDescription;
    }

    public String getBundleName()
    {
        return bundleName;
    }

    public void setBundleName( String bundleName )
    {
        this.bundleName = bundleName;
    }

    public String getBundleNativeCode()
    {
        return bundleNativeCode;
    }

    public void setBundleNativeCode( String bundleNativeCode )
    {
        this.bundleNativeCode = bundleNativeCode;
    }

    public String getExportPackage()
    {
        return exportPackage;
    }

    public void setExportPackage( String exportPackage )
    {
        this.exportPackage = exportPackage;
    }

    public String getExportService()
    {
        return exportService;
    }

    public void setExportService( String exportService )
    {
        this.exportService = exportService;
    }

    public String getImportPackage()
    {
        return importPackage;
    }

    public void setImportPackage( String importPackage )
    {
        this.importPackage = importPackage;
    }

    public String getDynamicImportPackage()
    {
        return dynamicImportPackage;
    }

    public void setDynamicImportPackage( String dynamicImportPackage )
    {
        this.dynamicImportPackage = dynamicImportPackage;
    }

    public String getImportService()
    {
        return importService;
    }

    public void setImportService( String importService )
    {
        this.importService = importService;
    }

    public String getBundleVendor()
    {
        return bundleVendor;
    }

    public void setBundleVendor( String bundleVendor )
    {
        this.bundleVendor = bundleVendor;
    }

    public String getBundleVersion()
    {
        return bundleVersion;
    }

    public void setBundleVersion( String bundleVersion )
    {
        this.bundleVersion = bundleVersion;
    }

    public String getBundleDocUrl()
    {
        return bundleDocUrl;
    }

    public void setBundleDocUrl( String bundleDocUrl )
    {
        this.bundleDocUrl = bundleDocUrl;
    }

    public String getBundleContactAddress()
    {
        return bundleContactAddress;
    }

    public void setBundleContactAddress( String bundleContactAddress )
    {
        this.bundleContactAddress = bundleContactAddress;
    }

    public String getBundleActivator()
    {
        return bundleActivator;
    }

    public void setBundleActivator( String bundleActivator )
    {
        this.bundleActivator = bundleActivator;
    }

    public String getBundleUpdateLocation()
    {
        return bundleUpdateLocation;
    }

    public void setBundleUpdateLocation( String bundleUpdateLocation )
    {
        this.bundleUpdateLocation = bundleUpdateLocation;
    }

    public String getBundleRequiredExecutionEnvironment()
    {
        return bundleRequiredExecutionEnvironment;
    }

    public void setBundleRequiredExecutionEnvironment( String bundleRequiredExecutionEnvironment )
    {
        this.bundleRequiredExecutionEnvironment = bundleRequiredExecutionEnvironment;
    }

    public String getBundleSymbolicName()
    {
        return bundleSymbolicName;
    }

    public void setBundleSymbolicName( String bundleSymbolicName )
    {
        this.bundleSymbolicName = bundleSymbolicName;
    }

    public String getBundleLocalization()
    {
        return bundleLocalization;
    }

    public void setBundleLocalization( String bundleLocalization )
    {
        this.bundleLocalization = bundleLocalization;
    }

    public String getRequireBundle()
    {
        return requireBundle;
    }

    public void setRequireBundle( String requireBundle )
    {
        this.requireBundle = requireBundle;
    }

    public String getFragmentHost()
    {
        return fragmentHost;
    }

    public void setFragmentHost( String fragmentHost )
    {
        this.fragmentHost = fragmentHost;
    }

    public String getBundleManifestVersion()
    {
        return bundleManifestVersion;
    }

    public void setBundleManifestVersion( String bundleManifestVersion )
    {
        this.bundleManifestVersion = bundleManifestVersion;
    }

    public String getBundleSource()
    {
        return bundleSource;
    }

    public void setBundleSource( String bundleSource )
    {
        this.bundleSource = bundleSource;
    }

    public String getBundleDate()
    {
        return bundleDate;
    }

    public void setBundleDate( String bundleDate )
    {
        this.bundleDate = bundleDate;
    }

    public String getMetadataLocation()
    {
        return metadataLocation;
    }

    public void setMetadataLocation( String metadataLocation )
    {
        this.metadataLocation = metadataLocation;
    }
}
