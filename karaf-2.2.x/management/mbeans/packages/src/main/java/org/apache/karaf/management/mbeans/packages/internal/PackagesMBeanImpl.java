/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.management.mbeans.packages.internal;

import org.apache.karaf.management.mbeans.packages.PackagesMBean;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import java.util.ArrayList;
import java.util.List;

/**
 * Packages MBean implementation.
 */
public class PackagesMBeanImpl extends StandardMBean implements PackagesMBean {

    private BundleContext bundleContext;

    public PackagesMBeanImpl() throws NotCompliantMBeanException {
        super(PackagesMBean.class);
    }

    public List<String> exportedPackages() throws Exception {
        return exportedPackages(-1);
    }

    public List<String> exportedPackages(long bundleId) throws Exception {
        List<String> exportPackages = new ArrayList<String>();
        ServiceReference ref = bundleContext.getServiceReference(PackageAdmin.class.getName());
        if (ref == null) {
            throw new IllegalStateException("PackageAdmin is not available");
        }
        PackageAdmin packageAdmin = (PackageAdmin) bundleContext.getService(ref);
        if (packageAdmin == null) {
            throw new IllegalStateException("PackageAdmin is not available");
        }

        Bundle[] bundles;
        if (bundleId >= 0) {
            bundles = new Bundle[]{ bundleContext.getBundle(bundleId) };
        } else {
            bundles = bundleContext.getBundles();
        }

        for (Bundle bundle : bundles) {
            ExportedPackage[] packages = packageAdmin.getExportedPackages(bundle);
            if (packages != null) {
                for (ExportedPackage exportedPackage : packages) {
                    exportPackages.add(exportedPackage.getName());
                }
            }
        }

        return exportPackages;
    }

    public List<String> importedPackages() throws Exception {
        return importedPackages(-1);
    }

    public List<String> importedPackages(long bundleId) throws Exception {
        List<String> importPackages = new ArrayList<String>();
        ServiceReference ref = bundleContext.getServiceReference(PackageAdmin.class.getName());
        if (ref == null) {
            throw new IllegalStateException("PackageAdmin is not available");
        }
        PackageAdmin packageAdmin = (PackageAdmin) bundleContext.getService(ref);
        if (packageAdmin == null) {
            throw new IllegalStateException("PackageAdmin is not available");
        }

        ExportedPackage[] exportedPackages;
        if (bundleId >= 0) {
            exportedPackages = packageAdmin.getExportedPackages(bundleContext.getBundle(bundleId));
        } else {
            exportedPackages = packageAdmin.getExportedPackages((Bundle) null);
        }
        if (exportedPackages != null) {
            for (ExportedPackage exportedPackage : exportedPackages) {
                Bundle[] bundles = exportedPackage.getImportingBundles();
                if (bundles != null && bundles.length > 0) {
                    importPackages.add(exportedPackage.getName());
                }
            }
        }

        return importPackages;
    }

    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
