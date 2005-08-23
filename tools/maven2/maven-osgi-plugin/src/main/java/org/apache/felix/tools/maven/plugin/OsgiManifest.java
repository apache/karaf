package org.apache.felix.tools.maven.plugin;

import java.util.Map;
import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User: tbennett
 * Date: Aug 12, 2005
 * Time: 4:03:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class OsgiManifest {
    private String bundleActivator;
    private String bundleName;
    private String bundleDescription;
    private String bundleVendor;
    private String bundleVersion;
    //private String bundleClassPath;
    private String bundleDate;
    private String bundleUpdateLocation;
    private String exportPackage;
    private String metadataLocation;
    private Hashtable entries = new Hashtable();

    public Map getEntries() {
        if (getBundleActivator() != null) {
            entries.put("Bundle-Activator", getBundleActivator());
        }
        if (getBundleName() != null) {
            entries.put("Bundle-Name", getBundleName());
        }
        if (getBundleDescription() != null) {
            entries.put("Bundle-Description", getBundleDescription());
        }
        if (getBundleVendor() != null) {
            entries.put("Bundle-Vendor", getBundleVendor());
        }
        if (getBundleVersion() != null) {
            entries.put("Bundle-Version", getBundleVersion());
        }
        if (getExportPackage() != null) {
            entries.put("Export-Package", getExportPackage());
        }
        /*
        if (getBundleClassPath() != null) {
            entries.put("Bundle-ClassPath", getBundleClassPath());
        }
        */
        if (getBundleDate() != null) {
            entries.put("Bundle-Date", getBundleDate());
        }
        if (getBundleUpdateLocation() != null) {
            entries.put("Bundle-UpdateLocation", getBundleUpdateLocation());
        }
        if (getMetadataLocation() != null) {
            entries.put("Metadata-Location", getMetadataLocation());
        }
        return (Map) entries;
    }

    public String getBundleActivator() {
        return bundleActivator;
    }

    public void setBundleActivator(String bundleActivator) {
        this.bundleActivator = bundleActivator;
    }

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }

    public String getBundleDescription() {
        return bundleDescription;
    }

    public void setBundleDescription(String bundleDescription) {
        this.bundleDescription = bundleDescription;
    }

    public String getBundleVendor() {
        return bundleVendor;
    }

    public void setBundleVendor(String bundleVendor) {
        this.bundleVendor = bundleVendor;
    }

    public String getBundleVersion() {
        return bundleVersion;
    }

    public void setBundleVersion(String bundleVersion) {
        this.bundleVersion = bundleVersion;
    }

    public String getExportPackage() {
        return exportPackage;
    }

    public void setExportPackage(String exportPackage) {
        this.exportPackage = exportPackage;
    }
/*
    public String getBundleClassPath() {
        return bundleClassPath;
    }

    public void setBundleClassPath(String bundleClassPath) {
        this.bundleClassPath = bundleClassPath;
    }
*/
    public String getBundleDate() {
        return bundleDate;
    }

    public void setBundleDate(String bundleDate) {
        this.bundleDate = bundleDate;
    }

    public String getBundleUpdateLocation() {
        return bundleUpdateLocation;
    }

    public void setBundleUpdateLocation(String bundleUpdateLocation) {
        this.bundleUpdateLocation = bundleUpdateLocation;
    }

    public String getMetadataLocation() {
        return metadataLocation;
    }

    public void setMetadataLocation(String metadataLocation) {
        this.metadataLocation = metadataLocation;
    }
}
