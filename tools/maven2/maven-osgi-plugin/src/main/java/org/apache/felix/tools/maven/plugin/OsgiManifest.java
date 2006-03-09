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
    private String bundleActivator;
    private String bundleName;
    private String bundleSource;
    private String bundleDescription;
    private String bundleVendor;
    private String bundleDate;
    private String bundleUpdateLocation;
    private String bundleSymbolicName;
    private String bundleDocUrl;
    private String bundleCategory;
    private String importService;
    private String exportService;
    private String exportPackage;
    private String importPackage;
    private String dynamicImportPackage;
    private String metadataLocation;

    // private String bundleClasspath;

    private Properties entries = new Properties();

    public Properties getEntries()
    {
        if ( getBundleActivator() != null )
        {
            entries.put( "Bundle-Activator", getBundleActivator() );
        }

        if ( getBundleName() != null )
        {
            entries.put( "Bundle-Name", getBundleName() );
        }

        if ( getBundleDescription() != null )
        {
            entries.put( "Bundle-Description", getBundleDescription() );
        }

        if ( getBundleVendor() != null )
        {
            entries.put( "Bundle-Vendor", getBundleVendor() );
        }

        if ( getExportService() != null )
        {
            entries.put( "Export-Service", getExportService() );
        }

        if ( getImportService() != null )
        {
            entries.put( "Import-Service", getImportService() );
        }

        if ( getExportPackage() != null )
        {
            entries.put( "Export-Package", getExportPackage() );
        }

        if ( getImportPackage() != null )
        {
            entries.put( "Import-Package", getImportPackage() );
        }

        if ( getDynamicImportPackage() != null )
        {
            entries.put( "DynamicImport-Package", getDynamicImportPackage() );
        }

        /*
         if (getBundleClassPath() != null) {
         entries.put("Bundle-Classpath", getBundleClassPath());
         }
         */

        if ( getBundleDate() != null )
        {
            entries.put( "Bundle-Date", getBundleDate() );
        }

        if ( getBundleUpdateLocation() != null )
        {
            entries.put( "Bundle-UpdateLocation", getBundleUpdateLocation() );
        }

        if ( getBundleSymbolicName() != null )
        {
            entries.put( "Bundle-SymbolicName", getBundleSymbolicName() );
        }

        if ( getMetadataLocation() != null )
        {
            entries.put( "Metadata-Location", getMetadataLocation() );
        }

        if ( getBundleCategory() != null )
        {
            entries.put( "Bundle-Category", getBundleCategory() );
        }

        if ( getBundleDocUrl() != null )
        {
            entries.put( "Bundle-DocUrl", getBundleDocUrl() );
        }

        if ( getBundleDocUrl() != null )
        {
            entries.put( "Bundle-Source", getBundleSource() );
        }

        return entries;
    }

    public String getBundleDocUrl()
    {
        return bundleDocUrl;
    }

    public void setBundleDocUrl( String bundleDocUrl )
    {
        this.bundleDocUrl = bundleDocUrl;
    }

    public String getBundleCategory()
    {
        return bundleCategory;
    }

    public void setBundleCategory( String bundleCategory )
    {
        this.bundleCategory = bundleCategory;
    }

    public String getBundleActivator()
    {
        return bundleActivator;
    }

    public void setBundleActivator( String bundleActivator )
    {
        this.bundleActivator = bundleActivator;
    }

    public String getBundleName()
    {
        return bundleName;
    }

    public void setBundleName( String bundleName )
    {
        this.bundleName = bundleName;
    }

    public String getBundleDescription()
    {
        return bundleDescription;
    }

    public void setBundleDescription( String bundleDescription )
    {
        this.bundleDescription = bundleDescription;
    }

    public String getBundleVendor()
    {
        return bundleVendor;
    }

    public void setBundleVendor( String bundleVendor )
    {
        this.bundleVendor = bundleVendor;
    }

    public String getExportService()
    {
        return exportService;
    }

    public void setExportService( String exportService )
    {
        this.exportService = exportService;
    }

    public String getExportPackage()
    {
        return exportPackage;
    }

    public void setExportPackage( String exportPackage )
    {
        this.exportPackage = exportPackage;
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

    public void setDynamicImportPackage( String importPackage )
    {
        dynamicImportPackage = importPackage;
    }

    /*
     public String getBundleClasspath() {
     return bundleClasspath;
     }

     public void setBundleClasspath(String bundleClasspath) {
     this.bundleClasspath = bundleClasspath;
     }
     */

    public String getBundleDate()
    {
        return bundleDate;
    }

    public void setBundleDate( String bundleDate )
    {
        this.bundleDate = bundleDate;
    }

    public String getBundleUpdateLocation()
    {
        return bundleUpdateLocation;
    }

    public void setBundleUpdateLocation( String bundleUpdateLocation )
    {
        this.bundleUpdateLocation = bundleUpdateLocation;
    }

    public String getBundleSymbolicName()
    {
        return bundleSymbolicName;
    }

    public void setBundleSymbolicName( String bundleSymbolicName )
    {
        this.bundleSymbolicName = bundleSymbolicName;
    }

    public String getMetadataLocation()
    {
        return metadataLocation;
    }

    public void setMetadataLocation( String metadataLocation )
    {
        this.metadataLocation = metadataLocation;
    }

    public void setBundleSource( String bundleSource )
    {
        this.bundleSource = bundleSource;
    }

    public String getBundleSource()
    {
        return bundleSource;
    }

    public void setImportService( String importService )
    {
        this.importService = importService;
    }

    public String getImportService()
    {
        return importService;
    }
}
