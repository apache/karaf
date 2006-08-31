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

package org.apache.felix.jmood.core;

import org.apache.felix.jmood.core.instrumentation.PackageInfo;
import org.apache.felix.jmood.utils.InstrumentationSupport;
import org.osgi.service.packageadmin.ExportedPackage;

public class ManagedPackage implements ManagedPackageMBean {
    private ExportedPackage pkg;
    public ManagedPackage(ExportedPackage pkg) {
        super();
        this.pkg=pkg;
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedPackageMBean#getExportingBundle()
     */
    public String getExportingBundle() {
        return InstrumentationSupport.getSymbolicName(pkg.getExportingBundle());
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedPackageMBean#getImportingBundles()
     */
    public String[] getImportingBundles() {
        return InstrumentationSupport.getSymbolicNames(pkg.getImportingBundles());
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedPackageMBean#getName()
     */
    public String getName() {
        return pkg.getName();
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedPackageMBean#getVersion()
     */
    public String getVersion() {
        return pkg.getVersion().toString();
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedPackageMBean#isRemovalPending()
     */
    public boolean isRemovalPending() {
        return pkg.isRemovalPending();
    }
}
