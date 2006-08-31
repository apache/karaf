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

package org.apache.felix.jmood.core.instrumentation;

import java.io.Serializable;

public class PackageInfo implements Serializable{
    private BundleInfo exportingBundle;
    private BundleInfo [] importingBundles;
    private String name;
    private String version;
    private boolean removalPending;
    
    public PackageInfo() {
        super();
    }

    public BundleInfo getExportingBundle() {
        return exportingBundle;
    }

    protected void setExportingBundle(BundleInfo exportingBundle) {
        this.exportingBundle = exportingBundle;
    }

    public BundleInfo[] getImportingBundles() {
        return importingBundles;
    }

    protected void setImportingBundles(BundleInfo[] importingBundles) {
        this.importingBundles = importingBundles;
    }

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public boolean isRemovalPending() {
        return removalPending;
    }

    protected void setRemovalPending(boolean removalPending) {
        this.removalPending = removalPending;
    }

    public String getVersion() {
        return version;
    }

    protected void setVersion(String version) {
        this.version = version;
    }
}
